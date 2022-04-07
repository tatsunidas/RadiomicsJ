package RadiomicsJ;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
//import ij.plugin.Thresholder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;

/**
 * you can try 3d procedure.
 * see, GLDZM. 
 * 1. get main blob and minimum blob
 * 2. calculate how many minimum blob in main blob.
 * 3. create matrix.
 * 
 * or
 * 
 * calculate slice by slice, then aggregate it.
 * 
 * @author tatsunidas
 *
 */
public class BettiNumberMap {
	
	//test data
	static byte[] binary = new byte[] 
			{
				1,1,0,1,1,1,1,
				1,1,0,0,1,1,1,
				1,1,1,0,0,0,0,
				1,0,1,1,1,1,1,
				1,1,1,1,1,0,1,
				1,0,0,1,1,1,1,
				1,1,1,1,1,0,1
			};
	
	

//	//debug
//	public static void main(String[] args) {
//		/*
//		 * will become b0=2, b1=3
//		 */
//		//make binary (低信号領域0�?高信号領域255)//解析対象によって、背景が高信号か低信号化�?�変わる�?�シンプルにヒストグラ�?上で�?類されると割り�??って�?える�?(大�?�?な構�??物は255、�?��?�ルなどの小構�??物は0)
//		ByteProcessor binaryInput = new ByteProcessor(7, 7, binary);
////		System.out.println(new BettiNumberMap().getB0(binaryInput));
////		System.out.println(new BettiNumberMap().getB1(binaryInput));
//		
//		//crop test
////		binaryInput.multiply(255);
//		ImagePlus org = new ImagePlus("C:\\Users\\tatsu\\OneDrive\\�?スクトップ\\betti_test.png");
//		org.getProcessor().setAutoThreshold("Default");//.setBinaryThreshold();
//		ByteProcessor bp = Thresholder.createMask(org);
//		ImagePlus binaryImp = new ImagePlus("",bp);
//		IJ.saveAsTiff(binaryImp, "C:\\Users\\tatsu\\OneDrive\\�?スクトップ\\betti_test_th.tif");
//		new BettiNumberMap(binaryImp).calculate(5);
//	}
	
	private ImagePlus imp;
	private ImagePlus b0map;
	private ImagePlus b1map;
	private ImagePlus b2map;
	
	public BettiNumberMap() {
		this(null);
	}
	
	public BettiNumberMap(ImagePlus slice) {
		if (slice != null) {
			if (slice.getType() != ImagePlus.GRAY8) {
				ImagePlus dup8 = slice.createImagePlus();
				ImageProcessor bp = slice.getProcessor().convertToByte(true);
				dup8.setProcessor(bp);
				dup8.setCalibration(slice.getCalibration().copy());
				this.imp = dup8;
			}else {
				this.imp = slice;
			}
		}
	}
	
	public void calculate(int ksize) {
		if(ksize%2 == 0) {
			IJ.error("kernel size must be odd number", "can not calsulate betti map.");
			return;
		}
		if(this.imp == null) {
			return;
		}
		if(ksize > imp.getWidth() && ksize > imp.getHeight() ) {
			IJ.error("kernel size is too large", "can not calsulate betti map.");
			return;
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int kgap = (ksize-1)/2;
		/*
		 * padding to avoid out of range of roi location.
		 */
		byte pad_pixels[] = new byte[(w+kgap+kgap)*(h+kgap+kgap)];
		int pos = 0;
		for(int j=0;j<h+kgap*2;j++) {
			for(int i=0;i<w+kgap*2;i++) {
				if((i < kgap || i >= w+kgap) || (j < kgap || j >= h+kgap)) {
					pad_pixels[pos++] = (byte)0;
				}else{
					pad_pixels[pos++] = (byte) imp.getProcessor().get(i-kgap, j-kgap);
				}
			}
		}
		ImagePlus padImp = new ImagePlus("padded", new ByteProcessor(w+kgap+kgap, h+kgap+kgap, pad_pixels));
//		toString(padImp.getProcessor());
		int[] b0pix = new int[w*h];
		int[] b1pix = new int[w*h];
		float[] b2pix = new float[w*h];
		pos = 0;
		for(int j=kgap;j<h+kgap;j++) {
			for(int i=kgap;i<w+kgap;i++) {
				Roi rect = new Roi(i-kgap,j-kgap,ksize,ksize);//ROIを画像外に設定できな�?
				if(padImp.getRoi() != null) {
					padImp.deleteRoi();
				}
				padImp.setRoi(rect);
				padImp.updateImage();
				ImagePlus crop = padImp.crop("slice");
//				debug
//				crop = crop.resize(ksize*10, ksize*10, "none");
//				crop.setTitle((i+1)+","+(j+1));
//				crop.show();
				int b0 = getB0((ByteProcessor)crop.getProcessor());
				int b1 = getB1((ByteProcessor)crop.getProcessor());
				float b2 = (float)getB2(b0,b1);
				b0pix[pos] = b0;
				b1pix[pos] = b1;
				b2pix[pos] = b2;
				pos++;
			}
		}
		b0map = new ImagePlus("b0 map", new FloatProcessor(w,h,b0pix));
		b1map = new ImagePlus("b1 map", new FloatProcessor(w,h,b1pix));
		b2map = new ImagePlus("b2 map", new FloatProcessor(w,h,b2pix));
//		b0map = b0map.resize(70, 70, "none");
//		b1map = b1map.resize(70, 70, "none");
//		b2map = b2map.resize(70, 70, "none");
//		b0map.show();
//		b1map.show();
//		b2map.show();
		IJ.saveAsTiff(b0map, "C:\\Users\\tatsu\\OneDrive\\�?スクトップ\\betti_test_b0.tif");
		IJ.saveAsTiff(b1map, "C:\\Users\\tatsu\\OneDrive\\�?スクトップ\\betti_test_b1.tif");
		IJ.saveAsTiff(b2map, "C:\\Users\\tatsu\\OneDrive\\�?スクトップ\\betti_test_b2.tif");
	}
	
	/**
	 * foreground(at value of 255) connected component count
	 * @param binarySrc : calculate binary region array cropped from n*n kernel
	 * @return : b0 value
	 */
	public int getB0(ByteProcessor binarySrc){
		ByteProcessor binary = (ByteProcessor) binarySrc.duplicate();
		if(binary.getStats().max == 1.0) {
			binary.multiply(255);
		}
		binary.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);//255 to foreground
		ResultsTable rt = new ResultsTable();
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, Measurements.ALL_STATS, rt, 0.0, Double.POSITIVE_INFINITY);
		pa.analyze(new ImagePlus("", binary));
		return rt.size();//row count
	}
	
	/**
	 * "Cycles", closed area(at value of 255) count, without peripheral blob.
	 * @param binarySrc : calculate binary region array cropped from n*n kernel
	 * @return : b1 value
	 */
	public int getB1(ByteProcessor binarySrc) {
		/* pre invert */
		ByteProcessor binary = (ByteProcessor)binarySrc.duplicate();
		if(binary.getStats().max == 1.0) {
			binary.multiply(255);
		}
		/* invert */
		binary.invert();
		ByteProcessor binaryInv = (ByteProcessor) binary.duplicate();//blob 0, bg 255
		ByteProcessor pre_fillholes = (ByteProcessor) binaryInv.duplicate();
		
//		System.out.println("pre");
//		toString(pre_fillholes);
		
		int foreground = 0; //connected
		int background = 255; //blob
		/*
		 * b1 not include peripheral blob, which included perimeter.
		 * Fill holes filled only inner blob, it is ignore peripheral blobs, so,
		 * 1. do fill holes
		 * 2. subtract it
		 * 3. calulate roi count
		 */
		//fill holes
		fill(pre_fillholes, foreground, background);// fill by foreground value where region has background value that surrounded foreground value.
		ByteProcessor post_fillholes = (ByteProcessor) pre_fillholes.duplicate();
		
//		System.out.println("post");
//		toString(post_fillholes);
		
		//subtract, peripheral blob set to 0
		int w = binarySrc.getWidth();
		int h = binarySrc.getHeight();
		byte[] analysisArray = new byte[w*h];
		int num = 0;
		for(int j = 0;j<h;j++) {
			for(int i=0;i<w;i++) {
				analysisArray[num++] = (byte)(binaryInv.get(i, j) - (byte)post_fillholes.get(i, j));
			}
		}
		ByteProcessor analysisBp = new ByteProcessor(w, h, analysisArray);
//		System.out.println("Finally");
//		toString(analysisBp);
		analysisBp.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);//255 to be foreground
		ResultsTable rt = new ResultsTable();
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, Measurements.ALL_STATS, rt, 0.0, Double.POSITIVE_INFINITY);
//		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, ParticleAnalyzer.COMPOSITE_ROIS, rt, 0.0, Double.POSITIVE_INFINITY);
		pa.analyze(new ImagePlus("", analysisBp));
		return rt.size();//row count
	}
	
	public double getB2(int b0, int b1) {
		if(b0 <= 0.0) {
			return 0d;
		}
		return (double)b1/(double)b0;
	}
	
	static void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y=0; y<height; y++) {
            if (ip.getPixel(0,y)==background) ff.fill(0, y);
            if (ip.getPixel(width-1,y)==background) ff.fill(width-1, y);
        }
        for (int x=0; x<width; x++){
            if (ip.getPixel(x,0)==background) ff.fill(x, 0);
            if (ip.getPixel(x,height-1)==background) ff.fill(x, height-1);
        }
        byte[] pixels = (byte[])ip.getPixels();
        int n = width*height;
        for (int i=0; i<n; i++) {
        if (pixels[i]==127)
            pixels[i] = (byte)background;
        else
            pixels[i] = (byte)foreground;
        }
    }
	
	static void toString(ImageProcessor ip) {
		for(int j = 0;j<ip.getHeight();j++) {
			StringBuilder sb = new StringBuilder();
			for(int i=0;i<ip.getWidth();i++) {
				sb.append(ip.get(i, j));
				sb.append("\t");
				sb.append(",");
			}
			System.out.println(sb.toString());
		}
		System.out.println("\n");
	}

}

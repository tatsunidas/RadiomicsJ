package RadiomicsJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.apache.commons.math3.stat.StatUtils;
import org.scijava.vecmath.Point3i;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;

public class GLDZMFeatures {
	
	ImagePlus img;
	ImagePlus discImg;// discretised
	ImagePlus mask;
	ImagePlus dMap;// distance map
	Calibration orgCal;// backup
	
	int w ;
	int h ;
	int s ;
	
	int label = 1;
	int nBins;// 1 to N
	HashMap<Integer, int[]> angles = Utils.buildAngles();
	ArrayList<Integer> angle_ids;//0 to 26
	
	double[][] gldzm_raw;
	double[][] gldzm;// normalised
	
	double Ns = 0d;//sum of zone size count, without normalize
	double mu_i=0.0;
	double mu_j=0.0;
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	
	public GLDZMFeatures(ImagePlus img, ImagePlus mask, int label, boolean useBinCount, Integer nBins, Double binWidth) throws Exception {
		if (img == null) {
			return;
		} else {
			if (img.getType() == ImagePlus.COLOR_RGB) {
				JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
				return;
			}
			
			this.label = label;

			if (mask != null) {
				if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight()) {
					JOptionPane.showMessageDialog(null, "RadiomicsJ: please input same dimension image and mask.");
					return;
				}
			}else {
				// create full face mask
				mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, label, img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
			}
			
			if(nBins == null) {
				this.nBins = RadiomicsJ.nBins;
			}else {
				this.nBins = nBins;
			}
			
			this.img = img;
			this.orgCal = img.getCalibration();
			this.mask = mask;
			
			// discretised by roi mask.
			if(RadiomicsJ.discretisedImp != null) {
				discImg = RadiomicsJ.discretisedImp;
			}else {
				if(useBinCount) {
					discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
				}else {
					/*
					 * do Fixed Bin Width
					 */
					discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, binWidth);
					this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
				}
			}
			
			angle_ids = new ArrayList<>(angles.keySet());//0 to 26
			Collections.sort(angle_ids);
			
			w = discImg.getWidth();
			h= discImg.getHeight();
			s = discImg.getNSlices();
			
			try{
				fillGLDZM();
			}catch(StackOverflowError e) {
				System.out.println("Stack Overflow occured when executing fillGLDZM().");
				System.out.println("RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
//				JOptionPane.showMessageDialog(null, "RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
				return;
			}
		}
	}
	
	
	public void fillGLDZM() throws Exception {
		gldzm_raw = null;//init
		gldzm = null;
		//<grayVal - count zones by distance>
		HashMap<Integer, Integer[]> gldzm_map = new HashMap<Integer,Integer[]>();
		int w = discImg.getWidth();
		int h= discImg.getHeight();
		int s = discImg.getNSlices();
		Integer[][][] voxels = Utils.prepareVoxels(discImg, mask, label, nBins);//[z][y][x], temp voxels at it angle, for count up.
		Integer[][][] distance_map = getDistanceMap(mask);
		//search dist max
		int distance_max = 1;
		for(int z=0;z<s;z++) {
			for (int y = 0; y<h; y++) {
				for (int x = 0; x < w; x++) {
					if(distance_map[z][y][x] == null) {
						continue;
					}
					if (distance_max < distance_map[z][y][x]) {
						distance_max = distance_map[z][y][x];
					}
				}
			}
		}
		//get blobs
		ArrayList<ArrayList<Point3i>> blobs = collectBlobs(voxels);
		
		for(ArrayList<Point3i> blob : blobs) {
			if(blob == null || blob.size() < 1) {
				continue;
			}
			Integer blob_grayLevel = null;
			//search distance min in blob area
			int d = Integer.MAX_VALUE;
			for(Point3i p : blob) {
				int dv = distance_map[p.z][p.y][p.x];
				if (d > dv) {
					d = dv;
					if(blob_grayLevel == null) {
						blob_grayLevel = Integer.valueOf(voxels[p.z][p.y][p.x]);
					}
				}
			}
			if(d < 1) {
				//throw exception ??
				continue;
			}
			if(gldzm_map.get(blob_grayLevel) == null) {
				//init matrix
				//[zoneDistance1_count, zoneDistance2_count...] array for each gray level.
				Integer[] distance_zone_count = new Integer[distance_max];
//				for(Integer c : distance_zone_count) {
//					c = Integer.valueOf(0);//DO NOT USE
//				}
				int itr = 0;
				for(@SuppressWarnings("unused") Integer c : distance_zone_count) {
					distance_zone_count[itr++] = Integer.valueOf(0);
				}
				distance_zone_count[d-1] = Integer.valueOf(1);//d is 1 based.
				gldzm_map.put(blob_grayLevel, distance_zone_count);
			}else {
				gldzm_map.get(blob_grayLevel)[d-1]++;
			}
		}
		gldzm_raw = map2matrix(gldzm_map);
		gldzm = normalize(gldzm_raw);
		calculateCoefficients();
	}
	
	private void calculateCoefficients() {
		mu_i = 0.0;
		mu_j = 0.0;
		// sum the glszm mu
		for (int i=1; i<=gldzm.length; i++){
			for (int j=1; j<=gldzm[0].length; j++) {
				mu_i += gldzm [i-1][j-1] * i;
				mu_j += gldzm [i-1][j-1] * j;
			} 
		}
	}
	
	
	public ArrayList<ArrayList<Point3i>> collectBlobs(Integer[][][] src){
		int s = src.length;
		int h = src[0].length;
		int w = src[0][0].length;
		//copy src to replace values.
		Integer[][][] pixels = new Integer[s][h][w];
		for (int z = 0; z < s; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if(src[z][y][x] == null) {
						pixels[z][y][x] = Integer.MIN_VALUE;
					}else {
						pixels[z][y][x] = Integer.valueOf(src[z][y][x]);
					}
				}
			}
		}
		
		/*
		 * blob list
		 * blob points[[xyz],[xyz],[xyz]...]
		 */
		ArrayList<ArrayList<Point3i>> blobs = new ArrayList<>();
		for (int z = 0; z < s; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (pixels[z][y][x] == Integer.MIN_VALUE) {
						continue;
					}
					int v = pixels[z][y][x];
					ArrayList<Point3i> blob = new ArrayList<>();
//					searchNeighbor(v, pixels, x, y, z, blob);//stack overflow
					blob = searchNeighbor2(v, pixels, x, y, z);
					blobs.add(blob);
				}
			}
		}
		return blobs;
	}
	
	
//	private void searchNeighbor(int grayLevel, Integer[][][] pixels, int seedX, int seedY, int seedZ, ArrayList<Point3i> blob) {
//		if(pixels[seedZ][seedY][seedX] == null) {
//			return;
//		}
//		if(pixels[seedZ][seedY][seedX] == Integer.MIN_VALUE) {
//			return;
//		}
//		if(pixels[seedZ][seedY][seedX] != grayLevel) {
//			return;
//		}else {
//			pixels[seedZ][seedY][seedX] = Integer.MIN_VALUE;
//		}
//		Point3i xyz = new Point3i(seedX, seedY, seedZ);
//		int s = pixels.length;
//		int h = pixels[0].length;
//		int w = pixels[0][0].length;
//		int blobsize = blob.size();
//		boolean exists = false;
//		if(blobsize > 0) {
//			for (int i = 0; i < blobsize; i++) {
//				Point3i pt = blob.get(i);
//				if(pt.x == xyz.x && pt.y == xyz.y && pt.z == xyz.z) {
//					exists = true;
//				}
//			}
//			if(!exists) {
//				blob.add(xyz);
//			}
//		}else {
//			blob.add(xyz);
//		}
//		ArrayList<Point3i> connected = connectedNeighbor(seedX, seedY, seedZ, w, h, s);
//		for(Point3i np:connected) {
//			if(np == null) {
//				//out of pixels coordinate
//				continue;
//			}
//			if(pixels[np.z][np.y][np.x] == Integer.MIN_VALUE) {
//				continue;
//			}
//			if(pixels[np.z][np.y][np.x]==grayLevel) {
//				searchNeighbor(grayLevel,pixels,np.x,np.y, np.z, blob);
//			}
//		}
//	}
	
	private ArrayList<Point3i> searchNeighbor2(int grayLevel, Integer[][][] pixels, int seedX, int seedY, int seedZ) {
		ArrayList<Point3i> blob = null;
		if(pixels[seedZ][seedY][seedX] == null) {
			return blob;
		}
		if(pixels[seedZ][seedY][seedX] == Integer.MIN_VALUE) {
			return blob;
		}
		if(pixels[seedZ][seedY][seedX] != grayLevel) {
			return blob;
		}else {
			pixels[seedZ][seedY][seedX] = Integer.MIN_VALUE;
			Point3i xyz = new Point3i(seedX, seedY, seedZ);
			if(blob == null) {
				blob = new ArrayList<Point3i>();
			}
			blob.add(xyz);
		}
		
		//26 connected
		ArrayList<Point3i> connected = connectedNeighbor2(pixels, grayLevel, seedX, seedY, seedZ, w, h, s);
		int seedX2 = 0;
		int seedY2 = 0;
		int seedZ2 = 0;
		while(connected!=null) {
			if(connected != null) {
				ArrayList<Point3i> connected_itr = new ArrayList<>();
				for (Point3i np : connected) {
					if (np == null) {
						// out of pixels coordinate
						continue;
					}
					if (pixels[np.z][np.y][np.x] == null) {
						continue;
					}
					if (pixels[np.z][np.y][np.x] == Integer.MIN_VALUE) {
						continue;
					}
					if (pixels[np.z][np.y][np.x] == grayLevel) {
						Point3i xyz = new Point3i(np.x, np.y, np.z);
						blob.add(xyz);
						pixels[np.z][np.y][np.x] = Integer.MIN_VALUE;
						seedX2 = np.x;
						seedY2 = np.y;
						seedZ2 = np.z;
						ArrayList<Point3i> connected_ = connectedNeighbor2(pixels, grayLevel, seedX2, seedY2, seedZ2, w, h, s);
						if(connected_ != null) {
							connected_itr.addAll(connected_);
						}
					}
				}
				connected.addAll(connected_itr);
				connected = updateConnectedNeighbours(pixels, grayLevel, connected);
			}
		}
		return blob;
	}
	

//	private ArrayList<Point3i> connectedNeighbor(int seedX, int seedY, int seedZ, int max_w , int max_h, int max_s){
//		ArrayList<Integer> angle_ids = new ArrayList<>(angles.keySet());//0 to 26
//		Collections.sort(angle_ids);
//		ArrayList<Point3i> connect26 = new ArrayList<Point3i>();
//		for(Integer a_id:angle_ids) {
//			if(Integer.valueOf(13) == a_id) {
//				continue;//0,0,0
//			}
//			int[] a = angles.get(a_id);
//			int nX = seedX+a[2];
//			int nY = seedY+a[1];
//			int nZ = seedZ+a[0];
//			Point3i neighbor = new Point3i(nX,nY,nZ);
//			if(!Utils.isOutOfRange(new Point3i(nX,nY,nZ), max_w , max_h, max_s)) connect26.add(neighbor);
//		}
//		return connect26;
//	}
	
	private ArrayList<Point3i> connectedNeighbor2(Integer[][][] voxels, final int grayLevel, final int seedX, final int seedY, final int seedZ, final int max_w , final int max_h, final int max_s){
		ArrayList<Point3i> connected = new ArrayList<Point3i>();
		for(Integer a_id:angle_ids) {
			if(Integer.valueOf(13) == a_id) {
				continue;//0,0,0
			}
			int[] a = angles.get(a_id);
			int nX = seedX+a[2];
			int nY = seedY+a[1];
			int nZ = seedZ+a[0];
			Point3i neighbour = new Point3i(nX,nY,nZ);
			if(!Utils.isOutOfRange(neighbour, max_w, max_h, max_s)) {
				if(voxels[nZ][nY][nX] != null) {
					if(voxels[nZ][nY][nX] == grayLevel) {
						connected.add(neighbour);
					}
				}
			}
		}
		return connected.size() > 0 ? connected:null;
	}
	
	private ArrayList<Point3i> updateConnectedNeighbours(final Integer[][][] voxels, final int grayLevel, ArrayList<Point3i> connected) {
		if(connected == null) {
			return null;
		}
		if(connected.size() ==0 || connected.isEmpty()) {
			connected = null;
			return null;
		}
		ArrayList<Point3i> remove = new ArrayList<Point3i>();
		for(Point3i p:connected) {
			if (voxels[p.z][p.y][p.x] == null || voxels[p.z][p.y][p.x] == Integer.MIN_VALUE) {
				remove.add(p);
			}
		}
		connected.removeAll(remove);
		if(connected.size() ==0 || connected.isEmpty()) {
			connected = null;
			return null;
		}else {
			return connected;
		}
	}
	
	/**
	 * The distance to the ROI edge is defined according to 6 and 4-connectedness for 3D and 2D, respectively. 
	 * @param mask
	 * @return distance map [sliceZ [rowY [colX]]]
	 */
	public Integer[][][] getDistanceMap(ImagePlus mask) {
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		byte[][][] label_ones = convertToLabelFrom(mask);
		Integer[][][] distance_map = new Integer[s][h][w];
		byte[][][] current_eroded_res = Arrays.stream(label_ones)
	             .map((byte[][] slice) -> slice.clone())
	             .toArray((int length) -> new byte[length][][]);
		int distance = 1;
		while(!is_erode_result_all_zero(current_eroded_res)) {
			byte[][][] prev_reoded_res = Arrays.stream(current_eroded_res)
					.map((byte[][] slice) -> slice.clone())
		            .toArray((int length) -> new byte[length][][]);
			if(s == 1) {
				current_eroded_res = erode_by_4connected(current_eroded_res);
			}else {
				current_eroded_res = erode_by_6connected(current_eroded_res);
			}
			//get difference
			for(int z=0;z<s;z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						byte v = prev_reoded_res[z][y][x];
						byte e = current_eroded_res[z][y][x];
						if (v == 1 && e == 0) {
							distance_map[z][y][x] = distance;
						}
					}
				}
			}
			distance++;
		}
		return distance_map;
	}
	
	
	/*
	 * IMPORTANT
	 * z-y-x order ! [ z [y [x1,x2,,,]]]
	 * create label array which has only intensity 1.
	 */
	private byte[][][] convertToLabelFrom(ImagePlus mask) {
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		byte[][][] label_ones = new byte[s][h][w];//z-y-x order !!
		for(int z=0;z<s;z++) {
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int v = (int) mSlice[x][y];
					if(v == this.label) {
						label_ones[z][y][x] = 1;
					}else {
						label_ones[z][y][x] = 0;
					}
				}
			}
		}
		return label_ones;
	}
	
	/*
	 * The distance to the ROI edge is defined according to 6 and 4-connectedness for 3D and 2D, respectively.
	 */
	private byte[][][] erode_by_4connected(byte[][][] label_ones){
		int h = label_ones[0].length;
		int w = label_ones[0][0].length;
		//whether include 4-connected
		/*
		 *    1
		 *  1 1 1
		 *    1
		 */
		byte[][][] eroded = new byte[1][h][w];
		for(int y=1;y<h-1;y++) {
			for(int x=1;x<w-1;x++) {
				int center = label_ones[0][y][x];
				int upper = label_ones[0][y][x-1];
				int lower = label_ones[0][y][x+1];
				int left = label_ones[0][y-1][x];
				int right = label_ones[0][y+1][x];
				int sum_connected = center+upper+lower+left+right;
				if(sum_connected == 5) {
					eroded[0][y][x] = 1;
				}else {
					eroded[0][y][x] = 0;
				}
			}
		}
		return eroded;
	}
	
	/**
	 * 
	 * @param label_ones : z-y-x order
	 * @return
	 */
	private byte[][][] erode_by_6connected(byte[][][] label_ones){
		int s = label_ones.length;
		int h = label_ones[0].length;
		int w = label_ones[0][0].length;
		//whether include 6-connected
		/*
		 *    1 1 
		 *   111 
		 *  1 1 
		 */
		byte[][][] eroded = new byte[s][h][w];//z-y-x
		for(int z=1;z<s-1;z++) {
			for (int y = 1; y < h - 1; y++) {
				for (int x = 1; x < w - 1; x++) {
					int center = label_ones[z][y][x];
					int upper = label_ones[z][y - 1][x];
					int lower = label_ones[z][y + 1][x];
					int left = label_ones[z][y][x - 1];
					int right = label_ones[z][y][x + 1];
					int front = label_ones[z+1][y][x];
					int back = label_ones[z-1][y][x];
					int sum_connected = center + upper + lower + left + right + front + back;
					if (sum_connected == 7) {
						eroded[z][y][x] = 1;
					} else {
						eroded[z][y][x] = 0;
					}
				}
			}
		}
		return eroded;
	}
	
	private boolean is_erode_result_all_zero(byte[][][] eroded){
		int s = eroded.length;
		int h = eroded[0].length;
		int w = eroded[0][0].length;
		for(int z=0;z<s;z++) {
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int v = eroded[z][y][x];
					if(v > 0) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private double[][] normalize(double[][] gldzm_raw){
		// skip all zero matrix
		if (gldzm_raw == null) {
			return null;
		}
		int h = gldzm_raw.length;
		int w = gldzm_raw[0].length;
		double[][] norm_gldzm = new double[h][w];
		// init array
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				norm_gldzm[y][x] = 0d;
			}
		}
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				norm_gldzm[i][j] = gldzm_raw[i][j] / Ns;
			}
		}
		return norm_gldzm;
	}
	
	public Double calculate(String id) {
		/*
		 * feature names were shared from GLSZMFeatureType.
		 */
		String name = GLDZMFeatureType.findType(id);
		if (name.equals(GLDZMFeatureType.SmallDistanceEmphasis.name())) {
			return getSmallDistanceEmphasis();
		} else if (name.equals(GLDZMFeatureType.LargeDistanceEmphasis.name())) {
			return getLargeDistanceEmphasis();
		} else if (name.equals(GLDZMFeatureType.GrayLevelNonUniformity.name())) {
			return getGrayLevelNonUniformity();
		} else if (name.equals(GLDZMFeatureType.GrayLevelNonUniformityNormalized.name())) {
			return getGrayLevelNonUniformityNormalized();
		} else if (name.equals(GLDZMFeatureType.ZoneDistanceNonUniformity.name())) {
			return getZoneDistanceNonUniformity();
		} else if (name.equals(GLDZMFeatureType.ZoneDistanceNonUniformityNormalized.name())) {
			return getZoneDistanceNonUniformityNormalized();
		} else if (name.equals(GLDZMFeatureType.ZonePercentage.name())) {
			return getDistancePercentage();
		} else if (name.equals(GLDZMFeatureType.GrayLevelVariance.name())) {
			return getGrayLevelVariance();
		} else if (name.equals(GLDZMFeatureType.ZoneDistanceVariance.name())) {
			return getDistanceVariance();
		} else if (name.equals(GLDZMFeatureType.ZoneDistanceEntropy.name())) {
			return getDistanceEntropy();
		} else if (name.equals(GLDZMFeatureType.LowGrayLevelZoneEmphasis.name())) {
			return getLowGrayLevelDistanceEmphasis();
		} else if (name.equals(GLDZMFeatureType.HighGrayLevelZoneEmphasis.name())) {
			return getHighGrayLevelDistanceEmphasis();
		} else if (name.equals(GLDZMFeatureType.SmallDistanceLowGrayLevelEmphasis.name())) {
			return getSmallDistanceLowGrayLevelEmphasis();
		} else if (name.equals(GLDZMFeatureType.SmallDistanceHighGrayLevelEmphasis.name())) {
			return getSmallDistanceHighGrayLevelEmphasis();
		} else if (name.equals(GLDZMFeatureType.LargeDistanceLowGrayLevelEmphasis.name())) {
			return getLargeDistanceLowGrayLevelEmphasis();
		} else if (name.equals(GLDZMFeatureType.LargeDistanceHighGrayLevelEmphasis.name())) {
			return getLargeDistanceHighGrayLevelEmphasis();
		}
		return null;
	}
	
	private Double getSmallDistanceEmphasis() {
		Double sde = 0.0d;
		for(int j=1; j<=gldzm[0].length; j++) {
			double s_j = 0d;
			for(int i=1; i<=gldzm.length; i++) {
				s_j += gldzm[i-1][j-1];
			}
			sde += s_j/(j*j);
		}
		return sde;//already normalized
	}
	
	private Double getLargeDistanceEmphasis() {
		Double lde = 0.0d;
		for(int j=1; j<=gldzm[0].length; j++) {
			double s_j = 0d;
			for(int i=1; i<=gldzm.length; i++) {
				s_j += gldzm[i-1][j-1];
			}
			lde += s_j*(j*j);
		}
		return lde;//already normalized
	}
	
	
	private Double getLowGrayLevelDistanceEmphasis() {
		Double lglze = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			double s_i = 0d;
			for(int j=1; j<=gldzm[0].length; j++) {
				s_i += gldzm[i-1][j-1];
			}
			lglze += s_i/(i*i);
		}
		return lglze;//already normalized
	}
	
	private Double getHighGrayLevelDistanceEmphasis() {
		Double lglze = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			double s_i = 0d;
			for(int j=1; j<=gldzm[0].length; j++) {
				s_i += gldzm[i-1][j-1];
			}
			lglze += s_i*(i*i);
		}
		return lglze;//already normalized
	}
	
	
	private Double getSmallDistanceLowGrayLevelEmphasis() {
		Double sdlgle = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				sdlgle += gldzm[i-1][j-1]/(i*i*j*j);
			}
		}
		return sdlgle;//already normalized
	}
	
	private Double getSmallDistanceHighGrayLevelEmphasis() {
		Double sdhgle = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				sdhgle += (gldzm[i-1][j-1]*i*i)/(j*j);
			}
		}
//		return sahgle/Nz;
		return sdhgle;//already normalized
	}
	
	private Double getLargeDistanceLowGrayLevelEmphasis() {
		Double ldlgle = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				ldlgle += (gldzm[i-1][j-1]*j*j)/(i*i);
			}
		}
		return ldlgle;//already normalized
	}
	
	private Double getLargeDistanceHighGrayLevelEmphasis() {
		Double ldhgle = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				ldhgle += gldzm[i-1][j-1]*i*i*j*j;
			}
		}
		return ldhgle;//already normalized
	}
	
	private Double getGrayLevelNonUniformity() {
		Double gln = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			double s_i = 0.0;
			for(int j=1; j<=gldzm[0].length; j++) {
				s_i += gldzm_raw[i-1][j-1];//IMPORTANT
			}
			gln += (s_i * s_i);
		}
		return gln/Ns;
	}
	
	private Double getGrayLevelNonUniformityNormalized() {
		Double glnn = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			double s_i = 0.0;
			for(int j=1; j<=gldzm[0].length; j++) {
				s_i += gldzm[i-1][j-1];//IMPORTANT
			}
			glnn += (s_i * s_i);
		}
		return glnn;//already normalized
	}
	
	private Double getZoneDistanceNonUniformity() {
		Double zdn = 0.0d;
		for(int j=1; j<=gldzm[0].length; j++) {
			double inner = 0.0;
			for(int i=1; i<=gldzm.length; i++) {
				inner += gldzm_raw[i-1][j-1];//IMPORTANT
			}
			zdn += (inner * inner);
		}
		zdn /= Ns;
		return zdn;
	}
	
	private Double getZoneDistanceNonUniformityNormalized() {
		Double zdnn = 0.0d;
		for(int j=1; j<=gldzm[0].length; j++) {
			double inner = 0.0;
			for(int i=1; i<=gldzm.length; i++) {
				inner += gldzm[i-1][j-1];//IMPORTANT
			}
			zdnn += (inner * inner);
		}
		return zdnn;
	}
	
	private Double getDistancePercentage() {
		int Nv = Utils.getVoxels(discImg, mask, this.label).length;
		if(Nv == 0) {
			return null;
		}
		return  Ns/Nv;
	}
	
	private Double getGrayLevelVariance() {
		Double glv = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				glv += gldzm[i-1][j-1] * Math.pow(i-mu_i,2);
			}
		}
		return glv;
	}
	
	/*
	 * Distance size variance
	 */
	private Double getDistanceVariance() {
		Double zv = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				zv += gldzm[i-1][j-1] * Math.pow(j-mu_j,2);
			}
		}
		return zv;
	}
	
	private Double getDistanceEntropy() {
		Double ze = 0.0d;
		for(int i=1; i<=gldzm.length; i++) {
			for(int j=1; j<=gldzm[0].length; j++) {
				double p_ij = gldzm[i-1][j-1];
				if(p_ij == 0d) {
					p_ij = eps;
				}
				ze -= p_ij * (Math.log(p_ij)/Math.log(2.0));
			}
		}
		return ze;
	}
	
	private double[][] map2matrix(HashMap<Integer,Integer[]> gldzm){
		ArrayList<Integer> keys = new ArrayList<>(gldzm.keySet());
		Collections.sort(keys);
		double gray_max = StatUtils.max(Utils.getVoxels(discImg, mask, label));
		int distance_level = gldzm.get(keys.get(0)).length;
		double[][] res = new double[(int)gray_max][distance_level];
		int itr = 0;
		for(int i = 1;i<=gray_max; i++) {
			Integer[] row = gldzm.get(i);
			double[] row2 = new double[distance_level];
			if(row ==null) {
				for(int s=0;s<distance_level;s++) {
					row2[s] = 0d;
				}
			}else {
				for(int s=0;s<distance_level;s++) {
					row2[s] = (double)row[s];
					Ns += (double)row[s];
				}
			}
			res[itr++] = row2;
		}
		return res;
	}
	
	
	public double[][] getMatrix(boolean raw){
		if(!raw) {
			return gldzm;
		}else {
			return gldzm_raw;
		}
	}

	public String toString(double[][] mat){
		StringBuffer sb = new StringBuffer();
		int row = mat.length;
		int col = mat[0].length;
		for (int i=0; i<row;i++) {
			for (int j=0; j<col;j++) {
				sb.append(mat[i][j] + " ");
			}
			sb.append("\n");
		}
		return sb.toString() ;
	}

	
	public String toString(Integer[][][] res_map) {
		int s = res_map.length;
		int h = res_map[0].length;
		int w = res_map[0][0].length;
		for(int z=0;z<s;z++) {
			for(int y=0;y<h;y++) {
				String row = "";
				for(int x=0;x<w;x++) {
					if(res_map[z][y][x] != null) {
						row = row + res_map[z][y][x] + " ";
					}else {
						row = row + " " + " ";
					}
				}
				System.out.println(row);
			}
		}
		return null;
	}
	
	public String toString(ArrayList<ArrayList<Point3i>> blobs, Integer[][][] src) {
		System.out.println("********** check blobs *********");
		System.out.println("blob size:"+blobs.size());
		int src_s = src.length;
		int src_h = src[0].length;
		int src_w = src[0][0].length;
		int src_total = src_h * src_w * src_s;
		int search_total = 0;
		int[][][] refill = new int[src_s][src_h][src_w];
		int itr = 1;
		for(ArrayList<Point3i> blob : blobs) {
			String blob_content = "";
			for(Point3i p:blob) {
				int x = p.x;
				int y = p.y;
				int z = p.z;
				refill[z][y][x] = src[z][y][x];
				blob_content = blob_content + refill[z][y][x] + " ";
				search_total += 1;
			}
			System.out.println("blob"+(itr++)+" : "+blob_content);
		}
		System.out.println("search/total:"+search_total+"/"+src_total);
		for (int z = 0; z < src_s; z++) {
			for (int y = 0; y < src_h; y++) {
				String row = "";
				for (int x = 0; x < src_w; x++) {
					int v = refill[z][y][x];
					row = row + v + " ";
				}
				System.out.println(row);
			}
		}
		return null;
	}
	
	
	/**
	 * 
	 * @param size
	 * @param dim 2 or 3.
	 */
	public void testDistanceMap(int size, int dim) {
		int s = size;
		if(dim == 2) {
			s = 1;
		}
		ImageStack stack = new ImageStack(size, size);
		for (int z = 0; z < s; z++) {
			ByteProcessor bp = new ByteProcessor(size, size);
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					bp.set(x, y, 1);
				}
			}
			stack.addSlice(bp);
		}
		// test
		Integer dMap[][][] = getDistanceMap(new ImagePlus("test distance 3d", stack));
		StringBuilder sb = new StringBuilder();
		for (int z = 0; z < s; z++) {
			sb.append("slice " + (z + 1));
			sb.append("\n");
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					sb.append(dMap[z][y][x] + ",");
				}
				sb.append("\n");
			}
		}
		System.out.println(sb.toString());
	}
}

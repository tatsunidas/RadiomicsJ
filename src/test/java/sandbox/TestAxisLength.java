package sandbox;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.TestDataLoader;
import RadiomicsJ.Utils;
//import bonej.Ellipsoid;
import ij.ImagePlus;

public class TestAxisLength {

	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
//		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
//		ImagePlus isoMask = null;//Utils.isoVoxelize(mask, true);
		
//		ImagePlus maskByte = Utils.createMaskCopyAsGray8(isoMask);//ISO voxel
		
		ImagePlus maskByte = Utils.createMaskCopyAsGray8(mask,1);
		
		
		/*
		 * pattern1 : collect voxels using mesh
		 */
//		int threshold = RadiomicsJ.label-1;
//		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
//		MCTriangulator mct = new MCTriangulator();
//		int resamplingF = 2; // 1 to N.
//		@SuppressWarnings("unchecked")
//		List<Point3f> points = mct.getTriangles(maskByte, threshold, channels, resamplingF);
//		int nPoints = points.size();
//		double pts[][] = new double[nPoints][];
//		double[] pts_x = new double[nPoints];
//		double[] pts_y = new double[nPoints];
//		double[] pts_z = new double[nPoints];
//		for (int n = 0; n < nPoints; n++) {
//			Point3f p = points.get(n);
//			pts[n] = new double[] {p.x,p.y,p.z};
//			pts_x[n] = p.x;
//			pts_y[n] = p.y;
//			pts_z[n] = p.z;
//		}
		
		/*
		 * pettern 2 : no using mesh
		 */
		int nPoints = 0;
		for(int z=0;z<mask.getNSlices();z++) {
			for(int y=0;y<mask.getHeight();y++) {
				for(int x=0;x<mask.getWidth();x++) {
					maskByte.setSlice(z+1);
					int lbl = maskByte.getPixel(x, y)[0];
					if(lbl==1) {
						nPoints++;
					}
				}
			}
		}
		double[][] pts = new double[nPoints][];
		double[] pts_x = new double[nPoints];
		double[] pts_y = new double[nPoints];
		double[] pts_z = new double[nPoints];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for(int z=0;z<mask.getNSlices();z++) {
			for(int y=0;y<mask.getHeight();y++) {
				for(int x=0;x<mask.getWidth();x++) {
					maskByte.setSlice(z+1);
					int lbl = maskByte.getPixel(x, y)[0];
					if(lbl==1) {
						pts[n] = new double[] {x*dx,y*dy,z*dz};
						pts_x[n] = x*dx;
						pts_y[n] = y*dy;
						pts_z[n] = z*dz;
						n++;
					}
				}
			}
		}
		
		/*
		 * standardization//no need.
		 */
//		double mx = StatUtils.mean(pts_x);
//		double my = StatUtils.mean(pts_y);
//		double mz = StatUtils.mean(pts_z);
//		double varx = StatUtils.variance(pts_x);
//		double vary = StatUtils.variance(pts_y);
//		double varz = StatUtils.variance(pts_z);
//		for(int i=0;i<nPoints;i++) {
//			pts[i][0] = (pts[i][0]-mx)/Math.sqrt(varx);
//			pts[i][1] = (pts[i][1]-my)/Math.sqrt(vary);
//			pts[i][2] = (pts[i][2]-mz)/Math.sqrt(varz);
//		}
		
		/*
		 * bonej
		 * can not reproduce
		 */
//		Ellipsoid eli = FitEllipsoid.fitTo(pts);
//		double axis[] = eli.getRadii();
//		System.out.println(Math.sqrt(axis[0])*4);
//		System.out.println(Math.sqrt(axis[1])*4);
//		System.out.println(Math.sqrt(axis[2])*4);
		
		/*
		 * pca success !
		 */
		//create points in a double array
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		//see https://stats.stackexchange.com/questions/2691/making-sense-of-principal-component-analysis-eigenvectors-eigenvalues
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double axis[] = ed.getRealEigenvalues();
		@SuppressWarnings("unused")
		double axis2[] = ed.getImagEigenvalues();//0,0,0
//		double[] axis = svd.getSingularValues();//same result of ed.
		System.out.println(Math.sqrt(axis[0])*4);
		System.out.println(Math.sqrt(axis[1])*4);
		System.out.println(Math.sqrt(axis[2])*4);
//		System.out.println(axis2[0]);
//		System.out.println(axis2[1]);
//		System.out.println(axis2[2]);
		System.out.println(axis.length);
		
		/*
		 * 3d tool
		 * very close, but not the same.
		 */
//		MorphologicalFeatures mf = new MorphologicalFeatures(imp, mask);
//		System.out.println(mf.calculate(RadiomicsJ.MorphologicalFeatureTypes.MajorAxisLength.id()));//ok
//		System.out.println(mf.calculate(RadiomicsJ.MorphologicalFeatureTypes.MinorAxisLength.id()));//ok
//		System.out.println(mf.calculate(RadiomicsJ.MorphologicalFeatureTypes.LeastAxisLength.id()));
		System.exit(0);
	}

}

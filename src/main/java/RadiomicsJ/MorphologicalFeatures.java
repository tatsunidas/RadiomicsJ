package RadiomicsJ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.scijava.vecmath.Point3f;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;

import customnode.CustomTriangleMesh;
import fiji.plugins.Convex_Hull3DTool;
import fiji.plugins.Ellipsoid_3DTool;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import marchingcubes.MCTriangulator;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangle;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;

/**
 * 3.1 Morphological features HCUG
 * 3.1.1 Volume (mesh) RNU0
 * 3.1.2 Volume (voxel counting) YEKZ
 * 3.1.3 Surface area (mesh) C0JK
 * 3.1.4 Surface to volume ratio 2PR5
 * 3.1.5 Compactness 1 SKGS
 * 3.1.6 Compactness 2 BQWJ
 * 3.1.7 Spherical disproportion KRCK
 * 3.1.8 Sphericity QCFX
 * 3.1.9 Asphericity 25C7
 * 3.1.10 Centre of mass shift KLMA
 * 3.1.11 Maximum 3D diameter L0JK
 * 3.1.12 Major axis length TDIC
 * 3.1.13 Minor axis length P9VJ
 * 3.1.14 Least axis length 7J51
 * 3.1.15 Elongation Q3CK
 * 3.1.16 Flatness N17B
 * 3.1.17 Volume density (axis-aligned bounding box) PBX1
 * 3.1.18 Area density (axis-aligned bounding box) R59B
 * 3.1.19 Volume density (oriented minimum bounding box) ZH1A//deprecated
 * 3.1.20 Area density (oriented minimum bounding box) IQYR//deprecated
 * 3.1.21 Volume density (approximate enclosing ellipsoid) 6BDE
 * 3.1.22 Area density (approximate enclosing ellipsoid) RDD2
 * 3.1.23 Volume density (minimum volume enclosing ellipsoid) SWZ1//deprecated
 * [NOT IMPLEMENTED] 3.1.24 Area density (minimum volume enclosing ellipsoid) BRI8//deprecated, NOT IMPLEMENTED
 * 3.1.25 Volume density (convex hull) R3ER
 * 3.1.26 Area density (convex hull) 7T7F
 * 3.1.27 Integrated intensity 99N0
 * 3.1.28 Moran's I index N365
 * 3.1.29 Geary's C measure NPT7
 * 
 * @author tatsunidas
 *
 */
public class MorphologicalFeatures {
	
	
	ImagePlus orgImg;
	ImagePlus orgMask;
	/*
	 * iso voxels
	 */
	ImagePlus isoMask;
	Calibration orgCal;// backup
	int label;
	double[] voxels;
	double[] eigenValues;//major,minor,least
	
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	MCTriangulator mct = null;
	List<Point3f> points;//mesh triangles
	
	Double mesh_v;//mesh volume
	Double surfaceArea;
	
	public MorphologicalFeatures(ImagePlus img, ImagePlus mask, int label) {
		if (img == null) {
			return;
		}
		if (img.getType() == ImagePlus.COLOR_RGB) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
			return;
		}
		this.label = label;
		if (mask != null) {
			if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight()
					|| img.getNSlices() != mask.getNSlices()) {
				JOptionPane.showMessageDialog(null,
						"RadiomicsJ: please should be same dimension(w,h,s) images and masks.");
				return;
			}
		}else {
			// create full face mask
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		orgCal = img.getCalibration().copy();
		mask.setCalibration(orgCal.copy());
		mask.getCalibration().disableDensityCalibration();
		orgImg = img;
		orgMask = mask;
		isoMask = Utils.isoVoxelizeWithInterpolation(orgMask, true);
		ImagePlus isoMaskCopy = Utils.createMaskCopyAsGray8(isoMask, this.label);
		
		//create mesh first.
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		/*
		 * Resampling: how much resampling to apply to the stack while creating the
		 * surface mesh. A low number results in an accurate but jagged mesh with many
		 * triangles, while a high number results in a smooth mesh with fewer triangles.
		 * 
		 * In the case of digital phantom1,
		 * when resampling fator set to 1, can not get same result in IBSI.
		 * it set to 2, can get same IBSI result.
		 */
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		// should be use iso mask copy.
		List<Point3f> points = mct.getTriangles(isoMaskCopy, threshold, channels, resamplingF);
		this.points = points;
		voxels = Utils.getVoxels(orgImg, orgMask, this.label);
	}
	
	
	public Double calculate(String id) {
		String name = MorphologicalFeatureType.findType(id);
		if(name.equals(MorphologicalFeatureType.VolumeMesh.name())) {
			return getVolumeByMesh();
		}else if (name.equals(MorphologicalFeatureType.VolumeVoxelCounting.name())) {
			return getVolumeByVoxelCounting();
		}else if (name.equals(MorphologicalFeatureType.SurfaceAreaMesh.name())) {
			return getSurfaceAreaByMesh();
		}else if (name.equals(MorphologicalFeatureType.SurfaceToVolumeRatio.name())) {
			return getSurfaceToVolumeRatio();
		}else if (name.equals(MorphologicalFeatureType.Compactness1.name())) {
			return getCompactness1();
		}else if (name.equals(MorphologicalFeatureType.Compactness2.name())) {
			return getCompactness2();
		}else if (name.equals(MorphologicalFeatureType.SphericalDisproportion.name())) {
			return getSphericalDisproportion();
		}else if (name.equals(MorphologicalFeatureType.Sphericity.name())) {
			return getSphericity();
		}else if (name.equals(MorphologicalFeatureType.Asphericity.name())) {
			return getAsphericity();
		}else if (name.equals(MorphologicalFeatureType.CentreOfMassShift.name())) {
			return getCenterOfMassShift1();
		}else if (name.equals(MorphologicalFeatureType.Maximum3DDiameter.name())) {
//			return getMaximum3DDiameter();
//			return getMaximum3DDiameterUsingConvHull();
			return getMaximum3DDiameterByMesh();
		}else if (name.equals(MorphologicalFeatureType.MajorAxisLength.name())) {
			return getMajorAxisLength();
		}else if (name.equals(MorphologicalFeatureType.MinorAxisLength.name())) {
			return getMinorAxisLength();
		}else if (name.equals(MorphologicalFeatureType.LeastAxisLength.name())) {
			return getLeastAxisLength();
		}else if (name.equals(MorphologicalFeatureType.Elongation.name())) {
			return getElongation();
		}else if (name.equals(MorphologicalFeatureType.Flatness.name())) {
			return getFlatness();
		}else if (name.equals(MorphologicalFeatureType.VolumeDensity_AxisAlignedBoundingBox.name())) {
			return getVolumeDensityByAxisAlignedBoundingBox();
		}else if (name.equals(MorphologicalFeatureType.AreaDensity_AxisAlignedBoundingBox.name())) {
			return getAreaDensityByAxisAlignedBoundingBox();
		}else if (name.equals(MorphologicalFeatureType.VolumeDensity_OrientedMinimumBoundingBox.name())) {
			return getVolumeDensityByOrientedMinimumBoundingBox();
		}else if (name.equals(MorphologicalFeatureType.AreaDensity_OrientedMinimumBoundingBox.name())) {
			return getAreaDensityByOrientedMinimumBoundingBox();
		}else if (name.equals(MorphologicalFeatureType.VolumeDensity_ApproximateEnclosingEllipsoid.name())) {
			return getVolumeDensityByApproximateEnclosingEllipsoid2();
		}else if (name.equals(MorphologicalFeatureType.AreaDensity_ApproximateEnclosingEllipsoid.name())) {
			return getAreaDensityByApproximateEnclosingEllipsoid2();
		}else if (name.equals(MorphologicalFeatureType.VolumeDensity_MinimumVolumeEnclosingEllipsoid.name())) {
			return getVolumeDensityByMinimumVolumeEnclosingEllipsoid();
		}else if (name.equals(MorphologicalFeatureType.AreaDensity_MinimumVolumeEnclosingEllipsoid.name())) {
			return getAreaDensityByMinimumVolumeEnclosingEllipsoid();//return null
		}else if (name.equals(MorphologicalFeatureType.VolumeDensity_ConvexHull.name())) {
			return getVolumeDensityByConvexHull();
//			return getVolumeDensityByConvexHull2();
		}else if (name.equals(MorphologicalFeatureType.AreaDensity_ConvexHull.name())) {
			return getAreaDensityByConvexHull();
		}else if (name.equals(MorphologicalFeatureType.IntegratedIntensity.name())) {
			return getIntegratedIntensity();
		}else if (name.equals(MorphologicalFeatureType.MoransIIndex.name())) {
			return getMoransIIndex_IBSI2();
		}else if (name.equals(MorphologicalFeatureType.GearysCMeasure.name())) {
			return getGearysCMeasure2();
		}
		return null;
	}
	
	/**
	 * Requirements
	 * Image and mask should convert to iso-voxels (1,1,1)[unit].
	 * @return mesh volume
	 */
	private Double getVolumeByMesh() {
		if(this.mesh_v != null) {
			return mesh_v;
		}
		if(this.mct != null && this.points != null) {
			CustomTriangleMesh mesh = new CustomTriangleMesh(this.points);
			mesh_v = (double) mesh.getVolume();
			if(mesh_v < 0) {
				mesh_v *= -1.0; //correct negative value 
			}
			mesh = null;
			return mesh_v;
		}
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask, this.label);//needed
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		/*
		 * Resampling: how much resampling to apply to the stack while creating the
		 * surface mesh. A low number results in an accurate but jagged mesh with many
		 * triangles, while a high number results in a smooth mesh with fewer triangles.
		 * 
		 * In the case of digital phantom1,
		 * when resampling fator set to 1, can not get same result in IBSI.
		 * it set to 2, can get same IBSI result.
		 */
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		final List<Point3f> points = mct.getTriangles(mask, threshold, channels, resamplingF);
//		// calculate volume
		CustomTriangleMesh mesh = new CustomTriangleMesh(points);
		mesh_v = (double) mesh.getVolume();
		if(mesh_v < 0) {
			mesh_v *= -1.0; //correct negative value 
		}
		mesh = null;
		return mesh_v;
	}
	
	/*
	 * IMPORTANT : this method only used for convex-full
	 * for convex
	 */
	private Double getVolumeByMesh(ImagePlus isoMaskCopy) {
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		//only in method. do not replace fields mct. 
		MCTriangulator mct = new MCTriangulator();
		int resamplingF = 1; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points = mct.getTriangles(isoMaskCopy, threshold, channels, resamplingF);
		// calculate volume
		CustomTriangleMesh mesh = new CustomTriangleMesh(points);
		Double v = (double) mesh.getVolume();
		if(v < 0) {
			v *= -1.0; //correct negative value 
		}
		mesh = null;
		mct = null;
		points = null;
		return v;
	}
	
	private Double getVolumeByVoxelCounting() {
		if(voxels == null) {
			voxels = Utils.getVoxels(orgImg, orgMask, this.label);
		}
		if(voxels == null) {
			return 0d;
		}
		int count = voxels.length;
		double vx = orgCal != null ? orgCal.pixelWidth : 1.0;
		double vy = orgCal != null ? orgCal.pixelHeight : 1.0;
		double vz = orgCal != null ? orgCal.pixelDepth : 1.0;
		return count * vx * vy * vz;
	}
	
	/*
	 * https://forum.image.sc/t/computing-3d-surface-area-of-binary-object-using-
	 * imagej-ops/36807
	 * https://github.com/mdoube/BoneJ/blob/17ee483603afa8a7efb745512be60a29e093c94e
	 * /src/org/doube/bonej/MeasureSurface.java#L156
	 * https://www.javatips.net/api/BoneJ-master/src/org/doube/bonej/MeasureSurface.java
	 */
	/**
	 * @return mesh surface area
	 */
	private Double getSurfaceAreaByMesh() {
		if(this.surfaceArea != null) {
			return this.surfaceArea;
		}
		if(this.mct != null && this.points != null) {
			surfaceArea = 0d;
			final int nPoints = points.size();
			final Point3f origin = new Point3f((float)orgCal.xOrigin, (float)orgCal.yOrigin, (float)orgCal.zOrigin);
			for (int n = 0; n < nPoints; n += 3) {
				final Point3f point0 = points.get(n);
				final Point3f point1 = points.get(n+1);
				final Point3f point2  = points.get(n+2);
				final double x1 = (point1.x - point0.x);
				final double y1 = (point1.y - point0.y);
				final double z1 = (point1.z - point0.z);
				final double x2 = (point2.x - point0.x);
				final double y2 = (point2.y - point0.y);
				final double z2 = (point2.z - point0.z);
				final Point3f crossVector = new Point3f();
				crossVector.x = (float) (y1 * z2 - z1 * y2);
				crossVector.y = (float) (z1 * x2 - x1 * z2);
				crossVector.z = (float) (x1 * y2 - y1 * x2);
				final double deltaArea = 0.5 * crossVector.distance(origin);
				surfaceArea += deltaArea;
			}
			return surfaceArea;
		}
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);//ISO voxel, need copy
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		/*
		 * resample factor set to 1,
		 * can not get same result IBSI digital phantom1.
		 * set to 2, get same result. 
		 */
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points = mct.getTriangles(mask, threshold, channels, resamplingF);
		surfaceArea = 0d;
		final int nPoints = points.size();
		final Point3f origin = new Point3f((float)orgCal.xOrigin, (float)orgCal.yOrigin, (float)orgCal.zOrigin);
//		System.out.println("Calculating surface area.., num of points : " + nPoints / 3);
		for (int n = 0; n < nPoints; n += 3) {
			// https://github.com/mdoube/BoneJ/blob/17ee483603afa8a7efb745512be60a29e093c94e/src/org/doube/geometry/Vectors.java#L19
			final Point3f point0 = points.get(n);
			final Point3f point1 = points.get(n+1);
			final Point3f point2  = points.get(n+2);
			final double x1 = (point1.x - point0.x);
			final double y1 = (point1.y - point0.y);
			final double z1 = (point1.z - point0.z);
			final double x2 = (point2.x - point0.x);
			final double y2 = (point2.y - point0.y);
			final double z2 = (point2.z - point0.z);
			final Point3f crossVector = new Point3f();
			crossVector.x = (float) (y1 * z2 - z1 * y2);
			crossVector.y = (float) (z1 * x2 - x1 * z2);
			crossVector.z = (float) (x1 * y2 - y1 * x2);
			final double deltaArea = 0.5 * crossVector.distance(origin);
			surfaceArea += deltaArea;
		}
		mask = null;
		points = null;
		return surfaceArea;
	}
	
	/*
	 * IMPORTANT : this method only used for convex-full
	 * for convex
	 */
	private Double getSurfaceAreaByMesh(ImagePlus isoMaskCopy) {
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		//do not replace fields mct !
		MCTriangulator mct = new MCTriangulator();
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points = mct.getTriangles(isoMaskCopy, threshold, channels, resamplingF);
		Double sumArea = 0d;
		final int nPoints = points.size();
		final Point3f origin = new Point3f((float)orgCal.xOrigin, (float)orgCal.yOrigin, (float)orgCal.zOrigin);
//		System.out.println("Calculating surface area.., num of points : " + nPoints / 3);
		for (int n = 0; n < nPoints; n += 3) {
			// https://github.com/mdoube/BoneJ/blob/17ee483603afa8a7efb745512be60a29e093c94e/src/org/doube/geometry/Vectors.java#L19
			final Point3f point0 = points.get(n);
			final Point3f point1 = points.get(n+1);
			final Point3f point2  = points.get(n+2);
			final double x1 = (point1.x - point0.x);
			final double y1 = (point1.y - point0.y);
			final double z1 = (point1.z - point0.z);
			final double x2 = (point2.x - point0.x);
			final double y2 = (point2.y - point0.y);
			final double z2 = (point2.z - point0.z);
			final Point3f crossVector = new Point3f();
			crossVector.x = (float) (y1 * z2 - z1 * y2);
			crossVector.y = (float) (z1 * x2 - x1 * z2);
			crossVector.z = (float) (x1 * y2 - y1 * x2);
			final double deltaArea = 0.5 * crossVector.distance(origin);
			sumArea += deltaArea;
		}
		mct = null;
		points = null;
		return sumArea;
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private Double getSurfaceAreaByMeshUsingNonIsoVoxels() {
		ImagePlus mask = Utils.createMaskCopy(orgMask);// non ISO voxel
		mask.getProcessor().setBinaryThreshold();
		ByteProcessor mask8bit = mask.createThresholdMask();
		ImagePlus mask8bitImp = mask.createImagePlus();
		mask8bitImp.setProcessor(mask8bit);
		mask8bitImp.setCalibration(orgCal);//set calibration
		int threshold = 255;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		int resamplingF = 1; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points = mct.getTriangles(mask8bitImp, threshold, channels, resamplingF);
		Double sumArea = 0d;
		final int nPoints = points.size();
		final Point3f origin = new Point3f((float)orgCal.xOrigin, (float)orgCal.yOrigin, (float)orgCal.zOrigin);
//		System.out.println("Calculating surface area.., num of points : " + nPoints / 3);
		for (int n = 0; n < nPoints; n += 3) {
			// https://github.com/mdoube/BoneJ/blob/17ee483603afa8a7efb745512be60a29e093c94e/src/org/doube/geometry/Vectors.java#L19
			final Point3f point0 = points.get(n);
			final Point3f point1 = points.get(n+1);
			final Point3f point2  = points.get(n+2);
			final double x1 = orgCal.pixelWidth * (point1.x - point0.x);
			final double y1 = orgCal.pixelHeight * (point1.y - point0.y);
			final double z1 = orgCal.pixelDepth * (point1.z - point0.z);
			final double x2 = orgCal.pixelWidth * (point2.x - point0.x);
			final double y2 = orgCal.pixelHeight * (point2.y - point0.y);
			final double z2 = orgCal.pixelDepth * (point2.z - point0.z);
			final Point3f crossVector = new Point3f();
			crossVector.x = (float) (y1 * z2 - z1 * y2);
			crossVector.y = (float) (z1 * x2 - x1 * z2);
			crossVector.z = (float) (x1 * y2 - y1 * x2);
			final double deltaArea = 0.5 * crossVector.distance(origin);
			sumArea += deltaArea;
		}
		mct = null;
		mask = null;
		points = null;
		return sumArea;
	}
	
	private Double getSurfaceToVolumeRatio() {
		double a = getSurfaceAreaByMesh();
		double v = getVolumeByMesh();
		return a/v;
	}
	
	private Double getCompactness1() {
		double a = getSurfaceAreaByMesh();
		double v = getVolumeByMesh();
		return v/(Math.sqrt(Math.PI)*Math.sqrt(Math.pow(a,3)));
	}
	
	private Double getCompactness2() {
		double a = getSurfaceAreaByMesh();
		double v = getVolumeByMesh();
		return 36*Math.PI*Math.pow(v,2)/Math.pow(a,3);
	}
	
	private Double getSphericalDisproportion() {
		double a = getSurfaceAreaByMesh();
		double v = getVolumeByMesh();
		return a/Math.cbrt(36*Math.PI*Math.pow(v,2));
	}
	
	private Double getSphericity() {
		double a = getSurfaceAreaByMesh();
		double v = getVolumeByMesh();
		return Math.cbrt(36*Math.PI*Math.pow(v,2))/a;
	}
	
	private Double getAsphericity() {
		double a = getSurfaceAreaByMesh();
		double v = getVolumeByMesh();
		return Math.cbrt(1/(36*Math.PI)*Math.pow(a,3)/Math.pow(v,2))-1.;
	}
	
	private Double getCenterOfMassShift1() {
		int label = this.label;
		int w = orgImg.getWidth();
		int h = orgImg.getHeight();
		int s = orgImg.getNSlices();
		double sum1=0.0, x_gl_sum=0.0, y_gl_sum=0.0, z_gl_sum=0.0;
		double x_sum=0.0, y_sum=0.0, z_sum=0.0;
		double voxelCount = Double.MIN_VALUE;
		for(int z=0;z<s;z++) {
			ImageProcessor mp = orgMask.getStack().getProcessor(z+1);
			ImageProcessor ip = orgImg.getStack().getProcessor(z+1);
			float[][] mSlice = mp.getFloatArray();
			float[][] iSlice = ip.getFloatArray();
			for(int y=0; y<h ; y++) {
				for(int x=0; x<w; x++) {
					int lbl_val = (int)mSlice[x][y];
					if (lbl_val == label) {
						double v = (double)iSlice[x][y];
						sum1 += v;
						x_gl_sum += x*v;
						y_gl_sum += y*v;
						z_gl_sum += z*v;
						x_sum += (double)x;//no intensity weight
						y_sum += (double)y;//no intensity weight
						z_sum += (double)z;//no intensity weight
						voxelCount += 1.0d;
					}
				}
			}
		}
		//if voxels not found, can not calculation...
		if(voxelCount == Double.MIN_VALUE) {
			return null;
		}
		//CoM geom
		double xCenterOfMass_geom = (double)x_sum/(double)voxelCount+0.5;//need +0.5, see, IJ's ByteStatistics.calculateMoment()
		double yCenterOfMass_geom = (double)y_sum/(double)voxelCount+0.5;
		double zCenterOfMass_geom = (double)z_sum/(double)voxelCount+0.5;
		//CoM gl
		double xCenterOfMass_gl = x_gl_sum/sum1+0.5;//need +0.5, see, IJ's ByteStatistics.calculateMoment()
		double yCenterOfMass_gl = y_gl_sum/sum1+0.5;
		double zCenterOfMass_gl = z_gl_sum/sum1+0.5;
		if (orgCal!=null) {
			xCenterOfMass_geom = orgCal.getX(xCenterOfMass_geom);
			yCenterOfMass_geom = orgCal.getY(yCenterOfMass_geom, h);
			zCenterOfMass_geom = orgCal.getZ(zCenterOfMass_geom);
			xCenterOfMass_gl = orgCal.getX(xCenterOfMass_gl);
			yCenterOfMass_gl = orgCal.getY(yCenterOfMass_gl, h);
			zCenterOfMass_gl = orgCal.getZ(zCenterOfMass_gl);
		}
//		System.out.println("XCoMgeom:"+xCenterOfMass_geom+" YCoMgeom:"+yCenterOfMass_geom+" ZCoMgeom:"+zCenterOfMass_geom);
//		System.out.println("XCoM:"+xCenterOfMass_gl+" YCoM:"+yCenterOfMass_gl+" ZCoM:"+zCenterOfMass_gl);
		double CoMShift = Math.sqrt(
				Math.pow(xCenterOfMass_geom - xCenterOfMass_gl, 2) + 
				Math.pow(yCenterOfMass_geom - yCenterOfMass_gl, 2) + 
				Math.pow(zCenterOfMass_geom - zCenterOfMass_gl, 2)); 
		return CoMShift;
	}
	
	/*
	 * this method also can use.
	 */
	@SuppressWarnings("unused")
	private Double getCenterOfMassShift2() {
		int w = orgImg.getWidth();
		int h = orgImg.getHeight();
		int s = orgImg.getNSlices();
		double sum1=0.0, x_gl_sum=0.0, y_gl_sum=0.0, z_gl_sum=0.0;
		double x_sum=0.0, y_sum=0.0, z_sum=0.0;
		double voxelCount = Double.MIN_VALUE;
		for(int z=0;z<s;z++) {
			orgImg.setPosition(z+1);
			orgMask.setPosition(z+1);
			for(int y=0; y<h ; y++) {
				for(int x=0; x<w; x++) {
					int lbl_val = (int)orgMask.getProcessor().getPixelValue(x, y);
					if (lbl_val == label) {
						double v = (double)orgImg.getProcessor().getPixelValue(x, y)+0.0d;
						sum1 += v;
						x_gl_sum += (x+0.5)*v;
						y_gl_sum += (y+0.5)*v;
						z_gl_sum += (z+0.5)*v;
						x_sum += (x+0.5);//no intensity weight
						y_sum += (y+0.5);//no intensity weight
						z_sum += (z+0.5);//no intensity weight
						voxelCount += 1.0d;
					}
				}
			}
		}
		//if voxels not found, can not calculation...
		if(voxelCount == Double.MIN_VALUE) {
			return null;
		}
		//CoM geom
		double xCenterOfMass_geom = x_sum/voxelCount;
		double yCenterOfMass_geom = y_sum/voxelCount;
		double zCenterOfMass_geom = z_sum/voxelCount;
		//CoM gl
		double xCenterOfMass_gl = x_gl_sum/sum1;
		double yCenterOfMass_gl = y_gl_sum/sum1;
		double zCenterOfMass_gl = z_gl_sum/sum1;
		if (orgCal!=null) {
			xCenterOfMass_geom = orgCal.getX(xCenterOfMass_geom);
			yCenterOfMass_geom = orgCal.getY(yCenterOfMass_geom, h);
			zCenterOfMass_geom = orgCal.getZ(zCenterOfMass_geom);
			xCenterOfMass_gl = orgCal.getX(xCenterOfMass_gl);
			yCenterOfMass_gl = orgCal.getY(yCenterOfMass_gl, h);
			zCenterOfMass_gl = orgCal.getZ(zCenterOfMass_gl);
		}
//		System.out.println("XCoMgeom:"+xCenterOfMass_geom+" YCoMgeom:"+yCenterOfMass_geom+" ZCoMgeom:"+zCenterOfMass_geom);
//		System.out.println("XCoM:"+xCenterOfMass_gl+" YCoM:"+yCenterOfMass_gl+" ZCoM:"+zCenterOfMass_gl);
		double CoMShift = Math.sqrt(
				Math.pow(xCenterOfMass_geom - xCenterOfMass_gl, 2) + 
				Math.pow(yCenterOfMass_geom - yCenterOfMass_gl, 2) + 
				Math.pow(zCenterOfMass_geom - zCenterOfMass_gl, 2)); 
		return CoMShift;
	}
	
	/*
	 * voxel based diameter
	 */
	@SuppressWarnings("unused")
	private Double getMaximum3DDiameter() {
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		double px = orgCal.pixelWidth;
		double py = orgCal.pixelHeight;
		double pz = orgCal.pixelDepth;
		double max = 0;
		for(int z1=0; z1<s; z1++) {
			for(int y1=0; y1<h; y1++) {
				for(int x1=0; x1<w; x1++) {
					//calc all voxels
//					orgMask.setSlice(z1+1);
					if((int)orgMask.getStack().getProcessor(z1+1).getPixelValue(x1, y1) !=label) {
						continue;
					}
					for(int z2=0; z2<s; z2++) {
						for(int y2=0; y2<h; y2++) {
							for(int x2=0; x2<w; x2++) {
								if((int)orgMask.getStack().getProcessor(z2+1).getPixelValue(x2, y2)!=label) {
									continue;
								}
								double qx = Math.pow(Math.abs(x1-x2)*px, 2);
								double qy = Math.pow(Math.abs(y1-y2)*py, 2);
								double qz = Math.pow(Math.abs(z1-z2)*pz, 2);
								double d = Math.sqrt(qx+qy+qz);
								if(max < d) {
									max = d;
								}
							}
						}
					}
				}
			}
		}
		return max;
	}
	
	@SuppressWarnings("unused")
	private Double getMaximum3DDiameterUsingConvHull() {
		ImagePlus mask = new Convex_Hull3DTool().run(isoMask);
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		double max = 0;
		for(int z1=0; z1<s; z1++) {
			for(int y1=0; y1<h; y1++) {
				for(int x1=0; x1<w; x1++) {
					//calc all voxels
					if((int)mask.getStack().getProcessor(z1+1).getPixelValue(x1, y1)!=label) {
						continue;
					}
					for(int z2=0; z2<s; z2++) {
						for(int y2=0; y2<h; y2++) {
							for(int x2=0; x2<w; x2++) {
//								mask.setSlice(z2+1);
								if((int)mask.getStack().getProcessor(z2+1).getPixelValue(x2, y2)!=label) {
									continue;
								}
								double qx = Math.pow(x1-x2, 2);
								double qy = Math.pow(y1-y2, 2);
								double qz = Math.pow(z1-z2, 2);
								double d = Math.sqrt(qx+qy+qz);
								if(max < d) {
									max = d;
								}
							}
						}
					}
				}
			}
		}
		return max;
	}
	
	
	private Double getMaximum3DDiameterByMesh() {
		if(this.mct != null && this.points != null) {
			Double max = 0d;
			final int nPoints = points.size();
			for (int n = 0; n < nPoints; n++) {
				Point3f point0 = points.get(n);
				for(int m = 0; m < nPoints; m++) {
					if(n == m) {
						continue;
					}
					Point3f point1 = points.get(m);
					double d = point0.distance(point1);
					if(max < d){
						max = d;
					}
				}
			}
			return Double.valueOf(max);
		}
		/*
		 * when digital phantom1 test,
		 * reference value is not clear.
		 * In validation sheet = 11.7.
		 * In reference manual = 13.1.
		 * here, we calculate reference manual basis.
		 * 
		 * if you need validation sheet basis results,
		 * use getMaximum3DDiameterByMeshByOriginal().
		 */
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);
		int threshold = label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points_ = mct.getTriangles(mask, threshold, channels, resamplingF);
		this.points = points_;
		Double max = 0d;
		final int nPoints = points.size();
		for (int n = 0; n < nPoints; n++) {
			Point3f point0 = points.get(n);
			for(int m = 0; m < nPoints; m++) {
				if(n == m) {
					continue;
				}
				Point3f point1 = points.get(m);
				double d = point0.distance(point1);
				if(max < d){
					max = d;
				}
			}
		}
		return Double.valueOf(max);
	}
	
	/**
	 * when digital phantom1 test,
	 * reference value is not clear.
	 * In validation sheet = 11.7.
	 * In reference manual = 13.1.
	 * here, we calculate reference manual basis.
	 * 
	 * if you need validation sheet basis results,
	 * maybe can get nearly result by using org mask instead of isoMask.
	 * 
	 * IMPORTANT!
	 * if you use orgMask, do not replace mct and points.
	 */
	@SuppressWarnings("unused")
	private Double getMaximum3DDiameterByMeshByOriginal() {
		ImagePlus mask = Utils.createMaskCopyAsGray8(orgMask,this.label);//for validation sheet basis.
		int threshold = label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		MCTriangulator mct = new MCTriangulator();//DO NOT replace field variable.
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points_ = mct.getTriangles(mask, threshold, channels, resamplingF);
		Double max = 0d;
		final int nPoints = points_.size();
		for (int n = 0; n < nPoints; n++) {
			Point3f point0 = points_.get(n);
			for(int m = 0; m < nPoints; m++) {
				if(n == m) {
					continue;
				}
				Point3f point1 = points_.get(m);
				double d = point0.distance(point1);
				if(max < d){
					max = d;
				}
			}
		}
		mct = null;
		points_ = null;
		return Double.valueOf(max);
	}
	
	/*
	 * https://github.com/mcib3d/mcib3d-plugins
	 * https://imagejdocu.tudor.lu/tutorial/plugins/3d_ellipsoid
	 * 
	 * if using iso voxel that applyed to IBSI digital phantom1,
	 * get differ from reference value.
	 * but, when using non iso mask, will get very close result.
	 * However, Elipsoid_3D handle vovel size x-y as "resXY". This value not separated to x and y.
	 * Thus, I recommend iso-voxel based calculation.
	 */
//	private Double getMajorAxisLength() {
//		//should be use ISO-Voxel for using image.Elipsoid_3D. resXY of Elipsoid_3D is unclear.
//		if(orgCal.pixelWidth == orgCal.pixelHeight) {
//			return new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(orgMask, false)[0];
//		}else {
//			return new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(isoMask, false)[0];
//		}
//	}
//	
//	private Double getMinorAxisLength() {
//		if(orgCal.pixelWidth == orgCal.pixelHeight) {
//			return new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(orgMask, false)[1];
//		}else {
//			return new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(isoMask, false)[1];
//		}
//	}
//	
//	private Double getLeastAxisLength() {
//		if(orgCal.pixelWidth == orgCal.pixelHeight) {
//			return new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(orgMask,true)[2];
//		}else {
//			return new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(isoMask,true)[2];
//		}
//	}
	
	public Double getMajorAxisLength() {
		if(eigenValues != null) {
			return Math.sqrt(eigenValues[0])*4;
		}
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		if(this.voxels != null) {
			nPoints = voxels.length;
		}else {
			for (int z = 0; z < s; z++) {
				ImageProcessor mp = mask.getStack().getProcessor(z + 1);
				float[][] mSlice = mp.getFloatArray();
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mSlice[x][y];
						if (lbl == this.label) {
							nPoints++;
						}
					}
				}
			}
		}
		
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			float[][] mSlice = mp.getFloatArray();
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)mSlice[x][y];
					if(lbl==this.label) {
						pts[n] = new double[] {x*dx,y*dy,z*dz};
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		eigenValues = ed.getRealEigenvalues();
		return Math.sqrt(eigenValues[0])*4;
	}
	
	public Double getMinorAxisLength() {
		if(eigenValues != null) {
			return eigenValues[1] < eigenValues[2] ? Math.sqrt(eigenValues[2])*4 : Math.sqrt(eigenValues[1])*4;
		}
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		if(this.voxels != null) {
			nPoints = voxels.length;
		}else {
			for (int z = 0; z < s; z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mask.getStack().getProcessor(z + 1).getPixelValue(x, y);
						if (lbl == this.label) {
							nPoints++;
						}
					}
				}
			}
		}
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)mp.getPixelValue(x, y);
					if(lbl==this.label) {
						pts[n] = new double[] {x*dx,y*dy,z*dz};
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		eigenValues = ed.getRealEigenvalues();
		return eigenValues[1] < eigenValues[2] ? Math.sqrt(eigenValues[2])*4 : Math.sqrt(eigenValues[1])*4;
	}
	
	public Double getLeastAxisLength() {
		if(eigenValues != null) {
			return eigenValues[1] < eigenValues[2] ? Math.sqrt(eigenValues[1])*4 : Math.sqrt(eigenValues[2])*4;
		}
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		if(this.voxels != null) {
			nPoints = voxels.length;
		}else {
			for (int z = 0; z < s; z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mask.getStack().getProcessor(z + 1).getPixelValue(x, y);
						if (lbl == this.label) {
							nPoints++;
						}
					}
				}
			}
		}
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)mp.getPixelValue(x, y);
					if(lbl==this.label) {
						pts[n] = new double[] {x*dx,y*dy,z*dz};
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		eigenValues = ed.getRealEigenvalues();
		return eigenValues[1] < eigenValues[2] ? Math.sqrt(eigenValues[1])*4 : Math.sqrt(eigenValues[2])*4;
	}
	
	
//	private Double getElongation() {
//		double valueAxis[] = null;
//		if(orgCal.pixelWidth == orgCal.pixelHeight) {
//			valueAxis = new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(orgMask,false);
//		}else {
//			valueAxis = new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(isoMask,false);
//		}
//		return Math.sqrt(valueAxis[1]/valueAxis[0]);
//	}
	
	private Double getElongation() {
		if(eigenValues != null) {
			double axis[] = eigenValues;
			double major = axis[0];
			double minor = axis[1] > axis[2] ? axis[1]:axis[2];
			return Math.sqrt(minor/major);
		}
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		if(this.voxels != null) {
			nPoints = voxels.length;
		}else {
			for (int z = 0; z < s; z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mask.getStack().getProcessor(z + 1).getPixelValue(x, y);
						if (lbl == this.label) {
							nPoints++;
						}
					}
				}
			}
		}
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)mp.getPixelValue(x, y);
					if(lbl==this.label) {
						pts[n] = new double[] {x*dx,y*dy,z*dz};
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double axis[] = ed.getRealEigenvalues();
		double major = axis[0];
		double minor = axis[1] > axis[2] ? axis[1]:axis[2];
		return Math.sqrt(minor/major);
	}

	
	private Double getFlatness() {
		if(eigenValues != null) {
			double axis[] = eigenValues;
			double major = axis[0];
			double least = axis[1] < axis[2] ? axis[1]:axis[2];
			return Math.sqrt(least/major);
		}
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
	
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		if(this.voxels != null) {
			nPoints = voxels.length;
		}else {
			for (int z = 0; z < s; z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mask.getStack().getProcessor(z + 1).getPixelValue(x, y);
						if (lbl == this.label) {
							nPoints++;
						}
					}
				}
			}
		}
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)mp.getPixelValue(x, y);
					if(lbl==this.label) {
						pts[n] = new double[] {x*dx,y*dy,z*dz};
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		// create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		// create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//			SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double axis[] = ed.getRealEigenvalues();
		double major = axis[0];
		double least = axis[1] < axis[2] ? axis[1]:axis[2];
		return Math.sqrt(least/major);
	}
	
	/*
	 * This feature is also called extent
	 */
	private Double getVolumeDensityByAxisAlignedBoundingBox(){
		if(mct != null && mesh_v != null) {
			// calculate volume
			CustomTriangleMesh mesh = new CustomTriangleMesh(points);
			Double v = (double) mesh.getVolume();
			if(v < 0) {
				v *= -1.0; //correct negative value 
			}
			double minx=0, maxx = 0, miny=0, maxy=0, minz=0, maxz=0;
			for (Point3f vox : points) {
	            double x = vox.getX();
	            double y = vox.getY();
	            double z = vox.getZ();
	            if (x > maxx) {
	                maxx = x;
	            }
	            if (x < minx) {
	                minx = x;
	            }
	            if (y > maxy) {
	                maxy = y;
	            }
	            if (y < miny) {
	                miny = y;
	            }
	            if (z > maxz) {
	                maxz = z;
	            }
	            if (z < minz) {
	                minz = z;
	            }
	        }
			double Vaabb = Math.abs(maxx-minx)*Math.abs(maxy-miny)*Math.abs(maxz-minz);
			mesh = null;
			return v/Vaabb;
		}
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);
		int threshold = label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points = mct.getTriangles(mask, threshold, channels, resamplingF);
		this.points = points;
		// calculate volume
		CustomTriangleMesh mesh = new CustomTriangleMesh(points);
		Double v = (double) mesh.getVolume();
		if(v < 0) {
			v *= -1.0; //correct negative value 
		}
		double minx=0, maxx = 0, miny=0, maxy=0, minz=0, maxz=0;
		for (Point3f vox : points) {
            double x = vox.getX();
            double y = vox.getY();
            double z = vox.getZ();
            if (x > maxx) {
                maxx = x;
            }
            if (x < minx) {
                minx = x;
            }
            if (y > maxy) {
                maxy = y;
            }
            if (y < miny) {
                miny = y;
            }
            if (z > maxz) {
                maxz = z;
            }
            if (z < minz) {
                minz = z;
            }
        }
		double Vaabb = Math.abs(maxx-minx)*Math.abs(maxy-miny)*Math.abs(maxz-minz);
		mesh = null;
		return v/Vaabb;
	}
	
	private Double getAreaDensityByAxisAlignedBoundingBox(){
		if(mct != null && mesh_v != null) {
			if(surfaceArea != null) {
				double minx=0, maxx = 0, miny=0, maxy=0, minz=0, maxz=0;
				for (Point3f vox : points) {
		            double x = vox.getX();
		            double y = vox.getY();
		            double z = vox.getZ();
		            if (x > maxx) {
		                maxx = x;
		            }
		            if (x < minx) {
		                minx = x;
		            }
		            if (y > maxy) {
		                maxy = y;
		            }
		            if (y < miny) {
		                miny = y;
		            }
		            if (z > maxz) {
		                maxz = z;
		            }
		            if (z < minz) {
		                minz = z;
		            }
		        }
				double xy = Math.abs(maxx-minx) * Math.abs(maxy-miny) * 2;
				double xz = Math.abs(maxx-minx) * Math.abs(maxz-minz) * 2;
				double yz = Math.abs(maxy-miny) * Math.abs(maxz-minz) * 2;
				return surfaceArea/(xy+xz+yz);
			}else {
				surfaceArea = 0d;
				final int nPoints = points.size();
				final Point3f origin = new Point3f((float)orgCal.xOrigin, (float)orgCal.yOrigin, (float)orgCal.zOrigin);
				for (int n = 0; n < nPoints; n += 3) {
					final Point3f point0 = points.get(n);
					final Point3f point1 = points.get(n+1);
					final Point3f point2  = points.get(n+2);
					final double x1 = (point1.x - point0.x);
					final double y1 = (point1.y - point0.y);
					final double z1 = (point1.z - point0.z);
					final double x2 = (point2.x - point0.x);
					final double y2 = (point2.y - point0.y);
					final double z2 = (point2.z - point0.z);
					final Point3f crossVector = new Point3f();
					crossVector.x = (float) (y1 * z2 - z1 * y2);
					crossVector.y = (float) (z1 * x2 - x1 * z2);
					crossVector.z = (float) (x1 * y2 - y1 * x2);
					final double deltaArea = 0.5 * crossVector.distance(origin);
					surfaceArea += deltaArea;
				}
				double minx=0, maxx = 0, miny=0, maxy=0, minz=0, maxz=0;
				for (Point3f vox : points) {
		            double x = vox.getX();
		            double y = vox.getY();
		            double z = vox.getZ();
		            if (x > maxx) {
		                maxx = x;
		            }
		            if (x < minx) {
		                minx = x;
		            }
		            if (y > maxy) {
		                maxy = y;
		            }
		            if (y < miny) {
		                miny = y;
		            }
		            if (z > maxz) {
		                maxz = z;
		            }
		            if (z < minz) {
		                minz = z;
		            }
		        }
				double xy = Math.abs(maxx-minx) * Math.abs(maxy-miny) * 2;
				double xz = Math.abs(maxx-minx) * Math.abs(maxz-minz) * 2;
				double yz = Math.abs(maxy-miny) * Math.abs(maxz-minz) * 2;
				return surfaceArea/(xy+xz+yz);
			}
		}
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);//ISO voxel
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		mct = new MCTriangulator();
		/*
		 * resample factor set to 1,
		 * can not get same result IBSI digital phantom1.
		 * set to 2, get same result. 
		 */
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<Point3f> points = mct.getTriangles(mask, threshold, channels, resamplingF);
		Double sumArea = 0d;
		final int nPoints = points.size();
		final Point3f origin = new Point3f((float)orgCal.xOrigin, (float)orgCal.yOrigin, (float)orgCal.zOrigin);
		for (int n = 0; n < nPoints; n += 3) {
			final Point3f point0 = points.get(n);
			final Point3f point1 = points.get(n+1);
			final Point3f point2  = points.get(n+2);
			final double x1 = (point1.x - point0.x);
			final double y1 = (point1.y - point0.y);
			final double z1 = (point1.z - point0.z);
			final double x2 = (point2.x - point0.x);
			final double y2 = (point2.y - point0.y);
			final double z2 = (point2.z - point0.z);
			final Point3f crossVector = new Point3f();
			crossVector.x = (float) (y1 * z2 - z1 * y2);
			crossVector.y = (float) (z1 * x2 - x1 * z2);
			crossVector.z = (float) (x1 * y2 - y1 * x2);
			final double deltaArea = 0.5 * crossVector.distance(origin);
			sumArea += deltaArea;
		}
		double minx=0, maxx = 0, miny=0, maxy=0, minz=0, maxz=0;
		for (Point3f vox : points) {
            double x = vox.getX();
            double y = vox.getY();
            double z = vox.getZ();
            if (x > maxx) {
                maxx = x;
            }
            if (x < minx) {
                minx = x;
            }
            if (y > maxy) {
                maxy = y;
            }
            if (y < miny) {
                miny = y;
            }
            if (z > maxz) {
                maxz = z;
            }
            if (z < minz) {
                minz = z;
            }
        }
		double xy = Math.abs(maxx-minx) * Math.abs(maxy-miny) * 2;
		double xz = Math.abs(maxx-minx) * Math.abs(maxz-minz) * 2;
		double yz = Math.abs(maxy-miny) * Math.abs(maxz-minz) * 2;
		mask = null;
		points = null;
		return sumArea/(xy+xz+yz);
	}
	
	/*
	 * Note: This feature currently has no reference values and should not be used.
	 */
	@Deprecated
	private Double getVolumeDensityByOrientedMinimumBoundingBox(){
		double v = getVolumeByMesh();
		double[] rotatedBB = new Ellipsoid_3DTool().getOrientedBoundingBox(isoMask);//todo check label intensity.//xmin,xmax,ymin,ymax,zmin,zmax
		double Vaabb = Math.abs(rotatedBB[1]-rotatedBB[0]) * Math.abs(rotatedBB[3]-rotatedBB[2]) * Math.abs(rotatedBB[5]-rotatedBB[4]);
		return v/Vaabb;
	}
	
	/*
	 * Note: This feature currently has no reference values and should not be used.
	 */
	@Deprecated
	private Double getAreaDensityByOrientedMinimumBoundingBox(){
		double a = getSurfaceAreaByMesh();
		double[] rotatedBB = new Ellipsoid_3DTool().getOrientedBoundingBox(isoMask);//todo check label intensity.//xmin,xmax,ymin,ymax,zmin,zmax
		double xy =  Math.abs(rotatedBB[1]-rotatedBB[0]) * Math.abs(rotatedBB[3]-rotatedBB[2]) * 2;
		double xz =  Math.abs(rotatedBB[1]-rotatedBB[0]) * Math.abs(rotatedBB[5]-rotatedBB[4]) * 2;
		double yz =  Math.abs(rotatedBB[3]-rotatedBB[2]) * Math.abs(rotatedBB[5]-rotatedBB[4]) * 2;
		return a/(xy+xz+yz);
	}
	
	/*
	 * use getVolumeDensityByApproximateEnclosingEllipsoid2()
	 */
	@SuppressWarnings("unused")
	private Double getVolumeDensityByApproximateEnclosingEllipsoid(){
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		if(voxels != null) {
			nPoints = voxels.length;
		}else {
			for (int z = 0; z < s; z++) {
				float[][] mSlice = mask.getStack().getProcessor(z + 1).getFloatArray();
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mSlice[x][y];
						if (lbl == this.label) {
							nPoints++;
						}
					}
				}
			}
		}
		
		// create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for (int z = 0; z < s; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int)mask.getStack().getProcessor(z+1).getPixelValue(x, y);
					if (lbl==this.label) {
						pts[n] = new double[] { x * dx, y * dy, z * dz };
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		// create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		// create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//			SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double eigen[] = ed.getRealEigenvalues();
		double a = 2 * Math.sqrt(eigen[0]);
		double b = 2 * Math.sqrt(eigen[1]);
		double c = 2 * Math.sqrt(eigen[2]+eps);//fail safe, for single slice
		double v3 = getVolumeByMesh()*3;
		double pi4abc = 4*Math.PI*(a * b *c);
		return v3/pi4abc;
	}
	
	/*
	 * faster version
	 */
	private Double getVolumeDensityByApproximateEnclosingEllipsoid2(){
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		ArrayList<double[]> ptsList = new ArrayList<>();
		// create points in a double array
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for (int z = 0; z < s; z++) {
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int)mSlice[x][y];
					if (lbl==this.label) {
						double[] pts_ = new double[] { x * dx, y * dy, z * dz };
						ptsList.add(pts_);
					}
				}
			}
		}
		double[][] pts = ptsList.toArray(new double[ptsList.size()][]);
		/*
		 * pca
		 */
		// create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		// create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//			SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double eigen[] = ed.getRealEigenvalues();
		double a = 2 * Math.sqrt(eigen[0]);
		double b = 2 * Math.sqrt(eigen[1]);
		double c = 2 * Math.sqrt(eigen[2]+eps);//fail safe, for single slice
		double v3 = getVolumeByMesh()*3;
		double pi4abc = 4*Math.PI*(a * b *c);
		return v3/pi4abc;
	}
	
	/*
	 * use getAreaDensityByApproximateEnclosingEllipsoid2()
	 */
	@SuppressWarnings("unused")
	private Double getAreaDensityByApproximateEnclosingEllipsoid(){
		double sa = getSurfaceAreaByMesh();
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		for (int z = 0; z < s; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int)mask.getStack().getProcessor(z+1).getPixelValue(x, y);
					if (lbl==this.label) {
						nPoints++;
					}
				}
			}
		}
		// create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for (int z = 0; z < s; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int)mask.getStack().getProcessor(z+1).getPixelValue(x, y);
					if (lbl==this.label) {
						pts[n] = new double[] { x * dx, y * dy, z * dz };
						n++;
					}
				}
			}
		}
		/*
		 * pca
		 */
		// create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		// create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//			SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double eigen[] = ed.getRealEigenvalues();
		double a = 2 * Math.sqrt(eigen[0]);
		double b = 2 * Math.sqrt(eigen[1]);
		double c = 2 * Math.sqrt(eigen[2]+eps);
		
		double pi4ab = 4*Math.PI*(a*b);
		double alpha = Math.sqrt(1-(Math.pow(b, 2)/Math.pow(a, 2)));
		double beta = Math.sqrt(1-(Math.pow(c, 2)/Math.pow(a, 2)));
		int myu_stop = 20;
		double approximate_surface_factor = 0d;
		for(int i=0; i<myu_stop; i++) {
			double left = Math.pow(alpha*beta, i)/(1-4*Math.pow(i, 2));
			PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(i);
			double poly_v = legendre.value((Math.pow(alpha, 2)+Math.pow(beta, 2))/(2*alpha*beta));
			approximate_surface_factor += (left*poly_v);
		}
		return sa/(pi4ab*approximate_surface_factor);
	}
	
	private Double getAreaDensityByApproximateEnclosingEllipsoid2(){
		double sa = getSurfaceAreaByMesh();
		int w = orgMask.getWidth();
		int h = orgMask.getHeight();
		int s = orgMask.getNSlices();
		ImagePlus mask = orgMask;
		/*
		 * no using mesh
		 */
		// create points in a double array
		ArrayList<double[]> ptsList = new ArrayList<>();
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		double dz = mask.getCalibration().pixelDepth;
		for (int z = 0; z < s; z++) {
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int)mSlice[x][y];
					if (lbl==this.label) {
						double[] pts_ = new double[] { x * dx, y * dy, z * dz };
						ptsList.add(pts_);
					}
				}
			}
		}
		double[][] pts = ptsList.toArray(new double[ptsList.size()][]);
		/*
		 * pca
		 */
		// create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		// create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//			SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double eigen[] = ed.getRealEigenvalues();
		double a = 2 * Math.sqrt(eigen[0]);
		double b = 2 * Math.sqrt(eigen[1]);
		double c = 2 * Math.sqrt(eigen[2]+eps);
		
		double pi4ab = 4*Math.PI*(a*b);
		double alpha = Math.sqrt(1-(Math.pow(b, 2)/Math.pow(a, 2)));
		double beta = Math.sqrt(1-(Math.pow(c, 2)/Math.pow(a, 2)));
		int myu_stop = 20;
		double approximate_surface_factor = 0d;
		for(int i=0; i<myu_stop; i++) {
			double left = Math.pow(alpha*beta, i)/(1-4*Math.pow(i, 2));
			PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(i);
			double poly_v = legendre.value((Math.pow(alpha, 2)+Math.pow(beta, 2))/(2*alpha*beta));
			approximate_surface_factor += (left*poly_v);
		}
		return sa/(pi4ab*approximate_surface_factor);
	}
	
	/*
	 * Note: This feature currently has no reference values and should not be used.
	 */
	@Deprecated
	private Double getVolumeDensityByMinimumVolumeEnclosingEllipsoid(){
		double[] res = new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(isoMask,false,label);
		double a = res[0]*0.5;
		double b = res[1]*0.5;
		double c = res[2]*0.5;
		double v_mvee = (4*Math.PI*(a*b*c))/3;
		double v = getVolumeByMesh();
		return v/v_mvee;
	}
	
	/*
	 * Note: This feature currently has no reference values and should not be used.
	 */
	@Deprecated
	private Double getAreaDensityByMinimumVolumeEnclosingEllipsoid(){
		return null;
	}
	
	/*
	 * This feature is also called solidity 
	 */
	private Double getVolumeDensityByConvexHull(){
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);
		Convex_Hull3DTool cht = new Convex_Hull3DTool();
		//will return 16 bit
		ImagePlus maskConvex = cht.run(mask);//keep iso voxel
		maskConvex = Utils.createMaskCopyAsGray8(maskConvex,this.label);
		double v = getVolumeByMesh();
		double v_convex = getVolumeByMesh(maskConvex);
		cht = null;
		mask = null;
		maskConvex =null;
		return v/v_convex;
	}
	
	private Double getAreaDensityByConvexHull(){
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);
		Convex_Hull3DTool cht = new Convex_Hull3DTool();
		ImagePlus maskConvex = cht.run(mask);//keep iso voxel
		maskConvex = Utils.createMaskCopyAsGray8(maskConvex,this.label);
		double a = getSurfaceAreaByMesh();
		double a_convex = getSurfaceAreaByMesh(maskConvex);
		cht = null;
		mask = null;
		maskConvex = null;
		return a/a_convex;
	}
	
	/*
	 * used : com.github.quickhull3d
	 * TriangleArray: illegal vertexCount
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private Double getVolumeDensityByConvexHull2() {
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		MCTriangulator mct = new MCTriangulator();
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<org.scijava.vecmath.Point3f> pointsf = mct.getTriangles(mask, threshold, channels, resamplingF);
		System.out.println(pointsf.size());
		CustomTriangleMesh mesh = new CustomTriangleMesh(pointsf);
		double v = mesh.getVolume();
		
		com.github.quickhull3d.Point3d[] points_hull = new com.github.quickhull3d.Point3d[pointsf.size()];
		int itr = 0;
		for(org.scijava.vecmath.Point3f pf : pointsf) {
			com.github.quickhull3d.Point3d pd = new Point3d(pf.x, pf.y, pf.z);
			points_hull[itr] = pd;
			itr++;
		}
		QuickHull3D hull = new QuickHull3D();
		hull.build(points_hull);
		hull.triangulate();
		com.github.quickhull3d.Point3d[] vertices = hull.getVertices();
		int[][] faces = hull.getFaces();
		System.out.println(vertices.length);//28
		System.out.println(faces.length);//52
		System.out.println(faces[0].length);//52
		//re-convert
		List<org.scijava.vecmath.Point3f> points_hull_ = new ArrayList<>();
		boolean first = true;
//		for(com.github.quickhull3d.Point3d vp : vertices) {
//			if(first) {
//				first = false;
//				continue;
//			}
//			org.scijava.vecmath.Point3f pf = new org.scijava.vecmath.Point3f((float)vp.x, (float)vp.y, (float)vp.z);
//			points_hull_.add(pf);
//		}
		for(int[] vp : faces) {
			if(first) {
				first = false;
				continue;
			}
			org.scijava.vecmath.Point3f pf = new org.scijava.vecmath.Point3f((float)vp[0], (float)vp[1], (float)vp[2]);
			points_hull_.add(pf);
		}
		CustomTriangleMesh hull_mesh = new CustomTriangleMesh(points_hull_);
		double vh = hull_mesh.getVolume();
		return v/vh;
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private Double getVolumeDensityByConvexHull3() {
		ImagePlus mask = Utils.createMaskCopyAsGray8(isoMask,this.label);
		int threshold = this.label-1;
		boolean[] channels = { true, false, false }; // r,g,b, but only used r because image is always binary 8 bit.
		MCTriangulator mct = new MCTriangulator();
		int resamplingF = 2; // 1 to N.
		@SuppressWarnings("unchecked")
		List<org.scijava.vecmath.Point3f> pointsf = mct.getTriangles(mask, threshold, channels, resamplingF);
		CustomTriangleMesh mesh = new CustomTriangleMesh(pointsf);
		double v = -1 * mesh.getVolume();
		System.out.println(v);//556
		@SuppressWarnings("rawtypes")
		RandomAccessibleInterval rai = ImageJFunctions.wrapReal(mask);
//		Mesh m = new DefaultMarchingCubes<>().calculate(rai);//Meshes.marchingCubes(rai);
		@SuppressWarnings("unchecked")
		net.imagej.mesh.Mesh m = Meshes.marchingCubes(rai,threshold);
		DefaultConvexHull3D hull = new DefaultConvexHull3D();
		net.imagej.mesh.Mesh m_hull = hull.calculate(m);
		List<org.scijava.vecmath.Point3f> hull_p = new ArrayList<>();
//		Iterator iter_v = m_hull.vertices().iterator();
		Iterator<Triangle> iter_t = m_hull.triangles().iterator();
		while(iter_t.hasNext()) {
//			Vertex p = (Vertex) iter_v.next();
//			hull_p.add(new Point3f(p.xf(), p.yf(), p.zf()));			
			Triangle p = (Triangle) iter_t.next();
			hull_p.add(new Point3f(p.v0xf(), p.v0yf(), p.v0zf()));	
			hull_p.add(new Point3f(p.v1xf(), p.v1yf(), p.v1zf()));	
			hull_p.add(new Point3f(p.v2xf(), p.v2yf(), p.v2zf()));	
		}
//		System.out.println(hull_p.size());//150
		CustomTriangleMesh mesh_hull = new CustomTriangleMesh(hull_p);
		double hv = mesh_hull.getVolume();
//		System.out.println(hv);//838
		return v/hv;
	}
	
	/**
	 * As simillar feature, the RawIntegratedDensity is exists. this is the same integrated intensity without density calibration.
	 * density was calibrated or no-calibrated is should be consider.
	 */
	private Double getIntegratedIntensity() {
		double[] voxels;
		if(this.voxels != null) {
			voxels = this.voxels;
		}else {
			voxels = Utils.getVoxels(orgImg, orgMask, this.label);
		}
		if(voxels == null || voxels.length == 0) {
			return 0d;
		}
		double sum = StatUtils.sum(voxels);
		int count = voxels.length;
		/*
		 * voxel count basis, it is also OK.
		 */
//		double vx = orgCal != null ? orgCal.pixelWidth : 1.0;
//		double vy = orgCal != null ? orgCal.pixelHeight : 1.0;
//		double vz = orgCal != null ? orgCal.pixelDepth : 1.0;
//		return count*(vx*vy*vz) * (sum/count);//1277
		/*
		 * mesh basis
		 */
		double res = getVolumeByMesh() * (sum/count);//1195
		//clean-up
		voxels = null;
		return res;
	}
	
	/**
	 * Should be use original image and mask. (NOT iso voxels.)
	 * take too long time...
	 */
	@SuppressWarnings("unused")
	private Double getMoransIIndex_IBSI() {
		ImagePlus img = orgImg;//IMPORTANT
		ImagePlus mask = orgMask;//IMPORTANT
		double vx = img.getCalibration().pixelWidth;
		double vy = img.getCalibration().pixelHeight;
		double vz = img.getCalibration().pixelDepth;
		double index = 0;
		double sumw = 0;
		if(this.voxels == null) {
			this.voxels = Utils.getVoxels(img, mask, label);
		}
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		HashMap<String, double[]> xyzMaskGeo = Utils.getRoiBoundingBoxInfo(mask, label, true);//axis aligned bb
		double[] aabbX = xyzMaskGeo.get("x");//0:min 1:max
		double[] aabbY = xyzMaskGeo.get("y");
		double[] aabbZ = xyzMaskGeo.get("z");
		
		int n = 0;
		double mu = StatUtils.mean(voxels);
		double sumsq = 0.0;
//		System.out.println(StatUtils.sumSq(voxels));//no subtract mean.
		for(int zi=(int)aabbZ[0]; zi<=(int)aabbZ[1] ;zi++) {
			for (int yi=(int)aabbY[0]; yi<=(int)aabbY[1]; yi++) {
				for (int xi=(int)aabbX[0]; xi<=(int)aabbX[1]; xi++) {
					int lbl_vali = (int)mask.getStack().getProcessor(zi+1).getPixelValue(xi, yi);
					if (lbl_vali == label) {
						double vi = img.getStack().getProcessor(zi+1).getPixelValue(xi, yi);
						sumsq += Math.pow(vi-mu,2);
						n++;
						// Look for pixels inside ROI
						for(int zj=(int)aabbZ[0]; zj<=(int)aabbZ[1]; zj++) {
							for (int yj=(int)aabbY[0]; yj<=(int)aabbY[1]; yj++) {
								for (int xj=(int)aabbX[0]; xj<=(int)aabbX[1]; xj++) {
									int lbl_valj = (int)mask.getStack().getProcessor(zj+1).getPixelValue(xj, yj);
									if (lbl_valj == label && ((xi!=xj)||(yi!=yj)||(zi!=zj))) {
										// inside roi and not voxel itself
										double vj = img.getStack().getProcessor(zj+1).getPixelValue(xj, yj);
										double distance = Math.sqrt(Math.pow(xi*vx - xj*vx, 2) + Math.pow(yi*vy - yj*vy, 2) + Math.pow(zi*vz - zj*vz, 2));
										sumw += 1./distance;
										index += (1./distance) * (vi - mu) * (vj - mu);
									}
								}
							}
						}
					}
				}
			}
		}
		index = (n / sumw) * (index / sumsq);
		return Double.valueOf(index);
	}
	
	/**
	 * Should be use original image and mask. (NOT iso voxels.)
	 * take too long time...
	 */
	@SuppressWarnings("unused")
	private Double getGearysCMeasure() {
		ImagePlus img = orgImg;//IMPORTANT, do not use iso voxel.
		ImagePlus mask = orgMask;//IMPORTANT
		double vx = img.getCalibration().pixelWidth;
		double vy = img.getCalibration().pixelHeight;
		double vz = img.getCalibration().pixelDepth;
		double index = 0;
		double sumw = 0;
		if(this.voxels == null) {
			voxels = Utils.getVoxels(img, mask, label);
		}
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		HashMap<String, double[]> xyzMaskGeo = Utils.getRoiBoundingBoxInfo(mask, label, true);//axis aligned bb
		double[] aabbX = xyzMaskGeo.get("x");//0:min 1:max
		double[] aabbY = xyzMaskGeo.get("y");
		double[] aabbZ = xyzMaskGeo.get("z");
		
		int n = 0;//if not same voxels.length, error.
		double mu = StatUtils.mean(voxels);
		double sumsq = 0.0;
//		System.out.println(StatUtils.sumSq(voxels));//no subtract mean.DO NOT USE
		for(int zi=(int)aabbZ[0]; zi<=(int)aabbZ[1] ;zi++) {
			for (int yi=(int)aabbY[0]; yi<=(int)aabbY[1]; yi++) {
				for (int xi=(int)aabbX[0]; xi<=(int)aabbX[1]; xi++) {
//					img.setPosition(zi+1);//IMPORTANT, set it here.(not in zi loop.)
//					mask.setPosition(zi+1);//IMPORTANT
					int lbl_vali = (int)mask.getStack().getProcessor(zi+1).getPixelValue(xi, yi);
					if (lbl_vali == label) {
						double vi = img.getStack().getProcessor(zi+1).getPixelValue(xi, yi);
						sumsq += Math.pow(vi-mu,2);
						n++;
						// Look for pixels inside ROI
						for(int zj=(int)aabbZ[0]; zj<=(int)aabbZ[1]; zj++) {
							for (int yj=(int)aabbY[0]; yj<=(int)aabbY[1]; yj++) {
								for (int xj=(int)aabbX[0]; xj<=(int)aabbX[1]; xj++) {
//									img.setPosition(zj+1);//IMPORTANT
//									mask.setPosition(zj+1);//IMPORTANT
									int lbl_valj = (int)mask.getStack().getProcessor(zj+1).getPixelValue(xj, yj);
									if (lbl_valj == label && ((xi!=xj)||(yi!=yj)||(zi!=zj))) {
										// inside roi and not voxel itself
										double vj = img.getStack().getProcessor(zj+1).getPixelValue(xj, yj);
										double distance = Math.sqrt(Math.pow(xi*vx - xj*vx, 2) + Math.pow(yi*vy - yj*vy, 2) + Math.pow(zi*vz - zj*vz, 2));
										sumw += 1./distance;
										index += (1./distance) * Math.pow((vi - vj),2);
									}
								}
							}
						}
					}
				}
			}
		}
		index = ((n-1) / (2*sumw)) * (index / sumsq);
		return Double.valueOf(index);
	}
	
	/**
	 * Should be use original image and mask. (NOT iso voxels.)
	 * faster version
	 * @return
	 */
	private Double getMoransIIndex_IBSI2() {
		ImagePlus img = orgImg;//IMPORTANT
		ImagePlus mask = orgMask;//IMPORTANT
		int w = img.getWidth();
		int h = img.getHeight();
		int s = img.getNSlices();
		int slice = w*h;
		double vx = img.getCalibration().pixelWidth;
		double vy = img.getCalibration().pixelHeight;
		double vz = img.getCalibration().pixelDepth;
		double index = 0;
		double sumw = 0;
		if(this.voxels == null) {
			this.voxels = Utils.getVoxels(img, mask, label);
		}
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		HashMap<String, double[]> xyzMaskGeo = Utils.getRoiBoundingBoxInfo(mask, label, true);//axis aligned bb
		double[] aabbX = xyzMaskGeo.get("x");//0:min 1:max
		double[] aabbY = xyzMaskGeo.get("y");
		double[] aabbZ = xyzMaskGeo.get("z");
		
		int n = 0;
		double mu = StatUtils.mean(voxels);
		double sumsq = 0.0;
		
		//prepare pixels 1d-array
		float[] pixels = new float[w*h*s];
		float[] pixels_m = new float[pixels.length];
		int itr = 0;
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = img.getStack().getProcessor(z+1);
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					pixels[itr] = ip.getf(x, y);
					pixels_m[itr] = mp.getf(x, y);
					itr+=1;
				}
			}
		}
		
		for(int zi=(int)aabbZ[0]; zi<=(int)aabbZ[1] ;zi++) {
			for (int yi=(int)aabbY[0]; yi<=(int)aabbY[1]; yi++) {
				for (int xi=(int)aabbX[0]; xi<=(int)aabbX[1]; xi++) {
					int i_loc = slice * zi + yi * w + xi;
					int lbl_vali = (int)pixels_m[i_loc];
					if (lbl_vali == label) {
						double vi = pixels[i_loc];
						sumsq += Math.pow(vi-mu,2);
						n++;
						// Look for pixels inside ROI
						for(int zj=(int)aabbZ[0]; zj<=(int)aabbZ[1]; zj++) {
							for (int yj=(int)aabbY[0]; yj<=(int)aabbY[1]; yj++) {
								for (int xj=(int)aabbX[0]; xj<=(int)aabbX[1]; xj++) {
									int j_loc = slice * zj + yj * w + xj;
									int lbl_valj = (int)pixels_m[j_loc];
									if (lbl_valj == label && ((xi!=xj)||(yi!=yj)||(zi!=zj))) {
										// inside roi and not voxel itself
										double vj = pixels[j_loc];
										double distance = Math.sqrt(Math.pow(xi*vx - xj*vx, 2) + Math.pow(yi*vy - yj*vy, 2) + Math.pow(zi*vz - zj*vz, 2));
										sumw += 1./distance;
										index += (1./distance) * (vi - mu) * (vj - mu);
									}
								}
							}
						}
					}
				}
			}
		}
		index = (n / sumw) * (index / sumsq);
		return Double.valueOf(index);
	}
	
	
	/**
	 * Should be use original image and mask. (NOT iso voxels.)
	 * faster version
	 * @return
	 */
	private Double getGearysCMeasure2() {
		ImagePlus img = orgImg;//IMPORTANT, do not use iso voxel.
		ImagePlus mask = orgMask;//IMPORTANT
		double vx = img.getCalibration().pixelWidth;
		double vy = img.getCalibration().pixelHeight;
		double vz = img.getCalibration().pixelDepth;
		int w = img.getWidth();
		int h = img.getHeight();
		int s = img.getNSlices();
		int slice = w*h;
		
		double index = 0;
		double sumw = 0;
		if(this.voxels == null) {
			voxels = Utils.getVoxels(img, mask, label);
		}
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		HashMap<String, double[]> xyzMaskGeo = Utils.getRoiBoundingBoxInfo(mask, label, true);//axis aligned bb
		double[] aabbX = xyzMaskGeo.get("x");//0:min 1:max
		double[] aabbY = xyzMaskGeo.get("y");
		double[] aabbZ = xyzMaskGeo.get("z");
		
		int n = 0;//if not same voxels.length, error.
		double mu = StatUtils.mean(voxels);
		double sumsq = 0.0;
		
		//prepare pixels 1d-array
		float[] pixels = new float[w * h * s];
		float[] pixels_m = new float[pixels.length];
		int itr = 0;		
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = img.getStack().getProcessor(z+1);
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					pixels[itr] = ip.getf(x, y);
					pixels_m[itr] = mp.getf(x, y);
					itr+=1;
				}
			}
		}
		
		for(int zi=(int)aabbZ[0]; zi<=(int)aabbZ[1] ;zi++) {
			for (int yi=(int)aabbY[0]; yi<=(int)aabbY[1]; yi++) {
				for (int xi=(int)aabbX[0]; xi<=(int)aabbX[1]; xi++) {
					int i_loc = slice*zi + yi*w + xi;
					int lbl_vali = (int)pixels_m[i_loc];
					if (lbl_vali == label) {
						double vi = pixels[i_loc];
						sumsq += Math.pow(vi-mu,2);
						n++;
						// Look for pixels inside ROI
						for(int zj=(int)aabbZ[0]; zj<=(int)aabbZ[1]; zj++) {
							for (int yj=(int)aabbY[0]; yj<=(int)aabbY[1]; yj++) {
								for (int xj=(int)aabbX[0]; xj<=(int)aabbX[1]; xj++) {
									int j_loc = slice*zj + yj*w + xj;
									int lbl_valj = (int)pixels_m[j_loc];
									if (lbl_valj == label && ((xi!=xj)||(yi!=yj)||(zi!=zj))) {
										// inside roi and not voxel itself
										double vj = pixels[j_loc];
										double distance = Math.sqrt(Math.pow(xi*vx - xj*vx, 2) + Math.pow(yi*vy - yj*vy, 2) + Math.pow(zi*vz - zj*vz, 2));
										sumw += 1./distance;
										index += (1./distance) * Math.pow((vi - vj),2);
									}
								}
							}
						}
					}
				}
			}
		}
		index = ((n-1) / (2*sumw)) * (index / sumsq);
		return Double.valueOf(index);
	}
}

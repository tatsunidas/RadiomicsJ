package fiji.plugins;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Thresholder;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import mcib3d.geom.*;
import mcib3d.image3d.ImageInt;
import mcib3d.utils.ArrayUtil;

import java.text.NumberFormat;
import java.util.HashMap;

import RadiomicsJ.Utils;

/*
 * https://github.com/mcib3d/mcib3d-plugins/blob/master/src/main/java/mcib_plugins/Ellipsoids_3D.java
 */
public class Ellipsoid_3DTool {

	ImagePlus imp;
	float rad;

	/**
	 * Main processing method for the Axes3D_ object
	 * 
	 */
	public HashMap<String, Double> run(ImagePlus imp, boolean drawVectors, boolean drawOriented) {
		Calibration cal = imp.getCalibration();
		double resXY = 1.0;
		double resZ = 1.0;
		String unit = "pix";
		if (cal != null) {
			if (cal.scaled()) {
				resXY = cal.pixelWidth;
				resZ = cal.pixelDepth;
				unit = cal.getUnits();
			}
		}

		// drawing of ellipses
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		ObjectCreator3D ellipsoid = new ObjectCreator3D(w, h, s);
		ellipsoid.setResolution(resXY, resZ, unit);
		// drawing of main direction vectors
		ObjectCreator3D vectors1 = new ObjectCreator3D(w, h, s);
		vectors1.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors2 = new ObjectCreator3D(w, h, s);
		vectors2.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors3 = new ObjectCreator3D(w, h, s);
		vectors3.setResolution(resXY, resZ, unit);
		// drawing of oriented contours
		ObjectCreator3D oriC = new ObjectCreator3D(w, h, s);
		oriC.setResolution(resXY, resZ, unit);
		// all objects from count masks
		ImageInt imageInt = ImageInt.wrap(imp);
		Objects3DPopulation pop = new Objects3DPopulation(imageInt);
		HashMap<String, Double> result = new HashMap<String, Double>();
		for (int ob = 0; ob < pop.getNbObjects(); ob++) {
			Object3D obj = pop.getObject(ob);
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);
			if (obj.getVolumePixels() > 0) {
				// obj.computeMoments();
				double[] axy = new double[3];
				double[] axz = new double[3];
				double[] ayz = new double[3];
				for (int i = 0; i < 3; i++) {
					Vector3D V = obj.getVectorAxis(i);
//					System.out.println(ob + ": Vector " + i + " : " + V);
//					System.out.println(ob + ": Value  " + i + " : " + nf.format(obj.getValueAxis(i)));
//					System.out.println(ob + ": Value  sqrt " + i + " : " + nf.format(Math.sqrt(obj.getValueAxis(i))));
					axy[i] = V.anglePlaneDegrees(0, 0, 1, 0);
					axz[i] = V.anglePlaneDegrees(0, 1, 0, 0);
					ayz[i] = V.anglePlaneDegrees(1, 0, 0, 0);
//					System.out.println("Angle " + i + " with plane XY " + axy[i]);
//					System.out.println("Angle " + i + " with plane XZ " + axz[i]);
//					System.out.println("Angle " + i + " with plane YZ " + ayz[i]);
				}
				Vector3D V = obj.getVectorAxis(2); // main axis
				Vector3D W = obj.getVectorAxis(1);
				Vector3D U = obj.getVectorAxis(0);
				// obj.computeContours();
				// double r1 = obj.getDistCenterMax();
				double r1 = obj.getRadiusMoments(2);
				double rad1 = r1;
				double rad2 = Double.NaN;
				if (!Double.isNaN(obj.getMainElongation())) {
					rad2 = rad1 / obj.getMainElongation();
				}
				double rad3 = Double.NaN;
				if (!Double.isNaN(obj.getMedianElongation())) {
					rad3 = rad2 / obj.getMedianElongation();
				}
//				if ((!Double.isNaN(rad2)) && (!Double.isNaN(rad3))) {
//					System.out.println(ob + ": radii=" + nf.format(rad1) + " " + nf.format(rad2) + " " + nf.format(rad3));
//				} else {
//					System.out.println(ob + ": radii=" + nf.format(rad1) + " " + nf.format(rad2) + " " + "NaN");
//				}
//				System.out.println("Max :" + r1);
				// dist in first axis
//				double d1 = obj.distPixelBorderUnit((int) obj.getCenterX(), (int) obj.getCenterY(),
//						(int) obj.getCenterZ(), obj.getVectorAxis(2));
//				double d2 = obj.distPixelBorderUnit((int) obj.getCenterX(), (int) obj.getCenterY(),
//						(int) obj.getCenterZ(), obj.getVectorAxis(2).multiply(-1));
//				System.out.println("major from distance " + d1 + " " + d2);
				// center
//				System.out.println("Center : " + obj.getCenterX() + " " + obj.getCenterY() + " " + obj.getCenterZ());
				// draw ellipsoid
				int val = obj.getValue();
				ellipsoid.createEllipsoidAxesUnit(obj.getCenterX() * resXY, obj.getCenterY() * resXY,
						obj.getCenterZ() * resZ, rad1, rad2, rad3, val, V, W, false);
				// draw line for direction vectors
				Vector3D Vec = obj.getCenterAsVectorUnit();
				Vector3D end = Vec.add(obj.getVectorAxis(2), 1, rad1);
				vectors1.createLineUnit(Vec, end, val, 1);
				Vec = obj.getCenterAsVectorUnit();
				end = Vec.add(obj.getVectorAxis(1), 1, rad1);
				vectors2.createLineUnit(Vec, end, val, 1);
				Vec = obj.getCenterAsVectorUnit();
				end = Vec.add(obj.getVectorAxis(0), 1, rad1);
				vectors3.createLineUnit(Vec, end, val, 1);

				// The two poles as Feret
				System.out.println("Computing feret and poles");
				Voxel3D Feret1 = obj.getFeretVoxel1();
				Voxel3D Feret2 = obj.getFeretVoxel2();
				System.out.println("Pole1 as Feret 1 : " + Feret1);
				System.out.println("Pole2 as Feret 2 : " + Feret2);
				System.out.println("Pole1 as Feret 1 (calibrated) : " + Feret1.getX() * resXY + " " + Feret1.getY() * resXY + " "
						+ Feret1.getZ() * resZ);
				System.out.println("Pole2 as Feret 2 (calibrated) : " + Feret2.getX() * resXY + " " + Feret2.getY() * resXY + " "
						+ Feret2.getZ() * resZ);

				// The two poles as Feret of ellipsoid
				Object3D ell = new Object3DVoxels(ellipsoid.getImageHandler(), val);
				// ell.computeContours();
				Voxel3D Ell1 = null;
				Voxel3D Ell2 = null;
				if (ell.isEmpty()) {
					System.out.println("Cannot compute ellipsoid.");
				}else {
					Ell1 = ell.getFeretVoxel1();
					Ell2 = ell.getFeretVoxel2();
					System.out.println("Pole1 as ellipsoid 1 : " + Ell1);
					System.out.println("Pole2 as ellipsoid 2 : " + Ell2);
					System.out.println("Pole1 as ellipsoid 1 (calibrated) : " + Ell1.getX() * resXY + " " + Ell1.getY() * resXY
							+ " " + Ell1.getZ() * resZ);
					System.out.println("Pole2 as ellipsoid 2 (calibrated) : " + Ell2.getX() * resXY + " " + Ell2.getY() * resXY
							+ " " + Ell2.getZ() * resZ);
				}

				// ORIENTED BB
				oriC.drawVoxels(obj.getBoundingOriented());

				// BOUNDING BOX + ORIENTED //
				ArrayUtil tab = new ArrayUtil(obj.getBoundingBox());
				System.out.println("BB  : " + tab.toString());//(Xmin, Xmax, Ymin, Ymax, Zmin, Zmax)
				// VOLUMES
				System.out.println("Volumes:");
				System.out.println("obj:" + obj.getVolumeUnit() + " units");
				System.out.println("ell:" + obj.getVolumeEllipseUnit() + " unit");
				System.out.println("obj:" + obj.getVolumePixels() + " pixels");
				System.out.println("bb:" + obj.getVolumeBoundingBoxPixel() + " pixels");
				System.out.println("bbo:" + obj.getVolumeBoundingBoxOrientedPixel() + " pixels");

				// RESULTS
				// center
				result.put("Cx(pix)", obj.getCenterX());
				result.put("Cy(pix)", obj.getCenterY());
				result.put("Cz(pix)", obj.getCenterZ());
				// third axis
				result.put("Vx0(pix)", U.getX());
				result.put("Vy0(pix)", U.getY());
				result.put("Vz0(pix)", U.getZ());
				// second axis
				result.put("Vx1(pix)", W.getX());
				result.put("Vy1(pix)", W.getY());
				result.put("Vz1(pix)", W.getZ());
				// main axis
				result.put("Vx2(pix)", V.getX());
				result.put("Vy2(pix)", V.getY());
				result.put("Vz2(pix)", V.getZ());
				// radii
				result.put("R1(unit)", rad1);
				result.put("R2(unit)", rad2);
				result.put("R3(unit)", rad3);
				// angles
				result.put("XY0(deg)", axy[0]);
				result.put("XZ0(deg)", axz[0]);
				result.put("YZ0(deg)", ayz[0]);
				result.put("XY1(deg)", axy[1]);
				result.put("XZ1(deg)", axz[1]);
				result.put("YZ1(deg)", ayz[1]);
				result.put("XY2(deg)", axy[2]);
				result.put("XZ2(deg)", axz[2]);
				result.put("YZ2(deg)", ayz[2]);
				// volumes
				result.put("Vobj(pix)", (double)obj.getVolumePixels());
				result.put("Vobj(unit)", obj.getVolumeUnit());
				result.put("Vell(unit)", obj.getVolumeEllipseUnit());
				result.put("RatioVobjVell", obj.getRatioEllipsoid());
				result.put("Vbb(pix)", obj.getVolumeBoundingBoxPixel());
				result.put("Vbbo(pix)", obj.getVolumeBoundingBoxOrientedPixel());
				// poles obj
				result.put("Feret1.X", Feret1.getX());
				result.put("Feret1.Y", Feret1.getY());
				result.put("Feret1.Z", Feret1.getZ());
				result.put("Feret2.X", Feret2.getX());
				result.put("Feret2.Y", Feret2.getY());
				result.put("Feret2.Z", Feret2.getZ());
				// poles obj
				if ((Ell1 != null) && (Ell2 != null)) {
					result.put("Pole1.X", Ell1.getX());
					result.put("Pole1.Y", Ell1.getY());
					result.put("Pole1.Z", Ell1.getZ());
					result.put("Pole2.X", Ell2.getX());
					result.put("Pole2.Y", Ell2.getY());
					result.put("Pole2.Z", Ell2.getZ());
				}
			}
		}

		// draw ellipsoids
		ImagePlus plus = new ImagePlus("Ellipsoids", ellipsoid.getStack());
		if (cal != null) {
			plus.setCalibration(cal);
		}
		plus.show();

		// draw vectors
		if (drawVectors) {
			ImagePlus plusV1 = new ImagePlus("Vectors1 (Max)", vectors1.getStack());
			ImagePlus plusV2 = new ImagePlus("Vectors2 (Middle)", vectors2.getStack());
			ImagePlus plusV3 = new ImagePlus("Vectors3 (Min)", vectors3.getStack());
			if (cal != null) {
				plusV1.setCalibration(cal);
				plusV2.setCalibration(cal);
				plusV3.setCalibration(cal);
			}
			plusV1.show();
			plusV2.show();
			plusV3.show();
		}

		// draw oriented contours
		if (drawOriented) {
			ImagePlus plus3 = new ImagePlus("Oriented Contours", oriC.getStack());
			if (cal != null) {
				plus3.setCalibration(cal);
			}
			plus3.show();
		}
		return result;
	}
	
	/*
	 * non oriented
	 */
	public int[] getBoundingBox(ImagePlus imp) {
		Calibration cal = imp.getCalibration();
		double resXY = 1.0;
		double resZ = 1.0;
		String unit = "pix";
		if (cal != null) {
			if (cal.scaled()) {
				resXY = cal.pixelWidth;
				resZ = cal.pixelDepth;
				unit = cal.getUnits();
			}
		}

		// drawing of ellipses
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		ObjectCreator3D ellipsoid = new ObjectCreator3D(w, h, s);
		ellipsoid.setResolution(resXY, resZ, unit);
		// drawing of main direction vectors
		ObjectCreator3D vectors1 = new ObjectCreator3D(w, h, s);
		vectors1.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors2 = new ObjectCreator3D(w, h, s);
		vectors2.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors3 = new ObjectCreator3D(w, h, s);
		vectors3.setResolution(resXY, resZ, unit);
		// drawing of oriented contours
		ObjectCreator3D oriC = new ObjectCreator3D(w, h, s);
		oriC.setResolution(resXY, resZ, unit);
		// all objects from count masks
		ImageInt imageInt = ImageInt.wrap(imp);
		Objects3DPopulation pop = new Objects3DPopulation(imageInt);
		if(pop.getNbObjects()>1) {
			System.out.println("This 3D obj contains multiple blob volumes, return null...");
			return null;
		}
		for (int ob = 0; ob < pop.getNbObjects(); ob++) {
			Object3D obj = pop.getObject(ob);
			if (obj.getVolumePixels() > 0) {
				// BOUNDING BOX without rotate//
				return obj.getBoundingBox();//(Xmin, Xmax, Ymin, Ymax, Zmin, Zmax)
			}
		}
		return null;
	}
	
	public double[] getOrientedBoundingBox(ImagePlus imp) {
		Calibration cal = imp.getCalibration();
		double resXY = 1.0;
		double resZ = 1.0;
		String unit = "pix";
		if (cal != null) {
			if (cal.scaled()) {
				resXY = cal.pixelWidth;
				resZ = cal.pixelDepth;
				unit = cal.getUnits();
			}
		}

		// drawing of ellipses
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		ObjectCreator3D ellipsoid = new ObjectCreator3D(w, h, s);
		ellipsoid.setResolution(resXY, resZ, unit);
		// drawing of main direction vectors
		ObjectCreator3D vectors1 = new ObjectCreator3D(w, h, s);
		vectors1.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors2 = new ObjectCreator3D(w, h, s);
		vectors2.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors3 = new ObjectCreator3D(w, h, s);
		vectors3.setResolution(resXY, resZ, unit);
		// drawing of oriented contours
		ObjectCreator3D oriC = new ObjectCreator3D(w, h, s);
		oriC.setResolution(resXY, resZ, unit);
		// all objects from count masks
		ImageInt imageInt = ImageInt.wrap(imp);
		Objects3DPopulation pop = new Objects3DPopulation(imageInt);
		if(pop.getNbObjects()>1) {
			System.out.println("This 3D obj contains multiple blob volumes, return null...");
			return null;
		}
		for (int ob = 0; ob < pop.getNbObjects(); ob++) {
			Object3D obj = pop.getObject(ob);
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);

			if (obj.getVolumePixels() > 0) {
				// ORIENTED BB
				double minx = Double.POSITIVE_INFINITY;
		        double maxx = Double.NEGATIVE_INFINITY;
		        double miny = Double.POSITIVE_INFINITY;
		        double maxy = Double.NEGATIVE_INFINITY;
		        double minz = Double.POSITIVE_INFINITY;
		        double maxz = Double.NEGATIVE_INFINITY;
		        for (Voxel3D vox : obj.getBoundingOriented()) {
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
		        double[] bbo = {minx*resXY, maxx*resXY, miny*resXY, maxy*resXY, minz*resZ, maxz*resZ};
		        return bbo;
			}
		}
		return null;
	}
	
	public double[] getEllipsoidPoles(ImagePlus imp) {
		Calibration cal = imp.getCalibration();
		double resXY = 1.0;
		double resZ = 1.0;
		String unit = "pix";
		if (cal != null) {
			if (cal.scaled()) {
				resXY = cal.pixelWidth;
				resZ = cal.pixelDepth;
				unit = cal.getUnits();
			}
		}

		// drawing of ellipses
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		ObjectCreator3D ellipsoid = new ObjectCreator3D(w, h, s);
		ellipsoid.setResolution(resXY, resZ, unit);
		// drawing of main direction vectors
		ObjectCreator3D vectors1 = new ObjectCreator3D(w, h, s);
		vectors1.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors2 = new ObjectCreator3D(w, h, s);
		vectors2.setResolution(resXY, resZ, unit);
		ObjectCreator3D vectors3 = new ObjectCreator3D(w, h, s);
		vectors3.setResolution(resXY, resZ, unit);
		// drawing of oriented contours
		ObjectCreator3D oriC = new ObjectCreator3D(w, h, s);
		oriC.setResolution(resXY, resZ, unit);
		// all objects from count masks
		ImageInt imageInt = ImageInt.wrap(imp);
		Objects3DPopulation pop = new Objects3DPopulation(imageInt);
		if(pop.getNbObjects()>1) {
			System.out.println("This 3D obj contains multiple blob volumes, return null...");
			return null;
		}
		for (int ob = 0; ob < pop.getNbObjects(); ob++) {
			Object3D obj = pop.getObject(ob);
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);
			if (obj.getVolumePixels() > 0) {
				double[] axy = new double[3];
				double[] axz = new double[3];
				double[] ayz = new double[3];
				//angles
				for (int i = 0; i < 3; i++) {
					Vector3D V = obj.getVectorAxis(i);
					axy[i] = V.anglePlaneDegrees(0, 0, 1, 0);
					axz[i] = V.anglePlaneDegrees(0, 1, 0, 0);
					ayz[i] = V.anglePlaneDegrees(1, 0, 0, 0);
				}
				Vector3D V = obj.getVectorAxis(2); // main axis
				
//				System.out.println(V.x);
//				System.out.println(V.y);
//				System.out.println(V.z);
//				System.out.println(4*Math.sqrt(obj.getValueAxis(2)));
				
				Vector3D W = obj.getVectorAxis(1);
//				Vector3D U = obj.getVectorAxis(0);
				double r1 = obj.getRadiusMoments(2);
				double rad1 = r1;
				double rad2 = Double.NaN;
				if (!Double.isNaN(obj.getMainElongation())) {
					rad2 = rad1 / obj.getMainElongation();
				}
				double rad3 = Double.NaN;
				if (!Double.isNaN(obj.getMedianElongation())) {
					rad3 = rad2 / obj.getMedianElongation();
				}
				// dist in first axis
//				double d1 = obj.distPixelBorderUnit((int) obj.getCenterX(), (int) obj.getCenterY(),
//						(int) obj.getCenterZ(), obj.getVectorAxis(2));
//				double d2 = obj.distPixelBorderUnit((int) obj.getCenterX(), (int) obj.getCenterY(),
//						(int) obj.getCenterZ(), obj.getVectorAxis(2).multiply(-1));
				// ellipsoid
				int val = obj.getValue();
				ellipsoid.createEllipsoidAxesUnit(obj.getCenterX() * resXY, obj.getCenterY() * resXY,
						obj.getCenterZ() * resZ, rad1, rad2, rad3, val, V, W, false);
				// The two poles as Feret
//				Voxel3D Feret1 = obj.getFeretVoxel1();
//				Voxel3D Feret2 = obj.getFeretVoxel2();
//				System.out.println("Feret diameter(maximum): "+obj.getFeret());
//				System.out.println("Pole1 as Feret 1 : " + Feret1);
//				System.out.println("Pole2 as Feret 2 : " + Feret2);
//				System.out.println("Pole1 as Feret 1 (calibrated) : " + Feret1.getX() * resXY + " " + Feret1.getY() * resXY + " "
//						+ Feret1.getZ() * resZ);
//				System.out.println("Pole2 as Feret 2 (calibrated) : " + Feret2.getX() * resXY + " " + Feret2.getY() * resXY + " "
//						+ Feret2.getZ() * resZ);

				// The two poles as Feret of ellipsoid
				Object3D ell = new Object3DVoxels(ellipsoid.getImageHandler(), val);
				// ell.computeContours();
				Voxel3D Ell1 = null;
				Voxel3D Ell2 = null;
				if (ell.isEmpty()) {
					System.out.println("Cannot compute ellipsoid.");
				}else {
					Ell1 = ell.getFeretVoxel1();
					Ell2 = ell.getFeretVoxel2();
					System.out.println("Pole1: " + Ell1);
					System.out.println("Pole2: " + Ell2);
					System.out.println("Pole1 (calibrated) : " + Ell1.getX() * resXY + " " + Ell1.getY() * resXY
							+ " " + Ell1.getZ() * resZ);
					System.out.println("Pole2 (calibrated) : " + Ell2.getX() * resXY + " " + Ell2.getY() * resXY
							+ " " + Ell2.getZ() * resZ);
					return new double[] {Ell1.getX()*resXY, Ell2.getX()*resXY, Ell1.getY()*resXY, Ell2.getY()*resXY, Ell1.getZ()*resZ, Ell2.getZ()*resZ};//(e1x, e2x, e1y, e2y, e1z, e2z)
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 
	 * @param imp : 8-bit images.
	 * @param fillHoles : be careful, check mask label and radiomicsj.label values are the same.
	 * @return
	 */
	public double[] getMajorMinorLeastAxisLength(ImagePlus imp, boolean fillHoles, int label) {
		Calibration cal = imp.getCalibration();
		double resXY = 1.0;
		double resZ = 1.0;
		String unit = "pix";
		if (cal != null) {
			if (cal.scaled()) {
				resXY = cal.pixelWidth;
				resZ = cal.pixelDepth;
				unit = cal.getUnits();
			}
		}

		// drawing of ellipses
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		ObjectCreator3D ellipsoid = new ObjectCreator3D(w, h, s);
		ellipsoid.setResolution(resXY, resZ, unit);
		
		/*
		 * fill holes
		 */
		ImagePlus temp = Utils.createMaskCopyAsGray8(imp,label);
		if(fillHoles) {
			int foreground = label;
			int background = 0;
			for(int z=0;z<temp.getNSlices();z++) {
				temp.setSlice(z+1);
				ImageProcessor ip = temp.getProcessor();
				Thresholder.setMethod("IsoData");
				ip.setAutoThreshold("IsoData");
				ip = Thresholder.createMask(temp);
				ip.invert();
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
		        temp.setProcessor(ip);
			}
		}
		
		// all objects from count masks
		ImageInt imageInt = ImageInt.wrap(temp);
		Objects3DPopulation pop = new Objects3DPopulation(imageInt);
		if(pop.getNbObjects()>1) {
			System.out.println("This 3D obj contains multiple blob volumes, return null...");
			return null;
		}
		for (int ob = 0; ob < pop.getNbObjects(); ob++) {
			Object3D obj = pop.getObject(ob);
			if (obj.getVolumePixels() > 0) {
//				Object3DVoxels obj_3d = obj.getEllipsoid();
				Object3DVoxels obj_3d = (Object3DVoxels)obj;
				
//				double lamdaMajor = obj_3d.getRadiusMoments(2);// main axis
//				double lamdaMinor = obj_3d.getRadiusMoments(1);// minor or least axis
//				double lamdaLeast = obj_3d.getRadiusMoments(0);// minor or least axis
				
				double lamdaMajor = obj_3d.getValueAxis(2);// main axis
				double lamdaMinor = obj_3d.getValueAxis(1);// minor or least axis
				double lamdaLeast = obj_3d.getValueAxis(0);// minor or least axis
				if(lamdaMinor < lamdaLeast) {
					lamdaMinor = lamdaLeast;
					lamdaLeast = lamdaMinor;
				}
				double major_al = 4*Math.sqrt(lamdaMajor);//major axis length
				double minor_al = 4*Math.sqrt(lamdaMinor);//minor axis length
				double least_al = 4*Math.sqrt(lamdaLeast);//least axis length
				
				//apache, same result Math.sqrt()
//				double major_al2 = 4* new Sqrt().value(lamdaMajor);//major axis length
//				double minor_al2 = 4*new Sqrt().value(lamdaMinor);//minor axis length
//				double least_al2 = 4*new Sqrt().value(lamdaLeast);//least axis length
				
				return new double[] { major_al,  minor_al, least_al};
			}
		}
		return null;
	}
	
}

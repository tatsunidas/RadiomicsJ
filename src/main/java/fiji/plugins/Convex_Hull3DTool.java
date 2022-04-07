package fiji.plugins;

import ij.ImagePlus;
import ij.measure.Calibration;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;

/**
 * plugin to compute the 3D hull, based on quickHull
 *
 * @author Thomas BOUDIER
 * @author tatsunidas
 * @created avril 2022
 */
public class Convex_Hull3DTool {

    /**
     * Main processing method for the plugin
     *
     * @param ip image to process, must be labelled
     */
    @SuppressWarnings("deprecation")
	public ImagePlus run(ImagePlus imp) {
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
        // no need calibration for computing hull
        ImageInt ima = ImageInt.wrap(imp.duplicate());
        ima.setScale(1, 1, "pix");
        //drawing of hulls
        ObjectCreator3D hulls = new ObjectCreator3D(ima.sizeX, ima.sizeY, ima.sizeZ);
        hulls.setResolution(resXY, resZ, unit);
        Object3DVoxels obj;
        // all objects from count masks
        int valmin = (int) ima.getMinAboveValue(0);
        int valmax = (int) ima.getMax();
        for (int val = valmin; val <= valmax; val++) {
            if (!ima.hasOneValue(val)) continue;
            obj = new Object3DVoxels(ima, val);
            if (obj.getVolumePixels() > 0) {
                Object3D objC = obj.getConvexObject();
                objC.draw(hulls, val);
            }
        }
        ImageHandler imageHandler = hulls.getImageHandler();
        if (cal != null) {
            imageHandler.setCalibration(cal);
        }
        ImagePlus plus = imageHandler.getImagePlus();
        plus.setCalibration(cal);
        plus.setSlice(1);
        plus.setDisplayRange(0, valmax);
        plus.setTitle("convex_Hull");
        return plus;
    }
}

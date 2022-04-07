package RadiomicsJ;

import java.util.HashMap;

import ij.ImagePlus;

/**
 * Diagnostics-initial image	Image dimension x
 * Diagnostics-initial image	Image dimension y
 * Diagnostics-initial image	Image dimension z
 * Diagnostics-initial image	Voxel dimension x
 * Diagnostics-initial image	Voxel dimension y
 * Diagnostics-initial image	Voxel dimension z
 * Diagnostics-initial image	Mean intensity
 * Diagnostics-initial image	Minimum intensity
 * Diagnostics-initial image	Maximum intensity
 * Diagnostics-interpolated image	Image dimension x
 * Diagnostics-interpolated image	Image dimension y
 * Diagnostics-interpolated image	Image dimension z
 * Diagnostics-interpolated image	Voxel dimension x
 * Diagnostics-interpolated image	Voxel dimension y
 * Diagnostics-interpolated image	Voxel dimension z
 * Diagnostics-interpolated image	Mean intensity
 * Diagnostics-interpolated image	Minimum intensity
 * Diagnostics-interpolated image	Maximum intensity
 * Diagnostics-initial ROI	Int. mask dimension x
 * Diagnostics-initial ROI	Int. mask dimension y
 * Diagnostics-initial ROI	Int. mask dimension z
 * Diagnostics-initial ROI	Int. mask bounding box dimension x
 * Diagnostics-initial ROI	Int. mask bounding box dimension y
 * Diagnostics-initial ROI	Int. mask bounding box dimension z
 * Diagnostics-initial ROI	Morph. mask bounding box dimension x
 * Diagnostics-initial ROI	Morph. mask bounding box dimension y
 * Diagnostics-initial ROI	Morph. mask bounding box dimension z
 * Diagnostics-initial ROI	Int. mask voxel count
 * Diagnostics-initial ROI	Morph. mask voxel count
 * Diagnostics-initial ROI	Int. mask mean intensity
 * Diagnostics-initial ROI	Int. mask minimum intensity
 * Diagnostics-initial ROI	Int. mask maximum intensity
 * Diagnostics-interpolated ROI	Int. mask dimension x
 * Diagnostics-interpolated ROI	Int. mask dimension y
 * Diagnostics-interpolated ROI	Int. mask dimension z
 * Diagnostics-interpolated ROI	Int. mask bounding box dimension x
 * Diagnostics-interpolated ROI	Int. mask bounding box dimension y
 * Diagnostics-interpolated ROI	Int. mask bounding box dimension z
 * Diagnostics-interpolated ROI	Morph. mask bounding box dimension x
 * Diagnostics-interpolated ROI	Morph. mask bounding box dimension y
 * Diagnostics-interpolated ROI	Morph. mask bounding box dimension z
 * Diagnostics-interpolated ROI	Int. mask voxel count
 * Diagnostics-interpolated ROI	Morph. mask voxel count
 * Diagnostics-interpolated ROI	Int. mask mean intensity
 * Diagnostics-interpolated ROI	Int. mask minimum intensity
 * Diagnostics-interpolated ROI	Int. mask maximum intensity
 * Diagnostics-resegmented ROI	Int. mask dimension x
 * Diagnostics-resegmented ROI	Int. mask dimension y
 * Diagnostics-resegmented ROI	Int. mask dimension z
 * Diagnostics-resegmented ROI	Int. mask bounding box dimension x
 * Diagnostics-resegmented ROI	Int. mask bounding box dimension y
 * Diagnostics-resegmented ROI	Int. mask bounding box dimension z
 * Diagnostics-resegmented ROI	Morph. mask bounding box dimension x
 * Diagnostics-resegmented ROI	Morph. mask bounding box dimension y
 * Diagnostics-resegmented ROI	Morph. mask bounding box dimension z
 * Diagnostics-resegmented ROI	Int. mask voxel count
 * Diagnostics-resegmented ROI	Morph. mask voxel count
 * Diagnostics-resegmented ROI	Int. mask mean intensity
 * Diagnostics-resegmented ROI	Int. mask minimum intensity
 * Diagnostics-resegmented ROI	Int. mask maximum intensity
 * @author tatsunidas
 *
 */
public class DiagnosticsInfo {
	
	/**
	 * 
	 * @param originalImp
	 * @param originalMask
	 * @param resampledImp
	 * @param resampleMask
	 * @param resegmentedMask : after resampled + resegmented.
	 * @param label
	 */
	HashMap<String, Double> diagnostics;
	public DiagnosticsInfo(ImagePlus originalImp, ImagePlus originalMask, ImagePlus resampledImp, ImagePlus resampledMask, ImagePlus resegmentedMask, int label) {
		if(originalImp == null || originalMask == null || resampledImp == null || resampledMask == null || resegmentedMask == null) {
			return;
		}
		diagnostics = new HashMap<>();
		/*
		 * initial images
		 */
		//get stats with full face mask
		IntensityBasedStatisticalFeatures ibsf = new IntensityBasedStatisticalFeatures(originalImp, null,label);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageImageDimensionX.name(), (double)originalImp.getWidth());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageImageDimensionY.name(), (double)originalImp.getHeight());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageImageDimensionZ.name(), (double)originalImp.getNSlices());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageVoxelDimensionX.name(), (double)originalImp.getCalibration().pixelWidth);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageVoxelDimensionY.name(), (double)originalImp.getCalibration().pixelHeight);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageVoxelDimensionZ.name(), (double)originalImp.getCalibration().pixelDepth);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageMeanIntensity.name(),ibsf.getMean());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageMinimumIntensity.name(),ibsf.getMinimum());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialImageMaximumIntensity.name(),ibsf.getMaximum());
		/*
		 * resampled images
		 */
		//get stats with full face mask
		ibsf = new IntensityBasedStatisticalFeatures(resampledImp, null,label);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageImageDimensionX.name(), (double)resampledImp.getWidth());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageImageDimensionY.name(), (double)resampledImp.getHeight());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageImageDimensionZ.name(), (double)resampledImp.getNSlices());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageVoxelDimensionX.name(), (double)resampledImp.getCalibration().pixelWidth);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageVoxelDimensionY.name(), (double)resampledImp.getCalibration().pixelHeight);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageVoxelDimensionZ.name(), (double)resampledImp.getCalibration().pixelDepth);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageMeanIntensity.name(),ibsf.getMean());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageMinimumIntensity.name(),ibsf.getMinimum());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedImageMaximumIntensity.name(),ibsf.getMaximum());
		/*
		 * initial roi mask
		 */
		HashMap<String, double[]> xyz = Utils.getRoiBoundingBoxInfo(originalMask, label, RadiomicsJ.debug);
		ibsf = new IntensityBasedStatisticalFeatures(originalImp, originalMask, label);
		double count = ibsf.countRoiVoxel();
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMaskDimensionX.name(),(double)originalMask.getWidth());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMaskDimensionY.name(),(double)originalMask.getHeight());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMaskDimensionZ.name(),(double)originalMask.getNSlices());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMaskBoundingBoxDimensionX.name(),xyz.get("x")[1]-xyz.get("x")[0]+1);	
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMaskBoundingBoxDimensionY.name(),xyz.get("y")[1]-xyz.get("y")[0]+1);	
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMaskBoundingBoxDimensionZ.name(),xyz.get("z")[1]-xyz.get("z")[0]+1);
		// RadiomicsJ, MorphMask is same as ROIMask
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMorphMaskBoundingBoxDimensionX.name(),xyz.get("x")[1]-xyz.get("x")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMorphMaskBoundingBoxDimensionY.name(),xyz.get("y")[1]-xyz.get("y")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMorphMaskBoundingBoxDimensionZ.name(),xyz.get("z")[1]-xyz.get("z")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIIntMaskVoxelCount.name(), count);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIMorphMaskVoxelCount.name(), count);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIIntMaskMeanIntensity.name(),ibsf.getMean());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIIntMaskMinimumIntensity.name(),ibsf.getMinimum());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInitialROIIntMaskMaximumIntensity.name(),ibsf.getMaximum());
		/*
		 * resampled roi mask
		 */
		xyz = Utils.getRoiBoundingBoxInfo(resampledMask, label, RadiomicsJ.debug);
		ibsf = new IntensityBasedStatisticalFeatures(resampledImp, resampledMask, label);
		count = ibsf.countRoiVoxel();
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskDimensionX.name(),(double)resampledMask.getWidth());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskDimensionY.name(),(double)resampledMask.getHeight());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskDimensionZ.name(),(double)resampledMask.getNSlices());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskBoundingBoxDimensionX.name(),xyz.get("x")[1]-xyz.get("x")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskBoundingBoxDimensionY.name(),xyz.get("y")[1]-xyz.get("y")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskBoundingBoxDimensionZ.name(),xyz.get("z")[1]-xyz.get("z")[0]+1);
		// RadiomicsJ, InterpolatedMorphMask is same as resampledROIMask
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIMorphMaskBoundingBoxDimensionX.name(),xyz.get("x")[1]-xyz.get("x")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIMorphMaskBoundingBoxDimensionY.name(),xyz.get("y")[1]-xyz.get("y")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIMorphMaskBoundingBoxDimensionZ.name(),xyz.get("z")[1]-xyz.get("z")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskVoxelCount.name(),count);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIMorphMaskVoxelCount.name(),count);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskMeanIntensity.name(),ibsf.getMean());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskMinimumIntensity.name(),ibsf.getMinimum());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsInterpolatedROIIntMaskMaximumIntensity.name(),ibsf.getMaximum());
		/*
		 * re-segmented roi
		 */
		xyz = Utils.getRoiBoundingBoxInfo(resegmentedMask, label, RadiomicsJ.debug);
		ibsf = new IntensityBasedStatisticalFeatures(resampledImp, resegmentedMask, label);
		count = ibsf.countRoiVoxel();
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskDimensionX.name(),(double)resegmentedMask.getWidth());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskDimensionY.name(),(double)resegmentedMask.getHeight());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskDimensionZ.name(),(double)resegmentedMask.getNSlices());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskBoundingBoxDimensionX.name(),xyz.get("x")[1]-xyz.get("x")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskBoundingBoxDimensionY.name(),xyz.get("y")[1]-xyz.get("y")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskBoundingBoxDimensionZ.name(),xyz.get("z")[1]-xyz.get("z")[0]+1);
		// RadiomicsJ, Re-segmented MorphMask is same as re-segmented ROIMask
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIMorphMaskBoundingBoxDimensionX.name(),xyz.get("x")[1]-xyz.get("x")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIMorphMaskBoundingBoxDimensionY.name(),xyz.get("y")[1]-xyz.get("y")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIMorphMaskBoundingBoxDimensionZ.name(),xyz.get("z")[1]-xyz.get("z")[0]+1);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskVoxelCount.name(),count);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIMorphMaskVoxelCount.name(),count);
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskMeanIntensity.name(),ibsf.getMean());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskMinimumIntensity.name(),ibsf.getMinimum());
		diagnostics.put(DiagnosticsInfoType.DiagnosticsResegmentedROIIntMaskMaximumIntensity.name(),ibsf.getMaximum());
	}
	
	public Double getDiagnosticsBy(String name) {
		if(name == null) {
			return null;
		}
		for(DiagnosticsInfoType infoType : DiagnosticsInfoType.values()) {
			if(infoType.name().equals(name)) {
				return diagnostics.get(name);
			}
		}
		return null;
	}
	
	public HashMap<String, Double> getAllDiagnostics() {
		return diagnostics;
	}
	
	public String toString() {
		for(DiagnosticsInfoType infoType : DiagnosticsInfoType.values()) {
			System.out.println(infoType.name()+" , "+diagnostics.get(infoType.name()));
		}
		return null;
	}
}

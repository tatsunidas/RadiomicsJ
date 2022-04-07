/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package wavelet;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

//debug imports
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * @author Martin Capek
 */
public class Wavelet_Denoise implements PlugInFilter, DialogListener, WindowListener {

	// ENUMERATIONS //

	/**
	 * Defines types of modes to distinguish various tools for coefficients
	 * modification.
	 */
	private enum Mode {
		Nothing, Suppress, SoftDenoise, HardDenoise, SuppressApprox, SuppressDetail
	}

	// CONSTANTS //

	/**
	 * Default value of scale (level of detail).
	 */
	private final int defaultLODVal = 1;

	/**
	 * Default value of suppression.
	 */
	private final int defaultSuppVal = 10;

	/**
	 * Default value of denoising.
	 */
	private final int defaultDenoiseVal = 5;

	/**
	 * Represents selected levels of detail, suppress, soft and hard thresholding.
	 */
	private final int minScale = 1, minSuppress = 0, minSoftThresh = 1, minHardThresh = 1, minSuppApprox = 0,
			minSuppDetail = 0;

	/**
	 * Represents maximum levels of suppress, soft and hard thresholding.
	 */
	private final int maxSuppress = 100, maxSoftThresh = 128, maxHardThresh = 128, maxSuppApprox = 100,
			maxSuppDetail = 100;

	/**
	 * Applicable wavelet filters.
	 */
	private String[] waveletFilterStrings = { "Haar 1",

			"Daubechies 1", "Daubechies 2", "Daubechies 3", "Daubechies 4", "Daubechies 5", "Daubechies 6",
			"Daubechies 7", "Daubechies 8", "Daubechies 9", "Daubechies 10", "Daubechies 11", "Daubechies 12",
			"Daubechies 13", "Daubechies 14", "Daubechies 15", "Daubechies 16", "Daubechies 17", "Daubechies 18",
			"Daubechies 19", "Daubechies 20",

			"Symlets 2", "Symlets 3", "Symlets 4", "Symlets 5", "Symlets 6", "Symlets 7", "Symlets 8", "Symlets 9",
			"Symlets 10", "Symlets 10", "Symlets 11", "Symlets 12", "Symlets 13", "Symlets 14", "Symlets 15",
			"Symlets 16", "Symlets 17", "Symlets 18", "Symlets 19", "Symlets 20",

			"Coiflets 1", "Coiflets 2", "Coiflets 3", "Coiflets 4", "Coiflets 5",

			"Biorthogonal 1.1", "Biorthogonal 1.3", "Biorthogonal 1.5", "Biorthogonal 2.2", "Biorthogonal 2.4",
			"Biorthogonal 2.6", "Biorthogonal 2.8", "Biorthogonal 3.1", "Biorthogonal 3.3", "Biorthogonal 3.5",
			"Biorthogonal 3.7", "Biorthogonal 3.9", "Biorthogonal 4.4", "Biorthogonal 5.5", "Biorthogonal 6.8",

			"Reverse Biorthogonal 1.1", "Reverse Biorthogonal 1.3", "Reverse Biorthogonal 1.5",
			"Reverse Biorthogonal 2.2", "Reverse Biorthogonal 2.4", "Reverse Biorthogonal 2.6",
			"Reverse Biorthogonal 2.8", "Reverse Biorthogonal 3.1", "Reverse Biorthogonal 3.3",
			"Reverse Biorthogonal 3.5", "Reverse Biorthogonal 3.7", "Reverse Biorthogonal 3.9",
			"Reverse Biorthogonal 4.4", "Reverse Biorthogonal 5.5", "Reverse Biorthogonal 6.8",

			"Discrete Meyer 1" };

	/**
	 * Filtration methods used.
	 */
	private final String[] filtMethods = { "Nothing", "Suppress AC & DC", "Soft Thresholding", "Hard Thresholding",
			"Suppress AC - applied with \"Suppress DC\"", "Suppress DC - applied with \"Suppress AC\"" };

	/**
	 * String to be shown during computation of Wavelet Transform, either in Fiji
	 * status bar or in the dialog.
	 */
	private final String computMessageWT = "Computing Wavelet Transform ... ";

	// ATTRIBUTES //

	/**
	 * Main dialog.
	 */
	private GenericDialog gdMain;

	/**
	 * Represents instance of the <c>ImageData</c> class.
	 */
	public ImageData imageData;

	/**
	 * String of a selected Wavelet filter.
	 */
	private String selectedWFString;

	/**
	 * Represents selected level of detail, suppress, soft and hard thresholding.
	 */
	private int selectedScale, selectedSuppress, selectedThreshSoft, selectedThreshHard, selectedSuppApprox,
			selectedSuppDetail;

	/**
	 * Represents maximum level of detail.
	 */
	private int maxScale;

	/**
	 * Represents selected mode to distinguish which tool to use.
	 */
	private Mode selectedMode;

	/**
	 * Reference to Scrollbars, NumberFields, Labels, Checkboxes, PopUp menu of the
	 * dialog
	 */
	private Vector<?> scrollbars, numberFields, checkboxes, radioButtonGroup, choice;

	/**
	 * Reference to the textmessage area for displaying the progressbar in percents.
	 */
	private Label percentsArea;

	private boolean dialogShown = false;

	private static boolean showWT = false; // Image Stack with Wavelet Transformation (Stretched)
	private static boolean showNoStretchWT = false; // Image Stack with Float values of Wavelet Transformation (Not
													// Stretched to 0-255)
	private boolean stretchWTWindowShown = false; // Is the window already shown?
	private boolean stretchWTWindowOpening = false; // Is the window opening?
	private boolean WTWindowShown = false; // Is the window already shown?
	private boolean WTWindowOpening = false; // Is the window opening?

	private boolean paramChanged = false; // Params changed in the main dialog?
	private boolean previewChecked = false; // One slice preview mode
	private boolean previewCheckedWasTrue = false; // One slice preview mode was true
	private boolean previewClosing = false; // One slice preview mode closing
	private boolean previewPressed = false;
	private boolean paramsChangedDuringPreview = false; // Were any parameter changed during preview?
	private boolean recompAllDataDone = false; // Recomputing All Data done?

	private int newScale; // LOD (scale) choice
	private int newSuppress; // Suppress value
	private int newThreshSoft; // SoftThresh value
	private int newThreshHard; // HardThresh value
	private int newSuppApprox; // Suppress Approx value
	private int newSuppDetail; // Suppress Detail value
	private String newWFString; // String of Wavelet filter chosen
	private Mode newMode = Mode.Nothing; // Mode value
	private Panel buttonsPanel; // Panel for Preview button
	private Panel resetPanel; // Panel for Reset button
	private Button previewButton; // Preview button
	private Button recomputeButton; // Recompute All Data button
	private Button resetButton; // Reset button

	// GETTERS AND SETTERS
	/**
	 * @return the showWT
	 */
	protected static boolean isShownWT() {
		return showWT;
	}

	/**
	 * @return the showNoStretchWT
	 */
	protected static boolean isShownNoStretchWT() {
		return showNoStretchWT;
	}

	// METHODS //

	/**
	 * Initializes the plugin.
	 * 
	 * @param imp - ImagePlus image (possible multi-dimensional).
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		if (imp == null) {
			GenericDialog gd = new GenericDialog("Wavelet_Denoise: No Image");
			gd.addMessage("At least one image should be opened!");
			gd.hideCancelButton();
			gd.showDialog();
			return DONE;
		}

		// get dimensions of the data
		int dim[] = imp.getDimensions();
		if (dim[2] > 1) {
			GenericDialog gd = new GenericDialog("Wavelet_Denoise: Multiple Channel Data");
			gd.addMessage("Multiple channel data not supported, run \"Split Channels\" first!");
			gd.hideCancelButton();
			gd.showDialog();
			return DONE;
		}

		// checking sizes of image matrix, both must be power of 2
		int width = imp.getWidth();
		int height = imp.getHeight();
		if (IsWrongSize(width, height)) {
			GenericDialog gd = new GenericDialog("Wavelet_Denoise: Bad Sizes of the Image");
			gd.addMessage(
					"Both width and height of the image must be power of 2. Crop, Resample or change Canvas Size first!");
			gd.hideCancelButton();
			gd.showDialog();
			return DONE;
		}

		imageData = new ImageData(imp);
		selectedScale = defaultLODVal;
		selectedWFString = "Haar 1";
		selectedSuppress = defaultSuppVal;
		selectedThreshSoft = defaultDenoiseVal;
		selectedThreshHard = defaultDenoiseVal;
		selectedSuppApprox = defaultSuppVal;
		selectedSuppDetail = defaultSuppVal;
		maxScale = imageData.changeScale(defaultLODVal);

		showWT = false;
		showNoStretchWT = false;

		return DOES_8G | DOES_16 | DOES_32;
	}

	/**
	 * Runs the plugin.
	 * 
	 * @param ip - Unused.
	 */
	@Override
	public void run(ImageProcessor ip) {
		// visualize
		String title = imageData.imageOrig.getTitle();
		String newTitle = "WT-" + title;
		imageData.imageWave.setTitle(newTitle);
		newTitle = "WT-NoStretch-" + title;
		imageData.imageWaveNoStretch.setTitle(newTitle);
		title = imageData.imageOrig.getTitle();
		newTitle = "Filtered-" + title;
		imageData.imageModif.setTitle(newTitle);
		imageData.imageModif.show();

		// WT
		imageData.changeScale(defaultLODVal);
		imageData.setWaveletFilter(waveletFilterStrings[0]); // Haar 1/Daubechies 1 by default
		selectedMode = Mode.Nothing;
		transform();

		// tileImages(); // removed, since it tiles all images opened in Fiji
		synchronizeImages();
		showDialog();
	}
	
	public void execute(ImageProcessor ip, boolean showResult) {
		// visualize
		String title = imageData.imageOrig.getTitle();
		imageData.imageWave.setTitle("WT-"+title);
		imageData.imageWaveNoStretch.setTitle("WT-NoStretch-" + title);
		imageData.imageModif.setTitle("Filtered-" + title);
		
		// WT
		imageData.changeScale(defaultLODVal);
		imageData.setWaveletFilter(waveletFilterStrings[0]); // Haar 1/Daubechies 1 by default
		selectedMode = Mode.Nothing;
		transform();
		if(showResult) {
			imageData.imageWave.show();
			imageData.imageWaveNoStretch.show();
			imageData.imageModif.show();
		}
	}

	/**
	 * Determines if the image has wrong size (must be width & height = power of 2).
	 * 
	 * @return Returns true if the image has wrong size, otherwise returns false.
	 */
	private boolean IsWrongSize(final int width, final int height) {
		if (((width & (width - 1)) != 0) || ((height & (height - 1)) != 0))
			return true; // image dimensions are not power of 2
		else
			return false;
	}

	/**
	 * Opens Synchronize All dialog.
	 */
	private void synchronizeImages() {
		// windows synchronizing dialog
		ij.plugin.frame.SyncWindows syncW = new ij.plugin.frame.SyncWindows();
		syncW.run(null);
	}

	/**
	 * Does FWT and IWT of full data, including progressBar.
	 */
	private void transform() {
		IJ.showStatus(computMessageWT);
		IJ.showProgress(0, imageData.getNSlices());
		myShowProgressInPercents(0, imageData.getNSlices());
		imageData.bitmapToData(); // imageOrig into imageData
		for (int z = 1; z <= imageData.getNSlices(); z++) {
			imageData.fwdTransform(z); // FWT2D from imageData into transformedData by slices + stretching
										// transformedData into imageWave for visualization

			switch (selectedMode) {
			case Nothing:
				break;
			case Suppress:
				imageData.suppressWave(z, selectedSuppress);
				break;
			case SoftDenoise:
				imageData.denoiseWave(z, selectedThreshSoft, false);
				break;
			case HardDenoise:
				imageData.denoiseWave(z, selectedThreshHard, true);
				break;
			case SuppressApprox:
				imageData.suppressWave_SeparatedCoeffs(z, selectedSuppApprox, selectedSuppDetail);
				break;
			case SuppressDetail:
				imageData.suppressWave_SeparatedCoeffs(z, selectedSuppApprox, selectedSuppDetail);
				break;
			}

			imageData.invTransform(z); // IWT2D from transformedData into imageData by slices
			IJ.showStatus(computMessageWT);
			IJ.showProgress(z, imageData.getNSlices());
			myShowProgressInPercents(z, imageData.getNSlices());
		}
		imageData.dataToBitmap(); // transformed (and filtered) imageData into imageModif
		IJ.showStatus("");
		myShowProgressStatus("Ready");

		if (showWT)
			imageData.imageWave.updateAndDraw();
		imageData.imageModif.updateAndDraw();
		if (showNoStretchWT)
			imageData.imageWaveNoStretch.updateAndDraw();
	}

	/**
	 * Does FWT and IWT of one slice, including progressBar.
	 */
	private void transform(int z) {
		myShowProgressStatus(computMessageWT);
		imageData.bitmapToData(z); // imageOrig into imageData
		imageData.fwdTransform(z); // FWT2D from imageData into transformedData by slices + stretching
									// transformedData into imageWave for visualization

		switch (selectedMode) {
		case Nothing:
			break;
		case Suppress:
			imageData.suppressWave(z, selectedSuppress);
			break;
		case SoftDenoise:
			imageData.denoiseWave(z, selectedThreshSoft, false);
			break;
		case HardDenoise:
			imageData.denoiseWave(z, selectedThreshHard, true);
			break;
		case SuppressApprox:
			imageData.suppressWave_SeparatedCoeffs(z, selectedSuppApprox, selectedSuppDetail);
			break;
		case SuppressDetail:
			imageData.suppressWave_SeparatedCoeffs(z, selectedSuppApprox, selectedSuppDetail);
			break;
		}

		imageData.invTransform(z); // IWT2D from transformedData into imageData by slices
		myShowProgressInPercents(z, imageData.getNSlices());
		imageData.dataToBitmap(z); // transformed (and filtered) imageData into imageModif
		myShowProgressStatus("Ready");

		if (showWT)
			imageData.imageWave.updateAndDraw();
		imageData.imageModif.updateAndDraw();
		if (showNoStretchWT)
			imageData.imageWaveNoStretch.updateAndDraw();
	}

	/**
	 * @param actVal
	 * @param maxVal
	 */
	private void myShowProgressInPercents(double actVal, double maxVal) {
		if (dialogShown) {
			int actualPercents = (int) ((actVal / maxVal) * 100.0);
			percentsArea.setText(computMessageWT + actualPercents + "%");
		}
	}

	/**
	 * @param status
	 */
	private void myShowProgressStatus(String status) {
		if (dialogShown)
			percentsArea.setText(status);
	}

	/**
	 * @return
	 */
	private boolean showDialog() {
		gdMain = new GenericDialog("Wavelet_Denoise");
		gdMain.setModal(false);
		// Reset button
		buttonReset(gdMain);

		String levelOfDetailSliderStr = "Level of Details (1-" + maxScale + ")";
		gdMain.addSlider(levelOfDetailSliderStr, minScale, maxScale, selectedScale);

		// Wavelet Filters Selection
		gdMain.addChoice("Wavelet Filter", waveletFilterStrings, "Haar");

		gdMain.addRadioButtonGroup("Filtration Method", filtMethods, filtMethods.length, 1, filtMethods[0]);
		gdMain.addSlider("Suppress AC & DC (0-100%)", minSuppress, maxSuppress, selectedSuppress);
		gdMain.addSlider("Soft Thresholding (1-128)", minSoftThresh, maxSoftThresh, selectedThreshSoft);
		gdMain.addSlider("Hard Thresholding (1-128)", minHardThresh, maxHardThresh, selectedThreshHard);
		gdMain.addSlider("Suppress AC (0-100%)", minSuppApprox, maxSuppApprox, selectedSuppApprox);
		gdMain.addSlider("Suppress DC (0-100%)", minSuppDetail, maxSuppDetail, selectedSuppDetail);
		gdMain.addCheckbox("Show WT Coefficients", showWT);
		gdMain.addCheckbox("Show Float WT Coefficients (For Experts)", showNoStretchWT);
		gdMain.addCheckbox("Enable 1 Slice Preview - Use \"Synchronize All Windows\"", previewChecked);

		// Preview and Recompute All Data buttons
		buttonsPreviewAndRecompute(gdMain);
		// Message
		gdMain.addMessage("Ready");
		percentsArea = (Label) gdMain.getMessage();

		choice = gdMain.getChoices();
		scrollbars = gdMain.getSliders();
		radioButtonGroup = gdMain.getRadioButtonGroups();
		numberFields = gdMain.getNumericFields();
		checkboxes = gdMain.getCheckboxes();
		selectedMode = Mode.Nothing;

		setSlidersVisibilityFiltMethods(selectedMode);

		gdMain.addDialogListener(this); // listening to changes in this dialog
		gdMain.addWindowListener(this); // listening to changes of the main window
		gdMain.setResizable(false);
		gdMain.setAlwaysOnTop(true);
		gdMain.setOKLabel("Done!");
		gdMain.hideCancelButton();
		gdMain.setLocation(200, 200);
		dialogShown = true;
		gdMain.showDialog();

		return true;
	}

	/**
	 * Button and Panel Reset creation.
	 * 
	 * @param gd
	 */
	private void buttonReset(GenericDialog gd) {
		// Panel with a Reset button
		resetPanel = new Panel();
		resetButton = new Button("Reset");
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (allImageWindowOpened() && originalImageDataTypePreserved() && allSizesPreserved()) {
					// LOD
					newScale = 1;
					((Scrollbar) (scrollbars.elementAt(0))).setValue(newScale);
					((TextField) (numberFields.elementAt(0))).setText("1");
					// Wavelet filter
					Choice ch = (Choice) choice.get(0);
					ch.select(0);
					newWFString = waveletFilterStrings[0];
					// Filtration method
					newMode = Mode.Nothing;
					selectedMode = newMode;
					CheckboxGroup chbg = ((CheckboxGroup) (radioButtonGroup.elementAt(0)));
					chbg.setSelectedCheckbox(null);
					// scrollbars visibility and values
					for (int i = 1; i < scrollbars.size(); i++) {
						((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
						((TextField) (numberFields.elementAt(i))).setEnabled(false);
					}
					newSuppress = defaultSuppVal;
					((Scrollbar) (scrollbars.elementAt(1))).setValue(newSuppress);
					;
					((TextField) (numberFields.elementAt(1))).setText(Integer.toString(newSuppress));
					newThreshSoft = defaultDenoiseVal;
					((Scrollbar) (scrollbars.elementAt(2))).setValue(newThreshSoft);
					;
					((TextField) (numberFields.elementAt(2))).setText(Integer.toString(newThreshSoft));
					newThreshHard = defaultDenoiseVal;
					((Scrollbar) (scrollbars.elementAt(3))).setValue(newThreshHard);
					;
					((TextField) (numberFields.elementAt(3))).setText(Integer.toString(newThreshHard));
					newSuppApprox = defaultSuppVal;
					((Scrollbar) (scrollbars.elementAt(4))).setValue(newSuppApprox);
					((TextField) (numberFields.elementAt(4))).setText(Integer.toString(newSuppApprox));
					newSuppDetail = defaultSuppVal;
					((Scrollbar) (scrollbars.elementAt(5))).setValue(newSuppDetail);
					((TextField) (numberFields.elementAt(5))).setText(Integer.toString(newSuppDetail));
					previewChecked = false;
					previewCheckedWasTrue = false;
					paramsChangedDuringPreview = false;
					previewButton.setEnabled(false);
					recomputeButton.setEnabled(false);
					((Checkbox) (checkboxes.elementAt(2))).setEnabled(false);
					((Checkbox) (checkboxes.elementAt(2))).setState(false);
					transform();
				}
			}
		});

		resetPanel.add(resetButton);
		gd.addPanel(resetPanel);
	}

	/**
	 * Button and Panel Preview and Recompute All Data creation.
	 * 
	 * @param gd
	 */
	private void buttonsPreviewAndRecompute(GenericDialog gd) {
		// Panel with a Preview button.
		buttonsPanel = new Panel();

		previewButton = new Button("Preview 1 Slice");
		previewButton.setEnabled(false);
		previewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (allImageWindowOpened() && originalImageDataTypePreserved()) {
					transform(imageData.imageOrig.getSlice());
					previewPressed = true;
					recomputeButton.setEnabled(true);
				}
			}
		});

		recomputeButton = new Button("Recomputing All Data Required");
		recomputeButton.setEnabled(false);
		recomputeButton.setForeground(new Color(0, 0, 255));
		recomputeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (allImageWindowOpened() && originalImageDataTypePreserved()) {
					transform();
					recompAllDataDone = true;
					recomputeButton.setEnabled(false);
				}
			}
		});

		buttonsPanel.add(previewButton);
		buttonsPanel.add(recomputeButton);
		gd.addPanel(buttonsPanel);
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {

		if (!allImageWindowOpened() || !originalImageDataTypePreserved() || !allSizesPreserved())
			return true;

		// Method choice
		String filterMethodChoice = gd.getNextRadioButton();
		newMode = setNewMode(filterMethodChoice);

		setSlidersVisibilityFiltMethods(newMode);

		newScale = (int) gd.getNextNumber(); // LOD (scale) choice
		newWFString = gd.getNextChoice(); // string of WF selected
		newSuppress = (int) gd.getNextNumber(); // Suppress value
		newThreshSoft = (int) gd.getNextNumber(); // SoftThresh value
		newThreshHard = (int) gd.getNextNumber(); // HardThresh value
		newSuppApprox = (int) gd.getNextNumber(); // Suppress Approx value
		newSuppDetail = (int) gd.getNextNumber(); // Suppress Detail value
		showWT = gd.getNextBoolean(); // Showing image with 8bit values of WT coeffs.
		showNoStretchWT = gd.getNextBoolean(); // Showing image with double values of WT coeffs.
		previewChecked = gd.getNextBoolean(); // One Slice Preview enabled

		if (!inputParametersCorrect())
			return false;

		paramChanged = parameterChanged();

		// Preview switched on
		if (previewChecked && !previewCheckedWasTrue) {
			previewButton.setEnabled(true);
			// recomputeButton.setEnabled(true);
			((Checkbox) (checkboxes.elementAt(0))).setEnabled(false);
			((Checkbox) (checkboxes.elementAt(1))).setEnabled(false);
			previewCheckedWasTrue = true;
			paramsChangedDuringPreview = false;
		}

		// Preview is running
		if (previewChecked && previewCheckedWasTrue)
			if (paramChanged)
				paramsChangedDuringPreview = true;

		// Preview switched off
		if (!previewChecked && previewCheckedWasTrue) {
			previewButton.setEnabled(false);
			recomputeButton.setEnabled(false);
			((Checkbox) (checkboxes.elementAt(0))).setEnabled(true);
			((Checkbox) (checkboxes.elementAt(1))).setEnabled(true);
			previewCheckedWasTrue = false;
			if (paramsChangedDuringPreview)
				previewClosing = true;
		}

		// No Stretch WT window
		if (showWT && !WTWindowShown) {
			ImagePlus ip = imageData.imageWave;
			if (ip == null) {
				GenericDialog gdMessage = new GenericDialog("imageWave null");
				gdMessage.showDialog();
			}

			imageData.imageWave.show();

			// tileImages();
			WTWindowShown = WTWindowOpening = true;
		}
		if (!showWT && WTWindowShown) {
			imageData.imageWave.hide();
			WTWindowShown = false;
		}

		// Stretch Float WT window
		if (showNoStretchWT && !stretchWTWindowShown) {
			imageData.imageWaveNoStretch.show();
			// tileImages();
			stretchWTWindowShown = stretchWTWindowOpening = true;
		}
		if (!showNoStretchWT && stretchWTWindowShown) {
			imageData.imageWaveNoStretch.hide();
			stretchWTWindowShown = false;
		}

		// set variables
		if (paramChanged || WTWindowOpening || stretchWTWindowOpening || previewClosing) {
			WTWindowOpening = false;
			stretchWTWindowOpening = false;
			previewClosing = false;
			setVariablesDoTransform();
		}

		recompAllDataDone = false;
		if (previewChecked && recompAllDataDone)
			recomputeButton.setEnabled(true);
		previewPressed = false;

		return true;
	}

	/**
	 * Checking if image windows are still opened, since the main dialog is
	 * modeless.
	 */
	private boolean allImageWindowOpened() {
		if (imageData.imageOrig.isVisible() == false) {
			percentsArea.setText("Window with Original Image closed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if ((showWT == true) && (imageData.imageWave.isVisible() == false)) {
			percentsArea.setText("Window with Wavelet Transform closed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if ((showNoStretchWT == true) && (imageData.imageWaveNoStretch.isVisible() == false)) {
			percentsArea.setText("Window with Float Wavelet Transform closed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if (imageData.imageModif.isVisible() == false) {
			percentsArea.setText("Window with Filtered Image closed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		return true;
	}

	/**
	 * Checking if images are of original data type, since the main dialog is
	 * modeless.
	 */
	private boolean originalImageDataTypePreserved() {
		if (imageData.imageOrig.getType() != imageData.getType()) {
			percentsArea.setText("Original Image data type changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if (imageData.imageWave.getType() != imageData.getType()) {
			percentsArea.setText("Wavelet Transform data type changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if (imageData.imageModif.getType() != imageData.getType()) {
			percentsArea.setText("Filtered Image data type changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if (imageData.imageWaveNoStretch.getType() != ImagePlus.GRAY32) {
			percentsArea.setText("Float Wavelet Transform data type changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		return true;
	}

	/**
	 * Checking if image sizes are still preserved, since the main dialog is
	 * modeless.
	 */
	private boolean allSizesPreserved() {
		if (imageData.imageOrig.getWidth() != imageData.getWidth()
				|| imageData.imageOrig.getHeight() != imageData.getHeight()
				|| imageData.imageOrig.getNSlices() != imageData.getNSlices()) {
			percentsArea.setText("Size of the Original Image changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if (imageData.imageWave.getWidth() != imageData.getWidth()
				|| imageData.imageWave.getHeight() != imageData.getHeight()
				|| imageData.imageWave.getNSlices() != imageData.getNSlices()) {
			percentsArea.setText("Size of the Wavelet Transform changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if ((showNoStretchWT == true) && (imageData.imageWaveNoStretch.getWidth() != imageData.getWidth()
				|| imageData.imageWaveNoStretch.getHeight() != imageData.getHeight()
				|| imageData.imageWaveNoStretch.getNSlices() != imageData.getNSlices())) {
			percentsArea.setText("Size of the Float Wavelet Transform changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		if (imageData.imageModif.getWidth() != imageData.getWidth()
				|| imageData.imageModif.getHeight() != imageData.getHeight()
				|| imageData.imageModif.getNSlices() != imageData.getNSlices()) {
			percentsArea.setText("Size of the Filtered Image changed! Close the dialog...");
			percentsArea.setForeground(new Color(255, 0, 0));
			return false;
		}

		return true;
	}

	/**
	 * Test if parameters of the main dialog were changed by a user.
	 * 
	 * @return
	 */
	private boolean parameterChanged() {
		if ((newScale != selectedScale) || (newWFString != selectedWFString) || (newSuppress != selectedSuppress)
				|| (newThreshSoft != selectedThreshSoft) || (newThreshHard != selectedThreshHard)
				|| (newMode != selectedMode) || (newSuppApprox != selectedSuppApprox)
				|| (newSuppDetail != selectedSuppDetail))
			return true;
		else
			return false;
	}

	/**
	 * Sets variables and does all transforms.
	 */
	private void setVariablesDoTransform() {

		boolean transformAllowed = true;
		// checking if switching between SuppressAC and SuppressDC modes and if value of
		// selectedSuppApprox or selectedSuppDetail changed
		if ((((selectedMode == Mode.SuppressApprox) && (newMode == Mode.SuppressDetail)
				|| ((newMode == Mode.SuppressApprox) && (selectedMode == Mode.SuppressDetail)))
				&& (selectedSuppApprox == newSuppApprox) && (selectedSuppDetail == newSuppDetail)))
			transformAllowed = false;

		selectedScale = newScale;
		imageData.setScale(selectedScale);
		selectedWFString = newWFString;
		imageData.setWaveletFilter(newWFString);
		selectedMode = newMode;
		selectedSuppress = newSuppress;
		selectedThreshSoft = newThreshSoft;
		selectedThreshHard = newThreshHard;
		selectedSuppApprox = newSuppApprox;
		selectedSuppDetail = newSuppDetail;

		if ((transformAllowed) && (!previewChecked) && (!recompAllDataDone))
			transform();
	}

	/**
	 * @param filterMethodChoice
	 */
	private Mode setNewMode(String filterMethodChoice) {
		if (filterMethodChoice.equals(filtMethods[0])) // "Nothing"
			return Mode.Nothing;
		else if (filterMethodChoice.equals(filtMethods[1])) // "Suppress"
			return Mode.Suppress;
		else if (filterMethodChoice.equals(filtMethods[2])) // "Soft Thresholding"
			return Mode.SoftDenoise;
		else if (filterMethodChoice.equals(filtMethods[3])) // "Hard Thresholding"
			return Mode.HardDenoise;
		else if (filterMethodChoice.equals(filtMethods[4])) // "Suppress Approx"
			return Mode.SuppressApprox;
		else if (filterMethodChoice.equals(filtMethods[5])) // "Suppress Detail"
			return Mode.SuppressDetail;

		return Mode.Nothing;
	}

	/**
	 * Are all input parameters correct?
	 */
	private boolean inputParametersCorrect() {
		if (newScale > maxScale) {
			newScale = maxScale;
			return false;
		}
		if (newScale < minScale) {
			newScale = minScale;
			return false;
		}
		if (newSuppress > maxSuppress) {
			newSuppress = maxSuppress;
			return false;
		}
		if (newSuppress < minSuppress) {
			newSuppress = minSuppress;
			return false;
		}
		if (newThreshSoft > maxSoftThresh) {
			newThreshSoft = maxSoftThresh;
			return false;
		}
		if (newThreshSoft < minSoftThresh) {
			newThreshSoft = minSoftThresh;
			return false;
		}
		if (newThreshHard > maxHardThresh) {
			newThreshHard = maxHardThresh;
			return false;
		}
		if (newThreshHard < minHardThresh) {
			newThreshHard = minHardThresh;
			return false;
		}
		if (newSuppApprox > maxSuppApprox) {
			newSuppApprox = maxSuppApprox;
			return false;
		}
		if (newSuppApprox < minSuppApprox) {
			newSuppApprox = minSuppApprox;
			return false;
		}
		if (newSuppDetail > maxSuppDetail) {
			newSuppDetail = maxSuppDetail;
			return false;
		}
		if (newSuppDetail < minSuppDetail) {
			newSuppDetail = minSuppDetail;
			return false;
		}

		return true;
	}

	/**
	 * @param filterMethodChoice
	 */
	private void setSlidersVisibilityFiltMethods(Mode mode) {
		switch (mode) {
		case Nothing:
			for (int i = 1; i < scrollbars.size(); i++) {
				((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
				((TextField) (numberFields.elementAt(i))).setEnabled(false);
			}
			((Checkbox) (checkboxes.elementAt(2))).setEnabled(false);
			break;
		case Suppress:
			for (int i = 1; i < scrollbars.size(); i++)
				if (i != 1) {
					((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
					((TextField) (numberFields.elementAt(i))).setEnabled(false);
				}
			((Scrollbar) (scrollbars.elementAt(1))).setEnabled(true);
			((TextField) (numberFields.elementAt(1))).setEnabled(true);
			((Checkbox) (checkboxes.elementAt(2))).setEnabled(true);
			break;
		case SoftDenoise:
			for (int i = 1; i < scrollbars.size(); i++)
				if (i != 2) {
					((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
					((TextField) (numberFields.elementAt(i))).setEnabled(false);
				}
			((Scrollbar) (scrollbars.elementAt(2))).setEnabled(true);
			((TextField) (numberFields.elementAt(2))).setEnabled(true);
			((Checkbox) (checkboxes.elementAt(2))).setEnabled(true);
			break;
		case HardDenoise:
			for (int i = 1; i < scrollbars.size(); i++)
				if (i != 3) {
					((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
					((TextField) (numberFields.elementAt(i))).setEnabled(false);
				}
			((Scrollbar) (scrollbars.elementAt(3))).setEnabled(true);
			((TextField) (numberFields.elementAt(3))).setEnabled(true);
			((Checkbox) (checkboxes.elementAt(2))).setEnabled(true);
			break;
		case SuppressApprox:
			for (int i = 1; i < scrollbars.size(); i++)
				if (i != 4) {
					((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
					((TextField) (numberFields.elementAt(i))).setEnabled(false);
				}
			((Scrollbar) (scrollbars.elementAt(4))).setEnabled(true);
			((TextField) (numberFields.elementAt(4))).setEnabled(true);
			((Checkbox) (checkboxes.elementAt(2))).setEnabled(true);
			break;
		case SuppressDetail:
			for (int i = 1; i < scrollbars.size(); i++)
				if (i != 5) {
					((Scrollbar) (scrollbars.elementAt(i))).setEnabled(false);
					((TextField) (numberFields.elementAt(i))).setEnabled(false);
				}
			((Scrollbar) (scrollbars.elementAt(5))).setEnabled(true);
			((TextField) (numberFields.elementAt(5))).setEnabled(true);
			((Checkbox) (checkboxes.elementAt(2))).setEnabled(true);
			break;
		}
	}

	/**
	 * Does the tiling of all open images in Fiji.
	 */
	/*
	 * private void tileImages() { WindowOrganizer org = new WindowOrganizer();
	 * org.run("tile"); }
	 */

	public void showAbout() {
		IJ.showMessage("ProcessPixels", "a template for processing each pixel of an image");
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
		if (previewChecked && previewPressed && !recompAllDataDone && (newMode != Mode.Nothing)) {
			GenericDialog gd = new GenericDialog("Wavelet_Denoise: Message");
			gd.addMessage("'1-Slice-Preview' was not applied to the whole data!");
			gd.addMessage("Press 'Recomputing All Data Required' before closing the dialog!");
			gd.hideCancelButton();
			gd.showDialog();
		}
		gdMain.dispose();
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		
		ImagePlus imp = IJ.openImage("https://samples.fiji.sc/blobs.png");
		imp.setProcessor(imp.getProcessor().resize(256, 256));
		Wavelet_Denoise wd = new Wavelet_Denoise();
		wd.setup("", imp);
		wd.execute(imp.getProcessor(), false);
		
		double[][][] res = wd.imageData.getCoefficients();
		int s = res.length;
		int w = res[0][0].length;
		int h = res[0].length;
		
		ImageStack stack = new ImageStack(w/2, h/2);
		for(int z=0;z<s;z++) {
			ImageProcessor ll = new FloatProcessor(w/2, h/2);
			ImageProcessor lh = new FloatProcessor(w/2, h/2);
			ImageProcessor hl = new FloatProcessor(w/2, h/2);
			ImageProcessor hh = new FloatProcessor(w/2, h/2);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if(x < w/2 && y < h/2) {
						ll.setf(x, y, (float) res[z][y][x]);
					}else if(x >= w/2 && y < h/2) {
						lh.setf(x, y, (float) res[z][y][x]);
					}else if(x < w/2 && y >= h/2) {
						hl.setf(x, y, (float) res[z][y][x]);
					}else {
						hh.setf(x, y, (float) res[z][y][x]);
					}
				}
			}
			stack.addSlice(ll);
			stack.addSlice(lh);
			stack.addSlice(hl);
			stack.addSlice(hh);
		}
		
		IJ.saveAs(new ImagePlus("",stack), "tif", "wavelet_coeffs.tif");
	}

	public static void test() {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Wavelet_Denoise.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(),
				url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		// ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lenna_8bit.tif");

		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/WhiteCircle_8bit.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/WhiteCircle_16bit.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/WhiteCircle_8bit_8x8.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/WhiteCircle_8bit_4x4.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/WhiteCircle_8bit_16x16.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Line_8x1_2WhitePoints.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Line_16x1_2WhitePoints.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Line_32x1_2WhitePoints.tif");

		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Ch1-Sl15.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_32bit_488_568_oil1516_SI_002_SIR.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Scale-0_5.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_16bit_488_568_oil1516_SI_002_SIR-Scale-0_5.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale-0_5.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Scale-0_25.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_16bit_488_568_oil1516_SI_002_SIR-Scale-0_25.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_16bit_488_568_oil1516_SI_002_SIR-Scale-0_12-Ch1-Sl16.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale-0_25.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Scale-0_25-Ch1-Sl21.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Scale-0_12-Ch1-Sl21.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Ch1-Sl21-32x32.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/Lamina_8bit_488_568_oil1516_SI_002_SIR-Ch1-Sl21-32x32-Contrasted.tif");

		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/C1-Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale-2.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/C1-Lamina_32bit_488_568_oil1516_SI_002_SIR.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/C1-Lamina_8bit_488_568_oil1516_SI_002_SIR-Scale-0_25.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/C1-Lamina_488_568_oil1516_SI_002_SIR-z15.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/IMG_20190919_135741-Crop-Grey.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/C1-Lamina_32bit_488_568_oil1516_SI_002_SIR-Crop1024-768.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Wavelet_Denoise/C1-Lamina_32bit_488_568_oil1516_SI_002_SIR-Crop983-357.tif");

		// Prezentace BM
		// ImagePlus image = IJ.openImage("d:/Obrazova Data/IQA/HoneyComb
		// Artefact/__Prezentace_BM/Lamina/C1-Lamina_488_568_oil1516_SI_002_SIR-16bit-Z15.tif");
		// ImagePlus image = IJ.openImage("d:/Obrazova Data/IQA/HoneyComb
		// Artefact/__Prezentace_BM/Lamina/C2-Lamina_488_568_oil1516_SI_002_SIR-32bit-z15.tif");
		// ImagePlus image = IJ.openImage("d:/Obrazova Data/IQA/HoneyComb
		// Artefact/__Prezentace_BM/NUP/NUP_AF555_60xoil-1516_003_SIR-32bit-z15.tif");
		// ImagePlus image = IJ.openImage("d:/Obrazova Data/IQA/HoneyComb
		// Artefact/__Prezentace_BM/TUB/TUB_AF555_60xoil-1516_001_SIR-32bit-z13.tif");
		// ImagePlus image = IJ.openImage("d:/Obrazova Data/IQA/HoneyComb
		// Artefact/__Prezentace_BM/STED/Tub555 - gSTED001.tif");
		// ImagePlus image = IJ.openImage("d:/Wintexts/2019 FGU Prednaska BM
		// Wavelet/__Prezentace_BM/05_Coloc/C1-HeLa_DAPI-x484-488_Nup62-568_Lamin-647_60oi_SI_1516_25_001_SIR_ALX-32bit-Z10.tif");
		// ImagePlus image = IJ.openImage("d:/Wintexts/2019 FGU Prednaska BM
		// Wavelet/__Prezentace_BM/05_Coloc/C2-HeLa_DAPI-x484-488_Nup62-568_Lamin-647_60oi_SI_1516_25_001_SIR_ALX-32bit-Z10.tif");
		// ImagePlus image = IJ.openImage("d:/Wintexts/2019 FGU Prednaska BM
		// Wavelet/__Prezentace_BM/05_Coloc/C3-HeLa_DAPI-x484-488_Nup62-568_Lamin-647_60oi_SI_1516_25_001_SIR_ALX-32bit-Z10.tif");

		// LENOVO
		// ImagePlus image =
		// IJ.openImage("c:/Users/Martin/Documents/DATA_LENOVO/Programovani/eclipse-workspace/Images/C1-Lamina_8bit_488_568_oil1516_SI_002_SIR-Scale-0_25.tif");
		// ImagePlus image =
		// IJ.openImage("c:/Users/Martin/Documents/DATA_LENOVO/Programovani/eclipse-workspace/Images/C2-Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale0_25.tif");
		// ImagePlus image =
		// IJ.openImage("c:/Users/Martin/Documents/DATA_LENOVO/Programovani/eclipse-workspace/Images/C2-Lamina_32bit_488_568_oil1516_SI_002_SIR.tif");
		// ImagePlus image =
		// IJ.openImage("c:/Users/Martin/Documents/DATA_LENOVO/Programovani/eclipse-workspace/Images/C1-Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale-2.tif");

		// Pr�ce - nov� m�sto pro obr�zky
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Images/C2-Lamina_32bit_488_568_oil1516_SI_002_SIR.tif");
		// ImagePlus image =
		// IJ.openImage("d:/Programovani/Java_plugins/Images/C2-Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale0_25.tif");

		ImagePlus image = IJ.openImage(
				"d:/Programovani/Java_plugins/Images/C2-Lamina_32bit_488_568_oil1516_SI_002_SIR-Scale0_25.tif");

		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}

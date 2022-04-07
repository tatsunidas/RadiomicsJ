package wavelet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

//THIS PRODUCT IS DISTRIBUTED UNDER THE BSD 3-Clause License:

//Copyright (c) 2015, Petr Sečkář (C#); 2019 Martin Čapek (adaption to Java/Fiji)
//All rights reserved.

//Redistribution and use in source and binary forms, with or without modification, 
//are permitted provided that the following conditions are met:

//1. Redistributions of source code must retain the above copyright notice, this 
//list of conditions and the following disclaimer.

//2. Redistributions in binary form must reproduce the above copyright notice, this 
//list of conditions and the following disclaimer in the documentation and/or other 
//materials provided with the distribution.

//3. Neither the name of the copyright holder nor the names of its contributors may 
//be used to endorse or promote products derived from this software without specific 
//prior written permission.

//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
//ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
//WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
//IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
//INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
//BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
//OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
//WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
//ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
//POSSIBILITY OF SUCH DAMAGE.

public class ImageData {
	
	private final boolean DEBUG = false;
	
	// ATTRIBUTES //
	
	/**
	 * Original image.
	 */
	protected ImagePlus imageOrig;
	
	/**
	 * Wavelet transform coefficients of the original image - stretched to <0, 255>
	 */
	protected ImagePlus imageWave;
	
	/**
	 * Wavelet transform coefficients of the original image - non-stretched, 32-bit data.
	 */
	protected ImagePlus imageWaveNoStretch;
	
	/**
	 * Mofified image.
	 */
	protected ImagePlus imageModif;
	
	/**
	 * Image type - bit depth.
	 */
	private int type;
	
	/**
	 * Represents width of the image. (x)
	 */
	private int width;
	
	/**
	 * Represents height of the image. (y)
	 */
	private int height;
		
	/**
	 * Number of channels in the multidimensional images.
	 */
	private int nChannels;
	
	/**
	 * Number of slices in the images.
	 */
	private int nSlices;
	
	/**
	 * Number of pixels in the image. 
	 */
	private int imageSize;
	
	/**
	 * 3D array of data for representation of the <c>imageOrig</c>.
	 */
	private double[][][] imageData;
	
	/**
	 * 3D array of data representing transform coefficients (it is afterwards converted to <c>imageWave</c>).
	 */
	private double[][][] transformedData;
	
	/**
	 * Represents level of detail (number of iterations of wavelet transform)
	 */
	private int scale;
	
	/**
	 * Represents values used for stretching the <c>imageWave</c>.
	 */
	private double lowCoeffA, lowCoeffD, highCoeffA, highCoeffD;
	
	/**
	 * Arrays with min and max intensity values of each input (original) slice.
	 */
	private double[] minInputVals, maxInputVals;
	
	/**
	 * Min and max intensity values of output (filtered) image before intensity stretching
	 */
	private double minOutputVal, maxOutputVal;
	
	/**
	 * Selected Wavelet Filter
	 */
	private WaveletFilters waveletFilter;
	
	private BufferedWriter bw;						// debugging output to a file	
	
	//  GETTERS & SETTERS //
	
	/**
	 * @return the nSlices
	 */
	protected int getNSlices() {
		return nSlices;
	}
	
	/**
	 * @return the scale
	 */
	protected int getScale() {
		return scale;
	}

	/**
	 * @param scale the scale to set
	 */
	protected void setScale(int scale) {
		this.scale = scale;
	}	
	
	/**
	 * @return the type
	 */
	protected int getType() {
		return type;
	}
	
	/**
	 * @return the height
	 */
	protected int getHeight() {
		return height;
	}

	/**
	 * @return the width
	 */
	protected int getWidth() {
		return width;
	}

	/**
	 * Constructor
	 * @param imp - Input ImagePlus data.
	 */
	protected ImageData(ImagePlus imp)
	{
		if (DEBUG)	
			try {
				bw = new BufferedWriter(new FileWriter("d:\\Programovani\\Java_plugins\\Wavelet_Denoise\\Debug.txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}		
		
		imageOrig = imp;
		type = imageOrig.getType();
		width = imageOrig.getWidth();
		height = imageOrig.getHeight();
		nSlices = imageOrig.getStackSize();
		nChannels = imageOrig.getNChannels();
		imageSize = width*height;
        imageData = new double[nSlices][height][width];  		//data obtained from the original image represented as double
        transformedData = new double[nSlices][height][width];   //imageData that are transformed using FWT
        minInputVals = new double[nSlices];						// minimum intensity value in each slice
        maxInputVals = new double[nSlices];						// maximum intensity value in each slice
    	waveletFilter = new WaveletFilters();					// Wavelet Filters
        
        // imageWave/Modif initializations by the original image 
		imageWave = imageOrig.duplicate();
		imageModif = imageOrig.duplicate();
		
		// imageWave without stretching - 32bit, stack, calibration
		Calibration cal = imageOrig.getCalibration();
		imageWaveNoStretch = IJ.createImage("WT-NoStretch", width, height, nSlices, 32);
		imageWaveNoStretch.setCalibration(cal);
				
		// konverze imageOrig to pixels and back for visualization in imageModif		
		bitmapToData();
		dataToBitmap();		
	}
	
	// METHODS //
	
	/**
	 * Initialization of imageWave and imageModif, with image filtration if useful.
	 * Including channels - CompositeImage.
	 * For debugging.
	 */
	protected CompositeImage initializeWithFiltering(int filterType) {
		ImageStack stack = new ImageStack(width, height);
		String title = imageOrig.getTitle() + "_Filtered";
		ImagePlus imageNew = new ImagePlus();
			
		for (int i = 1; i <= nSlices; i++)
		{           	
		    // Sobel operátor a vytvoreni noveho stacku
			ImageProcessor ip = imageOrig.getStack().getProcessor(i).duplicate();    	
		    ip.filter(filterType);
		    stack.addSlice("Filtered_" + i, ip);                                    
		}
		
		imageNew.setStack(title, stack);		
		imageNew.setCalibration(imageOrig.getCalibration().copy());
		imageNew.setDimensions(nChannels, nSlices/nChannels, 1);
		
		CompositeImage comp = new CompositeImage(imageNew, ij.CompositeImage.GRAYSCALE);
		
		return comp;
	}
	
    /**
     * Converts original 3D bitmap data to image data ready for transform.
     * 8/16/32 bit data
     */
    protected void bitmapToData()
    {    	
		if (type == ImagePlus.GRAY8) {			
			for (int z = 1; z <= nSlices; z++)          					
				image2DToData(z, (byte[]) imageOrig.getStack().getProcessor(z).getPixels());		
		}
		else if (type == ImagePlus.GRAY16) {
			for (int z = 1; z <= nSlices; z++)          					
				image2DToData(z, (short[]) imageOrig.getStack().getProcessor(z).getPixels());					
		}	
		else if (type == ImagePlus.GRAY32) {
			for (int z = 1; z <= nSlices; z++)          					
				image2DToData(z, (float[]) imageOrig.getStack().getProcessor(z).getPixels());			
		}		   			
		else 
			throw new RuntimeException("Not supported");		
    }    

    /**
     * Converts original 3D bitmap data to image data ready for transform, by one slice.
     * 8/16/32 bit data
     * @param z - Slice to be converted.
     */    
	protected void bitmapToData(int z)
    {    	
		if (type == ImagePlus.GRAY8) 			         					
			image2DToData(z, (byte[]) imageOrig.getStack().getProcessor(z).getPixels());		
		else if (type == ImagePlus.GRAY16)          					
			image2DToData(z, (short[]) imageOrig.getStack().getProcessor(z).getPixels());					
		else if (type == ImagePlus.GRAY32)         					
			image2DToData(z, (float[]) imageOrig.getStack().getProcessor(z).getPixels());						
		else 
			throw new RuntimeException("Not supported");		
    }
        
	/**
	 * Finding minimal and maximal values in the output filtered image for intensity stretching.
	 */
    private void prepareStretchOI(int z)
    {
    	minOutputVal = Double.MAX_VALUE;
    	maxOutputVal = Double.MIN_VALUE;
    	
    	for (int y=0; y<height; y++)
        	for (int x=0; x<width; x++)
	    	{
	    		double outputVal = imageData[z-1][y][x];
	    		if (minOutputVal > outputVal)
	    			minOutputVal = outputVal;
	    		if (maxOutputVal < outputVal)
	    			maxOutputVal = outputVal;	    		
	    	}
    }
    
    /**
     * Linear stretch that stretches intensities of output (filtered) images according to min and max values of input (original) images.
     * Just to have the same contrast of both imput and output data.
     * @param x - Value to be stretched.
     * @return - Stretched value.
     */    
    private double stretchOI(int z, double x)  
    {
        double outMax = maxInputVals[z-1];             
        double outMin = minInputVals[z-1];
        double inMax = maxOutputVal;
        double inMin = minOutputVal;
        double maxGreyVal = 255.0;
        
        double scale;
        if (type == ImagePlus.GRAY32)								// not necessary to check extents
        {
            scale = (outMax - outMin) / (inMax - inMin);        
            return scale * (x - inMin) + outMin;        	
        }
        		
        if (type == ImagePlus.GRAY8)								// necessary to check extents - minimum and maximum
        	maxGreyVal = 255.0;
        if (type == ImagePlus.GRAY16)								// necessary to check extents - minimum and maximum
        	maxGreyVal = 65535.0;
        
        if (inMin != inMax)
        {
            scale = (outMax - outMin) / (inMax - inMin);        
            return scale * (x - inMin) + outMin;        	
        }
        else if (x > maxGreyVal) 
        	return maxGreyVal;
        else if ((x >= 0) && (x <= maxGreyVal))
        	return x;
        else
        	return 0;
    }
    
	/**
	 * Converts one slice from bitmap to imageData with reading minimum and maximum intensity value in the slice.
	 * @param z
	 * @param pixels - overloaded for different data types
	 */
	private void image2DToData(int z, byte[] pixels) {
		double minVal = Double.MAX_VALUE;
		double maxVal = Double.MIN_VALUE;				
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;					
		        // conversion byte to double
		        byte pixelByte = pixels[index];
		        double pixelDouble = (double) (pixelByte & 0xff);				
		        imageData[z-1][y][x] = pixelDouble;
		        
		        // min and max
	    		if (minVal > pixelDouble)
	    			minVal = pixelDouble;
	    		if (maxVal < pixelDouble)
	    			maxVal = pixelDouble;	    				        
			}
		}
		
		minInputVals[z-1] = minVal;
		maxInputVals[z-1] = maxVal;
	}
	
	private void image2DToData(int z, short[] pixels) {
		double minVal = Double.MAX_VALUE;
		double maxVal = Double.MIN_VALUE;				
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;					
		        // conversion short to double
		        short pixelShort = pixels[index];
		        double pixelDouble = (double) (pixelShort & 0xffff);				
		        imageData[z-1][y][x] = pixelDouble;
		        
		        // min and max
	    		if (minVal > pixelDouble)
	    			minVal = pixelDouble;
	    		if (maxVal < pixelDouble)
	    			maxVal = pixelDouble;	    				        		        
			}
		}
		
		minInputVals[z-1] = minVal;
		maxInputVals[z-1] = maxVal;		
	}
	
	private void image2DToData(int z, float[] pixels) {
		double minVal = Double.MAX_VALUE;
		double maxVal = Double.MIN_VALUE;				
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;							
		        imageData[z-1][y][x] = pixels[index];

		        // min and max
	    		if (minVal > pixels[index])
	    			minVal = pixels[index];
	    		if (maxVal < pixels[index])
	    			maxVal = pixels[index];	    				        		        		        
			}
		}
		
		minInputVals[z-1] = minVal;
		maxInputVals[z-1] = maxVal;				
	}

    /**
     * Converts transformed image data back to bitmap data.
     */
    protected void dataToBitmap()
    {
		if (type == ImagePlus.GRAY8) {
			for (int z = 1; z <= nSlices; z++)        					
				dataToImage2D(z, (byte[]) imageModif.getStack().getProcessor(z).getPixels());		
		}
		else if (type == ImagePlus.GRAY16) {
			for (int z = 1; z <= nSlices; z++)        					
				dataToImage2D(z, (short[]) imageModif.getStack().getProcessor(z).getPixels());					
		}
		else if (type == ImagePlus.GRAY32) {
			for (int z = 1; z <= nSlices; z++)        					
				dataToImage2D(z, (float[]) imageModif.getStack().getProcessor(z).getPixels());								
		}		   			
		else 
			throw new RuntimeException("Not supported");		
	}
    
    /**
     * Converts transformed image data back to bitmap data, by one slice.
     * @param z - Slice to be converted.
     */
    protected void dataToBitmap(int z)
    {
		if (type == ImagePlus.GRAY8)       					
				dataToImage2D(z, (byte[]) imageModif.getStack().getProcessor(z).getPixels());		
		else if (type == ImagePlus.GRAY16)       					
				dataToImage2D(z, (short[]) imageModif.getStack().getProcessor(z).getPixels());					
		else if (type == ImagePlus.GRAY32)        					
				dataToImage2D(z, (float[]) imageModif.getStack().getProcessor(z).getPixels());										   			
		else 
			throw new RuntimeException("Not supported");		
	}    
    
	/**
	 * Converts one slice from imageData or from specified array to bitmap.
	 * @param z
	 * @param slice - 2D array with pixel intensities
	 * @param pixels - from ImagePlus, overloaded for different data types
	 */
	private void dataToImage2D(int z, byte[] pixels) {
		
		prepareStretchOI(z);
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;
				pixels[index] = (byte) stretchOI(z, imageData[z-1][y][x]);
				
				if (DEBUG)
					try {
						bw.write("Value double = " + imageData[z-1][y][x] + "; "
								+ "Value stretched = " + stretchOI(z, imageData[z-1][y][x]) 
								+ "; Value byte = " + pixels[index]);
						bw.newLine();
						bw.flush();
					} catch (IOException e) {
						e.printStackTrace();
					} 
			}
		}	
	}
	
	private void dataToImage2D(double[][] slice, byte[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;
				pixels[index] = (byte) slice[y][x];
			}
		}	
	}	
	
	private void dataToImage2D(int z, short[] pixels) {
		prepareStretchOI(z);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int index = x + y * width;
				pixels[index] = (short) stretchOI(z, imageData[z - 1][y][x]);

				if (DEBUG)
					try {
						bw.write("Value double = " + imageData[z - 1][y][x] + "; " + "Value stretched = "
								+ stretchOI(z, imageData[z - 1][y][x]) + "; Value short = " + pixels[index]);
						bw.newLine();
						bw.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
	}
	
	private void dataToImage2D(double[][] slice, short[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;
				pixels[index] = (short) slice[y][x];
			}
		}	
	}		
	
	private void dataToImage2D(int z, float[] pixels) {
		
		prepareStretchOI(z);
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;
				pixels[index] = (float) stretchOI(z, imageData[z-1][y][x]);
			}
		}	
	}
	
	private void dataToImage2D(double[][] slice, float[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				int index = x + y * width;
				pixels[index] = (float) slice[y][x];
			}
		}	
	}
	
    /**
     * Modifies (suppresses) values of the transformed data before transforming it back.
     * @param z - Slice.
     * @param level - Value from 0 to 100 (representing percentage of values to suppress).
     * @return - Mask showing suppressed values.
     */
    protected void suppressWave(int z, int level)
    {
        double[] tempData = new double[imageSize];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tempData[x + y * width] = Math.abs(transformedData[z-1][y][x]);

        Arrays.sort(tempData);
        
        int perc = 0;
        double threshold = 0;
        if (level == 0)
        	threshold = 0;
        else if (level == 100)
        {
        	perc = imageSize - 1;
        	threshold = tempData[perc];
        }
        else
        {
            perc = (int)((imageSize / 100.0 * level) - 1.0);
            threshold = tempData[perc];        	
        }           

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (Math.abs(transformedData[z-1][y][x]) <= threshold)    
                    // It is done the way that with raising level it removes values that are more and more far from 0. 
                    // That way it doesnt remove information about approximation before it removes almost all detail. 
                    transformedData[z-1][y][x] = 0.0;
        return;
    }
    
    /**
     * Modifies (suppresses) values of the transformed data before transforming it back. Separately for Approximated and Detailed Coeffients.
     * @param z - Slice.
     * @param levelAC - Value from 0 to 100 (representing percentage of values to suppress for approximated coefficients).
     * @param levelDC - Value from 0 to 100 (representing percentage of values to suppress for detailed coefficients).
     * @return - Mask showing suppressed values.
     */
    protected void suppressWave_SeparatedCoeffs(int z, int levelAC, int levelDC)
    {
    	int borderX = width  / (int)Math.pow(2, scale);
    	int borderY = height / (int)Math.pow(2, scale);
    	
    	// copy Approximated Coefficients with considering the scale    	
    	int approxCoeffsSize = borderX * borderY;
    	double []approxCoeffs = new double [approxCoeffsSize];
    	
        for (int y = 0; y < borderY; y++)
            for (int x = 0; x < borderX; x++)
            	approxCoeffs[y*borderX + x] = Math.abs(transformedData[z-1][y][x]);    	
    	
        Arrays.sort(approxCoeffs);
        
        int percAC = 0;
        double thresholdAC = 0;
        if (levelAC == 0)
        	thresholdAC = 0;
        else if (levelAC == 100)
        {
        	percAC = approxCoeffsSize - 1;
        	thresholdAC = approxCoeffs[percAC];
        }
        else
        {
            percAC = (int)((approxCoeffsSize / 100.0 * levelAC) - 1.0);
            thresholdAC = approxCoeffs[percAC];        	
        }
                
        // thresholding
        for (int y = 0; y < borderY; y++)
            for (int x = 0; x < borderX; x++)
                if (Math.abs(transformedData[z-1][y][x]) <= thresholdAC)    
                    transformedData[z-1][y][x] = 0.0;
        
    	// copy Detailed Coefficients with considering the scale    	
    	int detailCoeffsSize = imageSize - (borderX * borderY);
    	double []detailCoeffs = new double [detailCoeffsSize]; 
    	
    	int index = 0;
        for (int y = borderY; y < height; y++)
            for (int x = borderX; x < width; x++)
            	detailCoeffs[index++] = Math.abs(transformedData[z-1][y][x]);
        
        for (int y = borderY; y < height; y++)
            for (int x = 0; x < borderX; x++)
            	detailCoeffs[index++] = Math.abs(transformedData[z-1][y][x]);
        
        for (int y = 0; y < borderY; y++)
            for (int x = borderX; x < width; x++)
            	detailCoeffs[index++] = Math.abs(transformedData[z-1][y][x]);
        
        Arrays.sort(detailCoeffs);
        
        // thresholding        
        int percDC = 0;
        double thresholdDC = 0;
        if (levelDC == 0)
        	thresholdDC = 0;
        else if (levelDC == 100)
        {
        	percDC = detailCoeffsSize - 1;
        	thresholdDC = detailCoeffs[percDC];
        }
        else
        {
            percDC = (int)((detailCoeffsSize / 100.0 * levelDC) - 1.0);
            thresholdDC = detailCoeffs[percDC];        	
        }        
                
        for (int y = borderY; y < height; y++)
            for (int x = borderX; x < width; x++)
                if (Math.abs(transformedData[z-1][y][x]) <= thresholdDC)    
                    transformedData[z-1][y][x] = 0.0;
        
        for (int y = borderY; y < height; y++)
            for (int x = 0; x < borderX; x++)
                if (Math.abs(transformedData[z-1][y][x]) <= thresholdDC)    
                    transformedData[z-1][y][x] = 0.0;
        
        for (int y = 0; y < borderY; y++)
            for (int x = borderX; x < width; x++)
                if (Math.abs(transformedData[z-1][y][x]) <= thresholdDC)    
                    transformedData[z-1][y][x] = 0.0;        
        // It is done the way that with raising level it removes values that are more and more far from 0. 
        // That way it doesnt remove information about approximation before it removes almost all detail. 
    }    
    
    /**
     * Modifies (denoise) values of the transformed data before transforming it back.
     * @param z - Slice processed.
     * @param sigma - Sigma parameter of the VisuShrink denoising.
     * @param hardThreshold - True if hard thresholding is to be used, false if soft is to be used.
     */
    protected void denoiseWave(int z, int sigma, boolean hardThreshold)   //using VisuShrink, i.e. the Universal Threshold (Donoho & Johnstone)
    {
        double threshold = sigma * Math.sqrt(2.0 * Math.log(imageSize));

        if (hardThreshold)
        {
            for (int y = 0; y < height; y++)
            {
                for (int x = 0; x < width; x++)               	
                {
                    if (Math.abs(transformedData[z-1][y][x]) < threshold)
                        transformedData[z-1][y][x] = 0.0;
                }
            }
        }
        else
        {
        	for (int y = 0; y < height; y++)
            {
            	for (int x = 0; x < width; x++)                
                {
                	transformedData[z-1][y][x] = Math.signum(transformedData[z-1][y][x]) * 
                			Math.max(0, Math.abs(transformedData[z-1][y][x]) - sigma);
                }
            }
        }
    }    
	
    /**
     * Converts transformed image data to wave bitmap data.
     * @param z - Slice processed.
     */
	private void dataToWave(int z)
    {
        int row = 0;
        int col = 0;
        int borderX = width / (int)Math.pow(2, scale);        
        int borderY = height / (int)Math.pow(2, scale);
        double intensityStretched;

		// WT Coeffs. for visualization
		if (Wavelet_Denoise.isShownWT())
		{	        
	        double[][] tempDataStretched = new double[height][width];        
	        double maxVal = 255.0;
	        for (int i = 0; i < imageSize; i++)
	        {
	            intensityStretched = Math.min(maxVal, Math.max(0, stretchWC(transformedData[z-1][row][col], (row < borderY && col < borderX))));            
	            tempDataStretched[row][col] = intensityStretched;
	            col++;
	            if (col == width)
	            {
	                col = 0;
	                row++;
	            }
	        }
	        
			if (type == ImagePlus.GRAY8)			
		        dataToImage2D(tempDataStretched, (byte[]) imageWave.getStack().getProcessor(z).getPixels());
			else if (type == ImagePlus.GRAY16)
		        dataToImage2D(tempDataStretched, (short[]) imageWave.getStack().getProcessor(z).getPixels());
			else if (type == ImagePlus.GRAY32) 
		        dataToImage2D(tempDataStretched, (float[]) imageWave.getStack().getProcessor(z).getPixels());
			else
				throw new RuntimeException("Not supported");
		}
		
		// No Stretched WT Coeffs. for visualization
		if (Wavelet_Denoise.isShownNoStretchWT())
		{
	        double[][] tempDataNoStretch = new double[height][width];			
	        row = 0;
	        col = 0;	        
			for (int i = 0; i < imageSize; i++)
	        {
	            tempDataNoStretch[row][col] = transformedData[z-1][row][col];
	            col++;
	            if (col == width)
	            {
	                col = 0;
	                row++;
	            }
	        }
			dataToImage2D(tempDataNoStretch, (float[]) imageWaveNoStretch.getStack().getProcessor(z).getPixels());
		}
    }
	
    /**
     * Changes scale (level of detail).
     * @param scale - Scale to be changed.
     * @return 0 if the scale cannot be used, otherwise returns maximal scale.
     */
    protected int changeScale(int scale)
    {
        int maxScale = (int)(Math.log10(width < height ? width : height) / Math.log10(2));

        if (scale < 1 || scale > maxScale)
            return 0;   //invalid scale
        else
        {
            setScale(scale);
            return maxScale;
        }
    }
	
	/**
     * Sets attributes <c>dlf</c>, <c>dhf</c>, <c>rlf</c> and <c>rlf</c> according to the selected filter.
     * @param filterMenuName - Selected filter name.
     */
    protected void setWaveletFilter(String filterMenuName)
    {	
    	switch (filterMenuName)
    	{
    		// Haar
    		case "Haar 1":    		 
    			waveletFilter.setFilter("haar1.flt");
		    	break;
    		
    		// Daubechies
    		case "Daubechies 1":    		 
    			waveletFilter.setFilter("daub1.flt");
		    	break;
    		case "Daubechies 2":    		 
    			waveletFilter.setFilter("daub2.flt");
		    	break;
    		case "Daubechies 3":    		 
    			waveletFilter.setFilter("daub3.flt");
		    	break;
    		case "Daubechies 4":    		 
    			waveletFilter.setFilter("daub4.flt");
		    	break;
    		case "Daubechies 5":    		 
    			waveletFilter.setFilter("daub5.flt");
		    	break;
    		case "Daubechies 6":    		 
    			waveletFilter.setFilter("daub6.flt");
		    	break;
    		case "Daubechies 7":    		 
    			waveletFilter.setFilter("daub7.flt");
		    	break;
    		case "Daubechies 8":    		 
    			waveletFilter.setFilter("daub8.flt");
		    	break;
    		case "Daubechies 9":    		 
    			waveletFilter.setFilter("daub9.flt");
		    	break;
    		case "Daubechies 10":    		 
    			waveletFilter.setFilter("daub10.flt");
		    	break;
    		case "Daubechies 11":    		 
    			waveletFilter.setFilter("daub11.flt");
		    	break;
    		case "Daubechies 12":    		 
    			waveletFilter.setFilter("daub12.flt");
		    	break;
    		case "Daubechies 13":    		 
    			waveletFilter.setFilter("daub13.flt");
		    	break;
    		case "Daubechies 14":    		 
    			waveletFilter.setFilter("daub14.flt");
		    	break;
    		case "Daubechies 15":    		 
    			waveletFilter.setFilter("daub15.flt");
		    	break;
    		case "Daubechies 16":    		 
    			waveletFilter.setFilter("daub16.flt");
		    	break;
    		case "Daubechies 17":    		 
    			waveletFilter.setFilter("daub17.flt");
		    	break;
    		case "Daubechies 18":    		 
    			waveletFilter.setFilter("daub18.flt");
		    	break;
    		case "Daubechies 19":    		 
    			waveletFilter.setFilter("daub19.flt");
		    	break;
    		case "Daubechies 20":    		 
    			waveletFilter.setFilter("daub20.flt");
		    	break;
		    	
    		// Symlets
    		case "Symlets 2":    		 
    			waveletFilter.setFilter("sym2.flt");
		    	break;
    		case "Symlets 3":    		 
    			waveletFilter.setFilter("sym3.flt");
		    	break;
    		case "Symlets 4":    		 
    			waveletFilter.setFilter("sym4.flt");
		    	break;
    		case "Symlets 5":    		 
    			waveletFilter.setFilter("sym5.flt");
		    	break;
    		case "Symlets 6":    		 
    			waveletFilter.setFilter("sym6.flt");
		    	break;
    		case "Symlets 7":    		 
    			waveletFilter.setFilter("sym7.flt");
		    	break;
    		case "Symlets 8":    		 
    			waveletFilter.setFilter("sym8.flt");
		    	break;
    		case "Symlets 9":    		 
    			waveletFilter.setFilter("sym9.flt");
		    	break;
    		case "Symlets 10":    		 
    			waveletFilter.setFilter("sym10.flt");
		    	break;
    		case "Symlets 11":    		 
    			waveletFilter.setFilter("sym11.flt");
		    	break;
    		case "Symlets 12":    		 
    			waveletFilter.setFilter("sym12.flt");
		    	break;
    		case "Symlets 13":    		 
    			waveletFilter.setFilter("sym13.flt");
		    	break;
    		case "Symlets 14":    		 
    			waveletFilter.setFilter("sym14.flt");
		    	break;
    		case "Symlets 15":    		 
    			waveletFilter.setFilter("sym15.flt");
		    	break;
    		case "Symlets 16":    		 
    			waveletFilter.setFilter("sym16.flt");
		    	break;
    		case "Symlets 17":    		 
    			waveletFilter.setFilter("sym17.flt");
		    	break;
    		case "Symlets 18":    		 
    			waveletFilter.setFilter("sym18.flt");
		    	break;
    		case "Symlets 19":    		 
    			waveletFilter.setFilter("sym19.flt");
		    	break;
    		case "Symlets 20":    		 
    			waveletFilter.setFilter("sym20.flt");
		    	break;
		    	
    		// Coiflets
    		case "Coiflets 1":    		 
    			waveletFilter.setFilter("coif1.flt");
		    	break;
    		case "Coiflets 2":    		 
    			waveletFilter.setFilter("coif2.flt");
		    	break;
    		case "Coiflets 3":    		 
    			waveletFilter.setFilter("coif3.flt");
		    	break;
    		case "Coiflets 4":    		 
    			waveletFilter.setFilter("coif4.flt");
		    	break;
    		case "Coiflets 5":    		 
    			waveletFilter.setFilter("coif5.flt");
		    	break;

    		// Biorthogonal
    		case "Biorthogonal 1.1":    		 
    			waveletFilter.setFilter("biortho1.flt");
		    	break;
    		case "Biorthogonal 1.3":    		 
    			waveletFilter.setFilter("biortho2.flt");
		    	break;
    		case "Biorthogonal 1.5":    		 
    			waveletFilter.setFilter("biortho3.flt");
		    	break;
    		case "Biorthogonal 2.2":    		 
    			waveletFilter.setFilter("biortho4.flt");
		    	break;
    		case "Biorthogonal 2.4":    		 
    			waveletFilter.setFilter("biortho5.flt");
		    	break;
    		case "Biorthogonal 2.6":    		 
    			waveletFilter.setFilter("biortho6.flt");
		    	break;
    		case "Biorthogonal 2.8":    		 
    			waveletFilter.setFilter("biortho7.flt");
		    	break;
    		case "Biorthogonal 3.1":    		 
    			waveletFilter.setFilter("biortho8.flt");
		    	break;
    		case "Biorthogonal 3.3":    		 
    			waveletFilter.setFilter("biortho9.flt");
		    	break;
    		case "Biorthogonal 3.5":    		 
    			waveletFilter.setFilter("biortho10.flt");
		    	break;
    		case "Biorthogonal 3.7":    		 
    			waveletFilter.setFilter("biortho11.flt");
		    	break;
    		case "Biorthogonal 3.9":    		 
    			waveletFilter.setFilter("biortho12.flt");
		    	break;
    		case "Biorthogonal 4.4":    		 
    			waveletFilter.setFilter("biortho13.flt");
		    	break;
    		case "Biorthogonal 5.5":    		 
    			waveletFilter.setFilter("biortho14.flt");
		    	break;
    		case "Biorthogonal 6.8":    		 
    			waveletFilter.setFilter("biortho15.flt");
		    	break;	    	

    		// Reverse Biorthogonal
    		case "Reverse Biorthogonal 1.1":    		 
    			waveletFilter.setFilter("revbiortho1.flt");
		    	break;
    		case "Reverse Biorthogonal 1.3":    		 
    			waveletFilter.setFilter("revbiortho2.flt");
		    	break;
    		case "Reverse Biorthogonal 1.5":    		 
    			waveletFilter.setFilter("revbiortho3.flt");
		    	break;
    		case "Reverse Biorthogonal 2.2":    		 
    			waveletFilter.setFilter("revbiortho4.flt");
		    	break;
    		case "Reverse Biorthogonal 2.4":    		 
    			waveletFilter.setFilter("revbiortho5.flt");
		    	break;
    		case "Reverse Biorthogonal 2.6":    		 
    			waveletFilter.setFilter("revbiortho6.flt");
		    	break;
    		case "Reverse Biorthogonal 2.8":    		 
    			waveletFilter.setFilter("revbiortho7.flt");
		    	break;
    		case "Reverse Biorthogonal 3.1":    		 
    			waveletFilter.setFilter("revbiortho8.flt");
		    	break;
    		case "Reverse Biorthogonal 3.3":    		 
    			waveletFilter.setFilter("revbiortho9.flt");
		    	break;
    		case "Reverse Biorthogonal 3.5":    		 
    			waveletFilter.setFilter("revbiortho10.flt");
		    	break;
    		case "Reverse Biorthogonal 3.7":    		 
    			waveletFilter.setFilter("revbiortho11.flt");
		    	break;
    		case "Reverse Biorthogonal 3.9":    		 
    			waveletFilter.setFilter("revbiortho12.flt");
		    	break;
    		case "Reverse Biorthogonal 4.4":    		 
    			waveletFilter.setFilter("revbiortho13.flt");
		    	break;
    		case "Reverse Biorthogonal 5.5":    		 
    			waveletFilter.setFilter("revbiortho14.flt");
		    	break;
    		case "Reverse Biorthogonal 6.8":    		 
    			waveletFilter.setFilter("revbiortho15.flt");
		    	break;	    	    	
		    	
    		// Discrete Meyer 1
    		case "Discrete Meyer 1":    		 
    			waveletFilter.setFilter("meyer1.flt");
		    	break;	    	    	
    		
    		default:
		    	waveletFilter.setFilter("haar1.flt");
		    	break;
	    }
        
    	transposeFilters();
    }
	
    /**
     * Transpose selected filters so that they can be used for convolution.
     */
    private void transposeFilters()
    {
        int len = waveletFilter.dlf.length;
        double[] dlfT = new double[len];
        double[] dhfT = new double[len];
        double[] rlfT = new double[len];
        double[] rhfT = new double[len];
        for (int i = 0; i < waveletFilter.dlf.length; i++) //transposition
        {
            dlfT[i] = waveletFilter.dlf[len - i - 1];
            dhfT[i] = waveletFilter.dhf[len - i - 1];
            rlfT[i] = waveletFilter.rlf[len - i - 1];
            rhfT[i] = waveletFilter.rhf[len - i - 1];
        }
        waveletFilter.dlf = dlfT;
        waveletFilter.dhf = dhfT;
        waveletFilter.rlf = rlfT;
        waveletFilter.rhf = rhfT;
    }	
	
	/**
	 * Computes necessary values of Wavelet Coefficients needed in the <c>stretch</c> method.
	 */
    private void prepareStretchWC(int z)
    {
        if (Wavelet_Denoise.isShownWT())
        {
	    	int borderX = width / (int)Math.pow(2, scale);
	        int borderY = height / (int)Math.pow(2, scale);
	
	        double[] approx = new double[borderX * borderY];
	        double[] detail = new double[imageSize - approx.length];
	        int indexA = 0;
	        int indexD = 0;
	        for (int y = 0; y < height; y++)
	            for (int x = 0; x < width; x++)
	                if (y < borderY && x < borderX)
	                {
	                    approx[indexA] = transformedData[z-1][y][x];
	                    indexA++;
	                }
	                else
	                {
	                    detail[indexD] = transformedData[z-1][y][x];
	                    indexD++;
	                }
	
	        int percentile = 100;
	        Arrays.sort(approx);
	        lowCoeffA = approx[approx.length / percentile];
	        highCoeffA = approx[approx.length - 1 - approx.length / percentile];
	        
	        Arrays.sort(detail);
	
	        lowCoeffD = detail[detail.length / percentile];
	        highCoeffD = detail[detail.length - 1 - detail.length / percentile];
        }
    }	
	
    /**
     * Linear stretch that stretches Wavelet Coefficients - approximation and detail separatelly
     * @param x - Value to be stretched.
     * @param isApprox - True if the value is in approximation, false if in detail.
     * @return
     */
    private double stretchWC(double x, boolean isApprox)  
    {
        double outMax = 255.0;             
        double outMin = 0.0;
        double inMax = isApprox ? highCoeffA : highCoeffD;
        double inMin = isApprox ? lowCoeffA : lowCoeffD;
        
        double scale;
        if (inMin != inMax)
        {
            scale = (outMax - outMin) / (inMax - inMin);        
            return scale * (Math.min(inMax, Math.max(inMin, x)) - inMin) + outMin;        	
        }
        else if (x > 0) 
        	return 255.0;
        else 
        	return 0.0;
    }	
	
	/**
	 * Algorithm for 1D Fast Wavelet Transform. It is used in <c>FWT2D</c> method.
	 * @param data - 1D input data. The result of the transform is stored here afterwards.
	 */
    private void FWT(double[] data)
    {
        int dataLen = data.length;
        int kernelLen = waveletFilter.dlf.length;
        int mid = dataLen / 2;
        int midKernel = kernelLen / 2;
        double sumL, sumH;
        int pad = dataLen * 30;   //in order not to get index < 0 //TODO for smaller images a greater value might be needed, or some walkaround

        double[] approx = new double[dataLen];
        double[] detail = new double[dataLen];

        for (int i = 0; i < dataLen; i++)
        {
            sumL = 0.0;
            sumH = 0.0;
            for (int j = 0; j < kernelLen; j++)
            {
                sumL += data[(i - midKernel + j + pad) % dataLen] * waveletFilter.dlf[j];
                sumH += data[(i - midKernel + j + pad) % dataLen] * waveletFilter.dhf[j];
            }
            approx[i] = sumL;
            detail[i] = sumH;
        }

        int k;
        for (int i = 0; i < mid; i++)
        {
            k = i * 2;
            data[i] = approx[k];
            data[mid + i] = detail[k];
        }
    }	
	
    /**
     * Algorithm for 2D Fast Wavelet Transform. It performs 1D FWT algorithm for all rows and then all columns.
     * @param z - slice
     */
    private void FWT2D(int z)
    {
        double[][] tempData = new double[height][width]; //temporary data buffer for just this occurence, not needed after transform
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tempData[y][x] = imageData[z-1][y][x];

        double[] row;
        double[] col;

        for (int k = 0; k < scale; k++)
        {
            int lev = 1 << k;

            int levCols = height / lev;
            int levRows = width / lev;

            row = new double[levCols];
            for (int x = 0; x < levRows; x++)
            {
                for (int y = 0; y < row.length; y++)
                    row[y] = tempData[y][x];

                FWT(row);

                for (int y = 0; y < row.length; y++)
                    tempData[y][x] = row[y];
            }

            col = new double[levRows];
            for (int y = 0; y < levCols; y++)
            {
                for (int x = 0; x < col.length; x++)
                    col[x] = tempData[y][x];

                FWT(col);

                for (int x = 0; x < col.length; x++)
                    tempData[y][x] = col[x];
            }
        }

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                transformedData[z-1][y][x] = tempData[y][x];
    }

    /**
     * Algorithm for 1D Inverse Fast Wavelet Transform. It is used in <c>IWT2D</c> method.
     * @param data - 1D input data. The result of the transform is stored here afterwards.
     */
    private void IWT(double[] data)
    {
        int dataLen = data.length;
        int kernelLen = waveletFilter.rlf.length;
        int mid = dataLen / 2;
        int midKernel = kernelLen / 2;
        int k;
        double sumL, sumH;
        int pad = dataLen * 20;

        double[] approxUp = new double[dataLen];
        double[] detailUp = new double[dataLen];
        double[] temp = new double[dataLen];

        for (int i = 0; i < mid; i++)
        {
            k = i * 2;
            approxUp[k] = data[i];
            approxUp[k + 1] = 0.0;
            detailUp[k] = data[mid + i];
            detailUp[k + 1] = 0.0;
        }

        for (int i = 0; i < dataLen; i++)
        {
            sumL = 0;
            sumH = 0;
            for (int j = 0; j < kernelLen; j++)
            {
                sumL += approxUp[(i - midKernel + j + pad) % dataLen] * waveletFilter.rlf[j];
                sumH += detailUp[(i - midKernel + j + pad) % dataLen] * waveletFilter.rhf[j];
            }
            temp[i] = sumL + sumH;
        }

        for (int i = 0; i < dataLen - 1; i++)
        {
            data[i] = temp[i + 1];
        }
        data[dataLen - 1] = temp[0];
    }
    
    /**
     * Algorithm for 2D Inverse Fast Wavelet Transform. It performs 1D IWT algorithm for all columns and then all rows.
     * @param z - slice
     */
    private void IWT2D(int z)
    {
        double[][] tempData = new double[height][width];
        
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tempData[y][x] = transformedData[z-1][y][x];        
        
        double[] col;
        double[] row;

        for (int k = scale - 1; k >= 0; k--)
        {
            int lev = 1 << k;

            int levCols = height / lev;
            int levRows = width / lev;

            col = new double[levRows];
                
            for (int y = 0; y < levCols; y++)
            {
                for (int x = 0; x < col.length; x++)
                    col[x] = tempData[y][x];               

                IWT(col);

                for (int x = 0; x < col.length; x++)
                    tempData[y][x] = col[x];
            }

            row = new double[levCols];
            for (int x = 0; x < levRows; x++)
            {
                for (int y = 0; y < row.length; y++)
                    row[y] = tempData[y][x];

                IWT(row);

                for (int y = 0; y < row.length; y++)
                    tempData[y][x] = row[y];
            }
        }
        
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            	imageData[z-1][y][x] = tempData[y][x];
    }    
    
    /**
     * Forward wavelet transform.
     * Data preparation, modification and transformation.
     * @param z - slice to be computed.
     */
    protected void fwdTransform(int z)
    {
		FWT2D(z);				// FWT2D from imageData into transformedData by slices
		prepareStretchWC(z);	// intensity stretch preparation of transformedData slices		
    }
   
    /**
     * Backward (inverse) wavelet transform.
     * Inverse transformation, data convertion back to bitmap.
     * @param z - slice to be computed. 
     */
    protected void invTransform(int z)
    {

		dataToWave(z);			// stretched transformedData into imageWave for visualization
    	IWT2D(z);				// IWT2D from transformedData into imageData by slices
    }
    
    public double[][][] getCoefficients(){
    	return transformedData;
    }
}

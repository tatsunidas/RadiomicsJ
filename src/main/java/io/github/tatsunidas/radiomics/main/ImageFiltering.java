/*
 * Copyright 2022 Tatsuaki Kobayashi

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package io.github.tatsunidas.radiomics.main;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import io.github.tatsunidas.ij.plugin.wavelet.Wavelet_Denoise;

/**
 * 
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *
 */
public class ImageFiltering {
	
	/**
	 * |LL|HL|
	 *  -------
	 * |LH|HH|
	 * 
	 * @param imp
	 * @param rect_size: size % 2 = 0 !
	 * @return coeffs (LL,HL,LH,HH images)
	 */
	public static ImagePlus getWaveletCoeffs(ImagePlus imp, int rect_size){
		if(rect_size % 2 != 0) {
			System.out.println("Invalid rect size inputted, forcely change to 256.");
			rect_size = 256;
		}
		Calibration cal = imp.getCalibration().copy();
		Wavelet_Denoise wd = new Wavelet_Denoise();
		int s = imp.getStackSize();
		int w = rect_size;
		int h = rect_size;
		ImageStack stack = new ImageStack(rect_size/2, rect_size/2);
		for(int z=0;z<s;z++) {
			
			ImageProcessor tip = imp.getStack().getProcessor(z+1).duplicate().resize(rect_size, rect_size);
			ImagePlus timp = new ImagePlus(""+(z+1), tip);
			wd.setup("", timp);
			wd.execute(timp.getProcessor(), false);
			ImageProcessor ll = new FloatProcessor(w/2, h/2);
			ImageProcessor lh = new FloatProcessor(w/2, h/2);
			ImageProcessor hl = new FloatProcessor(w/2, h/2);
			ImageProcessor hh = new FloatProcessor(w/2, h/2);
			double[][][] res = wd.imageData.getCoefficients();
			for (int wy = 0; wy < h; wy++) {
				for (int wx = 0; wx < w; wx++) {
					if(wx < w/2 && wy < h/2) {
						ll.setf(wx, wy, (float) res[0][wy][wx]);
					}else if(wx >= w/2 && wy < h/2) {
						lh.setf(wx-(w/2), wy, (float) res[0][wy][wx]);
					}else if(wx < w/2 && wy >= h/2) {
						hl.setf(wx, wy-(h/2), (float) res[0][wy][wx]);
					}else {
						hh.setf(wx-(w/2), wy-(h/2), (float) res[0][wy][wx]);
					}
				}
			}
			stack.addSlice(ll);
			stack.addSlice(lh);
			stack.addSlice(hl);
			stack.addSlice(hh);
		}
		ImagePlus wavCoeff = new ImagePlus("wavelet-coeffs",stack);
		cal.disableDensityCalibration();
		wavCoeff.setCalibration(cal);
		return wavCoeff;
	}

}

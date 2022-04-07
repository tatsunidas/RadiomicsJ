package wavelet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ij.IJ;

class WaveletFilters {
	
	/**
	 * Represent decomposition and reconstrution low-pass and high-pass filters.
	 */
	protected double[] dlf, dhf, rlf, rhf;
	
	/**
	 * Reads paremeters of wavelet filters from corresponding files.
	 * @param filterName - Name of the filter file.
	 * @return String - Content of the file.
	 */
	private String getText(String filterName) {		        
		String text = "";
        try {
            // get the text resource as a stream
            InputStream is = getClass().getResourceAsStream("/waveletfilters"+File.separator+filterName);
            if (is==null) {
                IJ.showMessage("Wavelet_Denoise", "Filter file not found at " + filterName);
                return "";
            }
            InputStreamReader isr = new InputStreamReader(is);
            StringBuffer sb = new StringBuffer();
            char [] b = new char [8192];
            int n;
            //read a block and append any characters
            while ((n = isr.read(b)) > 0)
                sb.append(b,0,n);
            text = sb.toString();
        }
        catch (IOException e) {
            String msg = e.getMessage();
            if (msg==null || msg.equals(""))
                msg = "" + e;	
            IJ.showMessage("Wavelet_Denoise", msg);
        }
        return text;
    }	
		
	protected void setFilter(String filterName) {
		
		String content = getText(filterName);
		String[] lines;
		
		if (!content.isEmpty())
			lines = content.split("\\r?\\n|\\r");
		else
			return;		
		
		int filterSize = (lines.length - 3) / 4;
		
		dlf = new double[filterSize];
        dhf = new double[filterSize];
        rlf = new double[filterSize];
        rhf = new double[filterSize];
        
        int filterNumber = 0;
        int filterIndex = 0;
        for (int i = 0; i < lines.length; i++)
        {
            if (lines[i].isEmpty())
            {
                filterIndex = 0;
                filterNumber++;
            }
            else
            {
                switch (filterNumber)
                {
                    case 0:
                            dlf[filterIndex] = Double.parseDouble(lines[i]);
                            break;
                    case 1:
                            dhf[filterIndex] = Double.parseDouble(lines[i]);
                            break;
                    case 2:
                            rlf[filterIndex] = Double.parseDouble(lines[i]);
                            break;
                    case 3:
                            rhf[filterIndex] = Double.parseDouble(lines[i]);
                            break;
                }
                filterIndex++;
            }
        }        
	}
}


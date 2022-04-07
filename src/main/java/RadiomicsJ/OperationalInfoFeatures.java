package RadiomicsJ;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import ij.IJ;
import ij.ImagePlus;
import ij.util.DicomTools;

public class OperationalInfoFeatures {
	
	HashMap<String, String> info = new HashMap<>();
	
	public OperationalInfoFeatures(ImagePlus img) {
		info = new HashMap<>();
		String os_name = "win";
		if(IJ.isMacintosh() || IJ.isMacOSX()) {
			os_name = "mac";
		}else if(IJ.isLinux()) {
			os_name = "lunux";
		}
		
		Calendar cl = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String date_str = sdf.format(cl.getTime());
        
		String studyDate = DicomTools.getTag(img, "0008,0023");
		String bod = DicomTools.getTag(img, "0010,0030");
		String age = DicomTools.getTag(img, "0010,1010");
		Integer age_ = calcAge(age,studyDate,bod);
		
		info.put("Version", RadiomicsJ.version);
		info.put("OS", os_name);
		info.put("ExtractDate", date_str);
		info.put("StudyDate", studyDate);
		info.put("Modality", DicomTools.getTag(img, "0008,0060"));
		info.put("Manufacturer", DicomTools.getTag(img, "0008,0070"));
		info.put("ModelName", DicomTools.getTag(img, "0008,1090"));
		info.put("SubjectName", DicomTools.getTag(img, "0010,0010"));
		info.put("SubjectID", DicomTools.getTag(img, "0010,0020"));
		info.put("BirthOfDate", bod);
		info.put("Sex", DicomTools.getTag(img, "0010,0040"));
		info.put("Age", age_ != null ? String.valueOf(age_):null);
	}
	
	private Integer calcAge(String ageFromTag, String studyDate, String bod) {
		if(ageFromTag != null && ageFromTag.trim().length() != 0) {
			return Integer.valueOf(ageFromTag.trim());
		}
		if(studyDate == null || bod == null) {
			return null;
		}
		studyDate = studyDate.trim().replace(" ", "");//half space
		bod = bod.trim().replace(" ", "");//half space
		Date sd = convert2Date(studyDate);
		Date bd = convert2Date(bod);
		if(sd==null || bd == null) {
			return null;
		}
		if(!sd.after(bd)) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(sd);
		int studyYear = cal.get(Calendar.YEAR);
		int studyMonth = cal.get(Calendar.MONTH);
		int studyDay = cal.get(Calendar.DATE);
		cal.setTime(bd);
		int bodYear = cal.get(Calendar.YEAR);
		int bodMonth = cal.get(Calendar.MONTH);
		int bodDay = cal.get(Calendar.DATE);
		if(studyMonth >= bodMonth) {
			if(studyDay >= bodDay) {
				return studyYear - bodYear;
			}
		}
		int age = studyYear - bodYear -1;
		return age < 0 ? 0:age;
	}
	
	private java.util.Date convert2Date(String afterTrimmed) {
		Date date = null;
		if(afterTrimmed.contains("-")) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			try {
				date = dateFormat.parse(afterTrimmed);
			} catch (ParseException e) {
				return null;
			}
		}else if(afterTrimmed.contains(".")) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
			try {
				date = dateFormat.parse(afterTrimmed);
			} catch (ParseException e) {
				return null;
			}
		}else if(afterTrimmed.contains("/")) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			try {
				date = dateFormat.parse(afterTrimmed);
			} catch (ParseException e) {
				return null;
			}
		}else {
			return null;
		}
		return date;
	}
	
	public HashMap<String,String> getInfo(){
		return info;
	}
}

package sandbox;

public class TestOthers {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		int w = 3;
//		int h = 4;
//		int s = 5;
//		Integer[][][] v = new Integer[w][h][s];
//		
//		System.out.println(v.length);
//		System.out.println(v[0].length);
//		System.out.println(v[0][0].length);
		
		System.out.println(Math.round(1.287));
		System.out.println(Math.round(0.0087));
		
		double a = 0.00156;
		double b = 0.00156965;
		String numString = String.valueOf(a);
		int decimal = numString.substring(numString.indexOf(".")+1).length();
		System.out.println(decimal);
	    double roundOff = Math.round(b * Math.pow(10, decimal))/Math.pow(10, decimal);
		System.out.println(roundOff);
	}

}

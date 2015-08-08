package app.ai.imgproc;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

public class Helpers {

	//private static final String helpTAG = "imgproc::Helpers";
	
	public Helpers() {};
	
    public native void normalize(double thresh, long addrX, long addrY, long addrM);
    public native void matrixMagnitude(long addrX, long addrY, long addrM);
    public native void nativeGaussBlur(long addrSRC, long addrDST, int blrX, int blrY, double sigX, double  sigY);
    public native void invert(long addr);
    public native void circle(long addr, double x, double y, int size, int color);
		
	public double computeDynamicThreshold(Mat mat, double stdDevFactor) {
		MatOfDouble std = new MatOfDouble(), mean = new MatOfDouble();
		Core.meanStdDev(mat, mean, std);
		double stdDev = std.get(0,0)[0] / Math.sqrt(mat.rows()*mat.cols());
		return stdDevFactor *stdDev + mean.get(0,0)[0];
	}
}


package app.ai.imgproc;

import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;

public class FindEyes {

	//private final String eyeTAG = getClass().getSimpleName();;
	
	public static int eyeRegW, eyeRegH; //width and height of eye region
	public static double[] cEye; //coordinates of eyes center (relative to overall image)
	
	private static int rows, cols;
	private static Helpers _hp = new Helpers();
	
	public FindEyes() { cEye = new double[2]; }
		
	public Point unscalePoint(Point p, Rect origSize) {
		float ratio = ( (float) ProcVars.kFastEyeWidth/origSize.width );
		int x = (int) (p.x / ratio);
		int y = (int) (p.y / ratio);
		return (new Point(x, y) );
	}
	
	public Mat scaleToFastSize(Mat src) {
		Mat resized = src.clone();
		if (resized.cols() > 0)
		Imgproc.resize(src, resized, new Size(ProcVars.kFastEyeWidth, ProcVars.kFastEyeWidth/src.cols() * src.rows()));
			
		return resized;
	}

	public Mat computeGradient(Mat mat, int rows, int cols) {
		Mat out = Mat.zeros(rows, cols, CvType.CV_64F);
		
		for (int y = 0; y < rows; y++) {
			out.put( y, 0, mat.get(y, 1)[0] - mat.get(y, 0)[0] );
			
			for (int x = 1; x < cols - 1; x++)
				out.put( y, x, (mat.get(y, x+1)[0] - mat.get(y, x-1)[0]) / 2.0 );
			
			out.put(y, cols-1, mat.get(y, cols-1)[0] - mat.get(y, cols-2)[0]);
		}
		return out;
	}

	//declare native code
    public native void bottleNeck(long addrX, long addrY, long addrO);
    public native void normalize(double thresh, long addrX, long addrY, long addrM);
    //public native void gradient(long addrM, long addrO);
    //public native void gradientT(long addrM, long addrO);    
	
	/**
	 * Pupil detection algorithm; passed image should be scaled down to just the face region.
	 * @param face - image containing only a face
	 * @param eye - rectangle that dictates where eye region is within passed face image
	 * @return - pupil x&y coordinate relative to eye region (NOT face region)
	 */
	public Point findEyeCenter(Mat face, Rect eye) {
		double gradientThresh;
		//double gx, gy;
		Mat eyeROI = scaleToFastSize( face.submat(eye) );
		rows = eyeROI.rows()-1; cols = eyeROI.cols()-1;
		
		//if (ProcVars.debug) Core.rectangle(face, eye.tl(), eye.br(), ProcVars._COLOR); //draw eye region - for debugging
    	
		//find the gradient
		//Mat gradX = Mat.zeros(rows, cols, CvType.CV_64F), gradY = Mat.zeros(rows, cols, CvType.CV_64F);
		//gradient( eyeROI.getNativeObjAddr(), gradX.getNativeObjAddr() );
		//gradientT( eyeROI.getNativeObjAddr(), gradY.getNativeObjAddr() );
		
		Mat gradX = computeGradient(eyeROI, rows, cols);
		Mat gradY = computeGradient(eyeROI.t(), cols, rows).t();
		
		//compute all pixel magnitudes
		Mat mags = _hp.matrixMagnitude(gradX, gradY, rows, cols);
		gradientThresh = _hp.computeDynamicThreshold(mags, ProcVars.kGradientThresh); //compute all pixel threshold
		//normalize
		normalize( gradientThresh, gradX.getNativeObjAddr(), gradY.getNativeObjAddr(), mags.getNativeObjAddr() );
		//create a blurred and inverted image for weighting
		Mat weight = new Mat();
		Imgproc.GaussianBlur(eyeROI, weight, (new Size(ProcVars.kWeightBlurSize, ProcVars.kWeightBlurSize)), 0, 0);

		for (int y = 0; y < rows+1; y++)
			for (int x = 0; x < cols+1; x++) {
				double[] tmp =  weight.get(y, x);
				tmp[0] = 255 - tmp[0];
				weight.put(y, x, tmp);
			}
		
		//run algorithm with native processing to reduce lag
		Mat outSum = Mat.zeros(rows, cols, CvType.CV_64F);
        bottleNeck( gradX.getNativeObjAddr(), gradY.getNativeObjAddr(), outSum.getNativeObjAddr() );

		//find the maximum point
		Core.MinMaxLocResult mmr = Core.minMaxLoc(outSum);
		eyeROI.release(); gradX.release(); gradY.release(); mags.release(); weight.release(); outSum.release();
		
		return unscalePoint(mmr.maxLoc, eye);
	}
	
	/**
	 * Locates pupils for a given face within an image; assumes the image contains a face
	 * 
	 * @param frame_gray - image processed to the highest possible grey scale 
	 * @param face - face within image that module must locate pupil in
	 * @param leftEye - true if user wants the left pupil, false otherwise
	 * @return x & y coordinates relative to image
	 */
	public Point getPupil(Mat frame_gray, Rect face, boolean leftEye) {
		Mat faceROI = frame_gray.submat(face);
		Point pupil = new Point(-1, -1);

		//if (ProcVars.debug) Core.rectangle(frame_gray, face.tl(), face.br(), ProcVars._COLOR); //draw eye region - for debugging

		//determine a face to eye ratio and positioning based on percentages
		eyeRegW = (int) (face.width * (ProcVars.kEyePercentWidth/100.0));
		eyeRegH = (int) (face.height * (ProcVars.kEyePercentHeight/100.0));
		int eyeDexY = (int) (face.height * (ProcVars.kEyePercentTop/100.0));
		int eyeDexX = (int) (face.width * (ProcVars.kEyePercentSide/100.0));
		Rect lEyeReg = new Rect(eyeDexX, eyeDexY, eyeRegW, eyeRegH);
		double[] cFace = {face.width/2.0 + face.x, face.height/2.0 + face.y};

		if (leftEye) { //detect pupils for left eye region (the camera's left, not the model's)
			// 4 and 1 accommodate minor int arithmetic value lost in calculations
			cEye[0] = cFace[0] - lEyeReg.width - 4; cEye[1] = cFace[1] - lEyeReg.y - 1;
			pupil = findEyeCenter(faceROI,lEyeReg);
			pupil.x += cEye[0];
			pupil.y += cEye[1];

		} else {
			Rect rEyeReg = new Rect(face.width - eyeRegW - eyeDexX, eyeDexY, eyeRegW,eyeRegH);
			cEye[0] = cFace[0] + 4; cEye[1] = cFace[1] - rEyeReg.y - 1;
			pupil = findEyeCenter(faceROI,rEyeReg);
			pupil.x += cEye[0];
			pupil.y += cEye[1];
		}
		
		if (ProcVars.debug) Core.circle(frame_gray, pupil, ProcVars._THICK, ProcVars._COLOR);
		faceROI.release();
		
		return pupil;
	}

}
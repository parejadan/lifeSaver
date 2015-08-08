
package app.ai.imgproc;

import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;

public class FindEyes {

	//private final String TAG = "FindEyes";
	
	public static int eyeRegW, eyeRegH; //width and height of eye region
	public static double[] cEye; //coordinates of eyes center (relative to overall image)
	
	private static int rows, cols;
	private static Helpers _hp = new Helpers();
	
	public FindEyes() { cEye = new double[2]; }
		
	public Point unscalePoint(Point p, Rect origSize) {
		float ratio = ( (float) ProcV.kFastEyeWidth/origSize.width );
		p.x = (int) (p.x / ratio);
		p.y = (int) (p.y / ratio);
		return p;
	}
	
	public Mat scaleToFastSize(Mat src) {
		Mat resized = src.clone();
		if (resized.cols() > 0)
		Imgproc.resize(src, resized, new Size(ProcV.kFastEyeWidth, ProcV.kFastEyeWidth/src.cols() * src.rows()));
		src.release();
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
    
	/**
	 * Pupil detection algorithm; passed image should be scaled down to just the face region.
	 * @param face - image containing only a face
	 * @param eye - rectangle that dictates where eye region is within passed face image
	 * @return - pupil x & y coordinate relative to eye region (NOT face region)
	 */
	public Point findEyeCenter(Mat face, Rect eye) {
		double gradientThresh;
		
		//if (ProcV.debug) Core.rectangle(face, eye.tl(), eye.br(), ProcV._COLOR); //draw eye region - for debugging
		Mat eyeROI = scaleToFastSize( face.submat(eye) );
		rows = eyeROI.rows()-1; cols = eyeROI.cols()-1;
				
		Mat gradX = computeGradient(eyeROI, rows, cols);
		Mat gradY = computeGradient(eyeROI.t(), cols, rows).t();
		
		//compute all pixel magnitudes
		
		Mat mags = Mat.zeros(rows, cols, CvType.CV_64F);
		_hp.matrixMagnitude( gradX.getNativeObjAddr(), gradY.getNativeObjAddr(), mags.getNativeObjAddr() );
		
		gradientThresh = _hp.computeDynamicThreshold(mags, ProcV.kGradientThresh); //compute all pixel threshold
		//normalize
		_hp.normalize( gradientThresh, gradX.getNativeObjAddr(), gradY.getNativeObjAddr(), mags.getNativeObjAddr() );
		//create a blurred and inverted image for weighting
		Mat weight = new Mat();
		_hp.nativeGaussBlur(eyeROI.getNativeObjAddr(), weight.getNativeObjAddr(), ProcV.kWeightBlurSize, ProcV.kWeightBlurSize, 0, 0);
		_hp.invert( weight.getNativeObjAddr() );
		
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
		Point pupil = new Point(0.0, 0.0);

		//if (ProcV.debug) Core.rectangle(frame_gray, face.tl(), face.br(), ProcV._COLOR); //draw eye region - for debugging

		//determine a face to eye ratio and positioning based on percentages
		eyeRegW = (int) (face.width * (ProcV.kEyePercentWidth/100.0));
		eyeRegH = (int) (face.height * (ProcV.kEyePercentHeight/100.0));
		int eyeDexY = (int) (face.height * (ProcV.kEyePercentTop/100.0));
		int eyeDexX = (int) (face.width * (ProcV.kEyePercentSide/100.0));
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
		
		if (ProcV.debug) _hp.circle( frame_gray.getNativeObjAddr(), pupil.x, pupil.y, ProcV._THICK, ProcV._color );
		faceROI.release();
		
		return pupil;
	}

}
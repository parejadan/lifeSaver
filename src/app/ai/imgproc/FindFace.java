package app.ai.imgproc;

import java.util.ArrayList;

//import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

public class FindFace {

    //private final String TAG = "FindFace";
	
	private static CascadeClassifier detector;
	
	public FindFace(CascadeClassifier faceClassifier) { detector = faceClassifier; }
	
	/**
	 * Uses a cascade-classifier to detect the face in a given image. By default it returns
	 *  the largest face.
	 * @param inputFrame - original image to extract face from
	 * @return ProcStruct - structure to store processed input frame and detected face region
	 */
	public ProcStruct getFace(Mat inputFrame) {
        ArrayList<Mat> mv = new ArrayList<Mat>(3);
        MatOfRect faces = new MatOfRect();
        Rect mxFace = null;

        //read in frame and save its most gray-scale version
        org.opencv.core.Core.split(inputFrame, mv);
        Mat mGray = mv.get(2);
        if (ProcV.mAbsoluteFS == 0) //if face size is not initialize, estimate a possible minimum size
        	ProcV.mAbsoluteFS = (int) (mGray.rows() * ProcV.mRelativeFS);
        
        detector.detectMultiScale(mGray, faces, 1.1, 2, 2,
        		new org.opencv.core.Size(ProcV.mAbsoluteFS, ProcV.mRelativeFS), new org.opencv.core.Size());
        Rect[] facesArray = faces.toArray();

        if (facesArray.length > 0) {
            mxFace = facesArray[0];
            for (Rect tmp : facesArray) //locate driver's face by using the largest object
            	if (tmp.height+tmp.width > mxFace.height+mxFace.width) //addition is considered faster arithmetic than multiplication
            		mxFace = tmp;
            
            //if (ProcV.debug) Core.rectangle(mGray, mxFace.br(), mxFace.tl(), ProcV._COLOR, ProcV._THICK);
        }

        mv.get(0).release(); mv.get(1).release(); //address any possible memory leakage
        return new ProcStruct(mGray, mxFace);
	}
	
}

package app.ai.lifesaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import app.ai.imgproc.FindEyes;
import app.ai.imgproc.FindFace;
import app.ai.imgproc.ProcStruct;
import app.ai.stats.Stats;

public class TrainActivity extends Activity implements CvCameraViewListener2 {

    private static final String camTAG = "TrainingActivity";
    
    private CameraBridgeViewBase mOpenCvCameraView;
    private static int mxFwidth = 480;
    private static int mxFheight =  480;
    private static Mat frame = null;
    private static LimQueue<Point> trainingPnts = new LimQueue<Point>(CoreVars.queueSize+1);

    private static FindFace _faceD; //face detection object
    private static FindEyes _eyeD; //pupil detection object
    private static Stats _stool = new Stats();
    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) { //opencv library initialization 
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(camTAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        _faceD = new FindFace( new CascadeClassifier(mCascadeFile.getAbsolutePath()) );
                        
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(camTAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.setMaxFrameSize(mxFwidth, mxFheight);
                    mOpenCvCameraView.enableView();
                } break;
                
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public TrainActivity() {
        Log.i(camTAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(camTAG, "Collecting data..");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view); //set view to screen
        mOpenCvCameraView.setCameraIndex(1); //use front camera
        //mOpenCvCameraView.disableFpsMeter();
        mOpenCvCameraView.setMaxFrameSize(mxFwidth, mxFheight);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this); //receive image, process it then display
    }

    @Override
    public void onPause() { //stop frame collection
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {  //load opencv services
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() { //stop collecting frames and remove app from memory
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) { frame = new Mat(); }

    public void onCameraViewStopped() { frame.release(); }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

    	//if ( !trainingPnts.hasRoom() ) //read in frames until training fully collected
    	//	train();
    	
    	if (frame != null) frame.release();
    	
    	frame = inputFrame.rgba();
    	ProcStruct ps = _faceD.getFace(frame);
		android.util.Log.i(camTAG, "looking for face");
		android.util.Log.i(camTAG, String.format("face located: %b", (ps.getRect() == null) ? false : true) );
		/*
        if (ps.getRect() == null) { //face cannot be located
    		android.util.Log.i("returnning pupil", "2");

        	ps.setpupil( new org.opencv.core.Point(-1, -1) );
        	
        } else { //locate right pupil within face region
        	ps.setpupil( _eyeD.getPupil(ps.getImg(), ps.getRect(), false) );
        }*/
    	
        //trainingPnts.add( ps.getpupil() );
        
        return ps.getImg();
    }
    
    private void train()  {
    	Log.i(camTAG, "Processing Data..");
    	
		Point tmp, tmp2;
		int i, size = trainingPnts.size();
		tmp = trainingPnts.getVal(0);
		ArrayList<Double> mags = new ArrayList<Double>();
		
		for (i = 1; i < size; i++) {
			tmp2 = trainingPnts.getVal(i);

			//treat closed eye signals as no eye movement
			if (tmp.x == -1) mags.add( _stool.length(0.0, 0.0, tmp2.x, tmp2.y) );
			else if (tmp2.x == -1) mags.add( _stool.length(tmp.x, tmp.y, 0.0, 0.0) );
			else if (tmp.x  ==  -1 && tmp2.x == -1) {
				tmp = tmp2; //rotate coordinate for next computation
				continue;
			}
			else mags.add( _stool.length(tmp.x, tmp.y, tmp2.x, tmp2.y) );
			
			tmp = tmp2; //rotate coordinate for next computation
		}
		Log.i(camTAG, "Processing complete..");
    	Log.i(camTAG, String.format("recorded frames: %d\t| obtained magnitudes: %d", trainingPnts.size(), mags.size()));
		
		finish();
    }
    
}

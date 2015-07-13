package app.ai.lifesaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import app.ai.imgproc.FindEyes;
import app.ai.imgproc.FindFace;

public class CamActivity extends Activity implements CvCameraViewListener2 {

    private static final String camTAG = "lifesaver::CamActivity";
    
    private CameraBridgeViewBase mOpenCvCameraView;

    private static FindFace _fd; //face detection object
    private static FindEyes _ed; //pupil detection object
    
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

                        _fd = new FindFace( new CascadeClassifier(mCascadeFile.getAbsolutePath()) );
                        
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(camTAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CamActivity() {
        Log.i(camTAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(camTAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view); //set view to screen
        mOpenCvCameraView.setCameraIndex(1); //use front camera
        //mOpenCvCameraView.disableFpsMeter();
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

    public void onCameraViewStarted(int width, int height) {  }

    public void onCameraViewStopped() {  }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat frame = inputFrame.rgba();    	
    	    	
    	
    	
        return _fd.getFace(frame);
    }
}

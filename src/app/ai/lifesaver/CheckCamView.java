package app.ai.lifesaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import app.ai.imgproc.FindEyes;
import app.ai.imgproc.FindFace;
import app.ai.imgproc.ProcStruct;

public class CheckCamView extends Activity implements CvCameraViewListener2 {

    private final String TAG = getClass().getSimpleName();

    private CameraBridgeViewBase mOpenCvCameraView;
    private static Mat frame = null;

    private static FindFace _faceD; //face detection object
    private static FindEyes _eyeD; //pupil detection object

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) { //opencv library initialization 
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

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
                        _eyeD = new FindEyes();
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.setMaxFrameSize(CoreVars.maxWidth, CoreVars.maxHeight);
                    mOpenCvCameraView.enableView();
                } break;
                
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CheckCamView() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view); //set view to screen
        mOpenCvCameraView.setCameraIndex(1); //use front camera
        mOpenCvCameraView.setMaxFrameSize(CoreVars.maxWidth, CoreVars.maxHeight);
        mOpenCvCameraView.setCvCameraViewListener(this); //receive image, process it then display
        mOpenCvCameraView.enableFpsMeter();
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

        if (frame != null) frame.release();

        frame = inputFrame.rgba();
        ProcStruct ps = _faceD.getFace(frame);

        if (ps.getRect() == null) { //face cannot be located
            ps.setpupil(new org.opencv.core.Point(-1, -1));

        } else { //locate right pupil within face region
            ps.setpupil(_eyeD.getPupil(ps.getImg(), ps.getRect(), false));
        }

        return ps.getImg();
    }
}

package app.ai.lifesaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.io.BufferedReader;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.FastVector;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.content.res.Resources.NotFoundException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import android.util.Log;
import android.view.WindowManager;
import app.ai.imgproc.FindEyes;
import app.ai.imgproc.FindFace;
import app.ai.imgproc.ProcStruct;
import app.ai.core.*;
import app.ai.stats.Stats;


public class MonitorActivity extends Activity implements CvCameraViewListener2 {

    private final String TAG = "lifeSaver.Monitor";
    
    private static Classifier cModel;
    private static Evaluation eTest;
    private static FastVector fvWekaAttributes;
    private static Instances isTrainingSet;
    private static Instances dataUnlabeled;
    private static Instance newFrame;
    // Time variables
    private static String[] labels = {"non-distracted", "distracted"}; // or non-distracted
    private static long cTime, sTime, secondsWait = 5l;
    private static boolean wasDistracted = false;
    private static double isDistracted;

    private static Uri notification;
    private static Ringtone ring;
    
    private CameraBridgeViewBase mOpenCvCameraView;
    private static Mat frame = null;
    //data collection structure
    private static LimQueue<Double> mags = new LimQueue<Double>(CoreV.queueSize);
    private static Point prv = new Point(0, 0), cur;
    //APIs
    private static FindFace _faceD; //face detection object
    private static FindEyes _eyeD; //pupil detection object
    private static Stats _stool = new Stats();

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
                        cascadeDir.delete();
                        
                        _faceD = new FindFace( new CascadeClassifier(mCascadeFile.getAbsolutePath()) );
                        _eyeD = new FindEyes();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.setMaxFrameSize(CoreV.maxWidth, CoreV.maxHeight);
                    mOpenCvCameraView.enableView();
                } break;

                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MonitorActivity() {
        //Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        //SETUP WEKA CLASSIFIER
        Attribute[] attributes = new Attribute[CoreV.queueSize+1];
        fvWekaAttributes = new FastVector(4); // Declare the feature vector
        
        for (int i = 0; i < CoreV.queueSize; i++) {
            attributes[i] = new Attribute(String.format("magnitude%d", i) ); // create numeric attributes
            fvWekaAttributes.addElement(attributes[i]);
        }
        FastVector fvClassVal = new FastVector(2); // Declare the class attribute along with its values
        fvClassVal.addElement(labels[0]);
        fvClassVal.addElement(labels[1]);
        attributes[CoreV.queueSize] = new Attribute("theClass", fvClassVal);
        fvWekaAttributes.addElement(attributes[CoreV.queueSize]);

        try { isTrainingSet = loadArff( getResources().openRawResource(R.raw.training_data) ); }
        catch (NotFoundException e) {
            Log.i(TAG, "Had trouble loading training data");
            e.printStackTrace(); }
        catch (IOException e) {
            Log.e(TAG, "Had trouble loading training data");
            e.printStackTrace(); }

        isTrainingSet.setClassIndex(isTrainingSet.numAttributes() - 1);
        // After training data set is complete, build model
        cModel = new RandomForest();

        try { cModel.buildClassifier(isTrainingSet); }
        catch (Exception e) {
            Log.i(TAG, "Had trouble setting up model");
            e.printStackTrace(); }
        try { eTest = new Evaluation(isTrainingSet); }
        catch (Exception e) {
            Log.i(TAG, "Had trouble setting up model");
            e.printStackTrace(); }

        dataUnlabeled = new Instances("TestInstances", fvWekaAttributes, 0);

        //SETUP VIEW AND CAMERA
        Log.i(TAG, "Collecting data..");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ring = RingtoneManager.getRingtone(getApplicationContext(), notification);
        
        setContentView(R.layout.face_detect_surface_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view); //set view to screen
        mOpenCvCameraView.setCameraIndex(1); //use front camera
        mOpenCvCameraView.setMaxFrameSize(CoreV.maxWidth, CoreV.maxHeight);
        mOpenCvCameraView.setCvCameraViewListener(this); //receive image, process it then display

        sTime = new  Date().getTime();
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

        if ( !mags.hasRoom() ) {
            //Once queue is full app begins to monitor real time
            check();
        } else  {
            Log.i(TAG, String.format("Frames needed for queue to fill up: %d", CoreV.queueSize - mags.size() ));
        }
            
        //release previously recorded frames to prevent memory leaks
        if (frame != null) frame.release();
        frame = inputFrame.rgba();
        ProcStruct ps = _faceD.getFace(frame);

        if (ps.getRect() == null) { //face cannot be located
            cur = new Point(0.0, 0.0);
        } else { //locate right pupil within face region
            cur = _eyeD.getPupil(ps.getImg(), ps.getRect(), false);
        }
        
        if (cur.x == 0.0 && prv .x == 0.0) {
            mags.add( 0.0 ); //interpret as no pupil movement if eyes and/or face is not detected
        } else {
            mags.add( _stool.length(prv.x, prv.y, cur.x, cur.y) ); //compute magnitude for pupil movement
        }
        prv = cur;
        
        return ps.getImg();
    }

    private void check()  {
        //Log.i(TAG, "Real time monitoring is starting..");
        if ( ring.isPlaying() && (cTime - sTime) / 1000l >= secondsWait ) {
            Log.i(TAG, "sound is active, turning it off");
            ring.stop();
        }

        newFrame = new Instance(CoreV.queueSize+1);
        for (int k = 0; k < CoreV.queueSize; k++) newFrame.setValue((Attribute)fvWekaAttributes.elementAt(k), mags.getVal(k));
        dataUnlabeled.add(newFrame);
        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
        // Predict
        try { isDistracted = eTest.evaluateModelOnceAndRecordPrediction(cModel, dataUnlabeled.lastInstance() );
        } catch (Exception e) {
            Log.i(TAG, "Had trouble making prediction");
            e.printStackTrace(); }

        cTime = new Date().getTime();

        // Also throw in a check for whether driver has been alerted in last ~5s
        //Log.i(TAG, String.format("Classification: %s", labels[(int)isDistracted]) );
        Log.i( TAG, newFrame.toString() );
        if (isDistracted == 1 && wasDistracted  && (cTime - sTime) / 1000l >= secondsWait) {
            Log.i(TAG, "turning on sound");
            ring.play(); //notify user
            sTime = cTime; // update most recent alert
        }
        // Set up for next frame
        if (isDistracted == 1) wasDistracted = true;
        else wasDistracted = false;
    }

    /**Loads an Instances object from an .arff file
     *  pre-condition: assumes arff file name  (excluding path) is specified in _DB variable
     *  pre-condition: onCreate() method specifies and passes arff file store location
     *  Post-conditions: loads Instances from location.
     */
    Instances loadArff(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader( new InputStreamReader(is) );
        ArffReader arff = new ArffReader(reader);
        return arff.getData();
    }

}

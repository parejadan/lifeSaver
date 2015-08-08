package app.ai.lifesaver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends Activity {
	
    //private static final String TAG = "Menu";
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.activity_menu);
	}

	public void startMonitor(View v) {
		Intent  intent = new Intent(MenuActivity.this, MonitorActivity.class);
		startActivity(intent);
    }


	public void checkCamView(View v) {
		Intent  intent = new Intent(MenuActivity.this, CheckCamView.class);
		startActivity(intent);
	}

    public native void loadJNI();
    static {
        System.loadLibrary("bottleNeck");
        System.loadLibrary("normalize");
        System.loadLibrary("matrixMagnitude");
        System.loadLibrary("nativeGaussBlur");
        System.loadLibrary("invert");
        System.loadLibrary("circle");

    }
}

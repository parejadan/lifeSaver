package app.ai.lifesaver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends Activity {
	
    //private static final String TAG = "lifesaver::MenuActivity";
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.activity_menu);
	}

	public void startTraining(View v) {
		//Intent  intent = new Intent(MenuActivity.this, TrainingActivity.class);
		//startActivity(intent);

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivity(intent);


    }
    /*
    public void startTesting(View v) {
        Intent  intent = new Intent(MenuActivity.this, TestingActivity.class);
        startActivity(intent);
    }*/


	public void checkCamView(View v) {
		Intent  intent = new Intent(MenuActivity.this, CheckCamView.class);
		startActivity(intent);
	}
}

package app.ai.lifesaver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends Activity {
	
    //private static final String menuTAG = "lifesaver::MenuActivity";
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.activity_menu);
	}

	public void startTraining(View v) {
		Intent  intent = new Intent(MenuActivity.this, TrainActivity.class);
		startActivity(intent);
		finish();
	}
}

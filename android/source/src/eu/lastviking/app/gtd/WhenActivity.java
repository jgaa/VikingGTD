package eu.lastviking.app.gtd;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import eu.lastviking.app.vgtd.R;

public class WhenActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.when_layout);
        this.setTitle(R.string.when);
	}

	@Override
	public void onBackPressed() {
		SaveResult();
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		//SaveResult();
		super.onPause();
	}
	
	void SaveResult() {
		Fragment f = getFragmentManager().findFragmentById(R.id.when_fragment_node);
		if (null != f) {
			WhenFragment w = (WhenFragment)f;
			w.SaveResult();
		}
	}
	
	
}

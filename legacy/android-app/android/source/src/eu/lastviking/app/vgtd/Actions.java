package eu.lastviking.app.vgtd;

import eu.lastviking.app.vgtd.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class Actions extends Activity {

	private final String TAG = "Actions";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actions_layout);
        
        this.setTitle(R.string.actions);
    }
	
}

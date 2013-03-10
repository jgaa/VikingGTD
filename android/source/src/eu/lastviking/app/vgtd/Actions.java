package eu.lastviking.app.vgtd;

import eu.lastviking.app.vgtd.R;
import android.app.Activity;
import android.os.Bundle;

public class Actions extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actions_layout);
        
        this.setTitle(R.string.actions);
    }
}

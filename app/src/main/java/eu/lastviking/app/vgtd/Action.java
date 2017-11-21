package eu.lastviking.app.vgtd;

import eu.lastviking.app.vgtd.R;
import android.os.Bundle;

public class Action extends VikingBackHandlerActivity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.action_layout);
        this.setTitle(R.string.action);
	}

}

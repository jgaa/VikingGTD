package eu.lastviking.app.gtd;

import android.os.Bundle;
import eu.lastviking.app.vgtd.R;

public class Action extends VikingBackHandlerActivity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.action_layout);
        this.setTitle(R.string.action);
	}

}

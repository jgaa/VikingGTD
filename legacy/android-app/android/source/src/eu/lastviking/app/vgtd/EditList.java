package eu.lastviking.app.vgtd;

import eu.lastviking.app.vgtd.R;
import android.os.Bundle;
import android.util.Log;

public class EditList extends VikingBackHandlerActivity
{
	static public final String TAG = "EditList";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.edit_list);
		} catch(Exception ex) {
			Log.e(TAG, "Caught exception when inflating: " + ex.toString());
		}
	}
}


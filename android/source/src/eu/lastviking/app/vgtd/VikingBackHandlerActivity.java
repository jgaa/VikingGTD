package eu.lastviking.app.vgtd;
import android.app.Activity;
import android.util.Log;


public class VikingBackHandlerActivity extends Activity {
	
	private static final String TAG = "VikingBackHandlerActivity";
	private Handler handler_;

	public interface Handler {
		boolean OnBackButtonPressed();
	}
	
	public void SetBackHandler(Handler handler) {
		handler_ = handler;
	}

	@Override
	public void onBackPressed() {
		if ((null != handler_) && !handler_.OnBackButtonPressed()) {
			// Log.d(TAG, "Back button ignored");
			
		} else {
			// Log.d(TAG, "Back button OK");
			super.onBackPressed();
		}
	}
}


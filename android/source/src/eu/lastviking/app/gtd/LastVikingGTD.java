package eu.lastviking.app.gtd;

import java.io.IOException;
import java.io.InputStream;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class LastVikingGTD extends Application {

	private static LastVikingGTD self_;
	public static final String LOG_TAG = "vGTD";
	
	public static LastVikingGTD GetInstance() {
		return self_;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		self_ = this;
		
		
	}

	@Override
	public void onTerminate() {
		super.onTerminate();		
	}
	
	public void TerminateWithError(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		//Log.e("Application ABORT", message);
		//System.exit(-1);
	}
	
	public String ReadAssetFile(final String path) throws IOException
	{
		Log.d(LOG_TAG, "Reading asset file '" + path + "'");
		InputStream stream = getAssets().open(path);
		int size = stream.available();
		byte[] buffer = new byte[size];
		stream.read(buffer);
		stream.close();
		return new String(buffer);
	}
}

package eu.lastviking.app.vgtd;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class RepeatFragment extends Fragment {
	
	private String TAG = "RepeatFragment";
	
	public class RepeatData {
		public int type_ = 0;
		public int unit_ = 0;
		public int num_units_ = 0;
	}

	CharSequence[] repeat_types_;
	CharSequence[] unit_names_;
	RepeatData data_ = new RepeatData();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view;
		try {
			view = inflater.inflate(R.layout.repeat_fragment, container, false);
		} catch(Exception ex) {
			Log.e(TAG, "Failed to inflate the view: " + ex.getMessage());
			return null;
		}
		
		PopulateModeDropdown(view);
		
		return view;
	}

	private void PopulateModeDropdown(View view) {
		unit_names_ = getResources().getTextArray(R.array.repeat_unit_types);
		repeat_types_ = getResources().getTextArray(R.array.repeat_types);
		 
		for(int i = 0; i < repeat_types_.length; ++i) {
			String name = (String) repeat_types_[i];
			repeat_types_[i] = name.replace("@", unit_names_[data_.unit_]);
		}
		
		Spinner mode = (Spinner)view.findViewById(R.id.repeat_mode);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				getActivity(), R.id.repeat_mode, repeat_types_);
		mode.setSelection(data_.type_);
	}
}

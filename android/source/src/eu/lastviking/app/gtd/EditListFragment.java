package eu.lastviking.app.gtd;

// TODO: When adding, check for availability of the new name when the user press the back button.
// TODO: Add Cancel menu option

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import eu.lastviking.app.vgtd.R;

public class EditListFragment extends Fragment {
	
	private static String TAG = "EditListFragment"; 
	boolean cancel_flag_ = false;
	EditText name_;
	EditText descr_;
	long category_ = 1;
	long edit_id_ = 0;
	boolean saved_ = false;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_list, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.edit_list_fragment, container, false);
		
		name_ = (EditText)view.findViewById(R.id.list_name);
		descr_ = (EditText)view.findViewById(R.id.list_description);
		
		if (0 != edit_id_) {
			Load();
		}
		
		((VikingBackHandlerActivity)getActivity()).SetBackHandler(new VikingBackHandlerActivity.Handler() {
			
			@Override
			public boolean OnBackButtonPressed() {
				Log.d(TAG, "back button is pressed.");
				return Save();
			}
		});
		
		setHasOptionsMenu(true);
	
		
		return view;
	}

	@Override
	public void onPause() {
		if (!cancel_flag_) {
			try {
				Save();
			} catch(Exception e) {
				Log.e(TAG, "Failed to save list");
				e.printStackTrace();
				Toast.makeText(getActivity(), R.string.save_list_failed, Toast.LENGTH_LONG).show();
			}
		}
		
		super.onPause();
	}
	
	

	@Override
	public void onResume() {
		super.onResume();
		saved_ = false;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		final Intent intent = activity.getIntent();
		
		if (intent.hasExtra("id")) {
			edit_id_ = intent.getExtras().getLong("id", 0);
		} else {
			edit_id_ = 0;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.id.cancel_edit_list:
			cancel_flag_ = true;
			getActivity().finish();
			break;
		case R.id.save_edit_list:
			cancel_flag_ = false;
			saved_ = false;
			if (Save()) {
				getActivity().finish();
			}
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private Boolean IsAdding() { return edit_id_ == 0; }

	public boolean Save() {		
		if (!saved_) {
			String name =  name_.getText().toString().trim();
			if (name.length() > 0) {

				ContentValues values = new ContentValues();
				values.put(GtdContentProvider.ListsDef.NAME, name_.getText().toString());
				values.put(GtdContentProvider.ListsDef.DESCR, descr_.getText().toString());
				values.put(GtdContentProvider.ListsDef.CATEGORY, String.valueOf(category_));

				ContentResolver resolver = getActivity().getContentResolver();

				// TODO - handle duplicate entries
				try {
					if (IsAdding()) {
						final Uri uri = resolver.insert(GtdContentProvider.ListsDef.CONTENT_URI, values);
						edit_id_ = ContentUris.parseId(uri);
					} else {
						resolver.update(Uri.parse(GtdContentProvider.ListsDef.CONTENT_URI + "/" + edit_id_), values, null, null);
					}
				} catch(SQLiteConstraintException ex) {
					Toast.makeText(getActivity(), R.string.name_exist, Toast.LENGTH_LONG).show();
					return false;
				} catch(Exception ex) {
					Toast.makeText(getActivity(), R.string.save_failed, Toast.LENGTH_LONG).show();
					return false;
				}
			} else {
				Toast.makeText(getActivity(), R.string.no_name, Toast.LENGTH_LONG).show();
				return false;
			}

			saved_ = true;
		}
		return true;
	}
	
	public void Load() {
		final String[] projection = { GtdContentProvider.ListsDef._ID, 
				GtdContentProvider.ListsDef.NAME,
				GtdContentProvider.ListsDef.DESCR,
				GtdContentProvider.ListsDef.CATEGORY };
		
		ContentResolver resolver = getActivity().getContentResolver();
		Cursor c = resolver.query(Uri.parse(GtdContentProvider.ListsDef.CONTENT_URI + "/" + edit_id_), projection, null, null, null);
		if (c.moveToFirst()) {
			
			String name = c.getString(1);
			String descr = c.getString(2);
			category_ = c.getLong(3);
			name_.setText(name);
			descr_.setText(descr);
		} else {
			Log.w(TAG, "Unable to get data for List #" + edit_id_);
		}
		saved_ = false;
	}
	
}

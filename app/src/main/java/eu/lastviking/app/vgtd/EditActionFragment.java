package eu.lastviking.app.vgtd;

import java.util.ArrayList;
import java.util.List;

import eu.lastviking.app.vgtd.R;
import eu.lastviking.app.vgtd.GtdContentProvider.Actions2LocationsDef;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.renderscript.RenderScript.Priority;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class EditActionFragment extends Fragment {

	private String TAG = "EditActionFragment";
	public static final int RESULT_OK = 1;
	private static final int RESULT_WHEN = 1;
    private static final int RESULT_REPEAT = 2;
	
	public enum Priorities {
		Critical,
		VeryImportant,
		Higher,
		High,
		Normal,
		Medium,
		Low,
		Insignificant
	}
	
	public enum FocusNeeded {
		InspiredMoment,
		FullAttention,
		GoodAttention,
		Normal,
		RelativelyTrivial,
		Trivial
	}
	
	public static class Location extends Object implements MultiSpinner.Data {
		final public long id_;
		final public String name_;
		boolean selected_;
		
		public Location(long id, String name, boolean selected) {
			id_ = id;
			name_ = name;
			selected_ = selected;
		}
		
		@Override
		public String toString() {
			return name_;
		}

		@Override
		public boolean GetSelected() {
			return selected_;
		}

		@Override
		public void SetSelected(boolean selected) {
			selected_ = selected;
		}
	}
	
	// Data
	private boolean cancel_flag_ = false;
	private boolean saved_ = false;
	private long action_id_ = 0; // Zero if new item
	private long list_id_ = 0;
	String name_ = "";
	String desc_ = "";
	int priority_ = 4; // Normal
	When when_ = new When();
    RepeatData repeat_ = new RepeatData();
	List<MultiSpinner.Data> where_ = new ArrayList<MultiSpinner.Data>();
	int how_ = FocusNeeded.Normal.ordinal();
		
	// Controls
	EditText name_ctl_;
	EditText descr_ctl_;
	Spinner priority_ctl_;
	Button when_btn_;
	MultiSpinner where_ctl_;
	Spinner how_ctl_;
    Button repeat_btn_;
	
	
	public EditActionFragment() {
		super();
		Log.w(TAG, "Created.");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.edit_action_fragment, container, false);
		
		name_ctl_ = (EditText)view.findViewById(R.id.action_name);
		descr_ctl_ = (EditText)view.findViewById(R.id.action_descr);
		priority_ctl_ = (Spinner)view.findViewById(R.id.action_priority);
		when_btn_ = (Button)view.findViewById(R.id.action_when_btn);
		where_ctl_ =(MultiSpinner) view.findViewById(R.id.action_locations);
		how_ctl_ = (Spinner)view.findViewById(R.id.action_how);
        repeat_btn_ = (Button)view.findViewById(R.id.action_repeat_btn);
		
		when_btn_.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle b = new Bundle();
				b.putSerializable("when", when_);
				intent.putExtras(b);
				intent.setClass(getActivity(), WhenActivity.class);
				startActivityForResult(intent, RESULT_WHEN);
			}
		});

        repeat_btn_.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
               Intent intent = new Intent();
               Bundle b = new Bundle();
               b.putSerializable("repeat", repeat_);
               intent.putExtras(b);
               intent.setClass(getActivity(), RepeatActivity.class);
               startActivityForResult(intent, RESULT_REPEAT);
            }
       });
		
		((VikingBackHandlerActivity)getActivity()).SetBackHandler(new VikingBackHandlerActivity.Handler() {
			
			@Override
			public boolean OnBackButtonPressed() {
				Log.d(TAG, "back button is pressed.");
				FetchDataFromControls();
				return Save();
			}
		});
		
		if (!IsAdding()) {
			Load();
		}
		LoadLocations();
		UpdateControlsFromData();
		setHasOptionsMenu(true);
		
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_action, menu);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		final Intent intent = activity.getIntent();
		
		if (intent.hasExtra("list-id")) {
			list_id_ = intent.getExtras().getLong("list-id");
		} else {
			list_id_ = 0;
		}
		if (intent.hasExtra("id")) {
			action_id_ = intent.getExtras().getLong("id", 0);
		} else {
			action_id_ = 0;
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (RESULT_OK == resultCode) switch(requestCode) {
			case RESULT_WHEN: {
				Bundle b = data.getExtras();
				if (null != b) {
					When w = (When) b.getSerializable("when");
					if (null != w) {
						when_ = w;
						when_btn_.setText(when_.toString());
					}
				}
			}
			break;
            case RESULT_REPEAT: {
                Bundle b = data.getExtras();
                if (null != b) {
                    RepeatData r = (RepeatData) b.getSerializable("repeat");
                    if (null != r) {
                        repeat_ = r;
                        repeat_btn_.setText(repeat_.toString());
                    }
                }
            }
            break;
			default:
				super.onActivityResult(resultCode, resultCode, data);
			break;	
		} else {
			super.onActivityResult(resultCode, resultCode, data);
		}
	}
	
	@Override
	public void onPause() {
		if (!cancel_flag_) {
			try {
				FetchDataFromControls();
				Save();
			} catch(Exception e) {
				Log.e(TAG, "Failed to save action");
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
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.id.cancel_edit_action:
			cancel_flag_ = true;
			getActivity().finish();
			break;
		case R.id.save_edit_action:
			cancel_flag_ = false;
			saved_ = false;
			FetchDataFromControls();
			if (Save()) {
				getActivity().finish();
			}
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private Boolean IsAdding() { return action_id_ == 0; }
	
	private void FetchDataFromControls() {
		name_ = name_ctl_.getText().toString();
		desc_ = descr_ctl_.getText().toString();
		priority_ = priority_ctl_.getSelectedItemPosition();
		if (0 > priority_) {
			priority_ = Priorities.Normal.ordinal();
		}
		how_ = how_ctl_.getSelectedItemPosition();
		if (0 > how_) {
			how_ = FocusNeeded.Normal.ordinal();
		}
		where_ctl_.UpdateData();
	}
	
	private void UpdateControlsFromData() {
		name_ctl_.setText(name_);
		descr_ctl_.setText(desc_);
		priority_ctl_.setSelection(priority_);
		when_btn_.setText(when_.toString());
        repeat_btn_.setText(repeat_.toString());
		how_ctl_.setSelection(how_);
		where_ctl_.SetItems(where_, 
				getActivity().getText(R.string.anywhere).toString(), 
				getActivity().getText(R.string.not_set).toString(),
				null);
	}
	
	private boolean Save() {
		
		if (!saved_) {
			name_ =  name_ctl_.getText().toString().trim();
			if (name_.length() > 0) {

				ContentValues values = new ContentValues();

				values.put(GtdContentProvider.ActionsDef.NAME, name_);
				values.put(GtdContentProvider.ActionsDef.DESCR, desc_);
				values.put(GtdContentProvider.ActionsDef.PRIORITY, priority_);
				if (0 != list_id_) {
					values.put(GtdContentProvider.ActionsDef.LIST_ID, list_id_);
				}
				values.put(GtdContentProvider.ActionsDef.DUE_BY_TIME, when_.GetUnixTime());
				values.put(GtdContentProvider.ActionsDef.DUE_TYPE, when_.getDueType().ordinal());
				values.put(GtdContentProvider.ActionsDef.FOCUS_NEEDED, how_);
                values.put(GtdContentProvider.ActionsDef.REPEAT_TYPE, repeat_.getMode());
                values.put(GtdContentProvider.ActionsDef.REPEAT_UNIT, repeat_.getUnit());
                values.put(GtdContentProvider.ActionsDef.REPEAT_AFTER, repeat_.getNumUnits());

				ContentResolver resolver = getActivity().getContentResolver();

				final boolean need_to_clean = !IsAdding();

				try {
					if (IsAdding()) {
						final Uri uri = resolver.insert(GtdContentProvider.ActionsDef.CONTENT_URI, values);
						action_id_ = ContentUris.parseId(uri);

						//action_id_ = Long.parseLong(resolver.insert(GtdContentProvider.ActionsDef.CONTENT_URI, values).getFragment());
					} else {
						resolver.update(Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + action_id_), values, null, null);
					}
				} catch(Exception ex) {
					Log.e(TAG, "Caught exception during save: " + ex.getMessage());
					Toast.makeText(getActivity(), R.string.save_failed, Toast.LENGTH_LONG).show();
				}

				// Update selected locations by inserting them.
				if (0 != action_id_) {
					try {
						if (need_to_clean) {
							// Delete old values
							resolver.delete(GtdContentProvider.Actions2LocationsDef.CONTENT_URI, 
									Actions2LocationsDef.ACTION_ID + "=" + action_id_, null);
						}


						// Insert new values
						for(int i = 0; i < where_.size(); i++) {
							Location loc = (Location)where_.get(i);
							if (loc.selected_) {
								ContentValues v = new ContentValues();
								v.put(GtdContentProvider.Actions2LocationsDef.LOCATION_ID, loc.id_);
								v.put(GtdContentProvider.Actions2LocationsDef.ACTION_ID, action_id_);
								resolver.insert(GtdContentProvider.Actions2LocationsDef.CONTENT_URI, v);
							}
						}
					} catch(Exception ex) {
						Log.e(TAG, "Caught exception during save lof locations: " + ex.getMessage());
						Toast.makeText(getActivity(), R.string.save_failed, Toast.LENGTH_SHORT).show();
					}
				}

			} else {
				Toast.makeText(getActivity(), R.string.no_name, Toast.LENGTH_LONG).show();
				return false;
			}
			saved_ = true;
		}

		return true;
	}
	
	private void Load() {

		ContentResolver resolver = getActivity().getContentResolver();
		Cursor c = resolver.query(Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + action_id_), 
				GtdContentProvider.ActionsDef.PROJECTION_ALL, null, null, null);
		if (c.moveToFirst()) {
			
			name_ = c.getString(GtdContentProvider.ActionsDef.Fields.NAME.ordinal());
			desc_ = c.getString(GtdContentProvider.ActionsDef.Fields.DESCR.ordinal());
			priority_= c.getInt(GtdContentProvider.ActionsDef.Fields.PRIORITY.ordinal());

            {
                final long time = c.getLong(GtdContentProvider.ActionsDef.Fields.DUE_BY_TIME.ordinal()) * 1000;
                final int due_type = c.getInt(GtdContentProvider.ActionsDef.Fields.DUE_TYPE.ordinal());

                // Deal with invalid database entries
                try {
                    when_ = new When(time, due_type);
                } catch (IllegalArgumentException ex) {
                    when_ = new When(); // Unset
                }
            }

            {
                final long repeat_type = c.getLong(GtdContentProvider.ActionsDef.Fields.REPEAT_TYPE.ordinal());
                final long repeat_unit = c.getLong(GtdContentProvider.ActionsDef.Fields.REPEAT_UNIT.ordinal());
                final long repeat_after = c.getLong(GtdContentProvider.ActionsDef.Fields.REPEAT_AFTER.ordinal());

                try {
                    repeat_.setMode(repeat_type);
                    repeat_.setUnit(repeat_unit);
                    repeat_.setNumUnits(repeat_after);
                } catch (IllegalArgumentException ex) {
                    repeat_ = new RepeatData(); // Unset
                }
            }

			list_id_ = c.getLong(GtdContentProvider.ActionsDef.Fields.LIST_ID.ordinal());
			how_ = c.getInt(GtdContentProvider.ActionsDef.Fields.FOCUS_NEEDED.ordinal());
			
		} else {
			Log.w(TAG, "Unable to get data for List #" + action_id_);
		}
		saved_ = false;
	}
	
	// Load the selected locations, or -if we are inserting, just populate the locations list and select all (for now) 
	private void LoadLocations() {
		where_.clear();
		ContentResolver resolver = getActivity().getContentResolver();
		Cursor c = resolver.query(Uri.parse(GtdContentProvider.SelectedLocationsDef.CONTENT_URI + "/" + action_id_), 
				GtdContentProvider.SelectedLocationsDef.PROJECTION_ALL, null, null, null);
		if (c.moveToFirst()) {
			do {
				final boolean selected = (c.isNull(GtdContentProvider.SelectedLocationsDef.Fields.SELECTED.ordinal()) ? false : true);
				Location loc = new Location(c.getLong(GtdContentProvider.SelectedLocationsDef.Fields._ID.ordinal()),
						c.getString(GtdContentProvider.SelectedLocationsDef.Fields.NAME.ordinal()),
						selected);
				where_.add(loc);
			} while(c.moveToNext());
		}
	}
}

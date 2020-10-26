package eu.lastviking.app.vgtd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeSet;

import eu.lastviking.app.vgtd.R;
import eu.lastviking.app.vgtd.MultiSpinner.Data;
import eu.lastviking.app.vgtd.PickFragment.GenericListItem;
import eu.lastviking.app.vgtd.When.NoDateException;
import eu.lastviking.app.vgtd.When.State;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

public class ActionsListFragment extends ListFragment implements OnQueryTextListener {

	public enum Mode {
		SINGLE_LIST,
		TODAY,
		FILTER_ON_WHEN,
		FILTER_ON_WHERE_HOW,
		FULL_FILTER
	};
	
	private static final int LOADER_ACTIONS = 1;
	public static final int RESULT_OK = 1;
	private static final int RESULT_WHEN = 1;
	
	static private String TAG = "ActionsListFragment";
	protected int cur_list_position_ = 0;
	private long list_id_ = 0;
    private LoaderMgr lm_;
    protected ActionItemAdapter adapter_;
    private String search_string_ = "";
    protected static final int TAG_ID = 1;
    View.OnClickListener checkout_listener_;
    private ItemData current_item_; // for context menu
    private MenuItem group_actions_set_when_;
    private MenuItem group_actions_delete_;
    private MenuItem group_actions_move_;
    private boolean ready_to_requery_ = false; 
    
    static class ItemData {
    	long id_ = 0;
    	boolean completed_;
    	int priority_;
    	When when_;
    	ImageView image_view_;
    	TextView name_view_;
    	TextView proprerties_view_;
    	CheckBox selected_view_;    	
    }

    boolean IsListTodayOnly() {
    	return false;
    }
    
    String GetEmptyListText() {
    	Resources r = getResources();
    	if (null != r) {
    		return getResources().getString(R.string.no_actions);
    	}
    	return "";
    }
    
    class ActionItemAdapter extends CursorAdapter {
    	
    	public TreeSet<Long> selected_ = new TreeSet<Long>();
    	TreeSet<Long> expanded_ = new TreeSet<Long>();
    	Bitmap[] priority_map_;
    	boolean list_is_today_ = IsListTodayOnly();
    	
		public ActionItemAdapter(Context context, int flags) {
			super(context, null, flags);
			
			priority_map_ = new Bitmap[8];
			for(int i = 0; i < priority_map_.length; i++) {
				priority_map_[i] = GetCheckImage(i);
			}
			
			checkout_listener_ = new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final ItemData d = (ItemData)v.getTag();
					SetCompleted(d, true);
				}
		    	
		    };
		}
		
		private Bitmap GetCheckImage(final int priority) {
			
			int color = Color.WHITE;
			
			switch(priority) {
			case 0: // Critical
				color = Color.rgb(255, 0, 0); 
				break;
			case 1: // Very high
				color = Color.rgb(255, 60, 60); 
				break;
			case 2: // Higher
				color = Color.rgb(255, 36, 0); 
				break;
			case 3: // High
				color = Color.rgb(255, 127, 0); 
				break;
			case 4: // Normal
				//color = Color.WHITE; 
				break;
			case 5: // Medium
				color = Color.YELLOW; 
				break;
			case 6: // Low
				color = Color.rgb(219, 219, 112); 
				break;
			case 7: // Insignificant
				color = Color.rgb(255, 230, 140); 
				break;
			
			}
						
			Bitmap check = BitmapFactory.decodeResource(LastVikingGTD.GetInstance().getResources(), 
					R.drawable.btn_check_buttonless_off);
			
			final int width = check.getWidth();
			final int height = check.getHeight();
			
			Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);;
			Canvas c = new Canvas(bmp);
			Paint p = new Paint();
			p.setAntiAlias(true);
			p.setColor(color);
			p.setStyle(Paint.Style.FILL);
			c.drawCircle(width / 2, height / 2, width / 2, p);
			c.drawBitmap(check, 0, 0, null);
			return bmp;
		}

		@Override
		public void bindView(View v, Context context, Cursor c) {
			
			ItemData d = (ItemData)v.getTag();
			d.id_ = c.getLong(GtdContentProvider.ActionsDef.Fields._ID.ordinal());
			d.completed_ = 1 == c.getInt(GtdContentProvider.ActionsDef.Fields.COMPLETED.ordinal());
			d.priority_ = c.getInt(GtdContentProvider.ActionsDef.Fields.PRIORITY.ordinal());
			
			try {
				d.when_.SetTime(c.getLong(GtdContentProvider.ActionsDef.Fields.DUE_BY_TIME.ordinal()) * 1000, 
					c.getInt(GtdContentProvider.ActionsDef.Fields.DUE_TYPE.ordinal()));
			} catch(Exception ex) {
				Log.e(TAG, "Failed to set time: " + ex.getMessage());
			}
			
			if (d.completed_) {
				d.image_view_.setImageResource(R.drawable.btn_check_buttonless_on);
				v.setBackgroundColor(Color.LTGRAY);
			} else {				
				switch(d.when_.GetState()) {
				case UNASSIGNED:
					v.setBackgroundColor(Color.CYAN);
					break;
				case TODAY:
					if (list_is_today_) {
						v.setBackgroundColor(Color.WHITE);
					} else {
						v.setBackgroundColor(Color.YELLOW);
					}
					break;
				case TOMORROW:
					v.setBackgroundColor(Color.rgb(255, 255, 125));
					break;
				case THIS_WEEK:
					v.setBackgroundColor(Color.rgb(255, 255, 200));
					break;
				case LOOSELY_DEFINED_FUTURE:
					v.setBackgroundColor(Color.rgb(222,184,135));
					break;
				case FUTURE:
					v.setBackgroundColor(Color.WHITE);
					break;
				case IN_LOOSELY_DEFINED_PERIOD:
					v.setBackgroundColor(Color.rgb(205,133,63));
					break;
				case OVERDUE:
					v.setBackgroundColor(Color.rgb(255, 120, 120));
					break;
				}
				
				d.image_view_.setImageBitmap(priority_map_[d.priority_]);
				d.image_view_.setOnClickListener(checkout_listener_);
			}
			
			d.name_view_.setText(c.getString(GtdContentProvider.ActionsDef.Fields.NAME.ordinal()));
			StringBuilder properties = new StringBuilder();
			if (!list_is_today_ && (d.when_.getDueType() != When.DueTypes.NONE)) {
				properties.append(d.when_.toString());
			} else if (list_is_today_ && (d.when_.getDueType() == When.DueTypes.TIME)) {
				try {
					properties.append(d.when_.getTimeAsString());
				} catch (NoDateException e) {
					;
				}
			}
			if (properties.length() > 0) {
				d.proprerties_view_.setText(properties.toString());
				d.proprerties_view_.setVisibility(View.VISIBLE);
				d.name_view_.setGravity(Gravity.LEFT | Gravity.TOP);
			} else {
				d.proprerties_view_.setVisibility(View.GONE);
				d.name_view_.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			}
			final boolean is_selected =  selected_.contains(c.getLong(GtdContentProvider.ActionsDef.Fields._ID.ordinal()));
			d.selected_view_.setChecked(is_selected);
			
			//Log.d(TAG, "Item# " + d.id_ + " " + name + ": " + (d.completed_ ? "COMPLETE" : "pending"));
		}

		@Override
		public View newView(Context context, Cursor c, ViewGroup vg) {
			final LayoutInflater inflater = LayoutInflater.from(context);
	        View v = inflater.inflate(R.layout.action_item, vg, false);
	        ItemData d = new ItemData();
	        d.image_view_ = (ImageView)v.findViewById(R.id.action_done); 
	    	d.name_view_  = (TextView)v.findViewById(R.id.action_name);
	    	d.proprerties_view_ = (TextView)v.findViewById(R.id.action_properties);
	    	d.selected_view_ = (CheckBox)v.findViewById(R.id.action_selected);
	    	d.when_ = new When();
	    	v.setTag(d);
	    	d.image_view_.setTag(d); // for OnClick
	    	d.selected_view_.setTag(d); // for onCheckedChanged
	    	d.selected_view_.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					ItemData d = (ItemData)buttonView.getTag();
					if (null != d) {
						Select(d.id_, isChecked);
					}
				}
	    		
	    	});
			return v;
		}
		
		void Select(final long id, final boolean selected ) {
			Log.d(TAG, (selected ? "Selected " : "Unselected ") + id);
			final boolean was_empty = selected_.isEmpty(); 
			if (selected) {
				selected_.add(id);
			} else {
				selected_.remove(id);
			}
			
			if (was_empty != selected_.isEmpty()) {
				OnSelectionMenuChange(!selected_.isEmpty());
			}
		}
		
		void Expand(final long id, final boolean selected ) {
			Log.d(TAG, (selected ? "Expanded " : "Unexpanded ") + id);
			if (selected) {
				expanded_.add(id);
			} else {
				expanded_.remove(id);
			}
		}
		
		public int getStringConversionColumn() {
			return GtdContentProvider.ActionsDef.Fields.NAME.ordinal();
		}
		
    }

    void HandleRepeat(final long actionId) {
        ContentResolver resolver = getActivity().getContentResolver();
        long repeat_mode = 0;

        {
            // Check the repeat mode only, since the common
            // case is likely to be no repeat.
            String[] colname = { GtdContentProvider.ActionsDef.REPEAT_TYPE };

            Cursor c = resolver.query(Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + actionId),
                    colname, null, null, null);
            if (c.moveToFirst()) {
                try {
                    repeat_mode = c.getLong(0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.makeText(getActivity(), "Failed to query data: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            c.close();
        }

        RepeatData rd = new RepeatData();
        rd.setMode(repeat_mode);
        if (rd.mode_ == RepeatData.NO_REPEAT)
            return; // Nothing more to do

        // Get the full data record
        Cursor c = resolver.query(Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + actionId),
                GtdContentProvider.ActionsDef.PROJECTION_ALL, null, null, null);
        if (!c.moveToFirst()) {
            Toast.makeText(getActivity(), "Failed to fetch data (second iteration)!",  Toast.LENGTH_LONG).show();
            return; // No data - likely to be an error, unless synchronization just deleted it.
        }

        rd.setUnit(c.getLong(GtdContentProvider.ActionsDef.Fields.REPEAT_UNIT.ordinal()));
        rd.setNumUnits(c.getLong(GtdContentProvider.ActionsDef.Fields.REPEAT_AFTER.ordinal()));

        {
            ContentValues new_action = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(c, new_action);
            new_action.remove(GtdContentProvider.ActionsDef._ID);
            new_action.remove(GtdContentProvider.ActionsDef.COMPLETED);
            new_action.remove(GtdContentProvider.ActionsDef.COMPLETED_TIME);
            new_action.remove(GtdContentProvider.ActionsDef.DUE_BY_TIME);
            new_action.remove(GtdContentProvider.ActionsDef.DUE_TYPE);

            final long scheduled_time = c.getLong(GtdContentProvider.ActionsDef.Fields.DUE_BY_TIME.ordinal()) * 1000;
            final long completed_time = c.getLong(GtdContentProvider.ActionsDef.Fields.COMPLETED_TIME.ordinal());
            int due_type = c.getInt(GtdContentProvider.ActionsDef.Fields.DUE_TYPE.ordinal());
            When when;

            // Deal with invalid entries
            try {
                When scheduled = new When(scheduled_time, due_type);
                When completed = new When(completed_time, due_type);

                Calendar calendar = rd.Calculate(scheduled.getCalendar(), completed.getCalendar());
                if (calendar != null) {
                    when = new When(calendar.getTimeInMillis(), due_type);
                    new_action.put(GtdContentProvider.ActionsDef.DUE_BY_TIME, when.GetUnixTime());
                    new_action.put(GtdContentProvider.ActionsDef.DUE_TYPE, when.getDueType().ordinal());
                }
            } catch (IllegalArgumentException ex) {
                Toast.makeText(getActivity(), "Failed convert date: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return;
            } catch (NoDateException ex) {
                Toast.makeText(getActivity(), "Failed convert date: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            // Insert the new action to the database
            try {
                resolver.insert(GtdContentProvider.ActionsDef.CONTENT_URI, new_action);
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(getActivity(), "Failed to insert data: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Now, just clear the repeat data on the old entry.
        ContentValues old_action = new ContentValues();
        old_action.put(GtdContentProvider.ActionsDef.REPEAT_TYPE, 0);
        old_action.put(GtdContentProvider.ActionsDef.REPEAT_UNIT, 0);
        old_action.put(GtdContentProvider.ActionsDef.REPEAT_AFTER, 0);

        try {
            resolver.update(Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + actionId), old_action, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(getActivity(), "Failed to update the old action: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        c.close();
    }
    
    void SetCompleted(ItemData d, final boolean completed) {
		
    	if (null != d) {
    		if (d.completed_ != completed) {
    			ContentResolver resolver = getActivity().getContentResolver();
    			ContentValues values = new ContentValues();

    			if (completed) {
    				values.put(GtdContentProvider.ActionsDef.COMPLETED, 1);
    				values.put(GtdContentProvider.ActionsDef.COMPLETED_TIME, Calendar.getInstance().getTimeInMillis());
    			} else {
    				values.put(GtdContentProvider.ActionsDef.COMPLETED, 0);
    				values.put(GtdContentProvider.ActionsDef.COMPLETED_TIME, 0);
    			}

    			try {
    				resolver.update(Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + d.id_), values, null, null);
    			} catch(Exception ex) {
    				ex.printStackTrace();
    				Toast.makeText(getActivity(), "Failed to update data: " + ex.getMessage(), Toast.LENGTH_LONG).show();
    			}
    			d.completed_ = completed;

                if (completed) {
                    HandleRepeat(d.id_);
                }
    		}
    	}
    }
    
    // Overrides must return an empty string if there are no filter. Never return null.
    public String GetBaseFilterForDbQuery() {
    	return GtdContentProvider.ActionsDef.LIST_ID + "=" + list_id_;
    }
    
    public Uri GetUriForDbQuery() {
    	return GtdContentProvider.ActionsDef.CONTENT_URI;
    }
    
    public String[] GetDbProjection() {
    	return GtdContentProvider.ActionsDef.PROJECTION_ALL;
    }

	private class LoaderMgr implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			
			MySetEmptyTest(getResources().getString(R.string.loading_actions));
			Log.d(TAG, "LoaderMgr: onCreateLoader called for loader id " + id);
			
			Uri uri = GetUriForDbQuery();
			
			if (LOADER_ACTIONS == id) {
				String[] filter_args = null;
				String filter = GetBaseFilterForDbQuery();
				if (!search_string_.isEmpty()) {
					filter_args = new String[] {"%" + search_string_ + "%"};
					if (filter.length() != 0)
						filter = filter + " AND ";
					filter = filter + GtdContentProvider.ActionsDef.NAME  + " LIKE ?";
				} 
				try {
					Log.d(TAG, "Executing query: " + filter );
					return new CursorLoader(getActivity(), uri, GetDbProjection() ,
							filter, 
							filter_args,
							null /*GtdContentProvider.ActionsDef.COMPLETED_TIME + " ASC, " 
									+ GtdContentProvider.ActionsDef.PRIORITY + " ASC, "
									+ GtdContentProvider.ActionsDef.DUE_BY_TIME + " ASC, "
									+ GtdContentProvider.ActionsDef.CREATED_DATE + " ASC"*/);
				} catch(Exception ex) {
					Log.e(TAG, "Query failed: " + ex.getMessage());
				}
			}

			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			Log.d(TAG, "LoaderMgr: onLoadFinished called.");
			adapter_.swapCursor(cursor);
			ready_to_requery_ = true;
			MySetEmptyTest(GetEmptyListText());
		}

		@Override
		public void onLoaderReset(Loader<Cursor> cursor) {
			MySetEmptyTest(getResources().getString(R.string.loading_actions));
			Log.d(TAG, "LoaderMgr: onLoaderReset called.");
			if (null != adapter_) {
				adapter_.swapCursor(null);
			}
			
		}
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = null;
		try {
			view = inflater.inflate(R.layout.actions_list_fragment, container, false);
		} catch(Exception ex) {
			Log.e(TAG, "Failed to inflate fragment: " + ex.getMessage());
		}
		return view;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			setHasOptionsMenu(true);
		} catch(Exception ex) {
			Log.e(TAG, "Failed to create menu: " + ex.getMessage());
		}

		adapter_ = new ActionItemAdapter(getActivity(), CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		setListAdapter(adapter_);

		getLoaderManager().initLoader(LOADER_ACTIONS, null, lm_ = new LoaderMgr());

		if (savedInstanceState != null) {
			// Restore last state for checked position.
			cur_list_position_ = savedInstanceState.getInt("cur_list_position", 0);
		}

		registerForContextMenu(getListView());
	}

	@Override
	public void onAttach(Activity activity) {
		final Intent intent = activity.getIntent();
		
		if (intent.hasExtra("list-id")) {
			list_id_ = intent.getExtras().getLong("list-id");
		} 
		
		if (intent.hasExtra("title")) {
			this.getActivity().getActionBar().setTitle(intent.getExtras().getString("title"));
		} 
		
		super.onAttach(activity);
	}
	
	protected void SelectMenusToShow(Menu menu) {
		
	}

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {        	
		
		inflater.inflate(R.menu.actions, menu);

		// Add search
		SearchView sv = new SearchView(getActivity());
		sv.setOnQueryTextListener(this);
		menu.findItem(R.id.search_actions).setActionView(sv); 
		final int tv_id = sv.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
		if (0 < tv_id) {
			AutoCompleteTextView tv = (AutoCompleteTextView)sv.findViewById(tv_id);
			if (null != tv) {
				tv.setTextColor(Color.CYAN);
			}
		}
		group_actions_set_when_ = menu.findItem(R.id.group_set_when_actions);
	    group_actions_delete_ = menu.findItem(R.id.group_delete_actions);
	    group_actions_move_ = menu.findItem(R.id.group_move_actions);
	    
	    SelectMenusToShow(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		super.onCreateContextMenu(menu, v, menuInfo);
		current_item_ = null;

		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.actions_context_menu, menu);
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		
		// TODO: Fix. Does not work.
		try {
			final View sv = info.targetView;
			if (null != sv) {
				final ItemData d = (ItemData)sv.getTag();
				if (null != d) {
					if (d.completed_) {
						MenuItem mi = menu.findItem(R.id.uncomplete_action_menu_item).setVisible(true);
						if (null != mi) {
							mi.setVisible(true);
							current_item_ = d;
						}
					}
				}
			}
		} catch(Exception ex) {
			Log.e(TAG, "Caught exception while modifying the context menu: " + ex.getMessage());
		}
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {

		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch(item.getItemId()) {

		case R.id.edit_action_menu_item:
		{
			Intent intent = new Intent();
			final long selected_id = info.id;
			if (selected_id == ListView.INVALID_ROW_ID) {
				Log.w(TAG, "No List item selected");
			} else {
				EditAction(selected_id);
			}
			return true;
		}	
		case R.id.delete_action_menu_item:
		{
			final long selected_id = info.id;
			if (selected_id == ListView.INVALID_ROW_ID) {
				Log.w(TAG, "No List item selected");
			} else {
				DeleteAction(selected_id);
			}
			return true;
		}
		case R.id.uncomplete_action_menu_item:
			SetCompleted(current_item_, false);
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void DeleteAction(final long list_id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.delete_action_confirmation);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				ContentResolver resolver = getActivity().getContentResolver();
				Uri uri = Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + list_id);
				resolver.delete(uri, null, null);
			}
		});

		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch(item.getItemId()) {
		case R.id.add_action:
			Intent intent = new Intent();
			intent.putExtra("list-id", list_id_);
			intent.setClass(getActivity(), Action.class);
			startActivity(intent);
			return true;
		case R.id.group_delete_actions:
			DeleteSelected();
			return true;
		case R.id.group_set_when_actions:
			SetWhenOnSelected();
			return true;
		case R.id.group_move_actions:
			MoveOnSelected();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
						ContentResolver resolver = getActivity().getContentResolver();
						ContentValues values = new ContentValues();
						values.put(GtdContentProvider.ActionsDef.DUE_BY_TIME, w.GetUnixTime());
						values.put(GtdContentProvider.ActionsDef.DUE_TYPE, w.getDueType().ordinal());
						Iterator<Long> i = adapter_.selected_.iterator();
						StringBuilder filter = new StringBuilder();
						boolean virgin = true;
						while(i.hasNext()) {
							final long action_id = i.next();
							if (virgin) {
								virgin = false;
								filter.append(GtdContentProvider.ActionsDef._ID + " IN (");
							} else {
								filter.append(",");
							}
							filter.append(action_id);
						}
						filter.append(")");
						if (!virgin) {
							Log.d(TAG, "Query: " + filter.toString());
							resolver.update(GtdContentProvider.ActionsDef.CONTENT_URI, values, filter.toString(), null);
						}
						adapter_.selected_.clear();
						OnSelectionMenuChange(false);
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
	
	private void SetWhenOnSelected() {
		Intent intent = new Intent();
		Bundle b = new Bundle();
		When when = new When();
		b.putSerializable("when", when);
		intent.putExtras(b);
		intent.setClass(getActivity(), WhenActivity.class);
		startActivityForResult(intent, RESULT_WHEN);	
	}
	
	private void DeleteSelected() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.delete_action_confirmation);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				ContentResolver resolver = getActivity().getContentResolver();
				
				Iterator<Long> i = adapter_.selected_.iterator();
				while(i.hasNext()) {
					final long action_id = i.next();
					Uri uri = Uri.parse(GtdContentProvider.ActionsDef.CONTENT_URI + "/" + action_id);
					resolver.delete(uri, null, null);
				}
				
				adapter_.selected_.clear();
				OnSelectionMenuChange(false);
			}
		});

		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
		
	}
	
	private void MoveOnSelected() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final ArrayList<String> items = new ArrayList<String>();
		final ArrayList<Long> id_list = new ArrayList<Long>();
		
		{
			ContentResolver resolver = getActivity().getContentResolver();
			Cursor c = resolver.query(GtdContentProvider.ListsDef.CONTENT_URI, 
					new String[] { GtdContentProvider.ListsDef._ID, GtdContentProvider.ListsDef.NAME },
					null, null, null);
			if (c.moveToFirst()) {
				do {
					id_list.add(c.getLong(0));
					items.add(c.getString(1));
				} while(c.moveToNext());
			}
		}
		
		final String[] item_array = new String[id_list.size()];
		for(int i = 0; i < item_array.length; ++i) {
			item_array[i] = items.get(i);
		}
		
		builder.setTitle(R.string.move_actions);
		builder.setItems(item_array, new  DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentResolver resolver = getActivity().getContentResolver();
				ContentValues values = new ContentValues();
				values.put(GtdContentProvider.ActionsDef.LIST_ID, id_list.get(which));
				Iterator<Long> i = adapter_.selected_.iterator();
				StringBuilder filter = new StringBuilder();
				boolean virgin = true;
				while(i.hasNext()) {
					final long action_id = i.next();
					if (virgin) {
						virgin = false;
						filter.append(GtdContentProvider.ActionsDef._ID + " IN (");
					} else {
						filter.append(",");
					}
					filter.append(action_id);
				}
				filter.append(")");
				if (!virgin) {
					Log.d(TAG, "Query: " + filter.toString());
					resolver.update(GtdContentProvider.ActionsDef.CONTENT_URI, values, filter.toString(), null);
				}
				adapter_.selected_.clear();
				OnSelectionMenuChange(false);
				
				dialog.dismiss();
			}
			
		});
		
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
	}


	public boolean onQueryTextChange(String newText) {
		// Called when the action bar search text has changed.  Update
		// the search filter, and restart the loader to do a new query
		// with this filter.
		
		if (search_string_.equals(newText)) {
			Log.d(TAG, "onQueryTextChange: Android is lying to me. The search-string has not changed!");
			return true;
		}
		
		search_string_ = newText;
		Requery();
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("cur_list_position", cur_list_position_);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "Item clicked: " + id);
		EditAction(id);
	}
	
	
	private void EditAction(final long id) {
		Intent intent = new Intent();
		intent.putExtra("id", id);
		intent.setClass(getActivity(), Action.class);
		startActivity(intent);
	}
	
	void OnSelectionMenuChange(boolean haveSelected) {
		if (null != group_actions_set_when_)
			group_actions_set_when_.setVisible(haveSelected);
		if (null != group_actions_delete_)
			group_actions_delete_.setVisible(haveSelected);
		if (null != group_actions_move_)
			group_actions_move_.setVisible(haveSelected);
    }
	
	protected void Requery() {
		if (ready_to_requery_) {
			getLoaderManager().restartLoader(LOADER_ACTIONS, null, lm_);
		}
	}
	
	void MySetEmptyTest(String text) {
		View view = this.getView();
		if (null != view) {
			TextView tv = (TextView)view.findViewById(android.R.id.empty);
			if (null != tv) {
				tv.setText(text);
			}
		}
	}
}

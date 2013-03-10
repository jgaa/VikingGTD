package eu.lastviking.app.gtd;

import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeSet;

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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;
import eu.lastviking.app.vgtd.R;

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
    private String search_string_;
    protected static final int TAG_ID = 1;
    View.OnClickListener checkout_listener_;
    private ItemData current_item_; // for context menu
    private MenuItem group_actions_set_when_;
    private MenuItem group_actions_delete_;
    
    
    static class ItemData {
    	long id_;
    	boolean completed_;
    	int priority_;
    	When when_;
    	ImageView image_view_;
    	TextView name_view_;
    	CheckBox selected_view_;    	
    }

    
    class ActionItemAdapter extends CursorAdapter {
    	
    	public TreeSet<Long> selected_ = new TreeSet<Long>();
    	TreeSet<Long> expanded_ = new TreeSet<Long>();
    	Bitmap[] priority_map_;
    	
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
				d.when_.SetTime(c.getLong(GtdContentProvider.ActionsDef.Fields.DUE_BY_TIME.ordinal()), 
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
					v.setBackgroundColor(Color.YELLOW);
					break;
				case TOMORROW:
					v.setBackgroundColor(Color.rgb(255, 255, 125));
					break;
				case THIS_WEEK:
					v.setBackgroundColor(Color.rgb(255, 255, 200));
					break;
				case FUTURE:
					v.setBackgroundColor(Color.WHITE);
					break;
				case OVERDUE:
					v.setBackgroundColor(Color.rgb(255, 120, 120));
					break;
				}
				
				d.image_view_.setImageBitmap(priority_map_[d.priority_]);
				d.image_view_.setOnClickListener(checkout_listener_);
			}
			
			String name = c.getString(GtdContentProvider.ActionsDef.Fields.NAME.ordinal());
			d.name_view_.setText(name);
			final boolean is_selected =  selected_.contains(c.getLong(GtdContentProvider.ActionsDef.Fields._ID.ordinal()));
			d.selected_view_.setChecked(is_selected);
			
			Log.d(TAG, "Item# " + d.id_ + " " + name + ": " + (d.completed_ ? "COMPLETE" : "pending"));
		}

		@Override
		public View newView(Context context, Cursor c, ViewGroup vg) {
			final LayoutInflater inflater = LayoutInflater.from(context);
	        View v = inflater.inflate(R.layout.action_item, vg, false);
	        ItemData d = new ItemData();
	        d.image_view_ = (ImageView)v.findViewById(R.id.action_done); 
	    	d.name_view_  = (TextView)v.findViewById(R.id.action_name);
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
    		}
    	}
    }
    

	private class LoaderMgr implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			
			Log.d(TAG, "LoaderMgr: onCreateLoader called for loader id " + id);
			
			Uri uri = GtdContentProvider.ActionsDef.CONTENT_URI;
			
			if (LOADER_ACTIONS == id) {
				String[] filter_args = null;
				String filter = GtdContentProvider.ActionsDef.LIST_ID + "=?";
				if (null != search_string_) {
					filter_args = new String[] {String.valueOf(list_id_), "%" + search_string_ + "%"};
					filter = filter + " AND " + GtdContentProvider.ActionsDef.NAME  + " LIKE ?";
				} else {
					filter_args = new String[] {String.valueOf(list_id_)};
				}
				try {
					//Log.d(TAG, "Executing query: " + filter );
					return new CursorLoader(getActivity(), uri, GtdContentProvider.ActionsDef.PROJECTION_ALL, 
							filter, 
							filter_args,
							GtdContentProvider.ActionsDef.COMPLETED_TIME + " ASC, " 
									+ GtdContentProvider.ActionsDef.PRIORITY + " ASC, "
									+ GtdContentProvider.ActionsDef.DUE_BY_TIME + " ASC, "
									+ GtdContentProvider.ActionsDef.CREATED_DATE + " ASC");
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
		}

		@Override
		public void onLoaderReset(Loader<Cursor> cursor) {
			Log.d(TAG, "LoaderMgr: onLoaderReset called.");
			adapter_.swapCursor(null);
			
		}
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getResources().getString(R.string.no_actions));

		setHasOptionsMenu(true);

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
		
		list_id_ = intent.getExtras().getLong("list-id");
		
		if (intent.hasExtra("id")) {
			list_id_ = intent.getExtras().getLong("id", 0);
		} else {
			list_id_ = 0;
		}
		
		if (intent.hasExtra("title")) {
			this.getActivity().getActionBar().setTitle(intent.getExtras().getString("title"));
		}
		
		super.onAttach(activity);
	}

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {        	
		
		inflater.inflate(R.menu.actions, menu);

		// Add search
		SearchView sv = new SearchView(getActivity());
		sv.setOnQueryTextListener(this);
		menu.findItem(R.id.search_actions).setActionView(sv); 
		
		group_actions_set_when_ = menu.findItem(R.id.group_set_when_actions);
	    group_actions_delete_ = menu.findItem(R.id.group_delete_actions);
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
						values.put(GtdContentProvider.ActionsDef.DUE_BY_TIME, w.GetAsMilliseconds());
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
			}
		});

		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
		
	}

	public boolean onQueryTextChange(String newText) {
		// Called when the action bar search text has changed.  Update
		// the search filter, and restart the loader to do a new query
		// with this filter.
		search_string_ = !TextUtils.isEmpty(newText) ? newText : null;
		getLoaderManager().restartLoader(LOADER_ACTIONS, null, lm_);
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
		intent.putExtra("list-id", list_id_);
		intent.setClass(getActivity(), Action.class);
		startActivity(intent);
	}
	
	void OnSelectionMenuChange(boolean haveSelected) {
		if (null != group_actions_set_when_)
			group_actions_set_when_.setVisible(haveSelected);
		if (null != group_actions_delete_)
		group_actions_delete_.setVisible(haveSelected);
    }
}

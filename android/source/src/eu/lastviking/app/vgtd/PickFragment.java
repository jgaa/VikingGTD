package eu.lastviking.app.vgtd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class PickFragment extends ActionsListFragment {
	private final static String TAG = "PickFragment";
	private final static String FILTER_EXPANDED_KEY = "Pick-Filter-Expanded";

	private boolean expanded_ = true;
	private ImageView more_ctl_;
	private LinearLayout filters_;
	private MultiSpinner pick_lists_;
	private MultiSpinner pick_priority_;
	private MultiSpinner pick_where_;
	private MultiSpinner pick_how_;
	private MultiSpinner pick_when_;
	private MultiSpinner pick_misc_;
	Bitmap rotated_btn_ = null;
	
	private List<MultiSpinner.Data> lists_ = new ArrayList<MultiSpinner.Data>();
	private List<MultiSpinner.Data> priority_ = new ArrayList<MultiSpinner.Data>();
	private List<MultiSpinner.Data> where_ = new ArrayList<MultiSpinner.Data>();
	private List<MultiSpinner.Data> how_ = new ArrayList<MultiSpinner.Data>();
	private List<MultiSpinner.Data> when_ = new ArrayList<MultiSpinner.Data>();
	private List<MultiSpinner.Data> misc_ = new ArrayList<MultiSpinner.Data>();
	
	private void ClearFilters() {
		pick_lists_.ClearSelections();
		pick_lists_.UpdateData();
		
		pick_priority_.ClearSelections();
		pick_priority_.UpdateData();
		pick_where_.ClearSelections();
		pick_where_.UpdateData();
		pick_how_.ClearSelections();
		pick_how_.UpdateData();
		pick_when_.ClearSelections();
		pick_when_.UpdateData();
		pick_misc_.ClearSelections();
		pick_misc_.UpdateData();
		Requery();
	}

	
	class GenericListItem implements MultiSpinner.Data {

		GenericListItem(String name, long id, boolean selected) {
			name_ = name;
			id_ = id;
			selected_ = selected;
		}
		
		@Override
		public boolean GetSelected() {
			return selected_;
		}

		@Override
		public void SetSelected(boolean selected) {
			selected_ = selected;
		}
		
		@Override
		public String toString() {
			return name_;
		}
		
		public long GetId() {
			return id_;
		}
		
		private String name_;
		private long id_;
		private boolean selected_;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (null != savedInstanceState) {
			expanded_ = savedInstanceState.getBoolean(FILTER_EXPANDED_KEY, expanded_);
			// TODO: Restore the state of the filters themself
		}
		
		View view = null;
		try {
			view = inflater.inflate(R.layout.pick_actions, container, false);
			filters_ = (LinearLayout)view.findViewById(R.id.pick_filter_layout);
			more_ctl_ = (ImageView)view.findViewById(R.id.pick_more_img);
			pick_lists_ = (MultiSpinner)view.findViewById(R.id.pick_lists);
			pick_priority_ = (MultiSpinner)view.findViewById(R.id.pick_priority);
			pick_where_ = (MultiSpinner)view.findViewById(R.id.pick_where);
			pick_how_ = (MultiSpinner)view.findViewById(R.id.pick_how);
			pick_when_ = (MultiSpinner)view.findViewById(R.id.pick_when);
			pick_misc_ = (MultiSpinner)view.findViewById(R.id.pick_misc);
			
			InitializeFilterControls();
		} catch(Exception ex) {
			Log.e(TAG, "Failed to inflate the view: " + ex.getMessage());
		}
		
		filters_.setBackgroundColor(Color.rgb(230, 230, 255));
		more_ctl_.setBackgroundColor(Color.rgb(40, 40, 60));
		more_ctl_.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				expanded_ = !expanded_;
				PrepareFilter();
			}
		});
		
		setHasOptionsMenu(true);
		
		return view;
	}
	
		
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.pick, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.clear_pick_filters:
			ClearFilters();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}	


	private void GetSqlIn(String col, StringBuilder filter, final List<MultiSpinner.Data> data) {
		boolean virgin = true;
		
		Iterator<MultiSpinner.Data> it = data.iterator();
		while (it.hasNext()) {
			final GenericListItem d = (GenericListItem)it.next();
			if (d.GetSelected()) {
				if (virgin) {
					if (filter.length() > 0) {
						filter.append(" AND ");
					}
					filter.append(col + " IN (");
					virgin = false;
				} else {
					filter.append(",");
				}
				filter.append(Long.valueOf(d.GetId()));
			}
		}
		
		if (!virgin) {
			filter.append(")");
		}
	}

	// Will not prefix with AND 
	private void GetSqlOnTime(Calendar from, Calendar to, StringBuilder filter) {
		boolean virgin = true;
				
		if (null != from && from.getTimeInMillis() > 0) {		
			if (filter.length() > 0) {
				virgin = false;
			}
			filter.append(GtdContentProvider.ActionsDef.DUE_BY_TIME + " >= " + (from.getTimeInMillis() / 1000));
		}
		
		if (null != to && to.getTimeInMillis() > 0) {		
			if (filter.length() > 0) {
				if (!virgin) {
					filter.append(" AND ");
					virgin = false;
				}
			}
			filter.append(GtdContentProvider.ActionsDef.DUE_BY_TIME + " < " + (to.getTimeInMillis() / 1000));
		}	
	}
	
	private void GeqSqlOnDateSelection(StringBuilder filter) {
		Iterator<MultiSpinner.Data> it = when_.iterator();
		boolean virgin = true;
		while (it.hasNext()) {
			final GenericListItem d = (GenericListItem)it.next();
			if (d.GetSelected()) {
				Calendar from = null, to = null;
				switch((int)d.GetId()) {
				case 0: // Today
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.DAY_OF_YEAR, 1);
					break;

				case 1: // Tomorrow
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.add(Calendar.DAY_OF_YEAR, 1);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.DAY_OF_YEAR, 1);
					break;

				case 2: // This week
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.DAY_OF_YEAR, 7);
					break;

				case 3: // Next week 
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
					from.add(Calendar.DAY_OF_YEAR, 7);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.DAY_OF_YEAR, 7);
					break;

				case 4: // This month 
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_MONTH, 1);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.MONTH, 1);
					break;

				case 5: // Next month 
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_MONTH, 1);
					from.add(Calendar.MONTH, 1);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.MONTH, 1);
					break;

				case 6: // This quarter 
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_MONTH, 1);
					from.set(Calendar.MONTH, from.get(Calendar.MONTH) % 3);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.MONTH, 3);
					break;

				case 7: // Next quarter 
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_MONTH, 1);
					from.set(Calendar.MONTH, from.get(Calendar.MONTH) % 3);
					from.add(Calendar.MONTH, 3);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.MONTH, 3);
					break;

				case 8: // This year  
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_YEAR, 1);
					to = new GregorianCalendar();
					to.setTime(from.getTime());
					to.add(Calendar.YEAR, 1);
					break;

				case 9: // Next year or later 
					from = Calendar.getInstance();
					from.set(Calendar.HOUR_OF_DAY, 0);
					from.set(Calendar.MINUTE, 0);
					from.set(Calendar.SECOND, 0);
					from.set(Calendar.DAY_OF_YEAR, 1);
					from.add(Calendar.YEAR, 1);
					break;

				}
				
				if (virgin) {
					if (filter.length() > 0) {
						filter.append(" AND ");
					}
					filter.append(" ((");
					virgin = false;
				} else {
					filter.append(") OR (");
				}
					
				GetSqlOnTime(from, to, filter);
			}
		}
		
		if (!virgin) {
			filter.append("))");
		}
	}
	
	private void GeqSqlOnMisc(StringBuilder filter) {
		Iterator<MultiSpinner.Data> it = misc_.iterator();
		boolean skip_completed = false;
		while (it.hasNext()) {
			final GenericListItem d = (GenericListItem)it.next();

			switch((int)d.GetId()) {
			case 0: // overdue
				if (d.GetSelected()) {
					if (filter.length() > 0) {
						filter.append(" AND ");
					}

					GetSqlOnTime(null, Calendar.getInstance(), filter);
					filter.append(" AND " + GtdContentProvider.ActionsDef.COMPLETED + " = 0");
					filter.append(" AND " + GtdContentProvider.ActionsDef.DUE_TYPE + " >= " + When.DueTypes.YEAR.ordinal());
					skip_completed = true;
				}
				break;
			case 1: // Completed
				if (!skip_completed && !d.GetSelected()) {
					if (filter.length() > 0) {
						filter.append(" AND ");
					}

					filter.append(GtdContentProvider.ActionsDef.COMPLETED + " = 0");
				}
				break;
			case 2: // Scheduled loosely
				if (d.GetSelected()) {
					if (filter.length() > 0) {
						filter.append(" AND ");
					}
					if (misc_.get(3).GetSelected()) {
						filter.append("(");
					}
					filter.append("(");
					filter.append(GtdContentProvider.ActionsDef.DUE_TYPE + " < " + When.DueTypes.DATE.ordinal());
					filter.append(" AND ");
					filter.append(GtdContentProvider.ActionsDef.DUE_TYPE + " > " + When.DueTypes.NONE.ordinal());
					filter.append(")");
				}
				break;
			case 3: // Unscheduled
				if (d.GetSelected()) {
					boolean do_close_bracket = false;
					if (misc_.get(2).GetSelected()) {
						filter.append(" OR ");
						do_close_bracket = true;
					}
					else if (filter.length() > 0) {
						filter.append(" AND ");
					}
					filter.append(GtdContentProvider.ActionsDef.DUE_TYPE + " = " + When.DueTypes.NONE.ordinal());
					if (do_close_bracket) {
						filter.append(")"); // Closes loosely scheduled and unscheduled bracket
					}
				}
				break;
			}
		}
	}


	@Override
	public String GetBaseFilterForDbQuery() {
		StringBuilder filter = new StringBuilder();
	
		GetSqlIn(GtdContentProvider.ActionsDef.LIST_ID, filter, lists_);
		GetSqlIn(GtdContentProvider.ActionsDef.FOCUS_NEEDED, filter, how_);
		GetSqlIn(GtdContentProvider.ActionsDef.PRIORITY, filter, priority_);
		GetSqlIn(GtdContentProvider.ActionsWithLocDef.LOCATION_ID, filter, where_);
		GeqSqlOnDateSelection(filter);
		GeqSqlOnMisc(filter);
		
		return filter.toString();
    }
	
	@Override
	public String[] GetDbProjection()  {
		if (pick_where_.HasSelected()) {
			return GtdContentProvider.ActionsWithLocDef.PROJECTION_ALL;
		}
		return super.GetDbProjection();
	}
	
	@Override
	public Uri GetUriForDbQuery() {
		if (pick_where_.HasSelected()) {
			return GtdContentProvider.ActionsWithLocDef.CONTENT_URI;
		}
		return super.GetUriForDbQuery();
	}

	@Override
	protected void SelectMenusToShow(Menu menu) {
		// Remove add, as we don't have any active list
		MenuItem add = menu.findItem(R.id.add_action);
		if (null != add) {
			add.setVisible(false);
		}
	}

	@Override
	public void onResume() {
		PrepareFilter();
		super.onResume();
	}

	private void PrepareFilter() {
		
		if (expanded_) {
			if (null == rotated_btn_) {
				Bitmap bm =  BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_more);
				
				Matrix matrix = new Matrix();
				matrix.postRotate(180);

				rotated_btn_ = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(),
				        matrix, true);
			}
			
			more_ctl_.setImageBitmap(rotated_btn_);
			filters_.setVisibility(LinearLayout.VISIBLE);
		} else {
			// Update the show/hide button and filters
			more_ctl_.setImageResource(android.R.drawable.ic_menu_more);
			filters_.setVisibility(LinearLayout.GONE);
		}
	}
	
	private void InitializeFilterControls() {
				
		ContentResolver resolver = getActivity().getContentResolver();
			
		// Get lists
		{
			lists_.clear();
			Cursor c = resolver.query(GtdContentProvider.ListsDef.CONTENT_URI, 
					new String[] { GtdContentProvider.ListsDef._ID, GtdContentProvider.ListsDef.NAME },
					null, null, null);
			if (c.moveToFirst()) {
				do {
					lists_.add(new GenericListItem(
							c.getString(GtdContentProvider.ListsDef.Fields.NAME.ordinal()),
							c.getLong(GtdContentProvider.ListsDef.Fields._ID.ordinal()),
							false));					
				} while(c.moveToNext());
			}
		}
		
		
		// Get locations
		{
			where_.clear();
			Cursor c = resolver.query(GtdContentProvider.LocationsDef.CONTENT_URI, 
					GtdContentProvider.LocationsDef.PROJECTION_ALL, null, null, null);
			if (c.moveToFirst()) {
				do {
					where_.add(new GenericListItem(
							c.getString(GtdContentProvider.LocationsDef.Fields.NAME.ordinal()),
							c.getLong(GtdContentProvider.LocationsDef.Fields._ID.ordinal()),
							false));					
				} while(c.moveToNext());
			}
		}
		
		// Get priorities
		if (priority_.size() == 0) {
			String[] data = getActivity().getResources().getStringArray(R.array.priorities);
			for(int i = 0; i < data.length; ++i) {
				priority_.add(new GenericListItem(data[i], i, false));
			}
		}
		
		// Get how
		if (how_.size() == 0) {
			String[] data = getActivity().getResources().getStringArray(R.array.focuses_needed);
			for(int i = 0; i < data.length; ++i) {
				how_.add(new GenericListItem(data[i], i, false));
			}
		}
		
		// Get when
		if (when_.size() == 0) {
			String[] data = getActivity().getResources().getStringArray(R.array.when_filter);
			// Strip off the first and the last item. They are pointless here
			for(int i = 0; i < data.length; ++i) {
				when_.add(new GenericListItem(data[i], i, false));
			}
		}
		
		// Get misc
		if (misc_.size() == 0) {
			String[] data = getActivity().getResources().getStringArray(R.array.misc_filter);
			// Strip off the first and the last item. They are pointless here
			for(int i = 0; i < data.length; ++i) {
				misc_.add(new GenericListItem(data[i], i, false));
			}
		}
		
		MultiSpinner.MultiSpinnerListener on_change = new MultiSpinner.MultiSpinnerListener() {

			@Override
			public void onItemsSelected(boolean[] selected) {
				Requery();
			}
		};
				
		pick_lists_.SetItems(lists_, getActivity().getString(R.string.all_lists), getActivity().getString(R.string.all_lists), on_change);
		pick_where_.SetItems(where_, getActivity().getString(R.string.anywhere), getActivity().getString(R.string.anywhere), on_change);
		pick_priority_.SetItems(priority_, getActivity().getString(R.string.any_priority), getActivity().getString(R.string.any_priority), on_change);
		pick_how_.SetItems(how_, getActivity().getString(R.string.any_how), getActivity().getString(R.string.any_how), on_change);
		pick_when_.SetItems(when_, getActivity().getString(R.string.any_time), getActivity().getString(R.string.any_time), on_change);
		pick_misc_.SetItems(misc_, getActivity().getString(R.string.misc_any), getActivity().getString(R.string.misc_none), on_change);
	}

	@Override
	public void setEmptyText(CharSequence text) {
		// Do nothing
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(FILTER_EXPANDED_KEY, expanded_);
	}
}

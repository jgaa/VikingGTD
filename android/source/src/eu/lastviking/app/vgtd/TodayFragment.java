package eu.lastviking.app.vgtd;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class TodayFragment extends ActionsListFragment {
	private final static String TAG = "TodayFragment";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.today_list_fragment, container, false);
	}
	

	@Override
	public String GetBaseFilterForDbQuery() {
		StringBuilder filter = new StringBuilder();
		filter.append("(" + GtdContentProvider.ActionsDef.DUE_TYPE + " >= " + When.DueTypes.DATE.ordinal());
		
		Calendar from = Calendar.getInstance();
		from.set(Calendar.HOUR_OF_DAY, 0);
		from.set(Calendar.MINUTE, 0);
		from.set(Calendar.SECOND, 0);
		
		filter.append(" AND " + GtdContentProvider.ActionsDef.DUE_BY_TIME + " >= " + (from.getTimeInMillis() / 1000));
		
		Calendar to = new GregorianCalendar();
		to.setTime(from.getTime());
		to.add(Calendar.DAY_OF_YEAR, 1);
		
		filter.append(" AND " + GtdContentProvider.ActionsDef.DUE_BY_TIME + " < " + (to.getTimeInMillis() / 1000) + ")");
		
		// Log.d(TAG, "Filter: " + filter.toString());
		return filter.toString();
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
	boolean IsListTodayOnly() {
    	return true;
    }
}

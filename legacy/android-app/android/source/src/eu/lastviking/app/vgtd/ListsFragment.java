package eu.lastviking.app.vgtd;

import eu.lastviking.app.vgtd.R;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SearchView.OnQueryTextListener;

public class ListsFragment extends ListFragment implements OnQueryTextListener {

	private static final int LOADER_LISTS = 0;
	private final static String TAG = "ListsFragment";
	
    protected int cur_list_position_ = 0;
    protected SimpleCursorAdapter adapter_;
    protected String search_string_ = "";
    static int lm_instance_cnt_ = 0;
    protected LoaderMgr lm_ = null;
    
    private class LoaderMgr implements LoaderManager.LoaderCallbacks<Cursor> {

    	private int instance_id_ = 0;
    	
    	LoaderMgr() {
    		instance_id_ = ++lm_instance_cnt_;
    	}
    	
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			
			MySetEmptyTest(getResources().getString(R.string.loading_lists));
			Log.d(TAG, "LoaderMgr: onCreateLoader #" + instance_id_ + " called for loader id " + id);
			
			Uri uri = GtdContentProvider.ListsDef.CONTENT_URI;
			
			if (LOADER_LISTS == id) {
				String[] filter_args = null;
				String filter = null;
				if (!search_string_.isEmpty()) {
					filter_args = new String[] {"%" + search_string_ + "%"};
					filter = GtdContentProvider.ListsDef.NAME  + " LIKE ?";
				} 
				return new CursorLoader(getActivity(), uri, 
						new String[] { "_id", GtdContentProvider.ListsDef.NAME }, 
						filter, filter_args, null);
			}
			
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			Log.d(TAG, "LoaderMgr #" + instance_id_ + ": onLoadFinished called.");
			adapter_.swapCursor(cursor);
			MySetEmptyTest(getResources().getString(R.string.no_lists));
		}

		@Override
		public void onLoaderReset(Loader<Cursor> cursor) {
			Log.d(TAG, "LoaderMgr #" + instance_id_ + ": onLoaderReset called.");
			adapter_.swapCursor(null);
			MySetEmptyTest(getResources().getString(R.string.loading_lists));
		}
    }
    
    
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.lists_fragment, container, false);
	}


	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setHasOptionsMenu(true);

        adapter_ = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                new String[] { GtdContentProvider.ListsDef.NAME },
                new int[] { android.R.id.text1 }, 0);
        
        setListAdapter(adapter_);
                
        getLoaderManager().initLoader(LOADER_LISTS, null, lm_ = new LoaderMgr());
        
        
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            cur_list_position_ = savedInstanceState.getInt("cur_list_position", 0);
        }
        
        registerForContextMenu(getListView());
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
    
    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {        	
        inflater.inflate(R.menu.lists, menu);
        
        // Add search
        SearchView sv = new SearchView(getActivity());
        sv.setOnQueryTextListener(this);
        
        final int tv_id = sv.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
		if (0 < tv_id) {
			AutoCompleteTextView tv = (AutoCompleteTextView)sv.findViewById(tv_id);
			if (null != tv) {
				tv.setTextColor(Color.CYAN);
			}
		}
        
		menu.findItem(R.id.search_lists).setActionView(sv);                        
    }
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	
		MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.lists_context_menu, menu);			
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch(item.getItemId()) {
		
		case R.id.edit_list_menu_item:
		{
			Intent intent = new Intent();
			final long selected_id = info.id;
			if (selected_id == ListView.INVALID_ROW_ID) {
				Log.w(TAG, "No List item selected");
			} else {
				intent.putExtra("id", selected_id);
				intent.setClass(getActivity(), EditList.class);
				startActivity(intent);
			}
			return true;
		}	
		case R.id.delete_list_menu_item:
		{
			final long selected_id = info.id;
			if (selected_id == ListView.INVALID_ROW_ID) {
				Log.w(TAG, "No List item selected");
			} else {
				DeleteList(selected_id);
			}
			return true;
		}
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void DeleteList(final long list_id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.delete_list_confirmation);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   ContentResolver resolver = getActivity().getContentResolver();
	        	   Uri uri = Uri.parse(GtdContentProvider.ListsDef.CONTENT_URI + "/" + list_id);
	        	   resolver.delete(uri, null, null);
	           }
		});
		
		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
    	switch(item.getItemId()) {
    	case R.id.add_list:
    		Intent intent = new Intent();
            intent.setClass(getActivity(), EditList.class);
            startActivity(intent);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
	}
	

	public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
				
		if (search_string_.equals(newText)) {
			Log.d(TAG, "onQueryTextChange: Android is lying to me. The search-string has not changed!");
			return true;
		}
		
		Log.d(TAG, "onQueryTextChange: " + newText);
    	search_string_ = newText;
        getLoaderManager().restartLoader(LOADER_LISTS, null, lm_);
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
        showDetails(position, id, ((TextView)v).getText().toString());
    }

    void showDetails(final int index, final long id, final String label) {
    	cur_list_position_ = index;

    	Intent intent = new Intent();
    	if (0 < id) {
    		intent.putExtra("list-id", id);
    	}
    	intent.putExtra("title", label);
    	intent.setClass(getActivity(), Actions.class);
    	startActivity(intent);
    }  
}

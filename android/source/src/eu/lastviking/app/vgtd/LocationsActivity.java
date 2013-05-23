package eu.lastviking.app.vgtd;

import eu.lastviking.app.vgtd.GtdContentProvider.LocationsDef;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class LocationsActivity extends ListActivity {
	private static final String TAG = "LocationsActivity";
	private static final int LOADER_LOCATIONS = 0;
	private LoaderMgr lm_ = null;
    protected SimpleCursorAdapter adapter_; 

	Context GetContext() {
		return this;
	}
	
	private class LoaderMgr implements LoaderManager.LoaderCallbacks<Cursor> {
		
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			
			Log.d(TAG, "LoaderMgr: onCreateLoader called for loader id " + id);
						
			if (LOADER_LOCATIONS == id) {

				try {
					return new CursorLoader(GetContext(), LocationsDef.CONTENT_URI, LocationsDef.PROJECTION_ALL, 
							null, null, LocationsDef.NAME+ " ASC");
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
			if (null != adapter_) {
				adapter_.swapCursor(null);
			}
			
		}
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		adapter_ = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, null,
                new String[] { GtdContentProvider.ListsDef.NAME },
                new int[] { android.R.id.text1 }, 0);
        
        setListAdapter(adapter_);
        
        lm_ = new LoaderMgr();
        getLoaderManager().initLoader(LOADER_LOCATIONS, null, lm_);
        
        registerForContextMenu(getListView());
        
        getActionBar().setTitle(R.string.locations);
	}
	
	private void EditLocation(final long id, final String name) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		dialog.setTitle((0 < id) ? R.string.edit_location : R.string.add_location);

		final EditText input = new EditText(this);
		input.setText(name);
		dialog.setView(input);

		dialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {				
				try {
					SaveLocation(id, input.getText().toString());
				} catch(Exception ex) {
					Toast.makeText(GetContext(), R.string.save_location_failed, Toast.LENGTH_LONG).show();
					return;
				}
				dialog.dismiss();
			}
		});

		dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}	
		});

		dialog.show();
	}
	
	protected void SaveLocation(long id, String name) {
		
		ContentValues values = new ContentValues();
		values.put(GtdContentProvider.LocationsDef.NAME, name);
		if (0 < id) {
			// Update
			getContentResolver().update(Uri.parse(GtdContentProvider.LocationsDef.CONTENT_URI + "/" + id), values, null, null);
		} else {
			// Add
			getContentResolver().insert(GtdContentProvider.LocationsDef.CONTENT_URI, values);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_location) {
			EditLocation(0, "");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		if (item.getItemId() == R.id.delete_location_menu_item) {
			DeleteLocationButAsk(info.id);
			return true;
		}
			
		if (item.getItemId() == R.id.edit_locations_item) {
			EditLocation(info.id, ((TextView)info.targetView).getText().toString());
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void DeleteLocationButAsk(final long location_id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_location_confirmation);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   DeleteLocation(location_id);
	           }
		});
		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
	}

	private void DeleteLocation(long id) {
		
		try {
			getContentResolver().delete(Uri.parse(GtdContentProvider.LocationsDef.CONTENT_URI + "/" + id), null, null);
		} catch(Exception ex) {
			Log.e(TAG, "Failed to delete: " + ex.getMessage());
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		getMenuInflater().inflate(R.menu.locations_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.locations, menu);
		return super.onCreateOptionsMenu(menu);
	}
}

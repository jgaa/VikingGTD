package eu.lastviking.app.gtd;

import android.app.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import eu.lastviking.app.vgtd.R;


/* This class implements the List and Activities[-list] UI elements on 
 * horizontal and landscape mode.  
 */

public class Lists extends Activity  {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lists_layout);        
    }
	
	 @Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
				super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		super.onSaveInstanceState(outState);
	}
		
	
	 public static class EditList extends VikingBackHandlerActivity
     {
		 static public final String TAG = "EditList";
		 		 
		 
     	@Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.edit_list);
         }
     }
	 
	 	
    public static class ListsFragment extends ListFragment implements OnQueryTextListener {
        
    	private static final int LOADER_LISTS = 0;
    	private final static String TAG = "ListsFragment";
    	
        protected int cur_list_position_ = 0;
        private LoaderMgr lm_;
        protected SimpleCursorAdapter adapter_; 
        protected String search_string_;
        
        private class LoaderMgr implements LoaderManager.LoaderCallbacks<Cursor> {

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				
				Log.d(TAG, "LoaderMgr: onCreateLoader called for loader id " + id);
				
				Uri uri = GtdContentProvider.ListsDef.CONTENT_URI;
				
				if (LOADER_LISTS == id) {
					String[] filter_args = null;
					String filter = null;
					if (null != search_string_) {
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
        
        
        @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {        	
            inflater.inflate(R.menu.lists, menu);
            
            // Add search
            SearchView sv = new SearchView(getActivity());
            sv.setOnQueryTextListener(this);
    		menu.findItem(R.id.search_lists).setActionView(sv);                        
        }
        
        @Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			// TODO Auto-generated method stub
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
        	case R.id.import_task_list:
        		ImportTaskList();
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        	}
		}
		
		private void ImportTaskList() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.import_confirmation);
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   
		        	   Import importer = new ImportFromTaskList(getActivity());
		        	   ProgressDialog wait_dialog = new ProgressDialog(getActivity());
		        	   try {
		        		   wait_dialog.setMessage(getActivity().getText(R.string.importing));
		        		   wait_dialog.setIndeterminate(true);
		        		   wait_dialog.setCancelable(false);
		        		   wait_dialog.show();
		        		   
		        		   // Reset the database
		        		   ContentResolver resolver = getActivity().getContentResolver();
			        	   Uri uri = GtdContentProvider.ResetDatabaseHelperDef.CONTENT_URI;
			        	   resolver.delete(uri, null, null);
			        	   
			        	   importer.Import();
			        	   
		        	   } catch(Exception ex) {
		        		   Log.e(TAG, "Caught exeption during import: " + ex.getMessage());
		        		   Toast.makeText(getActivity(), "Import failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		        	   } finally {
		        		   wait_dialog.dismiss();
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
        		intent.putExtra("id", id);
        	}
        	intent.putExtra("title", label);
        	intent.setClass(getActivity(), Actions.class);
        	startActivity(intent);
        }
    }    
}


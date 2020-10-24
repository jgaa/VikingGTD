package eu.lastviking.app.vgtd;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class MainView extends Activity {
	
	private static final String TAG = "MainView";
	private static Activity self_;
	private ProgressDialog progress_dialog_ = null;
	private Handler handler_;
	Intent request_restore_intent;
		
	public MainView() {
		super();
		self_ = this;
	}
	
	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
	    private Fragment fragment_;
	    private final Activity activity_;
	    private final String tag_;
	    private final Class<T> class_;

	    /** Constructor used each time a new tab is created.
	      * @param activity  The host Activity, used to instantiate the fragment
	      * @param tag  The identifier tag for the fragment
	      * @param clz  The fragment's Class, used to instantiate the fragment
	      */
	    public TabListener(Activity activity, String tag, Class<T> clz) {
	        activity_ = activity;
	        tag_ = tag;
	        class_ = clz;
	    }

	    public void onTabSelected(Tab tab, FragmentTransaction ft) {	        
	        if ((null == fragment_) && ((fragment_ = self_.getFragmentManager().findFragmentByTag(tag_)) == null)){
	            fragment_ = Fragment.instantiate(activity_, class_.getName());
	            ft.add(R.id.main_view_content, fragment_, tag_);
	            Log.d(TAG, "Adding a new fragment list for tab " + tag_);
	        } else {
	            ft.attach(fragment_);
	            Log.d(TAG, "Recycling a fragment list for tab " + tag_);
	        }
	    }

	    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	        if (null != fragment_) {
	            ft.detach(fragment_);
	        }
	    }

	    public void onTabReselected(Tab tab, FragmentTransaction ft) {
	        ;
	    }
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		request_restore_intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		request_restore_intent.setType("*/*");

		// setup action bar for tabs
		ActionBar action_bar = getActionBar();
		action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		action_bar.setDisplayShowTitleEnabled(false);

		Tab tab = action_bar.newTab()
				.setText(R.string.lists)
				.setIcon(android.R.drawable.ic_menu_sort_by_size)
				.setTabListener(new TabListener<ListsFragment>(
						this, "lists", ListsFragment.class));
		action_bar.addTab(tab);

		tab = action_bar.newTab()
				.setText(R.string.today)
				.setIcon(android.R.drawable.ic_menu_today)
				.setTabListener(new TabListener<TodayFragment>(
						this, "today", TodayFragment.class));
		action_bar.addTab(tab);

		tab = action_bar.newTab()
				.setText(R.string.pick)
				.setIcon(android.R.drawable.ic_menu_view)
				.setTabListener(new TabListener<PickFragment>(
						this, "pick", PickFragment.class));
		action_bar.addTab(tab);
		action_bar.setTitle(R.string.app_name);
		
		handler_ = new Handler();
		progress_dialog_ = new ProgressDialog(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
    	switch(item.getItemId()) {
    	case R.id.import_task_list:
    		ImportTaskList();
    		return true;
    	case R.id.define_locations:
    		Intent intent = new Intent();
            intent.setClass(this, LocationsActivity.class);
            startActivity(intent);
    		return true;
    	case R.id.backup_to_sdcard:
    		Backup();
    		return true;
    	case R.id.restore_from_sdcard:
    		Restore();
    		return true; 
    	case R.id.export_backup:
    		ExportBackup();
    		return true;
		case R.id.import_backup:
			ImportBackup();
			return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
	}

	private void ImportBackup() {
		startActivityForResult(request_restore_intent, 0);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
								 Intent returnIntent) {
		// If the selection didn't work
		if (resultCode != RESULT_OK) {
			// Exit without doing anything else
			return;
		} else {
			// Get the file's content URI from the incoming Intent
			Uri returnUri = returnIntent.getData();
			try {
				ParcelFileDescriptor pathfd = getContentResolver().openFileDescriptor(returnUri, "r");
				FileDescriptor fd = pathfd.getFileDescriptor();
				Restore(fd);
			} catch (Exception e) {
				Log.e("MainActivity", "Import failed: "+ e.getMessage());
				return;
			}
		}
	}

	private void ExportBackup() {
		try {
			Uri uri = FileProvider.getUriForFile(getContext(), "eu.lastviking.app.vgtd.fileprovider", getBackupPath());
			Intent intent = ShareCompat.IntentBuilder.from(this)
					.setType("text/xml")
					.setStream(uri)
					.setChooserTitle("Choose bar")
					.createChooserIntent()
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

			startActivity(intent);
		} catch (Exception e) {
			Log.e("ExportBackup",
					"The file " + getBackupPath() + " cannot be shared: " + e.getMessage());
		}
	}

	private Context getContext() {
		return this;
	}

	public File getBackupPath() {
		final File path = getExternalFilesDir(null);
		final File imagesDir = new File(path, "backups");
		if (!imagesDir.isDirectory()) {
			imagesDir.mkdir();
		}
		return new File(imagesDir.getPath() + "/vGTD-Backup.xml");
	}

	private void Backup() {

		progress_dialog_.setMessage(getText(R.string.backing_up));
		progress_dialog_.setIndeterminate(true);
		progress_dialog_.setCancelable(false);
		progress_dialog_.show();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				final XmlBackupRestore backup = new XmlBackupRestore();
				try {
					backup.Backup(getContext(), getBackupPath());
					
					handler_.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getContext(), R.string.backup_done, Toast.LENGTH_SHORT).show();
						}
					});
				}  catch(final Exception ex) {
					Log.e(TAG, "Caught exeption during backup: " + ex.getMessage());

					handler_.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getContext(), "Backup failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
					
				} finally {
					handler_.post(new Runnable() {
						@Override
						public void run() {
							if (progress_dialog_.isShowing()) {
								progress_dialog_.dismiss();
							}
						}
					});	
				}
			};

		};

		new Thread(runnable).start();
	}
	
	private void Restore() {
		final File path = getBackupPath();
		if (!path.canRead()) {
			Toast.makeText(this, R.string.no_file_to_restore, Toast.LENGTH_LONG).show();
			return;
		}

		try {
			FileInputStream is = new FileInputStream(path);
			Restore(is.getFD());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void Restore(final FileDescriptor fd) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.restore);
		builder.setMessage(R.string.restore_confirmation);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				progress_dialog_.setMessage(getText(R.string.restoring));
				progress_dialog_.setIndeterminate(true);
				progress_dialog_.setCancelable(false);
				progress_dialog_.show();

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						final XmlBackupRestore restore = new XmlBackupRestore();
						//final File path = restore.GetDefaultPath();
						try {
							// Reset the database
							ContentResolver resolver = getContentResolver();
							Uri uri = GtdContentProvider.ResetDatabaseHelperDef.CONTENT_URI;
							resolver.delete(uri, null, null);
							resolver.delete(GtdContentProvider.LocationsDef.CONTENT_URI, null, null);

							restore.Restore(getContext(), fd);

							handler_.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getContext(), R.string.restore_done, Toast.LENGTH_SHORT).show();
								}
							});
						}  catch(final Exception ex) {
							Log.e(TAG, "Caught exeption during restore: " + ex.getMessage());

							handler_.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getContext(), "Restore failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
								}
							});

						} finally {
							handler_.post(new Runnable() {
								@Override
								public void run() {
									if (progress_dialog_.isShowing()) {
										progress_dialog_.dismiss();
									}
								}
							});	
						}
					};

				};

				new Thread(runnable).start();
			}
		});

		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
	}
	
	
	private void ImportTaskList() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.import_data);
		builder.setMessage(R.string.import_confirmation);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				progress_dialog_.setMessage(getText(R.string.importing));
				progress_dialog_.setIndeterminate(true);
				progress_dialog_.setCancelable(false);
				progress_dialog_.show();

				Runnable runnable = new Runnable() {

					@Override
					public void run() {

						try {
							// Reset the database
							ContentResolver resolver = getContentResolver();
							Uri uri = GtdContentProvider.ResetDatabaseHelperDef.CONTENT_URI;
							resolver.delete(uri, null, null);

							// Import
							Import importer = new ImportFromTaskList(getContext());
							importer.Import();

						} catch(final Exception ex) {
							Log.e(TAG, "Caught exeption during import: " + ex.getMessage());

							handler_.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getContext(), "Import failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
								}
							});

						} finally {
							handler_.post(new Runnable() {
								@Override
								public void run() {
									if (progress_dialog_.isShowing()) {
										progress_dialog_.dismiss();
									}
								}
							});	
						}
					}
				};
				new Thread(runnable).start();
			}
		});

		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		try {
			final int current_tab = savedInstanceState.getInt("current_tab");
			ActionBar ab = getActionBar();
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			ab.setSelectedNavigationItem(current_tab);
		} catch(Exception ex) {
			;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		final int current_tab = getActionBar().getSelectedTab().getPosition();
		outState.putInt("current_tab", current_tab);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}
}

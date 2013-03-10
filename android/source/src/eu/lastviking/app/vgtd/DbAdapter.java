package eu.lastviking.app.vgtd;

import java.util.ArrayList;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/* Since we are using a relatively simple database.
 * we wrap all the DB stuff up in one class.
 */

public class DbAdapter {

	protected static final String TAG = "DbAdapter"; 
	static final String DATABASE_NAME = "vGTD";
	static final int DATABASE_VERSION = 1;
	
	public static String LISTS_TABLE = "lists";
	public static String ACTIONS_TABLE = "actions";
	public static String LOCATIONS_TABLE = "locations";
	public static String ACTIONS2LOCATION_TABLE = "action2location";
	
	public static String SELECTED_LOCATIONS_VIEW = "selected_locations";
	
	//Context context_;
	protected static GtdContentProvider cp_;
	
	private DbHelper h_;
	private SQLiteDatabase db_;
	
	private class DbHelper extends SQLiteOpenHelper
	{
		
		private DbHelper(Context ctx)
		{
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		
		public void DeleteMostData() {
			db_.execSQL("DELETE FROM todos");
			db_.execSQL("DELETE FROM action2location");
			db_.execSQL("DELETE FROM actions");
			db_.execSQL("DELETE FROM lists");
		}
		
		public void DeleteAllData() {
			DeleteMostData();
			db_.execSQL("DELETE FROM list_categories");
			db_.execSQL("DELETE FROM locations");
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String script;
			
			Log.i(LastVikingGTD.LOG_TAG, "Creating database");
			
			try {
				
				script = cp_.ReadAssetFile("vGTD.sql");
				//script = LastVikingGTD.GetInstance().ReadAssetFile("vGTD.sql");
			} catch (Exception e) {
				Log.e(LastVikingGTD.LOG_TAG, "Failed to read database definition");
				e.printStackTrace();
				return;
			}
			
			String[] queries = script.split(";");
			String q = "***";
			
			try {
				// Drop the views and tables in order so we don't conflict with database constraints
				db.execSQL("DROP VIEW  IF EXISTS selected_locations");
				db.execSQL("DROP TABLE IF EXISTS todos");
				db.execSQL("DROP TABLE IF EXISTS action2location");
				db.execSQL("DROP TABLE IF EXISTS actions");
				db.execSQL("DROP TABLE IF EXISTS lists");
				db.execSQL("DROP TABLE IF EXISTS list_categories");
				db.execSQL("DROP TABLE IF EXISTS locations");
				
				for(String query : queries){
					if (null != query) {
						query = query.trim();
						q = query;
						if (query.length() > 0) {
							Log.d("SQL query", query);
							db.execSQL(query);
						}
					}
			    }
			} catch (SQLException e) {
				Log.e(LastVikingGTD.LOG_TAG, "Failed to create database!");
				Log.e(LastVikingGTD.LOG_TAG, "This SQL astatement failed: '" + q + "'");
				e.printStackTrace();
				LastVikingGTD.GetInstance().TerminateWithError("Failed to create database");
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
			// TODO Backup and restore the database!
			
			Log.w(LastVikingGTD.LOG_TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			onCreate(db);
		}
	}
	
	
	DbAdapter(GtdContentProvider cp) 
	{
		cp_ = cp;
		//context_ = ctx;
		h_ = new DbHelper(cp.getContext());
	}

	Boolean Open()
	{
		Log.d(LastVikingGTD.LOG_TAG, "Opening database");
		
		try {
			db_ = h_.getWritableDatabase();
		} catch(Exception e) {
			Log.e(LastVikingGTD.LOG_TAG, "Failed to open database!");
			e.printStackTrace();
		}
		return (null != db_) && !db_.isReadOnly();
	}
	
	void Close()
	{
		Log.d(LastVikingGTD.LOG_TAG, "Closing database");
		h_.close();
		db_ = null;
	}
	
	ArrayList<CategoryFilter.Data> GetListCategories() {
		
		ArrayList<CategoryFilter.Data> r = new ArrayList<CategoryFilter.Data>();
			
		final String[] rownames = new String[] { "_id", "name" };
			
		Cursor c = db_.query("list_categories", rownames, null, null, null, null, "name", null);
		if (null != c) {
			c.moveToFirst();
			while (c.isAfterLast() == false) 
			{
				CategoryFilter.Data d = new CategoryFilter.Data(c.getInt(0), c.getString(1));
				r.add(d);
			    c.moveToNext();
			}
		}
		
		return r;
	}
	
	SQLiteDatabase GetDb() {
		return db_;
	}
	
	// Reset the database. Delete most data (all lists and actions and dependent data).
	public void ResetDatabase() {
		h_.DeleteMostData();
	}
}



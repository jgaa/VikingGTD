package eu.lastviking.app.vgtd;

import java.io.IOException;
import java.io.InputStream;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class GtdContentProvider extends ContentProvider {

	// public constants for client development
	public static final String AUTHORITY = "eu.lastviking.app.vgtd.cp";
	//public static final Uri LISTS_URI = Uri.parse("content://" + AUTHORITY + "/" + ListsDef.CONTENT_PATH);
	
	private static interface UriType
	{
		public static final int LISTS_LIST = 1;
		public static final int LIST_ID = 2;
		public static final int ACTIONS_LIST = 3;
		public static final int ACTION_ID = 4;
		public static final int LOCATIONS_LIST = 5;
		public static final int LOCATION_ID = 6;
		public static final int SELECTED_LOCATIONS_ID = 7;
		public static final int ACTIONS2LOCATIONS_LIST = 8;
		public static final int RESET_DATABASE = 9;
		public static final int ACTIONS_WITH_LOCATIONS_LIST = 10;
	}
	
	private DbAdapter db_;
	static protected final String TAG = "GtdContentProvider";
	private static final UriMatcher uri_matcher_;

	
	public static interface ListsDef extends BaseColumns {

		public static final String CONTENT_PATH = "lists";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);;
		
		public static final String NAME = "name";
		public static final String DESCR = "descr";
		public static final String CATEGORY = "category";
		public static final String COLOR = "color";
		
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking." + CONTENT_PATH;
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.lastviking." + CONTENT_PATH;
		
		public enum Fields { _ID, NAME, DESCR, CATEGORY, COLOR };
		public static final String[] PROJECTION_ALL = { _ID, NAME, DESCR, CATEGORY, COLOR };
		public static final String SORT_ORDER_DEFAULT = NAME + " ASC";
	}
	
	public static interface ActionsDef extends BaseColumns {

		public static final String CONTENT_PATH = "actions";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);
		
		public static final String LIST_ID = "list_id";
		public static final String PRIORITY = "priority";
		public static final String NAME = "name";
		public static final String DESCR = "descr";
		public static final String CREATED_DATE = "created_date";
		public static final String DUE_TYPE = "due_type";
		public static final String DUE_BY_TIME = "due_by_time";
		public static final String COMPLETED_TIME = "completed_time";
		public static final String COMPLETED = "completed";
		public static final String TIME_ESTIMATE = "time_estimate";
		public static final String FOCUS_NEEDED = "focus_needed";
		public static final String RELATED_TO = "related_to";
		
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking."+ CONTENT_PATH;
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.lastviking." + CONTENT_PATH;
		
		enum Fields { _ID, LIST_ID, PRIORITY, NAME, DESCR, CREATED_DATE, DUE_TYPE, DUE_BY_TIME, COMPLETED_TIME, COMPLETED, 
			TIME_ESTIMATE, FOCUS_NEEDED, RELATED_TO };
		
		public static final String[] PROJECTION_ALL = {_ID, LIST_ID, PRIORITY, NAME, DESCR, CREATED_DATE, DUE_TYPE,
			DUE_BY_TIME, COMPLETED_TIME, COMPLETED, TIME_ESTIMATE, FOCUS_NEEDED, RELATED_TO };
						
		public static final String SORT_ORDER_DEFAULT = COMPLETED + " ASC " + PRIORITY + " ASC " + NAME + " ASC";
	}
	
	public static interface ActionsWithLocDef extends ActionsDef {
		public static final String CONTENT_PATH = "actionswloc";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking."+ CONTENT_PATH;
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.lastviking." + CONTENT_PATH;
		
		public static final String LOCATION_ID = "location_id";
		
		/*public static final String[] PROJECTION_ALL = {
			DbAdapter.ACTIONS_TABLE + "." + _ID, 
			DbAdapter.ACTIONS_TABLE + "." + LIST_ID, 
			DbAdapter.ACTIONS_TABLE + "." + PRIORITY, 
			DbAdapter.ACTIONS_TABLE + "." + NAME, 
			DbAdapter.ACTIONS_TABLE + "." + DESCR, 
			DbAdapter.ACTIONS_TABLE + "." + CREATED_DATE, 
			DbAdapter.ACTIONS_TABLE + "." + DUE_TYPE,
			DbAdapter.ACTIONS_TABLE + "." + DUE_BY_TIME, 
			DbAdapter.ACTIONS_TABLE + "." + COMPLETED_TIME, 
			DbAdapter.ACTIONS_TABLE + "." + COMPLETED, 
			DbAdapter.ACTIONS_TABLE + "." + TIME_ESTIMATE, 
			DbAdapter.ACTIONS_TABLE + "." + FOCUS_NEEDED, 
			DbAdapter.ACTIONS_TABLE + "." + RELATED_TO,
			DbAdapter.LOCATIONS_TABLE + "." + LOCATION_ID 
			};*/
	}
	
	public static interface LocationsDef extends BaseColumns {

		public static final String CONTENT_PATH = "locations";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);
		
		public static final String NAME = "name";
		
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking."+ CONTENT_PATH;
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.lastviking." + CONTENT_PATH;
		
		enum Fields { _ID, NAME };
		
		public static final String[] PROJECTION_ALL = {_ID, NAME };
	}
	
	// Calls view. Read-only and no single item access
	public static interface SelectedLocationsDef extends BaseColumns {

		public static final String CONTENT_PATH = "selected_locations";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);
		
		public static final String NAME = "name";
		public static final String SELECTED = "action_id";
		
		
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking."+ CONTENT_PATH;
		
		enum Fields { _ID, NAME, SELECTED };
		
		public static final String[] PROJECTION_ALL = {_ID, NAME, SELECTED };
	}
	
	public static interface Actions2LocationsDef extends BaseColumns {

		public static final String CONTENT_PATH = "actions2locations";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);
		
		public static final String ACTION_ID = "action_id";
		public static final String LOCATION_ID = "location_id";
		
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking."+ CONTENT_PATH;
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.lastviking." + CONTENT_PATH;
		
		enum Fields { ACTION_ID, LOCATION_ID };
		
		public static final String[] PROJECTION_ALL = {ACTION_ID, LOCATION_ID };
	}
	
	public static interface ResetDatabaseHelperDef extends BaseColumns {
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.lastviking.reset_db";
		public static final String CONTENT_PATH = "reset_db";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);
	}
	
	private interface Helper 
	{
		int delete(Uri uri, String selection, String[] selectionArgs);
		Uri insert(Uri uri, ContentValues values);
		Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);
		int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);
		String GetType();	
		String GetDefaultSortOrder();
	}
	
	private abstract class ItemsHelper implements Helper {
		
		protected String table_;
		protected boolean distinct_ = false;
		
		ItemsHelper(final String tableName) {
			table_ = tableName;
		}
		

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			return delete(uri, selection, selectionArgs, 0);
		}

		@Override
		public Uri insert(Uri uri, ContentValues values) {
			final long id = db_.GetDb().insertOrThrow(table_, null, values); 
			if (id > 0) { 
				// notify all listeners of changes and return itemUri: 
				Uri itemUri = ContentUris.withAppendedId(uri, id); 
				getContext().getContentResolver().notifyChange(itemUri, null); 
				return itemUri; 
			} 
			// s.th. went wrong:
			final String msg = "Problem while inserting into " + table_ + ", uri: " + uri;
			// Log.e(TAG, msg);
			
			throw new SQLException(msg); // use another exception here!!!
		}

		@Override
		public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
			return this.query(uri, projection, selection, selectionArgs, sortOrder, 0);
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {
			
			return this.update(uri, values, selection, selectionArgs, 0);
		}
		
		protected int delete(Uri uri, String selection, String[] selectionArgs, long id) {
			
			String where = selection;
			if (0 != id) {
				where = "_id = " + String.valueOf(id);
				if (!TextUtils.isEmpty(selection)) {
					where += " AND " + selection;
					
				}
			}
			
			final int count = db_.GetDb().delete(table_, where, selectionArgs); 
			
			if (count > 0) {
				getContext().getContentResolver().notifyChange(uri, null); 
			}
			
			return count;
			
		}
		
		protected Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, long id) {
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder(); 
			builder.setTables(table_); 
			if (TextUtils.isEmpty(sortOrder)) { 
				sortOrder = GetDefaultSortOrder();
			}
			if (0 != id) {
				builder.appendWhere(ListsDef._ID + "=" + String.valueOf(id));
			}
			
			builder.setDistinct(distinct_);
			
			//// Log.d(TAG, "Queryin '" + table_ + "' :" + (null == selectionArgs ? "" : selectionArgs.toString()));
			
			Cursor cursor = builder.query(db_.GetDb(), projection, selection, selectionArgs, null, null, sortOrder); 
			   cursor.setNotificationUri(getContext().getContentResolver(), uri); 
			   return cursor; 
		}
		
		protected int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs, long id) {
			
			String where = selection;
			if (0 != id) {
				where = "_id = " + String.valueOf(id);
				if (!TextUtils.isEmpty(selection)) {
					where += " AND " + selection;
					
				}
			}
			
			final int count = db_.GetDb().update(table_, values, where, selectionArgs); 
			
			if (count > 0) {
				getContext().getContentResolver().notifyChange(uri, null); 
			}
			
			return count;
		}

	}
	
	
	private abstract class ItemHelper extends ItemsHelper {
		
		ItemHelper (final String tableName) {
			super(tableName);
		}
		

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			return delete(uri, selection, selectionArgs, GetId(uri));
		}

		@Override
		public Uri insert(Uri uri, ContentValues values) {
			throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
		}

		@Override
		public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
			return this.query(uri, projection, selection, selectionArgs, sortOrder, GetId(uri));
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {
			return this.update(uri, values, selection, selectionArgs, GetId(uri));
		}
		
	}
	
	private final class ListsHelper extends ItemsHelper{
		
		ListsHelper() { super(DbAdapter.LISTS_TABLE); }
		
		@Override
		public String GetType() {
			
			return ListsDef.CONTENT_TYPE;
		}

		@Override
		public String GetDefaultSortOrder() {
			return ListsDef.NAME + " ASC";
		}
		
	}
	
	private final class ListHelper extends ItemHelper{
		
		ListHelper() { super(DbAdapter.LISTS_TABLE); }
		
		@Override
		public String GetType() {
			
			return ListsDef.CONTENT_ITEM_TYPE;
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
	}
	
	private final class ActionsHelper extends ItemsHelper{
		
		ActionsHelper() { super(DbAdapter.ACTIONS_TABLE); }
		
		@Override
		public String GetType() {
			
			return ActionsDef.CONTENT_TYPE;
		}

		@Override
		public String GetDefaultSortOrder() {
			return ActionsDef.COMPLETED_TIME + " ASC, " 
					+ ActionsDef.PRIORITY + " ASC, "
					+ ActionsDef.DUE_BY_TIME + " ASC, "
					+ ActionsDef.CREATED_DATE + " ASC";
		}
	}
	
	private final class ActionsWithLocHelper extends ItemsHelper{
		
		ActionsWithLocHelper() {
			super(DbAdapter.ACTIONS_TABLE + " LEFT OUTER JOIN " + DbAdapter.ACTIONS2LOCATION_TABLE 
					+ " ON (action_id = _id)");
			
			distinct_ = true;
		}

		@Override
		public String GetType() {
			
			return ActionsWithLocDef.CONTENT_TYPE;
		}
		
		@Override
		public Uri insert(Uri uri, ContentValues values) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		protected int delete(Uri uri, String selection, String[] selectionArgs,
				long id) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		protected int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs, long id) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
		
		
	}
	
	private final class ActionHelper extends ItemHelper{
		
		ActionHelper() { super(DbAdapter.ACTIONS_TABLE); }
		
		@Override
		public String GetType() {
			
			return ActionsDef.CONTENT_ITEM_TYPE;
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
	}
	
	private final class LocationsHelper extends ItemsHelper{
		
		LocationsHelper() { super(DbAdapter.LOCATIONS_TABLE); }
		
		@Override
		public String GetType() {
			
			return LocationsDef.CONTENT_TYPE;
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
		
	}
	
	private final class LocationHelper extends ItemHelper{
		
		LocationHelper() { super(DbAdapter.LOCATIONS_TABLE); }
		
		@Override
		public String GetType() {
			
			return LocationsDef.CONTENT_ITEM_TYPE;
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
	}
	
	private final class SelectedLocationsHelper extends ItemHelper{
		
		SelectedLocationsHelper() { super(""); }
		
		@Override
		public String GetType() {
			
			return SelectedLocationsDef.CONTENT_TYPE;
		}
		
		@Override
		public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
			final long action_id = GetIdOrZero(uri);
			
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder(); 
			builder.setTables(DbAdapter.LOCATIONS_TABLE + " LEFT OUTER JOIN " 
					+ DbAdapter.ACTIONS2LOCATION_TABLE
					+ " ON (location_id = _id  and "
					+ ((0 == action_id) 
							? "action_id is null)" 
							: "(action_id is null or action_id = "+ action_id + "))")
					);
			
			if (TextUtils.isEmpty(sortOrder)) { 
				sortOrder = LocationsDef.NAME + " ASC";
			}			
			
			return builder.query(db_.GetDb(), projection, selection, selectionArgs, null, null, sortOrder); 
		}

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			throw new UnsupportedOperationException("delete from read-only view");
		}

		@Override
		public Uri insert(Uri uri, ContentValues values) {
			throw new UnsupportedOperationException("insert into read-only view");
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {
			throw new UnsupportedOperationException("update read-only view");
		}

		@Override
		protected int delete(Uri uri, String selection, String[] selectionArgs,
				long id) {
			throw new UnsupportedOperationException("delete from read-only view");
		}

		@Override
		protected int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs, long id) {
			throw new UnsupportedOperationException("update read-only view");
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
		
	}
	
	private final class Actions2LocationsHelper extends ItemsHelper{
		
		Actions2LocationsHelper() { super(DbAdapter.ACTIONS2LOCATION_TABLE); }
		
		@Override
		public String GetType() {
			
			return Actions2LocationsDef.CONTENT_TYPE;
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {
			throw new UnsupportedOperationException("update of x-reference-table");
		}

		@Override
		protected int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs, long id) {
			throw new UnsupportedOperationException("update of x-reference-table");
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
		
	}
	
	private final class ResetDatabaseHelper extends ItemsHelper {
		
		ResetDatabaseHelper() { super(""); }
		
		@Override
		public String GetType() {
			return ResetDatabaseHelperDef.CONTENT_TYPE;
		}

		@Override
		public Uri insert(Uri uri, ContentValues values) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		protected int delete(Uri uri, String selection, String[] selectionArgs,
				long id) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		protected Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder, long id) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		protected int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs, long id) {
			throw new UnsupportedOperationException("Unsupported operation");
		}

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			// Log.w(TAG, "Resetting database. Most data will be lost");
			
			db_.ResetDatabase();
			
			return 1;
		}

		@Override
		public String GetDefaultSortOrder() {
			return null;
		}
	}
	
		
	// Index matches UriType values
	private Helper[] helper_; 
	
	static {
		uri_matcher_ = new UriMatcher(UriMatcher.NO_MATCH);
		uri_matcher_.addURI(AUTHORITY, ListsDef.CONTENT_PATH, UriType.LISTS_LIST);
		uri_matcher_.addURI(AUTHORITY, ListsDef.CONTENT_PATH + "/#", UriType.LIST_ID);
		uri_matcher_.addURI(AUTHORITY, ActionsDef.CONTENT_PATH, UriType.ACTIONS_LIST);
		uri_matcher_.addURI(AUTHORITY, ActionsDef.CONTENT_PATH + "/#", UriType.ACTION_ID);
		uri_matcher_.addURI(AUTHORITY, LocationsDef.CONTENT_PATH, UriType.LOCATIONS_LIST);
		uri_matcher_.addURI(AUTHORITY, LocationsDef.CONTENT_PATH + "/#", UriType.LOCATION_ID);
		uri_matcher_.addURI(AUTHORITY, SelectedLocationsDef.CONTENT_PATH + "/#", UriType.SELECTED_LOCATIONS_ID);
		uri_matcher_.addURI(AUTHORITY, Actions2LocationsDef.CONTENT_PATH, UriType.ACTIONS2LOCATIONS_LIST);
		uri_matcher_.addURI(AUTHORITY, ResetDatabaseHelperDef.CONTENT_PATH, UriType.RESET_DATABASE);
		uri_matcher_.addURI(AUTHORITY, ActionsWithLocDef.CONTENT_PATH, UriType.ACTIONS_WITH_LOCATIONS_LIST);
	}
	
	@Override
	public boolean onCreate() {
		
		// Log.d(TAG, "GTD Content provider created.");
		
		db_ = new DbAdapter(this);
        if (!db_.Open()) {
        	// Log.e(TAG, "Failed to open the database or the database was not writeable");
        	db_.Close();
        	return false;
        }
        
        helper_ = new Helper[] { null, 
        		new ListsHelper(), new ListHelper(), 
        		new ActionsHelper(), new ActionHelper(),
        		new LocationsHelper(), new LocationHelper(),
        		new SelectedLocationsHelper(),
        		new Actions2LocationsHelper(),
        		new ResetDatabaseHelper(),
        		new ActionsWithLocHelper()
        		};
        return true;
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		
		// Log.d(TAG, "Shutting down the GTD content propvider");
		db_.Close();
		db_ = null;
	}

	@Override
	public String getType(Uri uri) {
		return GetHelper(uri).GetType();
	}
	
	public Helper GetHelper(Uri uri) {
		final int ix = uri_matcher_.match(uri);
		if (1 > ix) {
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		return helper_[ix]; 
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		//// Log.d(TAG, "Deleting data with uri: " + uri + ", selection: " + selection);
		return GetHelper(uri).delete(uri, selection, selectionArgs);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//// Log.d(TAG, "Inserting data with uri: " + uri + ", values: " + values.toString());
		return GetHelper(uri).insert(uri, values);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return GetHelper(uri).query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		//// Log.d(TAG, "Updating data with uri: " + uri + ", values: " + values.toString());
		return GetHelper(uri).update(uri, values, selection, selectionArgs);
	}
	
	protected long GetId(final Uri uri) {
		final int id = Integer.parseInt(uri.getLastPathSegment());
		if (0 == id) {
			throw new IllegalArgumentException("Invalid id in uri: " + uri);
		}
		return id;
	}
	
	protected long GetIdOrZero(final Uri uri) {
		final int id = Integer.parseInt(uri.getLastPathSegment());
		return id;
	}
	
	public String ReadAssetFile(final String path) throws IOException
	{
		// Log.d(TAG, "Reading asset file '" + path + "'");
		InputStream stream = getContext().getAssets().open(path);
		int size = stream.available();
		byte[] buffer = new byte[size];
		stream.read(buffer);
		stream.close();
		return new String(buffer);
	}
}

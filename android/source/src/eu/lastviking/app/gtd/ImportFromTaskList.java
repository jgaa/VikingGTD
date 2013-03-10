package eu.lastviking.app.gtd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

public class ImportFromTaskList implements Import {

	private final static String TAG = "ImportFromTaskList";
	private Context ctx_;
	ArrayList<Long> locations_ = new ArrayList<Long>();
	
	ImportFromTaskList(Context ctx) {
		ctx_ = ctx;
		LoadLocations();
	}
	
	static File GetImportPath() {
		return new File(Environment.getExternalStorageDirectory(), "/TaskList/tasklist.xml");
	}
	
	public static boolean CanImport() {
		final File path = GetImportPath();
		return path.canRead();
	}
		
	@Override
	public String GetName() {
		return "TaskList";
	}

	@Override
	public void Import() {
		File path = GetImportPath(); 
		
		FileInputStream is = null;
		try {
			is = new FileInputStream(path);
			
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new InputStreamReader(is));
			parser.nextTag();
			DoImport(parser);
			
		} catch (Exception ex) { 
			Log.e(TAG, "Caught exception: " + ex.getMessage());
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	private void DoImport(XmlPullParser parser) throws XmlPullParserException, IOException {
		
		parser.require(XmlPullParser.START_TAG, "", "TaskList");
		
		while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        
	        String name = parser.getName();
	        if (name.equals("Category")) {
	        	AddCategory(parser);
	        } else {
	        	Skip(parser);
	        }
		}
	}
	
	private void AddTask(XmlPullParser parser, final long listId, ContentResolver resolver) 
			throws XmlPullParserException, IOException {
		
		ContentValues values = new ContentValues();

		int priority = 4; // normal
		{
			final int imported_priority = Integer.valueOf(parser.getAttributeValue(null, "Priority"));
			switch(imported_priority) {
			case 0:
			case 1:
			case 2:
				priority = 7;
				break;
			case 3:
				priority = 4;
				break;
			case 4:
			case 5:
				priority = 3;
				break;
			case 6:
			case 7:
			case 8:
				priority = 0;
				break;
			}
		}
		
		values.put(GtdContentProvider.ActionsDef.NAME, parser.getAttributeValue(null, "Caption"));
		
		values.put(GtdContentProvider.ActionsDef.DESCR, 
			parser.getAttributeValue(null, "Description").replaceAll("!br!", "\n"));
		values.put(GtdContentProvider.ActionsDef.PRIORITY, priority);
		values.put(GtdContentProvider.ActionsDef.LIST_ID, listId);
		
		final long due_time = Long.valueOf(parser.getAttributeValue(null, "DueTime"));
		if (0 != due_time) {
			values.put(GtdContentProvider.ActionsDef.DUE_BY_TIME, due_time);
			values.put(GtdContentProvider.ActionsDef.DUE_TYPE, When.DueTypes.DATE.ordinal());
		}
		
		if (parser.getAttributeValue(null, "Completed").equals("true")) {
			values.put(GtdContentProvider.ActionsDef.COMPLETED, 1);
			values.put(GtdContentProvider.ActionsDef.COMPLETED_TIME, Calendar.getInstance().getTimeInMillis());
		}
		
		final Uri uri = resolver.insert(GtdContentProvider.ActionsDef.CONTENT_URI, values);
		final long action_id = ContentUris.parseId(uri);
		
		// Select all locations
		for(int i = 0; i < locations_.size(); i++) {
			ContentValues v = new ContentValues();
			v.put(GtdContentProvider.Actions2LocationsDef.LOCATION_ID, locations_.get(i));
			v.put(GtdContentProvider.Actions2LocationsDef.ACTION_ID, action_id);
			resolver.insert(GtdContentProvider.Actions2LocationsDef.CONTENT_URI, v);
		}
	}

	private void AddCategory(XmlPullParser parser) throws XmlPullParserException, IOException {
		//parser.require(XmlPullParser.START_TAG, "", "Category");

		ContentResolver resolver = ctx_.getContentResolver();
		ContentValues values = new ContentValues();

		values.put(GtdContentProvider.ListsDef.NAME, parser.getAttributeValue(null, "Name"));
		// Save and get ID
		final Uri uri = resolver.insert(GtdContentProvider.ListsDef.CONTENT_URI, values);
		final long list_id = ContentUris.parseId(uri);
		int depth = 1;

		String name;
		while (true) {
			 switch (parser.next()) {
		        case XmlPullParser.END_TAG:
		            if (0 == --depth)
		            	return;
		            break;
		        case XmlPullParser.START_TAG:
		            ++depth;
		            name = parser.getName();
		            if (name.equals("Task")) {
		            	AddTask(parser, list_id, resolver);
		            }
		            break;
			 }
		}
	}
	
	private void Skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }
	
	private void LoadLocations() {
		
		ContentResolver resolver = ctx_.getContentResolver();
		Cursor c = resolver.query(Uri.parse(GtdContentProvider.SelectedLocationsDef.CONTENT_URI + "/" + 0), 
				GtdContentProvider.SelectedLocationsDef.PROJECTION_ALL, null, null, null);
		if (c.moveToFirst()) {
			do {
				locations_.add(Long.valueOf(c.getLong(GtdContentProvider.SelectedLocationsDef.Fields._ID.ordinal())));
			} while(c.moveToNext());
		}
	}
}

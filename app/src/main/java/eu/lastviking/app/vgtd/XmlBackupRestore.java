package eu.lastviking.app.vgtd;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import eu.lastviking.app.vgtd.EditActionFragment.Location;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

public class XmlBackupRestore {

	private final static String ROOT_ELEMENT = "VikingGtdBackup";
	private final static String TAG = "XmlBackupRestore";
	
	public File GetDefaultDir() {
		return new File(Environment.getExternalStorageDirectory(), "/VikingGTD");
	}
	
	public File GetDefaultPath() {
		return new File(GetDefaultDir().getPath() + "/backup.xml");
	}
	
	public void Backup(Context ctx, final File path) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		//Date date = new Date() ;
		//SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm") ;
		//File my_path = new File(path.getPath() + "/vGTD-Backup_" + dateFormat.format(date) + ".tmp");
		File my_path = new File(path.getPath() + ".tmp");
		FileOutputStream file = new FileOutputStream(my_path);
		Log.d(TAG, "Will backup to path: " + my_path.toString());

		XmlSerializer x = Xml.newSerializer();
		ContentResolver resolver = ctx.getContentResolver();

		x.setOutput(file, "UTF-8");
		x.startDocument(null, true);

		x.startTag(null, ROOT_ELEMENT);
		x.startTag(null, "metadata");
		x.attribute(null, "version", "1");
		x.endTag(null, "metadata");

		x.startTag(null, "db");

		// Locations
		DumpTable(resolver, DbAdapter.LOCATIONS_TABLE, 
				GtdContentProvider.LocationsDef.CONTENT_URI, 
				GtdContentProvider.LocationsDef.PROJECTION_ALL,
				x);

		DumpLists(resolver, x);

		x.endTag(null,"db");
		x.endTag(null, ROOT_ELEMENT);
		x.endDocument();
		x.flush();
		file.close();
		path.renameTo(new File(path.getPath() + ".bak"));
		my_path.renameTo(path);
		my_path.setWritable(true, false);
	}
	
//	public void DownloadBackup(Context ctx, File path)
//	{
//		String url = "http://192.168.1.1/secret-path/backup.xml";
//		 try {
//		      URL u = new URL(url);
//		      URLConnection conn = u.openConnection();
//		      int contentLength = conn.getContentLength();
//
//		      DataInputStream stream = new DataInputStream(u.openStream());
//
//		        byte[] buffer = new byte[contentLength];
//		        stream.readFully(buffer);
//		        stream.close();
//
//		        DataOutputStream fos = new DataOutputStream(new FileOutputStream(path));
//		        fos.write(buffer);
//		        fos.flush();
//		        fos.close();
//		  } catch(FileNotFoundException e) {
//		      return; // swallow a 404
//		  } catch (IOException e) {
//		      return; // swallow a 404
//		  }
//	}
	
	private void DumpLists(ContentResolver resolver, XmlSerializer x) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		
		x.startTag(null, "lists");
		
		Cursor c = resolver.query(GtdContentProvider.ListsDef.CONTENT_URI, 
				GtdContentProvider.ListsDef.PROJECTION_ALL, null, null, null);
		
		if (null != c) {
			c.moveToFirst();
			while(!c.isAfterLast()) {
				x.startTag(null, "list");
				x.attribute(null, "id", c.getString(0));
				SafeAttibute(GtdContentProvider.ListsDef.NAME, GtdContentProvider.ListsDef.Fields.NAME.ordinal(), c, x);
				SafeText(GtdContentProvider.ListsDef.DESCR, GtdContentProvider.ListsDef.Fields.DESCR.ordinal(), c, x);
							
				DumpActions(Long.valueOf(c.getString(0)), resolver, x);
				
				x.endTag(null, "list");
				c.moveToNext();
			}
		}
		
		x.endTag(null, "lists");
		c.close();
	}		
	
	private void DumpActions(final long listId, ContentResolver resolver, XmlSerializer x) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		
		x.startTag(null, "actions");
		
		Cursor c = resolver.query(GtdContentProvider.ActionsDef.CONTENT_URI, 
				GtdContentProvider.ActionsDef.PROJECTION_ALL, 
				GtdContentProvider.ActionsDef.LIST_ID + "=" + listId,
				null, null);
		
		if (null != c) {
			c.moveToFirst();
			while(!c.isAfterLast()) {
				x.startTag(null, "action");
				x.attribute(null, "id", c.getString(0));
				
				SafeAttibute(GtdContentProvider.ActionsDef.NAME, GtdContentProvider.ActionsDef.Fields.NAME.ordinal(), c, x);
				SafeAttibute(GtdContentProvider.ActionsDef.PRIORITY, GtdContentProvider.ActionsDef.Fields.PRIORITY.ordinal(), c, x);
				SafeAttibute(GtdContentProvider.ActionsDef.CREATED_DATE, GtdContentProvider.ActionsDef.Fields.CREATED_DATE.ordinal(), c, x);
				SafeAttibute(GtdContentProvider.ActionsDef.DUE_TYPE, GtdContentProvider.ActionsDef.Fields.DUE_TYPE.ordinal(), c, x);
				SafeAttibute(GtdContentProvider.ActionsDef.DUE_BY_TIME, GtdContentProvider.ActionsDef.Fields.DUE_BY_TIME.ordinal(), c, x);

				final int completed = c.getInt(GtdContentProvider.ActionsDef.Fields.COMPLETED.ordinal());
				if (0 < completed) {
					x.attribute(null, GtdContentProvider.ActionsDef.COMPLETED, "1");
					SafeAttibute(GtdContentProvider.ActionsDef.COMPLETED_TIME, GtdContentProvider.ActionsDef.Fields.COMPLETED_TIME.ordinal(), c, x);
				} else {
					x.attribute(null, GtdContentProvider.ActionsDef.COMPLETED, "0");
				}
				
				SafeAttibute(GtdContentProvider.ActionsDef.TIME_ESTIMATE, GtdContentProvider.ActionsDef.Fields.TIME_ESTIMATE.ordinal(), c, x);
				SafeAttibute(GtdContentProvider.ActionsDef.FOCUS_NEEDED, GtdContentProvider.ActionsDef.Fields.FOCUS_NEEDED.ordinal(), c, x);
				SafeAttibute(GtdContentProvider.ActionsDef.RELATED_TO, GtdContentProvider.ActionsDef.Fields.RELATED_TO.ordinal(), c, x);

                SafeAttibute(GtdContentProvider.ActionsDef.REPEAT_TYPE, GtdContentProvider.ActionsDef.Fields.REPEAT_TYPE.ordinal(), c, x);
                SafeAttibute(GtdContentProvider.ActionsDef.REPEAT_UNIT, GtdContentProvider.ActionsDef.Fields.REPEAT_UNIT.ordinal(), c, x);
                SafeAttibute(GtdContentProvider.ActionsDef.REPEAT_AFTER, GtdContentProvider.ActionsDef.Fields.REPEAT_AFTER.ordinal(), c, x);
				
				SafeText(GtdContentProvider.ActionsDef.DESCR, GtdContentProvider.ActionsDef.Fields.DESCR.ordinal(), c, x);
				
				DumpLocations(resolver, x, c.getLong(0));
				
				x.endTag(null, "action");
				c.moveToNext();
			}
		}		
		x.endTag(null, "actions");
		c.close();
	}

	private void DumpLocations(ContentResolver resolver, XmlSerializer x, final long actionId) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		
		boolean virgin = true;
		
		Cursor c = resolver.query(GtdContentProvider.Actions2LocationsDef.CONTENT_URI, 
				GtdContentProvider.Actions2LocationsDef.PROJECTION_ALL, 
				GtdContentProvider.Actions2LocationsDef.ACTION_ID + "=" + actionId, null, null);
		
		if (null != c) {
			c.moveToFirst();
			while(!c.isAfterLast()) {
				
				if (virgin) {
					virgin = false;
					x.startTag(null, "locations");
				}
				
				x.startTag(null, "location");
				x.attribute(null, "id", c.getString(GtdContentProvider.Actions2LocationsDef.Fields.LOCATION_ID.ordinal()));
				x.endTag(null, "location");
				c.moveToNext();
			}
		}
		
		if (!virgin) {
			x.endTag(null, "locations");
		}
		c.close();
	}

	public void Restore(Context ctx, final FileDescriptor is) {
		//FileInputStream is = null;
		
		try {
			ContentResolver resolver = ctx.getContentResolver();
			//is = new FileInputStream(path);

			XmlPullParser x = Xml.newPullParser();
			x.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			x.setInput(new FileReader(is));
			x.nextTag();
			
			x.require(XmlPullParser.START_TAG, "", ROOT_ELEMENT);
			
			
			while (x.next() != XmlPullParser.END_TAG) {
		        if (x.getEventType() != XmlPullParser.START_TAG) {
		            continue;
		        }
		        
		        String name = x.getName();
		        if (name.equals("db")) {
		        	RestoreDb(resolver, x);
		        } else {
		        	Skip(x);
		        }
			}
			
		} catch (Exception ex) { 
			Log.e(TAG, "Caught exception: " + ex.getMessage());
		}
	}
	
	private void RestoreDb(ContentResolver resolver, XmlPullParser x) 
			throws XmlPullParserException, IOException {
		String name;
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            return;
		        case XmlPullParser.START_TAG:
		            name = x.getName();
		            if (name.equals("locations")) {
		            	// Locations
		    			RestoreTable(resolver, DbAdapter.LOCATIONS_TABLE, 
		    					GtdContentProvider.LocationsDef.CONTENT_URI, 
		    					GtdContentProvider.LocationsDef.PROJECTION_ALL,
		    					x);
		            } else if (name.equals("lists")) {
		            	RestoreLists(resolver, x);
		            } else {
		            	Skip(x);
		            }
		            break;
			 }
		}
	}

	private void RestoreLists(ContentResolver resolver, XmlPullParser x) 
			throws IllegalArgumentException, IllegalStateException, XmlPullParserException, IOException {
		String name = "";
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            return;
		        case XmlPullParser.START_TAG:
		            name = x.getName();
		            if (name.equals("list")) {
		            	RestoreList(resolver, x);
		            } else {
		            	Skip(x);
		            }
		            break;
			 }
		}
	}

	private void RestoreList(ContentResolver resolver, XmlPullParser x) 
			throws XmlPullParserException, IOException {
		
		ContentValues values = new ContentValues();

		values.put(GtdContentProvider.ListsDef._ID, x.getAttributeValue(null, "id"));
		values.put(GtdContentProvider.ListsDef.NAME, x.getAttributeValue(null, "name"));
		// Save and get ID
		final Uri uri = resolver.insert(GtdContentProvider.ListsDef.CONTENT_URI, values);
		final long list_id = ContentUris.parseId(uri);
		
		String name = "";
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		        	return;
		        case XmlPullParser.START_TAG:
		            name = x.getName();
		            if (name.equals("actions")) {
		            	RestoreActions(list_id, resolver, x);
		            } else if (name.equals("descr")) {
		            	values.clear();
		            	values.put("descr", GetDescr(x));
		            	resolver.update(Uri.parse(GtdContentProvider.ListsDef.CONTENT_URI + "/" + list_id), values, null, null);
		            } else {
		            	Skip(x);
		            }
		            break;
			 }
		}
	}

	private String GetDescr(XmlPullParser x) 
			throws XmlPullParserException, IOException {
		
		int depth = 1;
		String value = "";
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            if (0 <= --depth)
		            	return value;
		            break;
		        case XmlPullParser.START_TAG:
		            ++depth;
		            break;
		        case XmlPullParser.TEXT:
		        	value = x.getText();
		        	if (null == value) {
		        		value = "";
		        	}
		        	break;
			 }
		}	
	}

	private void RestoreActions(long listId, ContentResolver resolver,
			XmlPullParser x) throws XmlPullParserException, IOException {
		String name = "";
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            return;
		        case XmlPullParser.START_TAG:
		            name = x.getName();
		            if (name.equals("action")) {
		            	RestoreAction(listId, resolver, x);
		            } else {
		            	Skip(x);
		            }
		            break;
			 }
		}
	}

	private void RestoreAction(long listId, ContentResolver resolver, XmlPullParser x) 
			throws XmlPullParserException, IOException {

		ContentValues values = new ContentValues();

		values.put(GtdContentProvider.ActionsDef._ID, x.getAttributeValue(null, "id"));
		values.put(GtdContentProvider.ActionsDef.LIST_ID, listId);
		values.put(GtdContentProvider.ActionsDef.NAME, x.getAttributeValue(null, "name"));
		values.put(GtdContentProvider.ActionsDef.PRIORITY, x.getAttributeValue(null, "priority"));
		values.put(GtdContentProvider.ActionsDef.CREATED_DATE, x.getAttributeValue(null, "created_date"));
		values.put(GtdContentProvider.ActionsDef.DUE_TYPE, x.getAttributeValue(null, "due_type"));
		values.put(GtdContentProvider.ActionsDef.DUE_BY_TIME, x.getAttributeValue(null, "due_by_time"));

        {
            // Repeat
            String v = x.getAttributeValue(null, GtdContentProvider.ActionsDef.REPEAT_TYPE);
            if ((v != null) && !v.isEmpty() && (!v.equals("1"))) {
                values.put(GtdContentProvider.ActionsDef.REPEAT_TYPE, v);
                values.put(GtdContentProvider.ActionsDef.REPEAT_UNIT, x.getAttributeValue(null, GtdContentProvider.ActionsDef.REPEAT_UNIT));
                values.put(GtdContentProvider.ActionsDef.REPEAT_AFTER, x.getAttributeValue(null, GtdContentProvider.ActionsDef.REPEAT_AFTER));
            }
        }

		String completed = x.getAttributeValue(null, "completed");
		values.put(GtdContentProvider.ActionsDef.COMPLETED, completed);
		if (completed.equals("1")) {
			values.put(GtdContentProvider.ActionsDef.COMPLETED_TIME, x.getAttributeValue(null, "completed_time"));
		}
		
		values.put(GtdContentProvider.ActionsDef.FOCUS_NEEDED, x.getAttributeValue(null, "focus_needed"));
		
		String name = "";
		List<Long> locations = null;
		while (true) {
			switch (x.next()) {
			case XmlPullParser.END_TAG:
				// Save
				final Uri uri = resolver.insert(GtdContentProvider.ActionsDef.CONTENT_URI, values);
				final long action_id = ContentUris.parseId(uri);

				if (null != locations) {
					for(int i = 0; i < locations.size(); i++) {

						ContentValues lv = new ContentValues();
						lv.put(GtdContentProvider.Actions2LocationsDef.LOCATION_ID, locations.get(i));
						lv.put(GtdContentProvider.Actions2LocationsDef.ACTION_ID, action_id);
						resolver.insert(GtdContentProvider.Actions2LocationsDef.CONTENT_URI, lv);
					}
				}

				return;
			case XmlPullParser.START_TAG:
				name = x.getName();
				if (name.equals("descr")) {
					values.put("descr", GetDescr(x));
				} else  if (name.equals("locations")) {
					locations = GetLocations(x);
				} else {
					Skip(x);
				}
				break;
			}
		}

	}

	private List<Long> GetLocations(XmlPullParser x) 
			throws NumberFormatException, XmlPullParserException, IOException {
		List<Long> list = new ArrayList<Long>();
		
		int depth = 1;
		String name = "";
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            if (0 == --depth)
		            	return list;
		            break;
		        case XmlPullParser.START_TAG:
		            ++depth;
		            name = x.getName();
		            if (name.equals("location")) {
		            	list.add(Long.valueOf(x.getAttributeValue(null, "id")));
		            } else {
		            	Skip(x);
		            	--depth;
		            }
		            break;
			 }
		}
	}

	public void MakeDefaultDir() {
		final File path = GetDefaultDir();
		if (!path.isDirectory()) {
			path.mkdirs();
		}
	}
	
	
	
	void DumpTable(ContentResolver resolver, String table, Uri uri, String[] projection, XmlSerializer x) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		x.startTag(null, table);
		Cursor c = resolver.query(uri, projection, null, null, null);
		if (null != c) {
			c.moveToFirst();
			int i = 0;
			while(!c.isAfterLast()) {
				x.startTag(null, "row");
				x.attribute(null, "id", c.getString(0));
				for(i = 1; i < projection.length; ++i) {
					x.startTag(null, projection[i]);
					x.text(c.getString(i));
					x.endTag(null, projection[i]);
				}
				x.endTag(null, "row");
				c.moveToNext();
			}
		}
		x.endTag(null, table);
		c.close();
	}
	
	void RestoreTable(ContentResolver resolver, String table, Uri uri, String[] projection, XmlPullParser x) 
			throws IllegalArgumentException, IllegalStateException, IOException, XmlPullParserException {
		
		int depth = 1;
		String name;
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            if (0 == --depth)
		            	return;
		            break;
		        case XmlPullParser.START_TAG:
		            ++depth;
		            name = x.getName();
		            if (name.equals("row")) {
		            	ContentValues cv = GetRowValues(x);
		            	Log.d(TAG, "Restoring " + table + ": " + cv.toString());
		            	resolver.insert(uri, cv);
		            } else {
		            	Skip(x);
		            }
		            break;
			 }
		}
	}
	
	private ContentValues GetRowValues(XmlPullParser x) 
			throws XmlPullParserException, IOException {
		ContentValues v = new ContentValues();
		
		v.put("_ID", x.getAttributeValue(null, "id"));
		int depth = 0;
		String name = "";
		while (true) {
			 switch (x.next()) {
		        case XmlPullParser.END_TAG:
		            if (0 >= --depth)
		            	return v;
		            break;
		        case XmlPullParser.START_TAG:
		            ++depth;
		            name = x.getName();
		            break;
		        case XmlPullParser.TEXT:
		        	v.put(name, x.getText());
		        	break;
			 }
		}		
	}

	private void SafeAttibute(final String name, final int idx, Cursor c, XmlSerializer x) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String v = c.getString(idx);
		if ((null != v) && v.length() > 0) {
			x.attribute(null,  name, v);
		}
	}
	
	private void SafeText(final String name, final int idx, Cursor c, XmlSerializer x) 
			throws IllegalArgumentException, IllegalStateException, IOException {
		String v = c.getString(idx);
		if ((null != v) && v.length() > 0) {
			x.startTag(null, name);
			x.text(v);
			x.endTag(null, name);
		}
	}
	
	private void Skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (true) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            if (0 >= --depth) {
	            	return;
	            }
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }	

}

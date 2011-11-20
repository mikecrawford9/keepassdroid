package edu.sjsu.cs.davsync;

import android.util.Log;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DAVDatabase {

	// davcync open helper - handles database initialization
	private class DSOH extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 2;
		private static final String DATABASE_NAME = "davsync_db";

		public DSOH(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE dav_profiles ( filename TEXT PRIMARY KEY, hostname TEXT, resource TEXT, username TEXT, password TEXT );");
		}

		@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	} // end DSOH

	private final String TAG = "davsync::DSDatabase";
	private DSOH dsoh;

	// cache the application context in order to access application assets and resources
	public DAVDatabase(Context context) {
		dsoh = new DSOH(context);
	}

	// save the server info to local storage
	public void addProfile(DAVProfile p) {
		Log.d(TAG, "saving data...");
		SQLiteDatabase db = dsoh.getWritableDatabase();
		//db.execSQL("DELETE FROM dav_profiles;"); // FIXME: delete all records until we support multiple profiles
		String q = "INSERT OR REPLACE INTO dav_profiles VALUES('"
			   + p.getFilename() + "','"
			   + p.getHostname() + "','"
			   + p.getResource() + "','"
			   + p.getUsername() + "','"
			   + p.getPassword() + "');";
		db.execSQL(q);
		db.close();
	}

	public DAVProfile getProfile(String filename) {
		Log.d(TAG, "retrieving profile...");
		// can use this to iterate over all rows in cursor: while (c.moveNext()) { ... }
		DAVProfile p;
		SQLiteDatabase db = dsoh.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM dav_profiles LIMIT 1 where filename = '" + filename + "';", null);
		if( c.getCount() == 0 ) {
			p = null;
		} else {
			c.moveToFirst();
			p = new DAVProfile(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
		}
		c.close();
		db.close();
		return p;
	}

	// clear the fields and delete local storage
	public void delProfile(DAVProfile p) {
		Log.d(TAG, "removing profile...");
		SQLiteDatabase db = dsoh.getWritableDatabase();
		// FIXME: for now, just drop all rows from dav_profiles...
		db.execSQL("DELETE FROM dav_profiles;");
		db.close();
	}

}


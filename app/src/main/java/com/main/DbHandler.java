package com.example.dsd6;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.Timestamp;
import java.util.ArrayList;


public class DbHandler extends SQLiteOpenHelper {

    public class BandActivity
    {
        public int type;
        public int flags;
        public long timestamp;
        public int value;
        public int value2;
    };

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "fitdb";
    private static final String TABLE_ACTIVITY = "activity";
    private static final String KEY_ACTIVITY_FLAGS = "flags";
    private static final String KEY_ACTIVITY_TYPE = "type";
    private static final String KEY_ACTIVITY_TIMESTAMP = "timestamp";
    private static final String KEY_ACTIVITY_VALUE = "value";
    private static final String KEY_ACTIVITY_VALUE2 = "value2";

    public DbHandler(Context context){
        super(context,DB_NAME, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        String CREATE_TABLE = "CREATE TABLE " + TABLE_ACTIVITY + "("
                + KEY_ACTIVITY_TYPE + " INTEGER,"
                + KEY_ACTIVITY_FLAGS + " INTEGER,"
                + KEY_ACTIVITY_TIMESTAMP + " LONG,"
                + KEY_ACTIVITY_VALUE + " INTEGER,"
                + KEY_ACTIVITY_VALUE2 + " INTEGER,"
                + "UNIQUE("+KEY_ACTIVITY_FLAGS +","+KEY_ACTIVITY_TYPE+","+KEY_ACTIVITY_TIMESTAMP+","+KEY_ACTIVITY_VALUE+","+KEY_ACTIVITY_VALUE+"))";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // Drop older table if exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITY);
        // Create tables again
        onCreate(db);
    }

    // Adding new User Details
    public void insertData(int type, int flags, long timestamp, int value, int value2){
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_ACTIVITY_TYPE, type);
        cValues.put(KEY_ACTIVITY_FLAGS, flags);
        cValues.put(KEY_ACTIVITY_TIMESTAMP, timestamp);
        cValues.put(KEY_ACTIVITY_VALUE, value);
        cValues.put(KEY_ACTIVITY_VALUE2, value2);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_ACTIVITY,null, cValues);
        db.close();
    }

    public void deleteRecord() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from activity where timestamp=2070768428;");
        db.close();
    }

    public void reformatDB(){
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("update activity set type=7 where value=11 and type is null;");
        db.execSQL("update activity set type=7 where value=12 and type is null;");
        db.execSQL("update activity set type=3 where flags=1 and type is null;");
        db.execSQL("update activity set type=7 where flags=3 and type is null;");
        db.execSQL("update activity set type=7 where flags=0 and value=0 and type is null;");
        db.execSQL("update activity set type=0 where flags=2 and type is null;");
        db.close();
    }

    // Get User Details
    public ArrayList<BandActivity> GetData(){
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<BandActivity> userList = new ArrayList<>();
        String query = "SELECT " +
                KEY_ACTIVITY_TYPE + ", " +
                KEY_ACTIVITY_FLAGS + ", " +
                KEY_ACTIVITY_TIMESTAMP + ", " +
                KEY_ACTIVITY_VALUE + ", " +
                KEY_ACTIVITY_VALUE2 + " " +
                " FROM "+ TABLE_ACTIVITY;
        Cursor cursor = db.rawQuery(query,null);
        while (cursor.moveToNext()){
            BandActivity user = new BandActivity();
            user.type = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_TYPE));
            user.flags = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_FLAGS));
            user.timestamp = cursor.getLong(cursor.getColumnIndex(KEY_ACTIVITY_TIMESTAMP));
            user.value= cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_VALUE));
            user.value2= cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_VALUE2));
            userList.add(user);
        }
        return  userList;
    }

    public ArrayList<BandActivity> GetDataRange(java.util.Date ini, java.util.Date fin){
        long iniTime = ini.getTime()/1000;
        long finTime = fin.getTime()/1000;

        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<BandActivity> userList = new ArrayList<>();
        String query = "SELECT " +
                KEY_ACTIVITY_TYPE + ", " +
                KEY_ACTIVITY_FLAGS + ", " +
                KEY_ACTIVITY_TIMESTAMP + ", " +
                KEY_ACTIVITY_VALUE + ", " +
                KEY_ACTIVITY_VALUE2 + " " +
                "FROM "+ TABLE_ACTIVITY + " WHERE " + KEY_ACTIVITY_TIMESTAMP + " BETWEEN " + iniTime + " AND " + finTime + " " +
                "ORDER BY " + KEY_ACTIVITY_TIMESTAMP + " ASC;";
        Cursor cursor = db.rawQuery(query,null);
        while (cursor.moveToNext()){
            BandActivity user = new BandActivity();
            user.type = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_TYPE));
            user.flags = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_FLAGS));
            user.timestamp = cursor.getLong(cursor.getColumnIndex(KEY_ACTIVITY_TIMESTAMP));
            user.value= cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_VALUE));
            user.value2= cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_VALUE2));
            userList.add(user);
        }
        return userList;
    }

//2070768428
    // Get User Details
    public BandActivity GetLastEntry(){
        //deleteData();
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<BandActivity> userList = new ArrayList<>();
        String query = "SELECT " +
                KEY_ACTIVITY_TYPE + ", " +
                KEY_ACTIVITY_FLAGS + ", " +
                KEY_ACTIVITY_TIMESTAMP + ", " +
                KEY_ACTIVITY_VALUE + "," +
                KEY_ACTIVITY_VALUE2 + " " +
                " FROM "+ TABLE_ACTIVITY + " ORDER BY " + KEY_ACTIVITY_TIMESTAMP + " DESC LIMIT 1;";
        Cursor cursor = db.rawQuery(query,null);
        cursor.moveToNext();
        BandActivity user = new BandActivity();
        user.type = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_TYPE));
        user.flags = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_FLAGS));
        user.timestamp = cursor.getLong(cursor.getColumnIndex(KEY_ACTIVITY_TIMESTAMP));
        user.value= cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_VALUE));
        user.value2= cursor.getInt(cursor.getColumnIndex(KEY_ACTIVITY_VALUE2));
        return user;
    }

}

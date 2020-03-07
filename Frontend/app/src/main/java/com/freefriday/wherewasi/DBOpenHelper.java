/*
데이터베이스 관리 객체
 */

package com.freefriday.wherewasi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper {
    private static final String DATABASE_NAME = "InnerDatabase(SQLite).db";
    private static final int DATABASE_VERSION = 1;
    public static SQLiteDatabase db;

    private DBHelper dbhelper;
    private Context cntxt;

    public DBOpenHelper(Context context){
        cntxt = context;
    }

    public DBOpenHelper open() throws SQLException {
        dbhelper = new DBHelper(cntxt,DATABASE_NAME,null, DATABASE_VERSION);
        db = dbhelper.getWritableDatabase();
        return this;
    }

    public void create(){
        dbhelper.onCreate(db);
    }

    public void close(){
        db.close();
    }

    void putinvalue(ContentValues values, String TIME, String LONG, String LAT, String ACCR){
        values.put(DB.CreateDB.TIME, TIME);
        values.put(DB.CreateDB.LONG, LONG);
        values.put(DB.CreateDB.LAT, LAT);
        values.put(DB.CreateDB.ACCR, ACCR);
    }

    public long getnums(){ //행의 수 가져옴
        return DatabaseUtils.queryNumEntries(db, DB.CreateDB.TABLE_NAME);
    }

    public long insertcolumn(String TIME, String LONG, String LAT, String ACCR){
        ContentValues values = new ContentValues();
        putinvalue(values,TIME, LONG, LAT, ACCR);
        return db.insert(DB.CreateDB.TABLE_NAME,null,values);
    }

    public Cursor selectcol(){
        return db.query(DB.CreateDB.TABLE_NAME,null,null,null,null,null,null);
    }

    public int updatecol(long id, String TIME, String LONG, String LAT, String ACCR){
        ContentValues values = new ContentValues();
        putinvalue(values,TIME, LONG, LAT, ACCR);
        return db.update(DB.CreateDB.TABLE_NAME,values,"_id="+id,null);
    }
    public void DelDB(){
        db.execSQL(DB.CreateDB.CLEARDB);
        db.close();
    }

    public int deletecol(long id){
        return db.delete(DB.CreateDB.TABLE_NAME,"_id="+id,null);
    }

    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context,name,factory,version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(DB.CreateDB.CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("drop table if exists "+DB.CreateDB.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }
}

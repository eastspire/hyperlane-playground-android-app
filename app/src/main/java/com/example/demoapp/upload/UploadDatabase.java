package com.example.demoapp.upload;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class UploadDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "upload_history.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_UPLOADS = "uploads";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SIZE = "size";
    private static final String COLUMN_PROGRESS = "progress";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_UPLOAD_TIME = "upload_time";

    public UploadDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_UPLOADS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_SIZE + " INTEGER, " +
                COLUMN_PROGRESS + " INTEGER, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_UPLOAD_TIME + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPLOADS);
        onCreate(db);
    }

    public void insertOrUpdate(UploadRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, record.getId());
        values.put(COLUMN_NAME, record.getName());
        values.put(COLUMN_SIZE, record.getSize());
        values.put(COLUMN_PROGRESS, record.getProgress());
        values.put(COLUMN_URL, record.getUrl());
        values.put(COLUMN_UPLOAD_TIME, record.getUploadTime());

        db.insertWithOnConflict(TABLE_UPLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public void saveUploadRecord(UploadRecord record) {
        insertOrUpdate(record);
    }

    public List<UploadRecord> getAllRecords() {
        List<UploadRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_UPLOADS, null, null, null, null, null, 
                COLUMN_UPLOAD_TIME + " DESC");

        while (cursor.moveToNext()) {
            UploadRecord record = new UploadRecord(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPLOAD_TIME))
            );
            records.add(record);
        }

        cursor.close();
        db.close();
        return records;
    }

    public void delete(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_UPLOADS, COLUMN_ID + " = ?", new String[]{id});
        db.close();
    }

    public void deleteRecord(String id) {
        delete(id);
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_UPLOADS, null, null);
        db.close();
    }
}

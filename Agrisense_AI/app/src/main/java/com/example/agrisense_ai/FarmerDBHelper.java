package com.example.agrisense_ai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FarmerDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "farmerapp.db";
    private static final int DATABASE_VERSION = 5;

    // Users table
    private static final String SQL_CREATE_USERS =
            "CREATE TABLE users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "location TEXT," +
                    "farm_type TEXT)";

    // Videos table
    private static final String SQL_CREATE_VIDEOS =
            "CREATE TABLE videos (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "file_path TEXT NOT NULL," +
                    "thumbnail_path TEXT," +
                    "upload_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id))";

    // Analysis table
    private static final String SQL_CREATE_ANALYSIS =
            "CREATE TABLE video_analysis (" +
                    "analysis_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "video_id INTEGER," +
                    "summary TEXT," +
                    "products TEXT," +
                    "keywords TEXT," +
                    "recommendations TEXT," +
                    "FOREIGN KEY(video_id) REFERENCES videos(_id))";

    public FarmerDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_VIDEOS);
        db.execSQL(SQL_CREATE_ANALYSIS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS video_analysis");
        db.execSQL("DROP TABLE IF EXISTS videos");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public boolean deleteVideo(long videoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        boolean success = false;

        try {
            cursor = db.query("videos",
                    new String[]{"file_path", "thumbnail_path"},
                    "_id = ?",
                    new String[]{String.valueOf(videoId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Delete video file
                String videoPath = cursor.getString(0);
                if (videoPath != null) {
                    new File(videoPath).delete();
                }

                // Delete thumbnail if exists
                String thumbPath = cursor.getString(1);
                if (thumbPath != null) {
                    new File(thumbPath).delete();
                }

                // Delete from database
                success = db.delete("videos", "_id = ?",
                        new String[]{String.valueOf(videoId)}) > 0;
            }
        } catch (Exception e) {
            Log.e("FarmerDBHelper", "Error deleting video", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return success;
    }




    public Cursor getAllVideos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT _id, title, description, file_path, thumbnail_path " +
                        "FROM videos ORDER BY upload_date DESC", null);
    }

    public String getVideoPath(int videoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String path = null;

        try {
            cursor = db.query("videos",
                    new String[]{"file_path"},
                    "_id = ?",
                    new String[]{String.valueOf(videoId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                path = cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    public long saveAnalysis(int videoId, String summary, List<String> products,
                             List<String> keywords, List<String> recommendations) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            values.put("video_id", videoId);
            values.put("summary", summary);
            values.put("products", new JSONArray(products).toString());
            values.put("keywords", new JSONArray(keywords).toString());
            values.put("recommendations", new JSONArray(recommendations).toString());

            return db.insert("video_analysis", null, values);
        } catch (Exception e) {
            Log.e("FarmerDBHelper", "Error saving analysis", e);
            return -1;
        }
    }

    public AnalysisResult getAnalysis(int videoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query("video_analysis",
                    new String[]{"summary", "products", "keywords", "recommendations"},
                    "video_id = ?",
                    new String[]{String.valueOf(videoId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                try {
                    String summary = cursor.getString(0);
                    List<String> products = jsonArrayToList(cursor.getString(1));
                    List<String> keywords = jsonArrayToList(cursor.getString(2));
                    List<String> recommendations = jsonArrayToList(cursor.getString(3));

                    return new AnalysisResult(summary, products, keywords, recommendations);
                } catch (JSONException e) {
                    Log.e("FarmerDBHelper", "Error parsing JSON", e);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public long addVideo(int userId, String title, String description, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            values.put("user_id", userId);
            values.put("title", title);
            values.put("description", description);
            values.put("file_path", filePath);

            return db.insert("videos", null, values);
        } catch (Exception e) {
            Log.e("FarmerDBHelper", "Error adding video", e);
            return -1;
        } finally {
            db.close();
        }
    }

    private List<String> jsonArrayToList(String json) throws JSONException {
        List<String> list = new ArrayList<>();
        if (json != null && !json.isEmpty()) {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        }
        return list;
    }
}

class AnalysisResult {
    String summary;
    List<String> products;
    List<String> keywords;
    List<String> recommendations;

    public AnalysisResult(String summary, List<String> products,
                          List<String> keywords, List<String> recommendations) {
        this.summary = summary;
        this.products = products;
        this.keywords = keywords;
        this.recommendations = recommendations;
    }
}
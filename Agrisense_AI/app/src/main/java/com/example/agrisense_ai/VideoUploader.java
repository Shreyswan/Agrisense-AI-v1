package com.example.agrisense_ai;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoUploader {
    private Context context;
    private FarmerDBHelper dbHelper;

    public VideoUploader(Context context) {
        this.context = context;
        this.dbHelper = new FarmerDBHelper(context);
    }

    public boolean uploadVideo(Uri videoUri, String title, String description, int userId) {
        String filePath = saveVideoToStorage(videoUri);
        if (filePath == null) {
            return false;
        }

        long result = dbHelper.addVideo(userId, title, description, filePath);
        return result != -1;
    }

    private String saveVideoToStorage(Uri videoUri) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "VID_" + timeStamp + ".mp4";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File videoFile = new File(storageDir, videoFileName);

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(videoUri);
            OutputStream outputStream = new FileOutputStream(videoFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            return videoFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("VideoUploader", "Error saving video", e);
            return null;
        }
    }
}
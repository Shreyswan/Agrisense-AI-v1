package com.example.agrisense_ai;

import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

public class VideoDetailActivity extends AppCompatActivity {
    private FarmerDBHelper dbHelper;
    private int videoId;
    private VideoView videoView;
    private ImageView thumbnailView;
    private Button deleteButton;
    private boolean isFullscreen = false;
    private int originalOrientation;
    private View[] viewsToHideInFullscreen;
    private boolean isVideoLandscape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        dbHelper = new FarmerDBHelper(this);
        videoView = findViewById(R.id.videoView);
        thumbnailView = findViewById(R.id.thumbnailView);
        videoId = getIntent().getIntExtra("video_id", -1);
        deleteButton = findViewById(R.id.deleteButton);

        originalOrientation = getRequestedOrientation();
        viewsToHideInFullscreen = new View[]{
                deleteButton,
                findViewById(R.id.summaryText),
                findViewById(R.id.productsText),
                findViewById(R.id.recommendationsText)
        };

        videoView.setOnClickListener(v -> toggleFullscreen());

        if (videoId == -1) {
            finish();
            return;
        }

        setupVideoPlayer();
        checkAnalysis();
        setupDeleteButton();
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        isFullscreen = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        for (View view : viewsToHideInFullscreen) {
            view.setVisibility(View.GONE);
        }

        // Lock orientation based on video aspect ratio
        int orientation = isVideoLandscape ?
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setRequestedOrientation(orientation);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        videoView.setLayoutParams(params);
    }

    private void exitFullscreen() {
        isFullscreen = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }

        for (View view : viewsToHideInFullscreen) {
            view.setVisibility(View.VISIBLE);
        }

        setRequestedOrientation(originalOrientation);

        // Restore original video size based on aspect ratio
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                isVideoLandscape ?
                        getResources().getDimensionPixelSize(R.dimen.video_height_landscape) :
                        getResources().getDimensionPixelSize(R.dimen.video_height_portrait));
        videoView.setLayoutParams(params);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            setResult(RESULT_OK);
            super.onBackPressed();
        }
    }

    private void setupVideoPlayer() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().query(
                    "videos",
                    new String[]{"file_path", "thumbnail_path", "title"},
                    "_id = ?",
                    new String[]{String.valueOf(videoId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String filePath = cursor.getString(0);
                String thumbPath = cursor.getString(1);
                String title = cursor.getString(2);

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }

                File videoFile = new File(filePath);
                if (!videoFile.exists()) {
                    Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Check video orientation
                isVideoLandscape = isVideoLandscape(filePath);

                // Set initial video size based on orientation
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        isVideoLandscape ?
                                getResources().getDimensionPixelSize(R.dimen.video_height_landscape) :
                                getResources().getDimensionPixelSize(R.dimen.video_height_portrait));
                videoView.setLayoutParams(params);

                if (thumbPath != null && new File(thumbPath).exists()) {
                    thumbnailView.setVisibility(View.VISIBLE);
                    Glide.with(this)
                            .load(new File(thumbPath))
                            .into(thumbnailView);

                    thumbnailView.setOnClickListener(v -> {
                        thumbnailView.setVisibility(View.GONE);
                        playVideo(filePath);
                    });
                } else {
                    playVideo(filePath);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void playVideo(String filePath) {
        try {
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.setVideoPath(filePath);

            videoView.setOnPreparedListener(mp -> {
                mediaController.show();
                videoView.start();

                // Adjust video view to maintain aspect ratio
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                float videoProportion = (float) videoWidth / (float) videoHeight;

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                float screenProportion = (float) screenWidth / (float) screenHeight;

                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) videoView.getLayoutParams();
                if (videoProportion > screenProportion) {
                    lp.width = screenWidth;
                    lp.height = (int) ((float) screenWidth / videoProportion);
                } else {
                    lp.width = (int) (videoProportion * (float) screenHeight);
                    lp.height = screenHeight;
                }

                videoView.setLayoutParams(lp);
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error setting up video player", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupDeleteButton() {
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Video")
                    .setMessage("Are you sure you want to delete this video?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (dbHelper.deleteVideo(videoId)) {
                            Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to delete video", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private boolean isVideoLandscape(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            if (widthStr != null && heightStr != null) {
                int width = Integer.parseInt(widthStr);
                int height = Integer.parseInt(heightStr);
                return width > height;
            }
        } catch (Exception e) {
            Log.e("VideoDetail", "Error checking video orientation", e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("VideoDetail", "Error releasing MediaMetadataRetriever", e);
            }
        }
        return false;
    }

    private void checkAnalysis() {
        new AsyncTask<Void, Void, AnalysisResult>() {
            @Override
            protected AnalysisResult doInBackground(Void... voids) {
                AnalysisResult result = dbHelper.getAnalysis(videoId);
                if (result == null) {
                    VideoAnalyzer analyzer = new VideoAnalyzer(VideoDetailActivity.this);
                    String filePath = dbHelper.getVideoPath(videoId);
                    if (filePath != null) {
                        result = analyzer.analyzeVideo(filePath);
                        dbHelper.saveAnalysis(videoId, result.summary,
                                result.products, result.keywords,
                                result.recommendations);
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(AnalysisResult result) {
                if (result != null) {
                    displayAnalysis(result);
                }
            }
        }.execute();
    }

    private void displayAnalysis(AnalysisResult result) {
        TextView summaryView = findViewById(R.id.summaryText);
        TextView productsView = findViewById(R.id.productsText);
        TextView recommendationsView = findViewById(R.id.recommendationsText);

        summaryView.setText(result.summary);
        productsView.setText("Products mentioned: " + String.join(", ", result.products));

        StringBuilder appsText = new StringBuilder("Recommended apps:\n");
        for (String app : result.recommendations) {
            appsText.append("• ").append(app).append("\n");
        }
        recommendationsView.setText(appsText.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        dbHelper.close();
    }
}
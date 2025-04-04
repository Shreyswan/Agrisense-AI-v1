package com.example.agrisense_ai;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrisense_ai.FarmerDBHelper;
import com.example.agrisense_ai.R;
import com.example.agrisense_ai.VideoDetailActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.ContentValues;
import android.content.Intent;

public class videoFragment extends Fragment {
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_PICK_VIDEO = 2;
    private static final int REQUEST_VIDEO_DETAIL = 3;

    private RecyclerView videoRecyclerView;
    private FarmerDBHelper dbHelper;
    private int currentUserId = 1;
    private String currentVideoPath;
    private String currentThumbnailPath;
    private VideoAdapter videoAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new FarmerDBHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        videoRecyclerView = view.findViewById(R.id.videoListView);
        Button recordButton = view.findViewById(R.id.recordButton);
        Button uploadButton = view.findViewById(R.id.uploadButton);

        // Setup RecyclerView
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        videoRecyclerView.setHasFixedSize(true);
        videoAdapter = new VideoAdapter(null);
        videoRecyclerView.setAdapter(videoAdapter);

        recordButton.setOnClickListener(v -> dispatchTakeVideoIntent());
        uploadButton.setOnClickListener(v -> pickVideoFromGallery());

        loadVideos();

        videoAdapter.setOnItemClickListener((videoId) -> {
            Intent intent = new Intent(requireActivity(), VideoDetailActivity.class);
            intent.putExtra("video_id", videoId);
            startActivityForResult(intent, REQUEST_VIDEO_DETAIL);
        });

        videoAdapter.setOnItemLongClickListener((videoId) -> {
            showDeleteDialog(videoId);
            return true;
        });

        return view;
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                String videoFileName = "VID_" + timeStamp + ".mp4";
                File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES);

                if (!storageDir.exists()) {
                    if (!storageDir.mkdirs()) {
                        Toast.makeText(requireContext(), "Failed to create directory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                File videoFile = new File(storageDir, videoFileName);
                currentVideoPath = videoFile.getAbsolutePath();

                Uri videoUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        videoFile
                );

                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("VideoFragment", "Camera error", e);
            }
        }
    }

    private void pickVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_PICK_VIDEO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != getActivity().RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_CAPTURE && currentVideoPath != null) {
                new File(currentVideoPath).delete();
            }
            return;
        }

        if (requestCode == REQUEST_VIDEO_DETAIL) {
            loadVideos();
        } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
            showTitleDialog(currentVideoPath);
        } else if (requestCode == REQUEST_PICK_VIDEO && data != null) {
            handlePickedVideo(data);
        }
    }

    private void handlePickedVideo(Intent data) {
        Uri selectedVideoUri = data.getData();
        if (selectedVideoUri == null) {
            Toast.makeText(requireContext(), "No video selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Get the actual file path from the URI
            String[] filePathColumn = {MediaStore.Video.Media.DATA};
            Cursor cursor = requireActivity().getContentResolver().query(
                    selectedVideoUri,
                    filePathColumn,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                currentVideoPath = cursor.getString(columnIndex);
                cursor.close();

                // Copy the file to app's private storage
                File originalFile = new File(currentVideoPath);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String newFileName = "VID_" + timeStamp + ".mp4";
                File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                File newFile = new File(storageDir, newFileName);

                // Copy the file
                try {
                    java.nio.file.Files.copy(originalFile.toPath(), newFile.toPath());
                    currentVideoPath = newFile.getAbsolutePath();
                    showTitleDialog(currentVideoPath);
                } catch (IOException e) {
                    Toast.makeText(requireContext(), "Error copying video", Toast.LENGTH_SHORT).show();
                    Log.e("VideoFragment", "File copy error", e);
                }
            } else {
                // For some content providers (like Google Photos), we need to use InputStream
                currentVideoPath = selectedVideoUri.toString();
                showTitleDialog(currentVideoPath);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error accessing video", Toast.LENGTH_SHORT).show();
            Log.e("VideoFragment", "Video access error", e);
        }
    }

    private void showTitleDialog(String videoPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Video Title");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            saveVideoToDatabase(title, videoPath);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void saveVideoToDatabase(String title, String videoPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", currentUserId);
        values.put("title", title);
        values.put("description", "Uploaded by user");
        values.put("file_path", videoPath);

        try {
            long result = db.insert("videos", null, values);
            if (result == -1) {
                throw new Exception("Database insert failed");
            }
            Toast.makeText(requireContext(), "Video saved successfully", Toast.LENGTH_SHORT).show();
            loadVideos();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to save video", Toast.LENGTH_SHORT).show();
            Log.e("VideoFragment", "Database error", e);
        }
    }

    private void showDeleteDialog(final int videoId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Video")
                .setMessage("Are you sure you want to delete this video?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbHelper.deleteVideo(videoId)) {
                        Toast.makeText(requireContext(), "Video deleted", Toast.LENGTH_SHORT).show();
                        loadVideos();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete video", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadVideos() {
        Cursor cursor = dbHelper.getAllVideos();
        videoAdapter.swapCursor(cursor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // VideoAdapter remains the same as in your original code
    private static class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private Cursor cursor;
        private OnItemClickListener itemClickListener;
        private OnItemLongClickListener itemLongClickListener;

        public interface OnItemClickListener {
            void onItemClick(int videoId);
        }

        public interface OnItemLongClickListener {
            boolean onItemLongClick(int videoId);
        }

        public VideoAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.itemClickListener = listener;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener listener) {
            this.itemLongClickListener = listener;
        }

        public void swapCursor(Cursor newCursor) {
            if (cursor != null) {
                cursor.close();
            }
            cursor = newCursor;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_list_item, parent, false);
            return new VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            if (!cursor.moveToPosition(position)) {
                return;
            }

            int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

            holder.titleText.setText(title);
            holder.descriptionText.setText(description);

            holder.itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(id);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (itemLongClickListener != null) {
                    return itemLongClickListener.onItemLongClick(id);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        static class VideoViewHolder extends RecyclerView.ViewHolder {
            TextView titleText;
            TextView descriptionText;

            public VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.videoTitle);
                descriptionText = itemView.findViewById(R.id.videoDescription);
            }
        }
    }
}
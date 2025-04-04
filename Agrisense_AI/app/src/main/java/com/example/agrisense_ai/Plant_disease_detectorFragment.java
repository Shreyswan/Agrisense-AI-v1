package com.example.agrisense_ai;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Plant_disease_detectorFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private ImageView imagePreview;
    private TextView tvResult;
    private Bitmap selectedImageBitmap;
    private Socket socket;
    private static final String SERVER_IP = "172.20.10.3";
    private static final int SERVER_PORT = 9991;

    public Plant_disease_detectorFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plant_disease_detector, container, false);

        imagePreview = view.findViewById(R.id.imagePreview);
        tvResult = view.findViewById(R.id.tvResult);
        Button btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        Button btnChooseFromGallery = view.findViewById(R.id.btnChooseFromGallery);

        btnTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
        btnChooseFromGallery.setOnClickListener(v -> openGallery());

        return view;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    Bundle extras = data.getExtras();
                    selectedImageBitmap = (Bitmap) extras.get("data");
                    imagePreview.setImageBitmap(selectedImageBitmap);
                    tvResult.setText("Photo taken successfully");
                } else if (requestCode == REQUEST_IMAGE_GALLERY) {
                    Uri selectedImageUri = data.getData();
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                    imagePreview.setImageBitmap(selectedImageBitmap);
                    tvResult.setText("Image selected from gallery");
                }

                // Start async task to send image
                if (selectedImageBitmap != null) {
                    new SendImageTask().execute();
                }
            } catch (IOException e) {
                Log.e("ImageError", "Error loading image", e);
                tvResult.setText("Error loading image");
                Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SendImageTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(outputStream);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();

                // Send image size first
                dos.writeInt(imageBytes.length);
                // Send image data
                dos.write(imageBytes);
                dos.flush();

                // Read response from server
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    return new String(buffer, 0, bytesRead);
                }

                return "Image sent successfully";
            } catch (UnknownHostException e) {
                Log.e("NetworkError", "Unknown host", e);
                return "Error: Unknown host";
            } catch (IOException e) {
                Log.e("NetworkError", "IO Error", e);
                return "Error: Connection failed";
            } finally {
                closeSocket();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            tvResult.setText(result);
            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e("SocketError", "Error closing socket", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeSocket();
    }
}
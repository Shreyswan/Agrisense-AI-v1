package com.example.agrisense_ai;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.api.Api;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class chatbotFragment extends Fragment {
    ImageButton send_btn;
    EditText query;
    TextView display_chat;
    Socket socket;
    PrintWriter printWriter;
    DataInputStream input;
    String msg;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chatbot, container, false);

        query = (EditText) view.findViewById(R.id.messageInput);
        send_btn = (ImageButton) view.findViewById(R.id.sendButton);
        display_chat = (TextView) view.findViewById(R.id.show_chat);

        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Client client = new Client();
                msg = query.getText().toString();
                client.execute();
            }
        });

        return view;
    }

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("/");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        ArrayList<String> file_data = new ArrayList<>();
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = uri.getPath();
            File file = new File(path);

            String file_path = file.getPath();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("StaticFieldLeak")
    class Client extends AsyncTask<Void, Void, Void> {
        String[] output_list;
        ArrayAdapter<String> output_adapter;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                socket = new Socket("172.20.10.3", 9991);
                InputStream is = socket.getInputStream();

                printWriter = new PrintWriter(socket.getOutputStream());
                printWriter.write(msg);
                printWriter.flush();

                byte[] buffer = new byte[1024];
                int read;
                String output = "";

                while ((read = is.read(buffer)) != -1) {
                    try {
                        output = new String(buffer, 0, read);
                        Log.d("Value1", output);
                        display_chat.setText(output);
                        System.out.flush();
                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        socket.close();
                    }
                }
                printWriter.close();
            } catch (UnknownHostException u) {
                // Toast.makeText(getApplicationContext(), "Failed :(", Toast.LENGTH_SHORT).show();
                u.printStackTrace();
            } catch (IOException i) {
                // Toast.makeText(getApplicationContext(), "Failed :(", Toast.LENGTH_SHORT).show();
                i.printStackTrace();
            }
            return null;
        }
    }
}
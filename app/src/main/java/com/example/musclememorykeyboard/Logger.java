package com.example.musclememorykeyboard;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.common.annotation.NonNullApi;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public abstract class Logger {


    public static void writeLog(Context context, ArrayList<String> points){
        String filename = "testFile.txt";
        File file = new File(context.getFilesDir(), filename);

        try{
            String output;
            output = points.toString();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.write(output);
            bufferedWriter.close();
            Log.d("FILE WRITER", "writeLog: SUCCESS");

        }
        catch(Exception e){
            Log.d("FILE WRITER", "writeLog: Exception");
            e.printStackTrace();
        }

    }

    public static void uploadLog(Context context, StorageReference storageReference, Context activity){
        try {
            ProgressDialog progressDialog = new ProgressDialog(activity);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            String fileName = "testFile.txt";
            //String fileName = "touches.csv";
            Uri filePath = Uri.fromFile(new File(context.getFilesDir(), fileName));

            StorageReference ref = storageReference.child("logs/" + fileName);

            ref.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Log successfully uploaded!", Toast.LENGTH_SHORT).show(); })
                    .addOnFailureListener(
                            e -> {progressDialog.dismiss();
                                Toast.makeText(context, "Log upload failed! :(", Toast.LENGTH_SHORT).show(); })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot snapshot) {
                            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploaded " + (int)progress + "%");
                        }
                    });
        }
        catch(Exception e){
            //progressDialog.dismiss();
            Log.d("LOG_UPLOAD", "Log upload failed! Reason: " + e);
        }
    }
}

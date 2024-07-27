package com.example.musclememorykeyboard;

import android.annotation.SuppressLint;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public abstract class Logger {
    public static boolean first = true;

    public static boolean isFirst() {
        return first;
    }

    public static void setFirst(boolean first) {
        Logger.first = first;
    }

    public static void writeToCSV(Context context) {
        String filename = Session.getUser() + "-" + Session.getSessionID()  +  ".txt";
        File file = new File(context.getFilesDir(), filename);
        Log.d("filepath", context.getFilesDir().toString());

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            if(first){
                String firstLine = "Basic info"+ ";Keyboard" + ";Target phrase" + ";Raw phrase"+ ";Raw TER" + ";Raw WMP" + ";Raw AWPM" + ";Distance phrase"
                        + ";Distance TER" +";Distance WMP" + ";Distance AWPM" + ";SDistance phrase" + ";SDistance TER" + ";SDistance WMP" + ";SDistance AWPM"
                        + ";Index" + "\n";
                bw.write(firstLine);
            }
            String output;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy@HH:mm:ss");
            String timestamp = df.format(new Date());

            int currentIndex = Session.getCurrentPhraseCount() - 1;
            HashMap<String, Double> rawStats = Session.getCurrentPlainStats(currentIndex);
            HashMap<String, Double> distanceStats = Session.getCurrentDistanceStats(currentIndex);
            HashMap<String, Double> stringDistanceStats = Session.getCurrentStringDistanceStats(currentIndex);
            String targetPhrase =  Session.getTargetPhrase(), rawPhrase = Session.getRawPhrase(),
                    distancePhrase = Session.getDistancePhrase(), stringDistancePhrase = Session.getStringDistancePhrase();

            output = timestamp + "-" + Session.getUser() + "-" + Session.getSessionID() + "layout: " + Session.getKeyboardLayout().toString() + "||time-"
                    + Session.getTime().replace(" ", "") + ";" + Session.getKeyboardName() +";" +Session.getTargetPhrase() +";"+ Session.getRawPhrase() + ";"
                    + String.format("%.2f", rawStats.get("TER")) + ";" + String.format("%.2f", rawStats.get("WPM"))+ ";" + String.format("%.2f", rawStats.get("AWPM"))
                    + ";" + Session.getDistancePhrase() + ";" + String.format("%.2f", distanceStats.get("TER")) + ";" + String.format("%.2f", distanceStats.get("WPM"))
                    + ";" + String.format("%.2f", distanceStats.get("AWPM")) + ";" + Session.getStringDistancePhrase() + ";" + String.format("%.2f", stringDistanceStats.get("TER"))
                    + ";" + String.format("%.2f", stringDistanceStats.get("WPM")) + ";" + String.format("%.2f", stringDistanceStats.get("AWPM"))
                    + ";" + (Session.getCurrentPhraseCount()) + "\n";


            bw.write(output);
            bw.close();
            Log.d("FILE WRITER", "writeToCSV: SUCCESS");
            first = false;
        } catch (IOException e) {
            Log.d("FILE WRITER", "writeToCSV: IOException");
            e.printStackTrace();
        }
    }



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

            String fileName = Session.getUser() + "-" + Session.getSessionID()  +  ".txt";
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

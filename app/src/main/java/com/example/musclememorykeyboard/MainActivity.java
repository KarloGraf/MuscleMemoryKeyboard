package com.example.musclememorykeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver touchReceiver;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<String> touchPoints = new ArrayList<>();
        Button upload = findViewById(R.id.buttonUpload);
        Context main = this;
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        touchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TouchTypes type = TouchTypes.valueOf(intent.getStringExtra("KeyType"));
                double x = intent.getDoubleExtra("x", 0);
                double y = intent.getDoubleExtra("y", 0);
                Log.d("TOUCH_BROADCAST", "TOUCH RECEIVED!\nX: " + x + "\nY: " + y);
                if(type.equals(TouchTypes.DEFAULT)){
                    touchPoints.add(String.valueOf(x) + " , " + String.valueOf(y));
                }
                else if(type.equals(TouchTypes.DELETE)){
                    if(!touchPoints.isEmpty()){
                        touchPoints.remove(touchPoints.size()-1);
                    }
                }
                else{
                    touchPoints.add("SPACE");
                }

            }
        };

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.writeLog(getApplicationContext(), touchPoints);
                Logger.uploadLog(getApplicationContext(), storageReference, getParent());

            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(CustomInputMethodService.KEYBOARD_TOUCH);
        registerReceiver(touchReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(touchReceiver);
    }
}
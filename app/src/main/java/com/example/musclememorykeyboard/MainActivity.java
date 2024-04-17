package com.example.musclememorykeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

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
                if (intent.getAction() != null) {
                    if (intent.getAction().equals(CustomInputMethodService.KEYBOARD_TOUCH)) {
                        TouchTypes type = TouchTypes.valueOf(intent.getStringExtra("KeyType"));
                        double x = intent.getDoubleExtra("x", 0);
                        double y = intent.getDoubleExtra("y", 0);
                        Log.d("TOUCH_BROADCAST", "TOUCH RECEIVED! X: " + x + " Y: " + y);
                        if (type == TouchTypes.DEFAULT) {
                            touchPoints.add(String.valueOf(x) + " , " + String.valueOf(y));
                        } else if (type == TouchTypes.DELETE) {
                            if (!touchPoints.isEmpty()) {
                                touchPoints.remove(touchPoints.size() - 1);
                            }
                        } else {
                            touchPoints.add("SPACE");
                        }
                    }
                    else if(intent.getAction().equals(CustomInputMethodService.KEYBOARD_OPENED)){
                        Log.d("KEYBOARD_OPEN_BROADCAST","RECEIVED!");
                    }
                }

            }
        };


        upload.setOnClickListener(view -> {
            Logger.writeLog(getApplicationContext(), touchPoints);
            Logger.uploadLog(getApplicationContext(), storageReference, this);
            touchPoints.clear();

        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(CustomInputMethodService.KEYBOARD_TOUCH);
        filter.addAction(CustomInputMethodService.KEYBOARD_OPENED);
        registerReceiver(touchReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(touchReceiver);
    }
}
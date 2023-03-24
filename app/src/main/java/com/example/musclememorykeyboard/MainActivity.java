package com.example.musclememorykeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver touchReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        touchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int x = intent.getIntExtra("x", 0);
                int y = intent.getIntExtra("y", 0);
                Log.d("TOUCH_BROADCAST", "TOUCH RECEIVED!\nX: " + x + "\nY: " + y);
            }
        };

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
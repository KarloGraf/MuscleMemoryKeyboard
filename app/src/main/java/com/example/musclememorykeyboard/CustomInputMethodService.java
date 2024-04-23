package com.example.musclememorykeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;

public class CustomInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener{
    public static final String KEYBOARD_TOUCH = "KeyboardTouched";
    public static final String KEYBOARD_OPENED = "KeyboardOpened";


    private KeyboardView keyboardView;
    boolean invis = true;
    Keyboard keyboard;

    @Override
    public View onCreateInputView(){
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setPreviewEnabled(false);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setOnTouchListener(this);
        return keyboardView;
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    public void broadcastOpen(Keyboard keyboard) {
        Intent intent = new Intent(CustomInputMethodService.KEYBOARD_OPENED);
        ArrayList<CustomKey> keyList = new ArrayList<>();
        for (Keyboard.Key key:
             keyboard.getKeys()) {
            if(key.codes[0] > 0) {
                CustomKey tmp = new CustomKey(key.x, key.y, key.label.toString());
                keyList.add(tmp);
            }
        }
        intent.putParcelableArrayListExtra("CustomKeyList",keyList);
        sendBroadcast(intent);
        Log.d("KEYBOARD_OPEN_BROADCAST", "SENT!");
    }

    @Override
    public void onKey(int code, int[] ints) {
        InputConnection inputConnection = getCurrentInputConnection();

        if(getCurrentInputConnection() != null){
            switch (code){
                case Keyboard.KEYCODE_DONE:
                    broadcastTouch(0,0,TouchTypes.OTHER);
                    break;
                case Keyboard.KEYCODE_DELETE:
                    broadcastTouch(0,0,TouchTypes.DELETE);
                    break;
                default:
                    broadcastTouch(0,0,TouchTypes.DEFAULT);
                    break;
            }
        }

    }


    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {
        Log.d("SWIPE", "RIGHT SWIPE DETECTED!");
        if(invis) {
            keyboard = new Keyboard(this, R.xml.normal_keys_layout);
            broadcastOpen(keyboard);
        }
        else{
            keyboard = new Keyboard(this, R.xml.keys_layout);
        }
        invis = !invis;
        keyboardView.setKeyboard(keyboard);
    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private void broadcastTouch(double x, double y, TouchTypes key) {
        Intent intent = new Intent(CustomInputMethodService.KEYBOARD_TOUCH);
        intent.putExtra("KeyType", key.name());
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        sendBroadcast(intent);
        //LocalBroadcastManager.getInstance(SmartInputService.this).sendBroadcast(intent);
        Log.d("TOUCH_BROADCAST", "SENT!");
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                Log.d("TouchEvent", "ACTION_DOWN");
                break;
            case MotionEvent.ACTION_UP:
                Log.d("TouchEvent", "ACTION_UP");
                double x = motionEvent.getX();
                double y = motionEvent.getY();
                Log.d("TOUCH", "pressX: " + x + "\n" +
                        "pressY: " + y);
                broadcastTouch(x, y, TouchTypes.DEFAULT);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("TouchEvent", "ACTION_MOVE");
                break;
        }
        return false;
    }

}

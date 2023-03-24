package com.example.musclememorykeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CustomInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener{
    public static final String KEYBOARD_TOUCH = "KeyboardTouched";

    private KeyboardView keyboardView;


    @Override
    public View onCreateInputView(){
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        Keyboard keyboard = new Keyboard(this, R.xml.keys_layout);
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

    @Override
    public void onKey(int i, int[] ints) {

    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private void broadcastTouch(int x, int y) {
        Intent intent = new Intent(CustomInputMethodService.KEYBOARD_TOUCH);
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        sendBroadcast(intent);
        //LocalBroadcastManager.getInstance(SmartInputService.this).sendBroadcast(intent);
        Log.d("TOUCH_BROADCAST", "SENT!");
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        Log.d("TOUCH", "pressX: " + x + "\n" +
                "pressY: " + y);

        broadcastTouch(x, y);
        return false;
    }
}

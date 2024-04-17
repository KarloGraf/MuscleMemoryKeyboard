package com.example.musclememorykeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.util.List;

public class CustomInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener{
    public static final String KEYBOARD_TOUCH = "KeyboardTouched";
    public static final String KEYBOARD_OPENED = "KeyboardOpened";

    private KeyboardView keyboardView;
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


    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Intent intent = new Intent(CustomInputMethodService.KEYBOARD_OPENED);
        intent.putExtra("Random_value","42");
        sendBroadcast(intent);
        Log.d("KEYBOARD_OPEN_BROADCAST", "SENT!");
        super.onStartInputView(attribute, restarting);

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
                    broadcastTouch(0,0,TouchTypes.OTHER);
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
        double x = motionEvent.getX();
        double y = motionEvent.getY();
        Log.d("TOUCH", "pressX: " + x + "\n" +
                "pressY: " + y);

        broadcastTouch(x, y, TouchTypes.DEFAULT);
        return false;
    }

}

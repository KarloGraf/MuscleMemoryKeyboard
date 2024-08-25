package com.example.musclememorykeyboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class CustomInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener{
    public static final String KEYBOARD_TOUCH = "KeyboardTouched";
    public static final String KEYBOARD_OPENED = "KeyboardOpened";
    public static final String KEY_DELETE = "Delete";
    public static final String KEY_SPACE = "Space";
    public static final String KEY_DONE = "Done";
    public static final String KEY_OTHER = "Other";

    private KeyboardView keyboardView;
    boolean invis = true;
    CustomKeyboard keyboard;

    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    @Override
    public View onCreateInputView(){
        keyboardView = (CustomKeyboardOverlay) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboard = new CustomKeyboard(this, R.xml.keys_layout);
        keyboard.changeKeyHeight();
        keyboardView.setKeyboard(keyboard);
        keyboardView.setPadding(dpToPx(72),0,0,0);
        keyboardView.setPreviewEnabled(false);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setOnTouchListener(this);
        keyboardView.invalidateAllKeys();

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
        if(getCurrentInputConnection() != null){
            /*Only care about done, delete and space,
            other keys are invisible or disabled*/
            switch (code){
                case -4:
                    broadcastTouch(0,0,KEY_DONE);
                    break;
                case -5:
                    broadcastTouch(0,0,KEY_DELETE);
                    break;
                case -7:
                    broadcastTouch(0,0,KEY_SPACE);
                    break;
                default:
                    break;
            }
        }
    }

    public int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {
        Log.d("SWIPE", "LEFT SWIPE DETECTED!");
        if(invis) {
            keyboard = new CustomKeyboard(this, R.xml.blank_keys_layout);
            keyboardView.setPadding(0,0,0,0);
        }
        else{
            keyboard = new CustomKeyboard(this, R.xml.keys_layout);
            keyboardView.setPadding(dpToPx(72),0,0,0);
        }
        invis = !invis;
        keyboardView.setKeyboard(keyboard);
    }

    @Override
    public void swipeRight() {
        Log.d("SWIPE", "RIGHT SWIPE DETECTED!");
        if(invis) {
            keyboard = new CustomKeyboard(this, R.xml.blank_keys_layout);
            keyboardView.setPadding(0,0,0,0);
        }
        else{
            keyboard = new CustomKeyboard(this, R.xml.keys_layout);
            keyboardView.setPadding(dpToPx(72),0,0,0);
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

    private void broadcastTouch(double x, double y, String key) {
        Intent intent = new Intent(CustomInputMethodService.KEYBOARD_TOUCH);
        intent.putExtra("KeyType", key);
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        sendBroadcast(intent);
        //LocalBroadcastManager.getInstance(SmartInputService.this).sendBroadcast(intent);
        Log.d("TOUCH_BROADCAST", "SENT!");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        double x;
        double y;
        int pointerIndex = motionEvent.getActionIndex();
        switch (motionEvent.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                x = motionEvent.getX();
                y = motionEvent.getY();
                broadcastTouch(x, y, KEY_OTHER);
                break;
            //ACTION_POINTER_DOWN is for when multiple touches happen at the same time
            case MotionEvent.ACTION_POINTER_DOWN:
                x = motionEvent.getX(motionEvent.getPointerId(pointerIndex));
                y = motionEvent.getY(motionEvent.getPointerId(pointerIndex));
                broadcastTouch(x, y, KEY_OTHER);
                break;
        }
        //Returning false allows the touch event to propagate further
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        Keyboard fullKeyboard = new Keyboard(this, R.xml.normal_keys_layout);
        broadcastOpen(fullKeyboard);
        super.onStartInputView(info, restarting);
    }
}

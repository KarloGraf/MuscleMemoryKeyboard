package com.example.musclememorykeyboard;

import android.os.Parcel;
import android.os.Parcelable;

public class CustomKey implements Parcelable {
    private double x;
    private double y;
    private String label;
    private double keyWidth = MainActivity.getKeyWidth();
    private double keyHeight = MainActivity.getKeyHeight();

    public CustomKey(double x, double y, String label) {
        //Calculating the middle of the key instead of top left
        this.x = x + keyWidth/2;
        this.y = y + keyHeight/2;
        this.label = label;
    }

    protected CustomKey(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
        label = in.readString();
    }


    @Override
    public String toString() {
        return "CustomKey{" +
                "x=" + x +
                ", y=" + y +
                ", label='" + label + '\'' +
                '}';
    }

    public static final Creator<CustomKey> CREATOR = new Creator<CustomKey>() {
        @Override
        public CustomKey createFromParcel(Parcel in) {
            return new CustomKey(in);
        }

        @Override
        public CustomKey[] newArray(int size) {
            return new CustomKey[size];
        }
    };

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    parcel.writeDouble(x);
    parcel.writeDouble(y);
    parcel.writeString(label);
    }

    public double distanceFrom(double x, double y){
        //Euclidean distance
        double distance = Math.pow((this.x - x),2) + Math.pow((this.y - y),2);
        return Math.sqrt(distance);
    }
}

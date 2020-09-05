package com.antware.joggerlogger;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

public class Duration implements Parcelable {
    int hours, minutes, seconds;

    Duration(int hours, int minutes, int seconds) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    protected Duration(Parcel in) {
        hours = in.readInt();
        minutes = in.readInt();
        seconds = in.readInt();
    }

    public static final Creator<Duration> CREATOR = new Creator<Duration>() {
        @Override
        public Duration createFromParcel(Parcel in) {
            return new Duration(in);
        }

        @Override
        public Duration[] newArray(int size) {
            return new Duration[size];
        }
    };

    @NotNull
    public static Duration getDurationFromMs(long duration) {
        return new Duration((int) (duration / 1000 / 60 / 60),(int) (duration / 1000 / 60 % 60),
                (int) (duration / 1000 % 60 % 60));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(hours);
        parcel.writeInt(minutes);
        parcel.writeInt(seconds);
    }
}

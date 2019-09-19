package com.example.locationtrackertest.Model;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Location_user")
public class Location {
    @PrimaryKey(autoGenerate = true)
    int id;

    @ColumnInfo(name = "user_longitude")
    double longitude;

    @ColumnInfo(name = "user_latitude")
    double latitude;




    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}

package com.example.locationtrackertest.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.locationtrackertest.Dao.LocationDAO;
import com.example.locationtrackertest.Model.Location;

@Database(entities = {Location.class}, version = 1, exportSchema = false)
public abstract class LocationDatabase extends RoomDatabase {

    public abstract LocationDAO locationDAO();

}

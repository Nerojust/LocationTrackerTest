package com.example.locationtrackertest.Dao;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.locationtrackertest.Model.Location;

import java.util.List;

@Dao
public interface LocationDAO {
    @Insert
    void addUserLocationToDB(Location location);

    @Query("select * from Location_user")
    List<Location> getUserLocationFromDB();

    @Query("SELECT * FROM Location_user WHERE id = :id")
    List<Location> getUserLocationFromDBbyId(int id);

}

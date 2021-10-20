package com.opiumfive.ycupwars

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DataDao {
    @Query("SELECT * FROM data")
    fun getAll(): List<Data>

    @Insert
    fun insert(data: Data)

    @Delete
    fun delete(user: Data)

    @Query("DELETE FROM data")
    fun nukeTable()
}
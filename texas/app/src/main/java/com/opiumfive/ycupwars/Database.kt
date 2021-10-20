package com.opiumfive.ycupwars

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Data::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataDao(): DataDao
}
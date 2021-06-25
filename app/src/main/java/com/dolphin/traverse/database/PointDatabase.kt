package com.dolphin.traverse.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dolphin.traverse.dao.ControlPointDao
import com.dolphin.traverse.dao.DxPointDao
import com.dolphin.traverse.entitiy.ControlPoint
import com.dolphin.traverse.entitiy.DxPoint

@Database(version = 1, entities = [ControlPoint::class, DxPoint::class])
abstract class PointDatabase : RoomDatabase() {

    abstract fun controlPointDao(): ControlPointDao
    abstract fun dxPointDao(): DxPointDao

    companion object {

        private var instance: PointDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): PointDatabase {
            instance?.let {
                return it
            }
            return Room.databaseBuilder(
                context.applicationContext,
                PointDatabase::class.java,
                "traverse_database"
            ).build().apply {
                instance = this
            }
        }
    }

}
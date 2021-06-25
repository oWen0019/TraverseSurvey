package com.dolphin.traverse.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dolphin.traverse.entitiy.DxPoint

@Dao
interface DxPointDao {

    @Insert
    fun insertDxPoint(dxPointDao: DxPoint): Long

    @Query("select * from DxPoint")
    fun loadAllDxPoint(): MutableList<DxPoint>

    @Query("delete from DxPoint")
    fun clearDxPoint(): Int
}
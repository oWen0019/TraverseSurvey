package com.dolphin.traverse.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dolphin.traverse.entitiy.ControlPoint

@Dao
interface ControlPointDao {

    @Insert
    fun insertControlPoint(controlPoint: ControlPoint): Long

    @Query("select * from ControlPoint")
    fun loadAllControlPoints(): MutableList<ControlPoint>

    @Query("delete from ControlPoint")
    fun clearControlPoint(): Int

}
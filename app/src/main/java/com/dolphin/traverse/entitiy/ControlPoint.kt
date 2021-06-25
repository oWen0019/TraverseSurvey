package com.dolphin.traverse.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ControlPoint(var x: Double, var y: Double){

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

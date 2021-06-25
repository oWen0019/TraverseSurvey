package com.dolphin.traverse.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DxPoint(var angle: String, var fbc: Double, var bbc: Double, var adjustAngle: Double = 0.0, var fwj: Double = 0.0,
                   var xz: Double = 0.0, var yz: Double = 0.0, var dx: Double = 0.0, var dy: Double = 0.0,
                    var x: Double = 0.0, var y: Double = 0.0){

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

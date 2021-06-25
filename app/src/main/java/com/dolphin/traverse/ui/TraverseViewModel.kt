package com.dolphin.traverse.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TraverseViewModel : ViewModel() {
    /**
     * 草图上控制点的计数
     */
    val controlPointCount: LiveData<Int>
        get() = _controlPointCount

    private val _controlPointCount = MutableLiveData<Int>(0)

    /**
     * 草图上导线点的计数
     */
    val dxPointCount: LiveData<Int>
        get() = _dxPointCount

    private val _dxPointCount = MutableLiveData(0)

    /**
     * MyPoint的集合
     */
//    private val _myPointList = MutableLiveData<List<MyPoint>>()
//    val myPointList: LiveData<List<MyPoint>>
//        get() = _myPointList

    init {

        Log.d("TAG", "viewModel init")
    }

    /**
     * 草图上点加一
     */
    fun plusControlCount() {
        _controlPointCount.value = _controlPointCount.value?.plus(1)
    }

    fun plusDxCount(){
        _dxPointCount.value = _dxPointCount.value?.plus(1)
    }



}
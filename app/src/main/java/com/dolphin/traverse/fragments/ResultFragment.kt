package com.dolphin.traverse.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dolphin.traverse.R
import com.dolphin.traverse.adapter.DxAdapter
import com.dolphin.traverse.adapter.KzAdapter
import com.dolphin.traverse.database.PointDatabase
import com.dolphin.traverse.entitiy.ControlPoint
import com.dolphin.traverse.entitiy.DxPoint
import com.dolphin.traverse.util.LogUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.math.*

const val TAG = "Result"
const val LEFT = "左角"
const val RIGHT = "右角"

class ResultFragment : Fragment() {

    lateinit var kzPointList: MutableList<ControlPoint>
    lateinit var dxPointList: MutableList<DxPoint>

    lateinit var kzRecyclerView: RecyclerView
    lateinit var dxRecyclerView: RecyclerView

    lateinit var dxType: String
    lateinit var dxFx: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            dxType = it.getString(DXTYPE).toString()
            dxFx = it.getString(ZZJFX).toString()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        kzRecyclerView = view.findViewById(R.id.kz_point_list)
        dxRecyclerView = view.findViewById(R.id.dx_point_list)

        //清空数据库
        val clearDataBtn: Button = view.findViewById(R.id.clear_data)
        clearDataBtn.setOnClickListener {
            val controlPointDao = PointDatabase.getDatabase(requireContext()).controlPointDao()
            val dxPointDao = PointDatabase.getDatabase(requireContext()).dxPointDao()
            thread {
                val deleteKzNum = controlPointDao.clearControlPoint()
                val deleteDxNum = dxPointDao.clearDxPoint()
                (requireActivity() as Activity).runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "清空 $deleteKzNum 个控制点和 $deleteDxNum 个导线点，",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            }
            kzRecyclerView.adapter = null
            dxRecyclerView.adapter = null
            kzRecyclerView.refreshDrawableState()
            dxRecyclerView.refreshDrawableState()

        }


        //核检
        val checkDataBtn: Button = view.findViewById(R.id.check)
        checkDataBtn.setOnClickListener {
            //精度评定，坐标方位角闭合差的三级限值
            val fs = 24 * sqrt(dxPointList.size.toDouble())
            //图根导线
            val ft = 40 * sqrt(dxPointList.size.toDouble())
            //导线全长闭合差
            var fd = 0.0
            //闭合差
            var fx: Double = 0.0

            //坐标增量闭合差
            var xzbzlbhc = 0.0
            var yzbzlbhc = 0.0

            var dxqcxdbhc = 0.0
            when (dxType) {
                FHDX -> {
                    if (kzPointList.size == 4) {
                        //计算初识已知方位角
                        val csfwj: Double = calculationAzimuthAngle(0, 1)
                        LogUtil.d("zsfwj", "$csfwj")
                        //计算终边已知方位角
                        val zbfwj: Double = calculationAzimuthAngle(2, 3)
                        LogUtil.d("zbfwj", "$zbfwj")
                        //推算出的终边方位角
                        var zbfwj_ts = 0.0
                        when (dxFx) {
                            //左角
                            LEFT -> {
                                var initj = csfwj
                                for (gcj in dxPointList){
                                    zbfwj_ts = initj + 180 + stringToAngle(gcj.angle)
                                    if (zbfwj_ts > 360 || zbfwj_ts < 0){
                                        if (zbfwj_ts > 360){
                                            zbfwj_ts -= 360
                                        }else{
                                            zbfwj_ts += 360
                                        }
                                    }
                                    LogUtil.d("传算角度", "$zbfwj_ts")
                                    initj = zbfwj_ts
                                }
                                LogUtil.d("zzj", "推导出的终边方位角：$zbfwj_ts")
                            }
                            //右角
                            RIGHT -> {
                                var initj = csfwj
                                for (gcj in dxPointList){
                                    zbfwj_ts = initj + 180 - stringToAngle(gcj.angle)
                                    if (zbfwj_ts > 360 || zbfwj_ts < 0){
                                        if (zbfwj_ts > 360){
                                            zbfwj_ts -= 360
                                        }else{
                                            zbfwj_ts += 360
                                        }
                                    }
                                    LogUtil.d("传算角度", "$zbfwj_ts")
                                    initj = zbfwj_ts
                                }
                                LogUtil.d("zzj", "推导出的终边方位角：$zbfwj_ts")
                            }
                        }
                        //计算角度差值
                        fx = zbfwj_ts - zbfwj
                        LogUtil.d("角度差", "$fx")
                        //分配闭合差
                        val fp_fx = fx / dxPointList.size
                        //转折角改正
                        when (dxFx) {
                            LEFT -> {
                                for (dp in dxPointList) {
                                    dp.adjustAngle = stringToAngle(dp.angle) - fp_fx
                                }
                            }

                            RIGHT -> {
                                for (dp in dxPointList) {
                                    dp.adjustAngle = stringToAngle(dp.angle) + fp_fx
                                }
                            }
                        }
                        //添加改正后方位角
                        when (dxFx) {
                            LEFT -> {
                                var hfwj = csfwj
                                for (cz in dxPointList) {
                                    //前方位角
                                    val qfwj = hfwj + 180 - cz.adjustAngle
                                    cz.fwj = qfwj
                                    hfwj = qfwj
                                }
                            }
                            RIGHT -> {
                                var hfwj = csfwj
                                for (cz in dxPointList) {
                                    //前方位角
                                    val qfwj = hfwj - 180 + cz.adjustAngle
                                    cz.fwj = qfwj
                                    hfwj = qfwj
                                }
                            }
                        }
                        //坐标增量
                        for (dp in dxPointList) {
                            val xz = dp.fbc * cos(angleTransToRadian(dp.fwj))
                            val yz = dp.fbc * sin(angleTransToRadian(dp.fwj))
                            dp.xz = xz
                            dp.yz = yz
                            LogUtil.d("cos", "${dp.fwj}")
                            LogUtil.d("边长", "${dp.fbc}")
                            LogUtil.d("x增量", "$xz")
                            LogUtil.d("y增量", "$yz")
                        }
                        //计算坐标增量闭合差
                        for (dp in dxPointList) {
                            xzbzlbhc += dp.xz
                            yzbzlbhc += dp.yz
                        }
                        LogUtil.d("坐标x增量和","$xzbzlbhc")
                        LogUtil.d("坐标y增量和","$yzbzlbhc")
                        xzbzlbhc -= (kzPointList[3].x - kzPointList[1].x)
                        yzbzlbhc -= (kzPointList[3].y - kzPointList[1].y)

                        LogUtil.d("坐标x增量闭合差","$xzbzlbhc")
                        LogUtil.d("坐标y增量闭合差","$yzbzlbhc")
                        //导线全长闭合差fd
                        fd = sqrt(xzbzlbhc.pow(2.0) + yzbzlbhc.pow(2.0))
                        //导线全长相对闭合差

                        //总边长
                        var zbc = 0.0
                        for (dp in dxPointList) {
                            zbc += dp.fbc
                        }
                        dxqcxdbhc = fd / zbc
                        LogUtil.d("导线全长相对闭合差", "$dxqcxdbhc")


                    } else {
                        Toast.makeText(requireContext(), "附和导线控制点少于4", Toast.LENGTH_SHORT).show()
                    }
                }

                BHDX -> {
                    //闭合导线内角和理论值
                    val bh = (dxPointList.size - 2) * 180
                    LogUtil.d("内角理论值", "$bh")

                    //测量的内角和
                    val c_bh = allAngleSum()
                    LogUtil.d("内角和", "$c_bh")

                    //闭合差
                    fx = c_bh - bh
                    LogUtil.d("闭合差", "$fx")

                    //分配闭合差
                    val fp_fx = fx / dxPointList.size
                    LogUtil.d("分配闭合差", "$fp_fx")

                    LogUtil.d("angle", "$dxPointList")
                    //分配了角度的新集合
                    when (dxFx) {
                        LEFT -> {
                            for (dp in dxPointList) {
//                                adjustAngleList.add(dp.angle.toDouble() - fp_fx)

                                dp.adjustAngle = stringToAngle(dp.angle) - fp_fx
                            }
                        }
                        RIGHT -> {
                            for (dp in dxPointList) {
//                                adjustAngleList.add(dp.angle.toDouble() + fp_fx)
                                dp.adjustAngle = stringToAngle(dp.angle) + fp_fx

                            }
                        }
                    }
                    LogUtil.d("angle", "$dxPointList")
                    //已知方位角
                    val csfwj: Double = calculationAzimuthAngle(0, 1)
                    //添加改正后方位角
                    when (dxFx) {
                        LEFT -> {
                            var hfwj = csfwj
                            for (cz in dxPointList) {
                                //前方位角
                                val qfwj = hfwj + 180 - cz.adjustAngle
                                cz.fwj = qfwj
                                hfwj = qfwj
                            }
                        }
                        RIGHT -> {
                            var hfwj = csfwj
                            for (cz in dxPointList) {
                                //前方位角
                                val qfwj = hfwj - 180 + cz.adjustAngle
                                cz.fwj = qfwj
                                hfwj = qfwj
                            }
                        }
                    }
                    //坐标增量
                    for (dp in dxPointList) {
                        val xz = dp.fbc * cos(angleTransToRadian(dp.fwj))
                        val yz = dp.fbc * sin(angleTransToRadian(dp.fwj))
                        dp.xz = xz
                        dp.yz = yz
                        LogUtil.d("cos", "${dp.fwj}")
                        LogUtil.d("边长", "${dp.fbc}")
                        LogUtil.d("x增量", "$xz")
                        LogUtil.d("y增量", "$yz")
                    }
                    //计算坐标增量闭合差
                    for (dp in dxPointList) {
                        xzbzlbhc += dp.xz
                        yzbzlbhc += dp.yz
                    }
                    LogUtil.d("坐标x增量和","$xzbzlbhc")
                    LogUtil.d("坐标y增量和","$yzbzlbhc")
                    xzbzlbhc -= (kzPointList[3].x - kzPointList[1].x)
                    yzbzlbhc -= (kzPointList[3].y - kzPointList[1].y)

                    LogUtil.d("坐标x增量闭合差","$xzbzlbhc")
                    LogUtil.d("坐标y增量闭合差","$yzbzlbhc")
                    //导线全长闭合差fd
                    fd = sqrt(xzbzlbhc.pow(2.0) + yzbzlbhc.pow(2.0))
                    LogUtil.d("闭合导线全长闭合差", "$fd")
                    //导线全长相对闭合差

                    //总边长
                    var zbc = 0.0
                    for (dp in dxPointList) {
                        zbc += dp.fbc
                    }
                    dxqcxdbhc = fd / zbc
                    LogUtil.d("导线全长相对闭合差", "$dxqcxdbhc")

                }
            }

            //检核控制点
            MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme).apply {
                setTitle("结果")
                setPositiveButton("保存", null)
                setNegativeButton("取消", null)
                setMessage("三级限差：\n\t\t\t\t$fs \" \n图根导线：\n\t\t\t\t$ft \" \n角度闭合差：\n\t\t\t\t${fx*3600} \" " +
                        "\n坐标增量闭合差：\n\t\t\t\tX:${xzbzlbhc}m \n\t\t\t\tY:${yzbzlbhc}m" +
                        "\n导线全长闭合差：\n\t\t\t\t${fd}m" +
                        "\n导线全长相对闭合差：\n\t\t\t\t${dxqcxdbhc} \n检核结果：符合" )
                show()
            }

        }

        //平差计算
        val pxBtn: Button = view.findViewById(R.id.px)
        pxBtn.setOnClickListener {

            try {
                when (dxType) {
                    //附和导线平差计算
                    FHDX -> {
                        if (kzPointList.size == 4) {
                            //计算初识已知方位角
                            val csfwj: Double = calculationAzimuthAngle(0, 1)
                            LogUtil.d("已知初始方位角", "$csfwj")
                            //计算终边已知方位角
                            val zbfwj: Double = calculationAzimuthAngle(2, 3)
                            LogUtil.d("已知终边方位角", "$zbfwj")
                            //推算出的终边方位角
                            var zbfwj_ts = 0.0
                            when (dxFx) {
                                //左角
                                LEFT -> {
                                    var initj = csfwj
                                    for (gcj in dxPointList){
                                        zbfwj_ts = initj + 180 + stringToAngle(gcj.angle)
                                        LogUtil.d("角度", "${gcj.angle} / ${stringToAngle(gcj.angle)}")
                                        if (zbfwj_ts > 360 || zbfwj_ts < 0){
                                            if (zbfwj_ts > 360){
                                                zbfwj_ts -= 360
                                            }else{
                                                zbfwj_ts += 360
                                            }
                                        }
                                        LogUtil.d("传算方位角", "$zbfwj_ts")
                                        initj = zbfwj_ts
                                    }
                                }
                                //右角
                                RIGHT -> {
                                    var initj = csfwj
                                    for (gcj in dxPointList){
                                        zbfwj_ts = initj + 180 - stringToAngle(gcj.angle)
                                        if (zbfwj_ts > 360 || zbfwj_ts < 0){
                                            if (zbfwj_ts > 360){
                                                zbfwj_ts -= 360
                                            }else{
                                                zbfwj_ts += 360
                                            }
                                        }
                                        LogUtil.d("传算角度", "$zbfwj_ts")
                                        initj = zbfwj_ts
                                    }
                                }
                            }
                            //计算角度差值
                            val f: Double = zbfwj_ts - zbfwj
                            LogUtil.d("角度差", "$f")
                            //分配闭合差
                            val fp_fx = f / dxPointList.size
                            LogUtil.d("闭合差", "${fp_fx * 3600}")
                            //转折角改正
                            when (dxFx) {
                                // 闭合差反号分配
                                LEFT -> {
                                    for (dp in dxPointList) {
                                        // 改正后转折角
                                        dp.adjustAngle = stringToAngle(dp.angle) - fp_fx
                                        LogUtil.d("改正后的转折角", "${dp.adjustAngle}")
                                    }
                                }
                                // 闭合差正号分配
                                RIGHT -> {
                                    for (dp in dxPointList) {
                                        dp.adjustAngle = stringToAngle(dp.angle) + fp_fx
                                    }
                                }
                            }
                            //添加改正后方位角
                            when (dxFx) {
                                LEFT -> {
                                    var initj = csfwj
                                    for (gcj in dxPointList){
                                        var gfwj = initj + 180 + gcj.adjustAngle
                                        if (gfwj > 360 || gfwj < 0){
                                            if (gfwj > 360){
                                                gfwj -= 360
                                            }else{
                                                gfwj += 360
                                            }
                                        }
                                        gcj.fwj = gfwj
                                        LogUtil.d("左角改正后方位角", "$gfwj")
                                        initj = gfwj
                                    }
                                }
                                RIGHT -> {
                                    var initj = csfwj
                                    for (gcj in dxPointList){
                                        var gfwj = initj + 180 - gcj.adjustAngle
                                        if (gfwj > 360 || gfwj < 0){
                                            if (gfwj > 360){
                                                gfwj -= 360
                                            }else{
                                                gfwj += 360
                                            }
                                        }
                                        gcj.fwj = gfwj
                                        LogUtil.d("右角改正后方位角", "$gfwj")
                                        initj = gfwj
                                    }
                                }
                            }
                            //坐标增量
                            for (dp in dxPointList) {
                                val xz = dp.fbc * cos(angleTransToRadian(dp.fwj))
                                val yz = dp.fbc * sin(angleTransToRadian(dp.fwj))
                                dp.xz = xz
                                dp.yz = yz
                                LogUtil.d("cos", "${dp.fwj}")
                                LogUtil.d("边长", "${dp.fbc}")
                                LogUtil.d("x增量", "$xz")
                                LogUtil.d("y增量", "$yz")
                            }
                            //计算坐标增量闭合差
                            var xzbzlbhc = 0.0
                            var yzbzlbhc = 0.0
                            for (dp in dxPointList) {
                                xzbzlbhc += dp.xz
                                yzbzlbhc += dp.yz
                            }
                            LogUtil.d("坐标x增量和","$xzbzlbhc")
                            LogUtil.d("坐标y增量和","$yzbzlbhc")
                            xzbzlbhc -= (kzPointList[3].x - kzPointList[1].x)
                            yzbzlbhc -= (kzPointList[3].y - kzPointList[1].y)

                            LogUtil.d("坐标x增量闭合差","$xzbzlbhc")
                            LogUtil.d("坐标y增量闭合差","$yzbzlbhc")
                            //导线全长闭合差fd
                            val fd = sqrt(xzbzlbhc.pow(2.0) + yzbzlbhc.pow(2.0))
                            //总边长
                            var zbc = 0.0
                            for (dp in dxPointList) {
                                zbc += dp.fbc
                            }
                            //按比例分配坐标增量闭合差
                            for (dp in dxPointList) {
                                dp.dx = dp.fbc / zbc * xzbzlbhc
                                dp.dy = dp.fbc / zbc * yzbzlbhc
                            }
                            //计算平差后的坐标点
                            var cskzx = kzPointList[1].x
                            var cskzy = kzPointList[1].y
                            for (dp in dxPointList) {
                                dp.x = cskzx + dp.xz - dp.dx
                                dp.y = cskzy + dp.yz - dp.dy
                            }

                            val msg = StringBuilder()
                            for (dp in dxPointList) {
                                val dx = String.format("%.4f", dp.dx)
                                val dy = String.format("%.4f", dp.dy)
                                val x = String.format("%.5f", dp.x)
                                val y = String.format("%.5f", dp.y)
                                val adjustAngle = String.format("%.4f", dp.adjustAngle)
                                msg.append("导线${dp.id} \n\t\t改正后角度：$adjustAngle \n\t\tx改正：${dx}m\n\t\t" +
                                        "y改正：${dy}m\n\t\t平差后坐标:\n($x, $y)\n")
                            }

                            MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.AlertDialogTheme
                            ).apply {
                                setTitle("平差结果")
                                setMessage(msg.toString())
                                setPositiveButton("保存", null)
                                show()
                            }

                        } else {
                            Toast.makeText(requireContext(), "附和导线控制点应等于4", Toast.LENGTH_SHORT)
                                .show()
                        }

                    }


                    //闭合导线平差计算
                    BHDX -> {
                        if (dxPointList.size == 2) {

                            //闭合导线内角和理论值
                            val bh = (dxPointList.size - 2) * 180
                            LogUtil.d("内角理论值", "$bh")

                            //测量的内角和
                            val c_bh = allAngleSum()
                            LogUtil.d("内角和", "$c_bh")

                            //闭合差
                            val fx = c_bh - bh
                            LogUtil.d("闭合差", "$fx")

                            //分配闭合差
                            val fp_fx = fx / dxPointList.size
                            LogUtil.d("分配闭合差", "$fp_fx")

                            LogUtil.d("angle", "$dxPointList")
                            //改正角度
                            when (dxFx) {
                                LEFT -> {
                                    for (dp in dxPointList) {
//                                adjustAngleList.add(dp.angle.toDouble() - fp_fx)

                                        dp.adjustAngle = dp.angle.toDouble() - fp_fx
                                    }
                                }
                                RIGHT -> {
                                    for (dp in dxPointList) {
//                                adjustAngleList.add(dp.angle.toDouble() + fp_fx)
                                        dp.adjustAngle = dp.angle.toDouble() + fp_fx

                                    }
                                }
                            }
                            LogUtil.d("angle", "$dxPointList")
                            //已知方位角
                            val csfwj: Double = calculationAzimuthAngle(0, 1)
                            //添加改正后方位角
                            when (dxFx) {
                                LEFT -> {
                                    var hfwj = csfwj
                                    for (cz in dxPointList) {
                                        //前方位角
                                        var qfwj = hfwj - 180 + cz.adjustAngle
                                        //方位角+-360
                                        while (qfwj > 360 || qfwj < 0) {
                                            if (qfwj > 360) {
                                                qfwj -= 360
                                            } else if (qfwj < 0) {
                                                qfwj += 360
                                            }
                                        }

                                        cz.fwj = qfwj
                                        hfwj = qfwj
                                    }
                                }
                                RIGHT -> {
                                    var hfwj = csfwj
                                    for (cz in dxPointList) {
                                        //前方位角
                                        var qfwj = hfwj + 180 - cz.adjustAngle
                                        //方位角+-360
                                        while (qfwj > 360 || qfwj < 0) {
                                            if (qfwj > 360) {
                                                qfwj -= 360
                                            } else if (qfwj < 0) {
                                                qfwj += 360
                                            }
                                        }
                                        cz.fwj = qfwj
                                        hfwj = qfwj
                                    }
                                }
                            }
                            //坐标增量
                            for (dp in dxPointList) {
                                val xz = dp.bbc * cos(angleTransToRadian(dp.fwj))
                                val yz = dp.bbc * sin(angleTransToRadian(dp.fwj))
                                dp.xz = xz
                                dp.yz = yz
                            }
                            //计算坐标增量闭合差
                            var xzbzlbhc = 0.0
                            var yzbzlbhc = 0.0
                            for (dp in dxPointList) {
                                xzbzlbhc += dp.xz
                                yzbzlbhc += dp.yz
                            }
                            //导线全长闭合差fd
                            val fd = sqrt(xzbzlbhc.pow(2.0) + yzbzlbhc.pow(2.0))
                            LogUtil.d("闭合导线全长闭合差", "$fd")

                            //总边长
                            var zbc = 0.0
                            for (dp in dxPointList) {
                                zbc += dp.bbc
                            }

                            //导线全长相对闭合差
                            val dxqcbhc = fd / zbc
                            LogUtil.d("导线全长相对闭合差", "$dxqcbhc")

                            //按比例分配坐标增量闭合差
                            for (dp in dxPointList) {
                                dp.dx = dp.bbc / zbc * xzbzlbhc
                                dp.dy = dp.bbc / zbc * yzbzlbhc
                            }

                            //计算平差后的坐标点
                            var cskzx = kzPointList[1].x
                            var cskzy = kzPointList[1].y
                            for (dp in dxPointList) {
                                dp.x = cskzx + dp.xz - dp.dx
                                dp.y = cskzy + dp.yz - dp.dy
                                cskzx = dp.x
                                cskzy = dp.y
                            }

                            val msg = StringBuilder()
                            for (dp in dxPointList) {
                                val dx = String.format("%.4f", dp.dx)
                                val dy = String.format("%.4f", dp.dy)
                                val x = String.format("%.5f", dp.x)
                                val y = String.format("%.5f", dp.y)
                                val adjustAngle = String.format("%.4f", dp.adjustAngle)
                                msg.append("导线${dp.id} \n角度改正：$adjustAngle \nx改正：$dx  y改正：$dy \n平差后坐标:($x, $y)\n")
                            }

                            MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.AlertDialogTheme
                            ).apply {
                                setTitle("平差结果")
                                setMessage(msg.toString())
                                setPositiveButton("保存", null)
                                show()
                            }

                        } else {
                            Toast.makeText(requireContext(), "闭合导线控制点应等于2", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }


                    ZDX -> {

                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            }


        }


        //获取控制点、导线点数据
        getDataFromDatabase()
        Log.d(TAG, "控制点: $kzPointList")
        Log.d(TAG, "导线点: $dxPointList")

        for (kzPoint in kzPointList) {
            Log.d(TAG, "onViewCreated: $kzPoint")
        }
        for (dxPoint in dxPointList) {
            Log.d(TAG, "onViewCreated: $dxPoint")
        }

        val kzLayoutManager = LinearLayoutManager(requireContext())
        kzRecyclerView.layoutManager = kzLayoutManager
        val kzAdapter = KzAdapter(requireContext(), kzPointList)
        kzRecyclerView.adapter = kzAdapter

        val dxLayoutManager = LinearLayoutManager(requireContext())
        dxRecyclerView.layoutManager = dxLayoutManager
        val dxAdapter = DxAdapter(requireContext(), dxPointList)
        dxRecyclerView.adapter = dxAdapter


    }

    /**
     * 十进制角度转弧度
     */
    private fun angleTransToRadian(angle: Double): Double {
        return angle * PI / 180
    }



    private fun storageToDatabase() {
        val controlPointDao = PointDatabase.getDatabase(requireContext()).controlPointDao()
        val dxPointDao = PointDatabase.getDatabase(requireContext()).dxPointDao()
        if (kzPointList.size == 0 && dxPointList.size == 0) {
            Toast.makeText(requireContext(), "无新数据", Toast.LENGTH_SHORT).show()
        } else {
            thread {
                for (controlPoint in kzPointList) {
                    controlPoint.id = controlPointDao.insertControlPoint(controlPoint)
                }
                for (dxPoint in dxPointList) {
                    dxPointDao.insertDxPoint(dxPoint)
                }
                (requireActivity() as Activity).runOnUiThread {
                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                }

                kzPointList.clear()
                dxPointList.clear()
            }
        }
    }

    /**
     * 计算所有测量角总和
     */
    private fun allAngleSum(): Double {
        var sum = 0.0
        for (ap in dxPointList) {
            sum += stringToAngle(ap.angle)
        }
        return sum
    }

    /**
     * 把度分秒转换成十进制角度
     */
    private fun stringToAngle(angle: String): Double {

        val dfm = angle.split(" ")
        LogUtil.d("dfm", dfm.toString())
        var angle = 0.0
        when (dfm.size) {
            3 -> {
                angle = dfm[0].toDouble() + dfm[1].toDouble() / 60 + dfm[2].toDouble() / 3600
            }
            2 -> {
                angle = dfm[0].toDouble() + dfm[1].toDouble() / 60
            }
            1 -> {
                angle = dfm[0].toDouble()
            }
        }
        return angle
    }

    /**
     * 计算初识方位角
     */
    private fun calculationAzimuthAngle(num1: Int, num2: Int): Double {
        val dx = kzPointList[num2].x.toDouble() - kzPointList[num1].x.toDouble()
        val dy = kzPointList[num2].y.toDouble() - kzPointList[num1].y.toDouble()

        LogUtil.d("x1", "${kzPointList[num2].x}")
        LogUtil.d("x2", "${kzPointList[num1].x}")
        LogUtil.d("y1", "${kzPointList[num2].y}")
        LogUtil.d("y2", "${kzPointList[num1].y}")
        LogUtil.d("dx", "$dx")
        LogUtil.d("dy", "$dy")

        //坐标推算
        var csj = 0.0
        if (dx == 0.0) {
            csj = if (dy < 0) {
                270.0
            } else {
                90.0
            }
        } else if (dy == 0.0) {
            csj = if (dx < 0) {
                180.0
            } else {
                0.0
            }
        } else if (dx > 0 && dy < 0) {  //第四象限情况
            csj = atan(dy / dx) / PI * 180 + 360
        } else if (dx > 0 && dy > 0) {    //第一象限情况
            csj = atan(dy / dx) / PI * 180
        } else if (dx < 0 && dy > 0) {  //第二象限情况
            csj = atan(dy / dx) / PI * 180 + 180
        } else if (dx < 0 && dy < 0) {   //第三象限情况
            csj = atan(dy / dx) / PI * 180 + 180
        }
        return csj
    }


    private fun getDataFromDatabase() {
        val controlPointDao = PointDatabase.getDatabase(requireContext()).controlPointDao()
        val dxPointDao = PointDatabase.getDatabase(requireContext()).dxPointDao()

//        thread {
//            val kzPoints = controlPointDao.loadAllControlPoints()
//            val dxPoints = dxPointDao.loadAllDxPoint()
//            Log.d("Result", "控制点: $kzPoints")
//            Log.d("Result", "导线点: $dxPoints")
//
//        }

        runBlocking {
            kzPointList = withContext(Dispatchers.Default) {
                controlPointDao.loadAllControlPoints()
            }
            dxPointList = withContext(Dispatchers.Default) {
                dxPointDao.loadAllDxPoint()
            }

        }


    }
}
package com.dolphin.traverse.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.dolphin.traverse.R
import com.dolphin.traverse.database.PointDatabase
import com.dolphin.traverse.databinding.FragmentTraverseBinding
import com.dolphin.traverse.entitiy.ControlPoint
import com.dolphin.traverse.entitiy.DxPoint
import com.dolphin.traverse.ui.TraverseViewModel
import com.dolphin.traverse.util.LogUtil
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.Polyline
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.ArcGISTiledLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.symbology.TextSymbol
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.layout_control_point_input.view.*
import kotlinx.android.synthetic.main.layout_dx_point_input.view.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt


const val URL = "http://map.geoq.cn/arcgis/rest/services/ChinaOnlineCommunity/MapServer"
const val MURL = "http://map.geoq.cn/arcgis/rest/services/ChinaOnlineCommunity_Mobile/MapServer"
const val MY_LICENSE = "runtimelite,1000,rud7692407488,none,S080TK8ELBAFPGXX3070"
const val MY_API_KEY =
    "AAPK0202c1be37754ba292b2dda6ba17995dv4gLaHk9nDiEzYdIVyuCfTzHcDA9lMvHcxbC2WXqIhE92opW2GCyDajMhwJpktbs"

const val BHDX = "????????????"
const val FHDX = "????????????"
const val ZDX = "?????????"
const val DXD = "?????????"
const val KZD = "?????????"
const val X = "??????x"
const val Y = "??????y"
const val ANGLE = "?????????"
const val FBC = "?????????"
const val BBC = "?????????"
const val DXTYPE = "????????????"
const val ZZJFX = "???????????????"

class TraverseFragment : Fragment() {

    val instance by lazy { activity }

    private val viewModel: TraverseViewModel by viewModels()

    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }


    private var _binding: FragmentTraverseBinding? = null
    private val binding get() = _binding!!

    private var pointList: MutableList<Point> = mutableListOf()

    private lateinit var mapView: MapView

    //??????????????????
    private var dxType: String = FHDX
    //??????????????????
    private var dxfx = LEFT

    //?????????????????????
    private var kzPointList: MutableList<ControlPoint> = mutableListOf<ControlPoint>()

    //?????????????????????
    private var dxPointList: MutableList<DxPoint> = mutableListOf<DxPoint>()

    //??????????????????????????????????????????
    private lateinit var beforePoint: Point

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //?????????true???????????????onCreateOptionsMenu
        setHasOptionsMenu(true)
        Log.d("TAG", "onCreate: Created")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTraverseBinding.inflate(inflater, container, false)

        mapView = binding.mapView

        // Observe ??????????????? LiveData.
        viewModel.dxPointCount.observe(viewLifecycleOwner, { count ->
            //???????????????
            binding.pointCount.text = getString(R.string.point_count, count.toString())
        })
        //???????????????????????????
        viewModel.controlPointCount.observe(viewLifecycleOwner, { count ->
            //???????????????
            binding.controlPointCount.text =
                getString(R.string.control_point_count, count.toString())

        })

        //????????????
        binding.calculation.setOnClickListener {
            val bundle: Bundle = Bundle()
            bundle.putString(DXTYPE, dxType)
            bundle.putString(ZZJFX, dxfx)
            it.findNavController().navigate(R.id.action_traverseFragment_to_resultFragment, bundle)
        }

        //????????????????????????
        binding.insertDb.setOnClickListener {
            storageToDatabase()
        }

        return binding.root
    }


    private fun storageToDatabase() {
        val controlPointDao = PointDatabase.getDatabase(requireContext()).controlPointDao()
        val dxPointDao = PointDatabase.getDatabase(requireContext()).dxPointDao()
        if (kzPointList.size == 0 && dxPointList.size == 0) {
            Toast.makeText(requireContext(), "????????????", Toast.LENGTH_SHORT).show()
        } else {
            thread {
                for (controlPoint in kzPointList) {
                    controlPoint.id = controlPointDao.insertControlPoint(controlPoint)
                }
                for (dxPoint in dxPointList) {
                    dxPointDao.insertDxPoint(dxPoint)
                }
                (requireActivity() as Activity).runOnUiThread {
                    Toast.makeText(requireContext(), "????????????", Toast.LENGTH_SHORT).show()
                }

                kzPointList.clear()
                dxPointList.clear()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //????????????
        setupMap()
        setupZoom()
    }

    /**
     * ????????????
     */
    private fun setupMap() {
        ArcGISRuntimeEnvironment.setLicense(MY_LICENSE)
//        ArcGISRuntimeEnvironment.setApiKey(MY_API_KEY)
        /* val map = ArcGISMap(Basemap(ArcGISTiledLayer(URL)))
         binding.mapView.map = map
         binding.mapView.setViewpoint(Viewpoint(32.082133, 118.640727, 10000.0))
         binding.mapView.isAttributionTextVisible = false*/

        mapView.apply {
            map = ArcGISMap(Basemap(ArcGISTiledLayer(URL)))
            graphicsOverlays.add(graphicsOverlay)
            setViewpoint(Viewpoint(32.082133, 118.640727, 10000.0))
            isAttributionTextVisible = false

        }
    }

    /**
     * ??????????????????
     */
    private fun setupZoom() {
        binding.zoomIn.setOnClickListener {
            //????????????????????????
            binding.mapView.setViewpointScaleAsync(binding.mapView.mapScale * 0.5)

        }
        binding.zoomOut.setOnClickListener {
            //????????????????????????
            binding.mapView.setViewpointScaleAsync(binding.mapView.mapScale * 2)

        }
    }


    /**
     * ??????actionbar??????menu
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.layout_menu, menu)
        Log.d("TAG", "onCreateOptionsMenu: Created")
    }

    /**
     * set listener for menu options
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_layout -> {
                showAlert()
                openDrawing(true)
                return true
            }
            R.id.undo -> {
                //????????????
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * ?????????????????????????????????
     */
    private fun showAlert() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.pick_type)
            setItems(R.array.traverse_type, DialogInterface.OnClickListener { _, which ->
                Toast.makeText(requireContext(), "???????????????$which", Toast.LENGTH_SHORT).show()
                val type = resources.getStringArray(R.array.traverse_type)[which]
                dxType = type
                Log.d(TAG, "showAlert: $dxType")
                //??????????????????????????????????????????
                chooseTypeModule(type)
                chooseZzjFx()
                Toast.makeText(requireContext(), getString(R.string.open_draw), Toast.LENGTH_LONG).show()

            })
            setCancelable(false)
            show()
        }

    }

    //?????????????????????
    private fun chooseZzjFx() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.pick_fx)
            setItems(R.array.zzjfx, DialogInterface.OnClickListener { _, which ->
                val fx = resources.getStringArray(R.array.zzjfx)[which]
                dxfx = fx
                LogUtil.d(TAG,"???????????????$fx")
            })
            setCancelable(false)
            show()
        }
    }

    /**
     * s: Traverse Type
     */
    private fun chooseTypeModule(s: String) {
        when (s) {
            BHDX -> {

            }
            FHDX -> {

            }
            ZDX -> {

            }

        }
    }

    /**
     * ????????????????????????
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun openDrawing(b: Boolean) {
        if (b) {
            mapView.onTouchListener = object : DefaultMapViewOnTouchListener(
                requireContext(),
                binding.mapView
            ) {
                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    //?????????????????????
                    popDialog(e!!)
                    return true
                }
            }
        }

    }


    /*
    * Sets and resets the text field error status.
    */
    private fun setErrorTextField(which: String, v: View, error: Boolean) {
        when (which) {
            X -> {
                if (error) {
                    v.x_textField.isErrorEnabled = true
                    v.x_textField.error = getString(R.string.non_null)
                } else {
                    v.x_textField.isErrorEnabled = false
//                    v.x_text_input_edit_text.text = null
                }
            }
            Y -> {
                if (error) {
                    v.y_textField.isErrorEnabled = true
                    v.y_textField.error = getString(R.string.non_null)
                } else {
                    v.y_textField.isErrorEnabled = false
//                    v.y_text_input_edit_text.text = null
                }
            }
            ANGLE -> {
                if (error) {
                    v.angle_textField.isErrorEnabled = true
                    v.angle_textField.error = getString(R.string.non_null)

                } else {
                    v.angle_textField.isErrorEnabled = false
                }
            }
            FBC -> {
                if (error) {
                    v.fb_textField.apply {
                        isErrorEnabled = true
                        setError(getString(R.string.non_null))
                    }
                } else {
                    v.fb_textField.isErrorEnabled = false
                }
            }
            FBC -> {
                if (error) {
                    v.bb_textField.apply {
                        isErrorEnabled = true
                        setError(getString(R.string.non_null))
                    }
                } else {
                    v.bb_textField.isErrorEnabled = false
                }
            }
        }

    }

    /**
     * ??????????????????
     */
    private fun drawPoint(e: MotionEvent, pointType: String) {
        Log.d("TAG", "drawPoint: $pointType")
        //?????????????????????
        val screenPoint: android.graphics.Point = android.graphics.Point(
            e.x.roundToInt(), e.y.roundToInt()
        )
        val newPoint = mapView.screenToLocation(screenPoint)
        val newPointText = Point(
            newPoint.x + 35,
            newPoint.y + 35
        )

        val dx_simpleMarkerSymbol = SimpleMarkerSymbol().apply {
            style = SimpleMarkerSymbol.Style.CIRCLE
            color = Color.BLACK
            size = 12F
        }

        val kz_simpleMarkerSymbol = SimpleMarkerSymbol().apply {
            style = SimpleMarkerSymbol.Style.DIAMOND
            color = Color.RED
            size = 12F
        }
        val dx_textSymbol = TextSymbol().apply {
            size = 12f
            color = Color.RED
            haloColor = Color.WHITE
            haloWidth = 3f
            text = resources.getString(
                R.string.point_name,
                pointType,
                viewModel.dxPointCount.value?.plus(1)
            )
        }

        val kz_textSymbol = TextSymbol().apply {
            size = 12f
            color = Color.RED
            haloColor = Color.WHITE
            haloWidth = 3f
            text = resources.getString(
                R.string.point_name,
                pointType,
                viewModel.controlPointCount.value?.plus(1)
            )
        }
        var graphic = Graphic()
        var textGraphic = Graphic()
        when (pointType) {
            DXD -> {
                graphic = Graphic(newPoint, dx_simpleMarkerSymbol)
                textGraphic = Graphic(newPointText, dx_textSymbol)
            }
            KZD -> {
                graphic = Graphic(newPoint, kz_simpleMarkerSymbol)
                textGraphic = Graphic(newPointText, kz_textSymbol)
            }
        }
        graphicsOverlay.graphics.add(graphic)
        graphicsOverlay.graphics.add(textGraphic)
        //???????????????
        pointList.add(newPoint)

        if (pointList.size > 1) {
            drawLine(newPoint)
        }
        beforePoint = newPoint
        //?????????????????????pointCount???1
        when (pointType) {
            KZD -> viewModel.plusControlCount()
            DXD -> viewModel.plusDxCount()
        }

    }

    /**
     * ?????????
     */
    private fun drawLine(newPoint: Point) {
        val lineSimple = SimpleLineSymbol().apply {
            style = SimpleLineSymbol.Style.SOLID
            color = -0xff9c01
            width = 2f
        }
        val polylinePoints = PointCollection(SpatialReferences.getWebMercator()).apply {
            add(beforePoint)
            add(newPoint)
        }
        val line = Polyline(polylinePoints)
        val polylineGraphic = Graphic(line, lineSimple)
        graphicsOverlay.graphics.add(polylineGraphic)
    }


    /**
     * ????????? ???????????????
     */
    private fun popDialog(e: MotionEvent) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.pick_point_type)
            setItems(R.array.point_type, DialogInterface.OnClickListener { _, which ->
                Toast.makeText(requireContext(), "???????????????$which", Toast.LENGTH_SHORT).show()
                val pointType = resources.getStringArray(R.array.point_type)[which]
                when (pointType) {
                    KZD -> inputControlPointAttribute(e, pointType)
                    DXD -> inputPointAttribute(e, pointType)
                }
                //????????????????????????
//                inputPointAttribute(e, resources.getStringArray(R.array.point_type)[which])
            })
            setCancelable(false)
            show()
        }


    }


    /**
     * ???????????????????????????
     */
    private fun inputControlPointAttribute(e: MotionEvent, pointType: String) {
        val view = layoutInflater.inflate(R.layout.layout_control_point_input,null)
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme).apply {
            setTitle(R.string.pls_c_input)
            setView(view)
            setPositiveButton(
                R.string.comfirm, null
            )
            setNegativeButton(
                R.string.cancel
            ) { _, _ ->
                Log.d("Main", "quxiao")
            }
            setCancelable(false)

        }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val x = view.x_text_input_edit_text.text.toString().trim()
                val y = view.y_text_input_edit_text.text.toString().trim()
                Log.d("TAG", "inputPointAttribute: $x , $y")
                if (x == "" || y == "") {
                    if (x == "") {
                        setErrorTextField(X, view, true)
                    } else {
                        setErrorTextField(X, view, false)
                    }
                    if (y == "") {
                        setErrorTextField(Y, view, true)
                    } else {
                        setErrorTextField(Y, view, false)
                    }
                } else {
                    if (kzPointList.size > 4) {
                        Toast.makeText(requireContext(), "?????????????????????", Toast.LENGTH_SHORT).show()
                    } else {
                        //????????????
                        kzPointList.add(ControlPoint(x.toDouble(), y.toDouble()))
                    }



                    Log.d("Main", "$kzPointList  $x , $y ")
                    //???????????????
                    drawPoint(e, pointType)
                    //????????????
                    alert.dismiss()
                }

            }
        }
        alert.show()
    }

    /**
     * ???????????????????????????
     */
    private fun inputPointAttribute(e: MotionEvent, pointType: String) {
//        val view = requireActivity().layoutInflater.inflate(R.layout.layout_dx_point_input, null)
        val view = layoutInflater.inflate(R.layout.layout_dx_point_input,null)
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme).apply {
            setTitle(R.string.pls_input)
            setView(view)
            setPositiveButton(
                R.string.comfirm,
                null
            )
            setNegativeButton(
                R.string.cancel,
                null
            )
            setCancelable(false)

        }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val angle = view.angle_text_input_edit_text.text.toString().trim()  //?????????
                val fbc = view.fb_text_input_edit_text.text.toString().trim()       //?????????
                val bbc = view.bb_text_input_edit_text.text.toString().trim()       //?????????
                Log.d("TAG", "inputPointAttribute: $angle $fbc $bbc")
                if (angle == "" || fbc == "" || bbc == "") {
                    if (angle == "") {
                        setErrorTextField(ANGLE, view, true)
                    } else {
                        setErrorTextField(ANGLE, view, false)
                    }
                    if (fbc == "") {
                        setErrorTextField(FBC, view, true)
                    } else {
                        setErrorTextField(FBC, view, false)
                    }
                    if (bbc == "") {
                        setErrorTextField(BBC, view, true)
                    } else {
                        setErrorTextField(BBC, view, false)
                    }
                } else {
                    dxPointList.add(DxPoint(angle, fbc.toDouble(), bbc.toDouble()))

                    drawPoint(e, pointType)
                    alert.dismiss()
                }

            }
        }
        alert.show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.graphicsOverlays.clear()
        binding.mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.dispose()

    }

}
package com.dolphin.traverse.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dolphin.traverse.R
import com.dolphin.traverse.entitiy.DxPoint

class DxAdapter(val context: Context, val dxPointList: List<DxPoint>): RecyclerView.Adapter<DxAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dxImageView: ImageView = view.findViewById(R.id.dx_img)
        val dxNameView : TextView = view.findViewById(R.id.dx_name)
        val dxTextView : TextView = view.findViewById(R.id.dx_text)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dx_point_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dxPoint = dxPointList[position]
        holder.dxImageView.setImageResource(R.drawable.ic_dx)
        holder.dxNameView.text = "导线点-${position.plus(1)}"
        holder.dxTextView.text = context.resources.getString(R.string.dx_text,dxPoint.angle, dxPoint.fbc.toString(), dxPoint.bbc.toString())
    }

    override fun getItemCount() = dxPointList.size
}
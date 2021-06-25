package com.dolphin.traverse.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dolphin.traverse.R
import com.dolphin.traverse.entitiy.ControlPoint

class KzAdapter(val context: Context,val kzPointList: List<ControlPoint>) :
    RecyclerView.Adapter<KzAdapter.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val kzImageView: ImageView = view.findViewById(R.id.kz_img)
        val kzName: TextView = view.findViewById(R.id.kz_name)
        val kzTextView : TextView = view.findViewById(R.id.kz_text)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.kz_point_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kzPoint = kzPointList[position]
        holder.kzImageView.setImageResource(R.drawable.ic_kz)
        holder.kzName.text = "控制点-${position.plus(1)}"
        holder.kzTextView.text = context.resources.getString(R.string.kz_text,kzPoint.x.toString(), kzPoint.y.toString())
    }

    override fun getItemCount() = kzPointList.size
}
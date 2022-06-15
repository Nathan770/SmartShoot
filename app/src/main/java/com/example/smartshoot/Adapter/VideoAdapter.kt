package com.example.smartshoot.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartshoot.Object.VideoObj
import com.example.smartshoot.R

class VideoAdapter(private val all: ArrayList<VideoObj>, val fragment: Fragment) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {
    private val allVideo: ArrayList<VideoObj> = this.all

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_card, parent, false)
        return ViewHolder(itemView)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val temp = allVideo[position]
        holder.name.text = temp.name
        holder.date.text = temp.date
        holder.time.text = temp.time
        fragment.view?.let { Glide.with(it).load(temp.photo).into(holder.photo) }

        holder.card.setOnClickListener {}
    }

    override fun getItemCount() = allVideo.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var card : CardView = itemView.findViewById(R.id.card_card)
        var name : TextView = itemView.findViewById(R.id.card_name)
        var time : TextView = itemView.findViewById(R.id.card_time)
        var date : TextView = itemView.findViewById(R.id.card_date)
        var photo : ImageView = itemView.findViewById(R.id.card_photo)
    }


}
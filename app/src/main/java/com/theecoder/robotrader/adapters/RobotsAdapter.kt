package com.theecoder.robotrader.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.theecoder.robotrader.R
import com.theecoder.robotrader.network.module.Licence
import com.theecoder.robotrader.utils.Constants


class RobotsAdapter: RecyclerView.Adapter<RobotsAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Licence>(){

        override fun areItemsTheSame(oldItem: Licence, newItem: Licence): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: Licence, newItem: Licence): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.robots_layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val licence = differ.currentList[position]
        holder.itemView.apply {

            findViewById<TextView>(R.id.bot_name).text = licence.ea_name

            when(licence.owner.logo){
                "none" ->{
                    val imageview = this.findViewById(R.id.logo_img) as ImageView
                    imageview.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_arrow_circle_right_24))
                }
                else ->{
                    Glide.with(this)
                        .load(Constants.LOGO_BASE_URL +licence.owner.logo)
                        .centerCrop()
                        .into(findViewById(R.id.logo_img))
                }
            }

            setOnClickListener {
                onItemClickListener?.let { it(licence) }
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemClickListener: ((Licence) -> Unit)? = null

    fun setOnItemClickListener(listener: (Licence) -> Unit) {
        onItemClickListener = listener
    }


}
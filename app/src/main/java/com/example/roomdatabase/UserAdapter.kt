package com.example.roomdatabase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private var users: List<UserEntity>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val id: TextView = itemView.findViewById(R.id.idValue)
        val name: TextView = itemView.findViewById(R.id.nameValue)
        val email: TextView = itemView.findViewById(R.id.emailValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.data_list_item, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.id.text = user.id.toString()
        holder.name.text = user.name
        holder.email.text = user.email
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun setUsers(users: List<UserEntity>) {
        this.users = users
        notifyDataSetChanged()
    }


}
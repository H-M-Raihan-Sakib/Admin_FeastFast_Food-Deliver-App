package com.example.adminfeastfast.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminfeastfast.AllMenu
import com.example.adminfeastfast.databinding.ItemItemBinding
import com.google.firebase.database.DatabaseReference
import android.content.Context
import com.bumptech.glide.Glide


class MenuItemAdapter(private val context: Context, private val menuList: ArrayList<AllMenu>, private val databaseReference: DatabaseReference) : RecyclerView.Adapter<MenuItemAdapter.AddItemViewHolder>() {
    private val itemQuantities = IntArray(menuList.size) {1}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddItemViewHolder {
        val binding = ItemItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddItemViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuList.size

    inner class AddItemViewHolder(private val binding: ItemItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                val quantity = itemQuantities[position]
                val menuItems = menuList[position]
                val uriString : String? = menuItems.foodImage.toString()

                foodNameTextView.text = menuItems.foodName
                priceTextView.text = menuItems.foodPrice
                Glide.with(context).load(uriString).into(foodImageView)

                quantityTextVIew.text = quantity.toString()

                minusButton.setOnClickListener {
                    decreaseQuantity(position)
                }
                plusButton.setOnClickListener {
                    increaseQuantity(position)
                }
                deleteButton.setOnClickListener {
                    deleteQuantity(position)
                }
            }
        }

        private fun increaseQuantity(position: Int) {
            if(itemQuantities[position] < 10) {
                itemQuantities[position]++
                binding.quantityTextVIew.text = itemQuantities[position].toString()
            }
        }

        private fun decreaseQuantity(position: Int) {
            if(itemQuantities[position] > 1) {
                itemQuantities[position]--
                binding.quantityTextVIew.text = itemQuantities[position].toString()
            }
        }

        private fun deleteQuantity(position: Int) {
            menuList.removeAt(position)
            menuList.removeAt(position)
            menuList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, menuList.size)
        }
    }
}
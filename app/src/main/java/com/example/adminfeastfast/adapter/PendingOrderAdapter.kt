package com.example.adminfeastfast.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.adminfeastfast.databinding.PendingOrdersItemBinding
import com.example.adminfeastfast.model.OrderDetails

class PendingOrderAdapter(
    private val context: Context,
    private val customerNames: MutableList<String>,
    private val totalPrice: MutableList<String>,
    private val foodImages: MutableList<String>, // Kept to prevent index errors, but not displayed
    private val orderList: MutableList<OrderDetails>,
    private val listener: OnItemClicked
) : RecyclerView.Adapter<PendingOrderAdapter.ViewHolder>() {

    interface OnItemClicked {
        fun onItemClickListener(position: Int)
        fun onItemAcceptClickListener(position: Int)
        fun onItemDispatchClickListener(position: Int)
        fun onItemPaymentReceivedClickListener(position: Int)
    }

    // Tracks the "Accept" vs "Dispatch" state for each item
    private val acceptedState = MutableList(customerNames.size) { false }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PendingOrdersItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = customerNames.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(private val binding: PendingOrdersItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val order = orderList[position]

            // 1. Bind Basic Info to XML Views
            binding.customerName.text = order.userName ?: "Unknown"
            binding.customerAddress.text = order.address ?: "No Address"
            binding.customerPhoneNumber.text = order.phoneNumber ?: "Not Provided"
            binding.totalAmount.text = order.totalPrice ?: "$0"

            // 2. Construct the Food List String (Name - Qty - Price)
            val foods = order.foodNames ?: ArrayList()
            val quantities = order.foodQuantities ?: ArrayList()
            val prices = order.foodPrices ?: ArrayList()

            val descriptionBuilder = StringBuilder()

            for (i in foods.indices) {
                val foodName = foods[i]

                // FIX: Handle quantity safely whether it's Int or String
                val qty = if (i < quantities.size) quantities[i].toString() else "1"
                val price = if (i < prices.size) prices[i] else ""

                // Format: Burger - 2 - $10
                descriptionBuilder.append("$foodName - $qty - $price\n")
            }

            binding.orderDetailsList.text = descriptionBuilder.toString()

            // 3. Item Click Listener (Open Details)
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClickListener(pos)
                }
            }

            // 4. Handle Payment Button State
            val isPaymentReceived = order.paymentReceived
            if (isPaymentReceived) {
                binding.paymentReceived.text = "Paid"
                binding.paymentReceived.isEnabled = false
                binding.paymentReceived.alpha = 0.5f
            } else {
                binding.paymentReceived.text = "Payment"
                binding.paymentReceived.isEnabled = true
                binding.paymentReceived.alpha = 1.0f
            }

            // 5. Handle Accept/Dispatch Button State
            val isAccepted = acceptedState[position]
            binding.orderAcceptButton.text = if (!isAccepted) "Accept" else "Dispatch"

            // --- CLICK LISTENERS ---

            // Payment Button (Safe Removal Logic)
            binding.paymentReceived.setOnClickListener {
                val pos = bindingAdapterPosition

                // Ensure position is valid for ALL lists to prevent IndexOutOfBounds
                if (pos != RecyclerView.NO_POSITION && pos < customerNames.size && pos < orderList.size) {

                    // 1. Notify Database
                    listener.onItemPaymentReceivedClickListener(pos)

                    // 2. Safe Removal from all lists
                    // Using if checks ensures we don't crash if one list is shorter
                    if (customerNames.size > pos) customerNames.removeAt(pos)
                    if (totalPrice.size > pos) totalPrice.removeAt(pos)
                    if (foodImages.size > pos) foodImages.removeAt(pos)
                    if (orderList.size > pos) orderList.removeAt(pos)
                    if (acceptedState.size > pos) acceptedState.removeAt(pos)

                    // 3. Update RecyclerView
                    notifyItemRemoved(pos)
                    // FIX: Calculate remaining items correctly to avoid OutOfBounds crash
                    notifyItemRangeChanged(pos, customerNames.size - pos)

                    Toast.makeText(context, "Payment Received & Order Removed", Toast.LENGTH_SHORT).show()
                }
            }

            // Accept / Dispatch Button
            binding.orderAcceptButton.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (!acceptedState[pos]) {
                    // ACTION: ACCEPT
                    acceptedState[pos] = true
                    binding.orderAcceptButton.text = "Dispatch"
                    Toast.makeText(context, "Order Accepted", Toast.LENGTH_SHORT).show()
                    listener.onItemAcceptClickListener(pos)
                } else {
                    // ACTION: DISPATCH
                    // 1. Notify Activity to update Database status
                    listener.onItemDispatchClickListener(pos)

                    // 2. Just show message (Do NOT remove item, user must click Payment to remove)
                    Toast.makeText(context, "Order Dispatched", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

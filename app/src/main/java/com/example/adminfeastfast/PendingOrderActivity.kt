package com.example.adminfeastfast

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminfeastfast.adapter.PendingOrderAdapter
import com.example.adminfeastfast.databinding.ActivityPendingOrderBinding
import com.example.adminfeastfast.model.OrderDetails
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PendingOrderActivity : AppCompatActivity(), PendingOrderAdapter.OnItemClicked {
    private lateinit var binding: ActivityPendingOrderBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseOrderDetails: DatabaseReference

    // Lists to hold data
    private var listOfName: MutableList<String> = mutableListOf()
    private var listOfTotalPrice: MutableList<String> = mutableListOf()
    private var listOfImageFirstFoodOrder: MutableList<String> = mutableListOf()
    private var listOfOrderItem: ArrayList<OrderDetails> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingOrderBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        databaseOrderDetails = database.reference.child("OrderDetails")

        getOrdersDetails()

        binding.backButton.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getOrdersDetails() {
        databaseOrderDetails.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear lists to prevent duplication
                listOfOrderItem.clear()
                listOfName.clear()
                listOfTotalPrice.clear()
                listOfImageFirstFoodOrder.clear()

                for (orderSnapshot in snapshot.children) {
                    val orderDetails = orderSnapshot.getValue(OrderDetails::class.java)

                    // Only add orders that haven't been dispatched yet (Pending Status)
                    if (orderDetails != null && !orderDetails.orderDispatch) {
                        listOfOrderItem.add(orderDetails)
                    }
                }
                addDataToListForAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PendingOrderActivity, "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addDataToListForAdapter() {
        for (orderItem in listOfOrderItem) {
            // Add Name
            orderItem.userName?.let { listOfName.add(it) }

            // Add Total Price
            orderItem.totalPrice?.let { listOfTotalPrice.add(it) }

            // Add Image (Get first image of the order)

        }
        setAdapter()
    }

    // From PendingOrderActivity.kt
    // In PendingOrderActivity.kt

    private fun setAdapter() {
        binding.pendingOrderRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PendingOrderAdapter(
            this,                       // 1. Context
            listOfName,                 // 2. Names List
            listOfTotalPrice,           // 3. Price List
            listOfImageFirstFoodOrder,  // 4. Image List
            listOfOrderItem,            // 5. <--- ADD THIS (The missing List of OrderDetails)
            this                        // 6. Listener
        )
        binding.pendingOrderRecyclerView.adapter = adapter
    }




    // --- Interface Implementation ---

    override fun onItemClickListener(position: Int) {
        //val intent = Intent(this, OrderDetails::class.java)
        //val userOrderDetails = listOfOrderItem[position]
        //intent.putExtra("UserOrderDetails", userOrderDetails)
        //startActivity(intent)
    }

    override fun onItemAcceptClickListener(position: Int) {
        // When Admin clicks Accept
        val childItemPushKey = listOfOrderItem[position].itemPushKey
        val clickItemOrderReference = childItemPushKey?.let {
            database.reference.child("OrderDetails").child(it)
        }

        clickItemOrderReference?.child("orderAccepted")?.setValue(true)
        updateOrderAcceptStatus(position)
    }

    override fun onItemDispatchClickListener(position: Int) {
        // When Admin clicks Dispatch
        val childItemPushKey = listOfOrderItem[position].itemPushKey
        val clickItemOrderReference = childItemPushKey?.let {
            database.reference.child("OrderDetails").child(it)
        }

        clickItemOrderReference?.child("orderDispatch")?.setValue(true)
    }

    private fun updateOrderAcceptStatus(position: Int) {
        // Update status in the User's history as well
        val userIdOfClickedItem = listOfOrderItem[position].userUid
        val pushKeyOfClickedItem = listOfOrderItem[position].itemPushKey

        if (userIdOfClickedItem != null && pushKeyOfClickedItem != null) {
            val buyHistoryReference = database.reference
                .child("users").child(userIdOfClickedItem)
                .child("BuyHistory").child(pushKeyOfClickedItem)

            buyHistoryReference.child("orderAccepted").setValue(true)
        }
    }
    // In PendingOrderActivity.kt

    override fun onItemPaymentReceivedClickListener(position: Int) {
        // 1. Get the Push Key of the item
        val childItemPushKey = listOfOrderItem[position].itemPushKey

        // 2. Update "OrderDetails" Node
        if (childItemPushKey != null) {
            val orderReference = database.reference.child("OrderDetails").child(childItemPushKey)
            orderReference.child("paymentReceived").setValue(true)
        }
    }

}

package com.example.adminfeastfast

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminfeastfast.adapter.DeliveryAdapter
import com.example.adminfeastfast.databinding.ActivityOutForDeliveryBinding
import com.example.adminfeastfast.model.OrderDetails // Ensure you have this data class
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OutForDeliveryActivity : AppCompatActivity() {
    private val binding: ActivityOutForDeliveryBinding by lazy {
        ActivityOutForDeliveryBinding.inflate(layoutInflater)
    }

    private lateinit var database: FirebaseDatabase
    private var listOfCompleteOrderList: ArrayList<OrderDetails> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // 1. Initialize Firebase
        database = FirebaseDatabase.getInstance()

        // 2. Fetch Data
        retrieveCompleteOrderDetails()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun retrieveCompleteOrderDetails() {
        // Initialize the node reference
        val completeOrderReference = database.reference.child("OrderDetails")
            .orderByChild("itemPushKey") // Optional: Sort logic if needed

        completeOrderReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear list to prevent duplicates
                listOfCompleteOrderList.clear()

                for (orderSnapshot in snapshot.children) {
                    // Get the order object
                    val completeOrder = orderSnapshot.getValue(OrderDetails::class.java)
                    completeOrder?.let {
                        listOfCompleteOrderList.add(it)
                    }
                }
                // Once loop is done, reverse list to show newest first (optional)
                listOfCompleteOrderList.reverse()

                setDataIntoRecyclerView()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OutForDeliveryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("OutForDelivery", error.message)
            }
        })
    }

    private fun setDataIntoRecyclerView() {
        // Initialize arrays to hold the specific data needed for the adapter
        val customerName = ArrayList<String>()
        val moneyStatus = ArrayList<String>()

        for (order in listOfCompleteOrderList) {
            order.userName?.let { customerName.add(it) }
            order.paymentReceived?.let {
                moneyStatus.add(if(it) "Received" else "Not Received")
            }
        }

        // Pass the extracted data to the adapter
        val adapter = DeliveryAdapter(customerName, moneyStatus)
        binding.deliveryRecyclerView.adapter = adapter
        binding.deliveryRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}

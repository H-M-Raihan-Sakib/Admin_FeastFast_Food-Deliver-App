package com.example.adminfeastfast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adminfeastfast.databinding.ActivityMainBinding
import com.example.adminfeastfast.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    // Notification constants
    private val CHANNEL_ID = "OrderChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val intent = Intent(this, OrderNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // 1. Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference.child("OrderDetails")

        // 2. Setup Notification Channel
        createNotificationChannel()

        // 3. Listen for New Orders specifically for Notifications
        listenForNewOrders()

        // 4. Update Dashboard UI (Your existing logic)
        updateDashboardData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Click Listeners ---
        binding.addMenu.setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }
        binding.allItemMenu.setOnClickListener {
            startActivity(Intent(this, AllItemActivity::class.java))
        }
        binding.outForDeliveryButton.setOnClickListener {
            startActivity(Intent(this, OutForDeliveryActivity::class.java))
        }
        binding.profile.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
        binding.pendingOrderTextView.setOnClickListener {
            startActivity(Intent(this, PendingOrderActivity::class.java))
        }
        binding.logOutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

// Make sure your ID in activity_main.xml for the cardView_chat has an onClick or
// ensure the TextView/ImageView IDs inside it are clickable if you prefer.
// Based on your XML, you named the constraint layout 'chatWithCustomer'
        binding.chatWithCustomer.setOnClickListener {
            val intent = Intent(this, ListOfUsersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun listenForNewOrders() {
        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // This method triggers for EVERY existing child when the app starts,
                // and then for every NEW child added afterwards.

                // To avoid notifying for old orders on startup, you could check timestamps
                // or just accept that on first launch you might get alerts.
                // A simple check: Only notify if the order is NOT accepted yet.

                val order = snapshot.getValue(OrderDetails::class.java)
                if (order != null && !order.orderAccepted && !order.orderDispatch) {
                    showNotification(order.userName ?: "Unknown User")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showNotification(customerName: String) {
        // Check for permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                return
            }
        }

        // Create an intent to open PendingOrderActivity when notification is clicked
        val intent = Intent(this, PendingOrderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.green_logo) // Make sure you have a valid icon
            .setContentTitle("New Order Received!")
            .setContentText("Customer $customerName just placed an order.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            // Use a unique ID (System.currentTimeMillis) so notifications don't overwrite each other
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Orders"
            val descriptionText = "Notifications for new food orders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ... (Keep your existing updateDashboardData function below) ...
    private fun updateDashboardData() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var pendingCount = 0
                var completedCount = 0
                var totalEarnings = 0.0

                for (orderSnapshot in snapshot.children) {
                    val order = orderSnapshot.getValue(OrderDetails::class.java)

                    if (order != null) {
                        if (!order.orderAccepted && !order.orderDispatch) {
                            pendingCount++
                        }
                        if (order.orderDispatch && order.paymentReceived) {
                            completedCount++
                        }
                        if (order.paymentReceived) {
                            val priceStr = order.totalPrice?.replace("$", "") ?: "0"
                            try {
                                totalEarnings += priceStr.toDouble()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                binding.pendingCount.text = pendingCount.toString()
                binding.completedOrder.text = completedCount.toString()
                binding.wholeTimeEarning.text = "$$totalEarnings"
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}

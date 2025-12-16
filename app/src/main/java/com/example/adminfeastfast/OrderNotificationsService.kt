package com.example.adminfeastfast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.adminfeastfast.model.MessageModel
import com.example.adminfeastfast.model.OrderDetails
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class OrderNotificationService : Service() {

    private lateinit var database: FirebaseDatabase
    private lateinit var ordersReference: DatabaseReference
    private lateinit var chatsReference: DatabaseReference

    private val CHANNEL_ID = "AdminChannel" // Combined channel for admin alerts
    private val FOREGROUND_ID = 999

    // Timestamp to prevent notifying for old messages when service restarts
    private var serviceStartTime: Long = 0

    override fun onCreate() {
        super.onCreate()

        serviceStartTime = System.currentTimeMillis()

        // 1. Initialize Firebase
        database = FirebaseDatabase.getInstance()
        ordersReference = database.reference.child("OrderDetails")
        chatsReference = database.reference.child("chats")

        // 2. Start Service in Foreground
        startForeground(FOREGROUND_ID, createForegroundNotification())

        // 3. Start Listeners
        listenForNewOrders()
        listenForNewMessages()
    }

    private fun listenForNewOrders() {
        ordersReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val order = snapshot.getValue(OrderDetails::class.java)

                // Only notify if order is created AFTER the service started (prevents spam on restart)
                // And only if it's pending
                if (order != null && !order.orderAccepted && !order.orderDispatch) {
                    showNewOrderNotification(order.userName ?: "Unknown Customer")
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- NEW: CHAT LISTENER ---
    private fun listenForNewMessages() {
        // We listen to the root 'chats' node.
        // When a user sends a message, their specific node changes.
        chatsReference.addChildEventListener(object : ChildEventListener {

            // 1. Handle Existing Users sending NEW messages
            override fun onChildChanged(userSnapshot: DataSnapshot, previousChildName: String?) {
                handleMessageNotification(userSnapshot)
            }

            // 2. Handle BRAND NEW Users starting a chat
            override fun onChildAdded(userSnapshot: DataSnapshot, previousChildName: String?) {
                handleMessageNotification(userSnapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleMessageNotification(userSnapshot: DataSnapshot) {
        val userId = userSnapshot.key ?: return

        // DEBUG LOG: See if the listener is even triggering
        android.util.Log.d("NotifyDebug", "Change detected for User: $userId")

        // 1. Try to find the message node
        var messageNode = userSnapshot.child("message") // Path: chats -> userId -> message

        // Fallback: If "message" node doesn't exist, maybe messages are direct children?
        // (Path: chats -> userId -> messageId)
        if (!messageNode.exists()) {
            android.util.Log.d("NotifyDebug", "Node 'message' not found. Checking direct children.")
            messageNode = userSnapshot
        }

        // 2. Get the last message
        val lastMessageSnapshot = messageNode.children.lastOrNull()

        if (lastMessageSnapshot == null) {
            android.util.Log.d("NotifyDebug", "No messages found for this user.")
            return
        }

        // 3. Convert to Object
        val message = try {
            lastMessageSnapshot.getValue(MessageModel::class.java)
        } catch (e: Exception) {
            android.util.Log.e("NotifyDebug", "Error converting message: ${e.message}")
            null
        }

        if (message != null) {
            android.util.Log.d("NotifyDebug", "Last Msg: '${message.message}' | SentByAdmin: ${message.sentByAdmin} | Time: ${message.timestamp}")
            android.util.Log.d("NotifyDebug", "Service Start Time: $serviceStartTime")

            // 4. CHECK CONDITIONS
            val isUserMessage = !message.sentByAdmin
            // IMPORTANT: For debugging, I commented out the timestamp check.
            // Uncomment it after you confirm notifications work.
            val isNewMessage = true // message.timestamp > serviceStartTime

            if (isUserMessage && isNewMessage) {
                android.util.Log.d("NotifyDebug", "CONDITIONS MET! Showing Notification.")

                // Update service time to avoid spamming the same notification loop
                serviceStartTime = message.timestamp

                val senderName = message.senderName ?: "Customer"
                val messageText = message.message ?: "Sent a photo"

                showChatNotification(userId, senderName, messageText)
            } else {
                android.util.Log.d("NotifyDebug", "Conditions Failed -> UserMsg: $isUserMessage, NewMsg: $isNewMessage")
            }
        }
    }


    private fun showChatNotification(userId: String, senderName: String, messageText: String) {
        // Create Intent to open ChatActivity directly
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("customerId", userId)
        intent.putExtra("customerName", senderName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            this, userId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Message from $senderName")
            .setContentText(messageText)
            .setSmallIcon(R.drawable.green_logo) // Ensure you have a notification icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use userId.hashCode() so notifications from different users stack,
        // but multiple messages from the same user update the existing notification
        notificationManager.notify(userId.hashCode(), notification)
    }

    // --- EXISTING HELPER FUNCTIONS ---

    private fun createForegroundNotification(): Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Admin FeastFast Running")
            .setContentText("Monitoring Orders & Chats...")
            .setSmallIcon(R.drawable.green_logo)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showNewOrderNotification(customerName: String) {
        val intent = Intent(this, PendingOrderActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("New Order Received!")
            .setContentText("Customer $customerName just placed an order.")
            .setSmallIcon(R.drawable.green_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Admin Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

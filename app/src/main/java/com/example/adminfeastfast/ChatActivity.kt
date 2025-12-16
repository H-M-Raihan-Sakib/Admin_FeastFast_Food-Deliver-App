package com.example.adminfeastfast

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminfeastfast.adapter.ChatAdapter
import com.example.adminfeastfast.databinding.ActivityChatBinding
import com.example.adminfeastfast.model.MessageModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var messageList: ArrayList<MessageModel>
    private lateinit var adapter: ChatAdapter

    // Data passed from the User List
    private var customerId: String? = null
    private var customerName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get info about who we are chatting with
        customerId = intent.getStringExtra("customerId")
        customerName = intent.getStringExtra("customerName")

        binding.customerNameTitle.text = customerName ?: "Customer"

        databaseReference = FirebaseDatabase.getInstance().reference
        messageList = ArrayList()
        adapter = ChatAdapter(messageList)

        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = adapter

        // Listen for messages
        if (customerId != null) {
            // Path: chats -> userId -> messages
            databaseReference.child("chats").child(customerId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        messageList.clear()
                        for (msgSnapshot in snapshot.children) {
                            val message = msgSnapshot.getValue(MessageModel::class.java)
                            if (message != null) {
                                messageList.add(message)
                            }
                        }
                        adapter.notifyDataSetChanged()
                        // Scroll to bottom
                        if (messageList.isNotEmpty()) {
                            binding.chatRecyclerView.smoothScrollToPosition(messageList.size - 1)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageBox.text.toString()
            if (messageText.isNotEmpty() && customerId != null) {
                sendMessage(messageText)
            }
        }

        binding.backButton.setOnClickListener { finish() }
    }

    private fun sendMessage(message: String) {
        val currentAdminId = FirebaseAuth.getInstance().currentUser?.uid

        // Admin sends message TO the customer's chat node
        val messageObject = MessageModel(
            message = message,
            senderId = currentAdminId,
            senderName = "Admin",
            sentByAdmin = true, // IMPORTANT: Marks this as Admin message
            timestamp = System.currentTimeMillis()
        )

        databaseReference.child("chats").child(customerId!!).push()
            .setValue(messageObject)
            .addOnSuccessListener {
                binding.messageBox.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }
}

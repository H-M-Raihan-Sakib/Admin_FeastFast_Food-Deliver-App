package com.example.adminfeastfast

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminfeastfast.databinding.ActivityListOfUsersBinding
import com.example.adminfeastfast.model.MessageModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListOfUsersActivity : AppCompatActivity() {
    private val binding: ActivityListOfUsersBinding by lazy {
        ActivityListOfUsersBinding.inflate(layoutInflater)
    }
    private lateinit var database: FirebaseDatabase
    private var listOfCustomers: ArrayList<Pair<String, String>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()

        fetchCustomersWithChats()

        binding.backButton.setOnClickListener { finish() }
    }

    private fun fetchCustomersWithChats() {
        // NOTE: This assumes your database path is "chats" -> "userId" -> "messages"
        val chatsRef = database.reference.child("chats")

        chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfCustomers.clear()
                val displayNames = ArrayList<String>()

                if (!snapshot.exists()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.userListView.visibility = View.GONE
                    return
                }

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key

                    // Default name
                    var userName = "Customer ($userId)"

                    // Try to find a real name inside the messages
                    // Assuming structure: chats -> userId -> messageId -> { senderName: "John" }
                    for(msg in userSnapshot.children) {
                        val m = msg.getValue(MessageModel::class.java)
                        // Find a message sent by the customer (not admin) to get their name
                        if(m != null && !m.sentByAdmin && m.senderName != null) {
                            userName = m.senderName
                            break
                        }
                    }

                    if (userId != null) {
                        listOfCustomers.add(Pair(userId, userName))
                        displayNames.add(userName)
                    }
                }

                if (displayNames.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.userListView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.userListView.visibility = View.VISIBLE
                    setupListView(displayNames)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListOfUsersActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupListView(names: ArrayList<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        binding.userListView.adapter = adapter

        binding.userListView.setOnItemClickListener { _, _, position, _ ->
            val selectedCustomer = listOfCustomers[position]
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("customerId", selectedCustomer.first) // Pass UserID
            intent.putExtra("customerName", selectedCustomer.second) // Pass Name
            startActivity(intent)
        }
    }
}

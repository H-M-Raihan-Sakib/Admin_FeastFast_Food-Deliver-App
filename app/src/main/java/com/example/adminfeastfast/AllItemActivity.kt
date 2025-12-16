package com.example.adminfeastfast

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminfeastfast.adapter.MenuItemAdapter
import com.example.adminfeastfast.databinding.ActivityAllItemBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// REMOVED: private val DatabaseReference.reference: DatabaseReference

class AllItemActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<AllMenu> = ArrayList()
    private val binding : ActivityAllItemBinding by lazy {
        ActivityAllItemBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // FIXED: Correct initialization
        databaseReference = FirebaseDatabase.getInstance().reference

        retrieveMenuItems()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.backButton.setOnClickListener {
            finish()
        }

        // REMOVED: These lines caused an "Unresolved reference: adapter" error
        // because data hasn't loaded yet. The adapter is set inside retrieveMenuItems.
        // binding.MenuRecyclerView.layoutManager = LinearLayoutManager(this)
        // binding.MenuRecyclerView.adapter = adapter
    }

    private fun retrieveMenuItems() {
        database = FirebaseDatabase.getInstance()
        val foodRef: DatabaseReference = database.reference.child("Menu")

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                menuItems.clear()
                for (foodSnapshot in dataSnapshot.children) {
                    val menuItem = foodSnapshot.getValue(AllMenu::class.java)
                    menuItem?.let {
                        menuItems.add(it)
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", error.message)
            }

            // This function is scoped inside the anonymous object, which is fine,
            // but usually better to put it in the main class body.
            // For now, this works to fix your build error.
            fun setAdapter() {
                val adapter = MenuItemAdapter(this@AllItemActivity, menuItems, databaseReference)

                binding.MenuRecyclerView.layoutManager = LinearLayoutManager(this@AllItemActivity)
                binding.MenuRecyclerView.adapter = adapter
            }
        }) // Added missing closing parenthesis for addListenerForSingleValueEvent
    }
}

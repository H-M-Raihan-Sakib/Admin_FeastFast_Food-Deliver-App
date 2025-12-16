package com.example.adminfeastfast

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adminfeastfast.databinding.ActivityAdminProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProfileActivity : AppCompatActivity() {
    private val binding: ActivityAdminProfileBinding by lazy {
        ActivityAdminProfileBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // 1. Initialize Firebase
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        // 2. Fetch and Display User Data
        retrieveUserData()

        binding.backButton.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Disable fields initially
        setFieldsEnabled(false)

        var isEnable = false
        binding.editButton.setOnClickListener {
            isEnable = !isEnable
            setFieldsEnabled(isEnable)

            if (isEnable) {
                // Entered Edit Mode
                binding.name.requestFocus()
                binding.editButton.text = "Save" // Change text to indicate saving
            } else {
                // Clicked "Save" -> Update Data
                updateUserData()
                binding.editButton.text = "Edit" // Change text back
            }
        }
    }

    private fun retrieveUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = databaseReference.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Assuming your database keys match these names
                        val ownerName = snapshot.child("name").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        val password = snapshot.child("password").getValue(String::class.java)
                        val address = snapshot.child("address").getValue(String::class.java)
                        val phone = snapshot.child("phone").getValue(String::class.java)

                        // Set text to views
                        setDataToViews(ownerName, email, password, address, phone)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminProfileActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setDataToViews(name: String?, email: String?, pass: String?, address: String?, phone: String?) {
        binding.name.setText(name)
        binding.email.setText(email)
        binding.password.setText(pass)
        binding.address.setText(address)
        binding.phone.setText(phone)
    }

    private fun updateUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = databaseReference.child("users").child(userId)

            val updateName = binding.name.text.toString()
            val updateEmail = binding.email.text.toString()
            val updatePassword = binding.password.text.toString()
            val updateAddress = binding.address.text.toString()
            val updatePhone = binding.phone.text.toString()

            val userData = hashMapOf(
                "name" to updateName,
                "email" to updateEmail,
                "password" to updatePassword,
                "address" to updateAddress,
                "phone" to updatePhone
            )

            userRef.updateChildren(userData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to Update Profile", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        binding.name.isEnabled = enabled
        binding.address.isEnabled = enabled
        binding.email.isEnabled = enabled
        binding.phone.isEnabled = enabled
        binding.password.isEnabled = enabled
    }
}

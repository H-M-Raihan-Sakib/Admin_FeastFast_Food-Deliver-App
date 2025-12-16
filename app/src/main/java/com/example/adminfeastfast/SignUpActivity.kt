package com.example.adminfeastfast

import android.content.Intent
import android.os.Bundle
import android.util.Log // Added
import android.widget.ArrayAdapter
import android.widget.Toast // Added
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adminfeastfast.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth // Added
import com.google.firebase.database.DatabaseReference // Added
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class SignUpActivity : AppCompatActivity() {

    private lateinit var name: String
    private lateinit var restaurentName: String
    private lateinit var EmailorPhone: String
    private lateinit var password: String

    // FIXED: Changed firebaseAuth to FirebaseAuth
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database.reference

        binding.CreateButton.setOnClickListener {
            name = binding.name.text.toString().trim()
            restaurentName = binding.restaurentName.text.toString().trim()
            EmailorPhone = binding.EmailOrPhone.text.toString().trim()
            password = binding.password.text.toString()

            if(name.isEmpty() || restaurentName.isEmpty() || EmailorPhone.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else{
                createAccount(name, restaurentName, EmailorPhone, password)
            }
            // Note: You might not want to start LoginActivity here immediately if createAccount is async,
            // but leaving it as per your logic for now.
        }
        binding.alreadyHaveAccountButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity((intent))
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val locationList = arrayOf("Cumilla", "Chandpur", "Lakkhipur", "Dhaka")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locationList)
        val autoCompleteTextView = binding.ListOfLocation
        autoCompleteTextView.setAdapter(adapter)

    }
    private fun createAccount(name: String, restaurentName: String, EmailorPhone: String, password: String) {
        auth.createUserWithEmailAndPassword(EmailorPhone, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                    saveUserData()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity((intent))

                    // FIXED: Changed Finish() to finish()
                    finish()
                }
                else {
                    Toast.makeText(this, "Account Creation Failed", Toast.LENGTH_SHORT).show()
                    Log.d("Error", task.exception.toString())
                }

            }


    }

    private fun saveUserData(){
        name = binding.name.text.toString().trim()
        restaurentName = binding.restaurentName.text.toString().trim()
        EmailorPhone = binding.EmailOrPhone.text.toString().trim()
        password = binding.password.text.toString()
        val user = UserModel(name, restaurentName, EmailorPhone, password)
        val userId : String = FirebaseAuth.getInstance().currentUser!!.uid
        database.child("users").child(userId).setValue(user)


    }
}

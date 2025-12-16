package com.example.adminfeastfast

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adminfeastfast.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // <--- THIS WAS MISSING
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount // Needed for your launcher
import com.google.firebase.auth.GoogleAuthProvider // Needed for your launcher
import androidx.activity.result.contract.ActivityResultContracts // Needed for registerForActivityResult
import android.app.Activity // Needed for Activity.RESULT_OK


class LoginActivity : AppCompatActivity() {

    // define variables
    private lateinit var emailOrPhone: String
    private lateinit var password: String
    private var name: String? = null
    private var restaurentName: String? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient


    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()


        // Initialize Firebase
        auth = Firebase.auth
        database = Firebase.database.reference
        // Pass the options variable you created
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)



        // Handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Login Button Logic
        binding.loginbutton.setOnClickListener {
            emailOrPhone = binding.emailId.text.toString().trim()
            password = binding.passWord.text.toString().trim()

            if (emailOrPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            } else {
                performLoginOrSignUp(emailOrPhone, password)
            }

        }
        binding.GoggleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }


        // Sign Up Button Logic
        binding.donthaveAccountButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    // Logic to Sign In, or Create Account if sign in fails (based on your logic)
    private fun performLoginOrSignUp(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    updateUi(user)
                } else {
                    // If sign in fails, try to create (based on your snippet logic)
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this) { createTask ->
                            if (createTask.isSuccessful) {
                                val user: FirebaseUser? = auth.currentUser
                                Toast.makeText(
                                    this,
                                    "Account Created Successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                updateUi(user)
                                finish()
                            } else {
                                Toast.makeText(this, "Account Creation Failed", Toast.LENGTH_SHORT)
                                    .show()
                                Log.d("Error", createTask.exception.toString())
                            }
                        }
                }
            }
    }

    private fun saveUserData() {
        val email = binding.emailId.text.toString().trim()
        val pass = binding.passWord.text.toString().trim()

        // Ensure you have a UserModel class created elsewhere in your project
        val userModel = UserModel(name, restaurentName, email, pass)

        val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            database.child("users").child(it).setValue(userModel)
        }
    }
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUi(currentUser)
    }

    private fun updateUi(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 1. Get the task object
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                // 2. Start the try block on a new line
                try {
                    // Add a try-catch block here to catch the specific ApiException
                    val account: GoogleSignInAccount = task.getResult(com.google.android.gms.common.api.ApiException::class.java)

                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Successfully Sign-In with Google", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: com.google.android.gms.common.api.ApiException) {
                    // THIS LOG WILL TELL YOU THE REAL REASON
                    Log.e("GoogleSignIn", "Google Sign-In failed code=" + e.statusCode)
                    Toast.makeText(this, "Google Sign-In Failed: " + e.statusCode, Toast.LENGTH_LONG).show()
                }
            }
        }

}

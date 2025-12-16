package com.example.adminfeastfast

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.adminfeastfast.databinding.ActivityAddItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddItemActivity : AppCompatActivity() {

    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredients: String
    private var foodImageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val binding: ActivityAddItemBinding by lazy {
        ActivityAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // pick image by clicking select image button
        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // also pick image when clicking the image view
        binding.selectedImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Add item button
        binding.AddItemButton.setOnClickListener {
            foodName = binding.foodName.text.toString().trim()
            foodPrice = binding.foodPrice.text.toString().trim()
            foodDescription = binding.description.text.toString().trim()
            foodIngredients = binding.ingredient.text.toString().trim()

            if (foodName.isEmpty() || foodPrice.isEmpty() ||
                foodDescription.isEmpty() || foodIngredients.isEmpty()) {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            } else if (foodImageUri == null) {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            } else {
                uploadImageToCloudinary()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        // window inset fix
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // STEP 1: pick image
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            foodImageUri = uri
            binding.selectedImage.setImageURI(uri)
        }
    }

    // STEP 2: upload to Cloudinary
    private fun uploadImageToCloudinary() {
        val requestId = MediaManager.get()
            .upload(foodImageUri)
            .unsigned("Default")   // <-- IMPORTANT: put your preset here
            .callback(object : UploadCallback {

                override fun onStart(requestId: String?) {
                    Toast.makeText(this@AddItemActivity, "Uploading image...", Toast.LENGTH_SHORT).show()
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<*, *>?) {
                    val imageUrl = resultData?.get("secure_url").toString()
                    uploadDataToFirebase(imageUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(this@AddItemActivity, "Upload failed: ${error?.description}", Toast.LENGTH_LONG).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}

            })
            .dispatch()
    }

    // STEP 3: store data in Firebase
    private fun uploadDataToFirebase(imageUrl: String) {
        val MenuRef = database.getReference("Menu")
        val MenuItemKey = MenuRef.push().key!!

        val newItem = AllMenu(
            foodName,
            foodPrice,
            foodDescription,
            foodIngredients,
            imageUrl
        )

        MenuRef.child(MenuItemKey)
            .setValue(newItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Item Added Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show()
            }
    }
}

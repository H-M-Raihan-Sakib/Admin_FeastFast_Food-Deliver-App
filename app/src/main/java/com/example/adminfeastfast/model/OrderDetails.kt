package com.example.adminfeastfast.model

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class OrderDetails(
    var userUid: String? = null,
    var userName: String? = null,

    // FIX: Map Firebase key "foodName" to the list variable
    @get:PropertyName("foodName") @set:PropertyName("foodName")
    var foodNames: ArrayList<String>? = null,

    // FIX: Map Firebase key "foodQuantity" to the list variable
    @get:PropertyName("foodQuantity") @set:PropertyName("foodQuantity")
    var foodQuantities: ArrayList<Int>? = null, // Changed to Int if database stores numbers

    // FIX: Map Firebase key "foodPrice" to the list variable
    @get:PropertyName("foodPrice") @set:PropertyName("foodPrice")
    var foodPrices: ArrayList<String>? = null,

    var address: String? = null,
    var totalPrice: String? = null,
    var phoneNumber: String? = null,
    var orderAccepted: Boolean = false,
    var paymentReceived: Boolean = false,
    var itemPushKey: String? = null,
    var currentTime: Long = 0,
    var orderDispatch: Boolean = false
) : Serializable

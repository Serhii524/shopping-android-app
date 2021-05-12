package com.vishalgaur.shoppingapp.data.source.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.source.UserDataSource
import com.vishalgaur.shoppingapp.data.utils.EmailMobileData
import kotlinx.coroutines.tasks.await

class AuthRemoteDataSource : UserDataSource {
	private val firebaseDb: FirebaseFirestore = Firebase.firestore

	private fun usersCollectionRef() = firebaseDb.collection(USERS_COLLECTION)
	private fun allEmailsMobilesRef() =
		firebaseDb.collection(USERS_COLLECTION).document(EMAIL_MOBILE_DOC)


	override suspend fun getUserById(userId: String): Result<UserData?> {
		val resRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		return if (!resRef.isEmpty) {
			Success(resRef.toObjects(UserData::class.java)[0])
		} else {
			Error(Exception("User Not Found!"))
		}
	}


	override suspend fun addUser(userData: UserData) {
		usersCollectionRef().add(userData.toHashMap())
			.addOnSuccessListener {
				Log.d(TAG, "Doc added")
			}
			.addOnFailureListener { e ->
				Log.d(TAG, "firestore error occurred: $e")
			}
	}

	override suspend fun getUserByMobile(phoneNumber: String): UserData =
		usersCollectionRef().whereEqualTo(USERS_MOBILE_FIELD, phoneNumber).get().await()
			.toObjects(UserData::class.java)[0]

	override suspend fun getAddressesByUserId(userId: String): Result<List<UserData.Address>?> {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		return if (!userRef.isEmpty) {
			val userData = userRef.documents[0].toObject(UserData::class.java)
			Success(userData!!.addresses)
		} else {
			Error(Exception("User Not Found!"))
		}
	}

	override suspend fun getUserByMobileAndPassword(
		mobile: String,
		password: String
	): MutableList<UserData> =
		usersCollectionRef().whereEqualTo(USERS_MOBILE_FIELD, mobile)
			.whereEqualTo(USERS_PWD_FIELD, password).get().await().toObjects(UserData::class.java)

	override suspend fun insertAddress(newAddress: UserData.Address, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			usersCollectionRef().document(docId)
				.update(USERS_ADDRESSES_FIELD, FieldValue.arrayUnion(newAddress.toHashMap()))
		}
	}

	override suspend fun updateAddress(newAddress: UserData.Address, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldAddressList =
				userRef.documents[0].toObject(UserData::class.java)?.addresses?.toMutableList()
			val idx = oldAddressList?.indexOfFirst { it.addressId == newAddress.addressId } ?: -1
			if (idx != -1) {
				oldAddressList?.set(idx, newAddress)
			}
			usersCollectionRef().document(docId)
				.update(USERS_ADDRESSES_FIELD, oldAddressList?.map { it.toHashMap() })
		}
	}

	override suspend fun deleteAddress(addressId: String, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldAddressList =
				userRef.documents[0].toObject(UserData::class.java)?.addresses?.toMutableList()
			val idx = oldAddressList?.indexOfFirst { it.addressId == addressId } ?: -1
			if (idx != -1) {
				oldAddressList?.removeAt(idx)
			}
			usersCollectionRef().document(docId)
				.update(USERS_ADDRESSES_FIELD, oldAddressList?.map { it.toHashMap() })
		}
	}

	override suspend fun insertCartItem(newItem: UserData.CartItem, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			usersCollectionRef().document(docId)
				.update(USERS_CART_FIELD, FieldValue.arrayUnion(newItem.toHashMap()))
		}
	}

	override suspend fun updateCartItem(item: UserData.CartItem, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldCart =
				userRef.documents[0].toObject(UserData::class.java)?.cart?.toMutableList()
			val idx = oldCart?.indexOfFirst { it.itemId == item.itemId } ?: -1
			if (idx != -1) {
				oldCart?.set(idx, item)
			}
			usersCollectionRef().document(docId)
				.update(USERS_CART_FIELD, oldCart?.map { it.toHashMap() })
		}
	}

	override suspend fun deleteCartItem(itemId: String, userId: String) {
		val userRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get().await()
		if (!userRef.isEmpty) {
			val docId = userRef.documents[0].id
			val oldCart =
				userRef.documents[0].toObject(UserData::class.java)?.cart?.toMutableList()
			val idx = oldCart?.indexOfFirst { it.itemId == itemId } ?: -1
			if (idx != -1) {
				oldCart?.removeAt(idx)
			}
			usersCollectionRef().document(docId)
				.update(USERS_CART_FIELD, oldCart?.map { it.toHashMap() })
		}
	}

	override fun updateEmailsAndMobiles(email: String, mobile: String) {
		allEmailsMobilesRef().update(EMAIL_MOBILE_EMAIL_FIELD, FieldValue.arrayUnion(email))
		allEmailsMobilesRef().update(EMAIL_MOBILE_MOB_FIELD, FieldValue.arrayUnion(mobile))
	}

	override suspend fun getEmailsAndMobiles() = allEmailsMobilesRef().get().await().toObject(
		EmailMobileData::class.java
	)

	companion object {
		private const val USERS_COLLECTION = "users"
		private const val USERS_ID_FIELD = "userId"
		private const val USERS_ADDRESSES_FIELD = "addresses"
		private const val USERS_CART_FIELD = "cart"
		private const val USERS_MOBILE_FIELD = "mobile"
		private const val USERS_PWD_FIELD = "password"
		private const val EMAIL_MOBILE_DOC = "emailAndMobiles"
		private const val EMAIL_MOBILE_EMAIL_FIELD = "emails"
		private const val EMAIL_MOBILE_MOB_FIELD = "mobiles"
		private const val TAG = "AuthRemoteDataSource"
	}
}
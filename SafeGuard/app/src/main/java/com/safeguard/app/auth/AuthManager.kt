package com.safeguard.app.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Simplified Firebase Authentication Manager
 */
class AuthManager(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "AuthManager"
    }
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val isSignedIn: Boolean
        get() = currentUser != null
    
    /**
     * Flow that emits auth state changes
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    /**
     * Sign in with Google ID Token (called after Google Sign-In intent)
     */
    suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            
            authResult.user?.let { user ->
                try {
                    createOrUpdateUserProfile(user)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update user profile", e)
                }
                Result.success(user)
            } ?: Result.failure(Exception("Sign-in succeeded but user is null"))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create or update user profile in Firestore
     */
    private suspend fun createOrUpdateUserProfile(user: FirebaseUser) {
        val userDoc = firestore.collection("users").document(user.uid)
        
        val userData = hashMapOf(
            "uid" to user.uid,
            "email" to user.email,
            "displayName" to user.displayName,
            "photoUrl" to user.photoUrl?.toString(),
            "lastSignIn" to System.currentTimeMillis()
        )
        
        userDoc.set(userData, com.google.firebase.firestore.SetOptions.merge()).await()
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            currentUser?.let { user ->
                try {
                    firestore.collection("users").document(user.uid).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete Firestore data", e)
                }
                user.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete account", e)
            Result.failure(e)
        }
    }
}

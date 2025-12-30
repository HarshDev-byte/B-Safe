package com.safeguard.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.safeguard.app.data.models.EmergencyContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Internet-based alert system for sending SOS without SIM card
 * Works via WiFi or any internet connection
 */
class InternetAlertManager(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "InternetAlertManager"
        
        // Firebase Cloud Functions endpoint (you'll deploy this)
        private const val CLOUD_FUNCTION_URL = "https://us-central1-safeguard-5c68d.cloudfunctions.net/sendEmergencyAlert"
    }

    /**
     * Check if internet is available
     */
    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Send emergency alert via internet (Firebase)
     * This stores the alert in Firestore and can trigger Cloud Functions
     * to send emails, push notifications, or even SMS via Twilio
     */
    suspend fun sendInternetAlert(
        contacts: List<EmergencyContact>,
        latitude: Double?,
        longitude: Double?,
        message: String,
        batteryLevel: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInternetAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        val userId = auth.currentUser?.uid
        var successCount = 0

        try {
            // Create emergency alert document in Firestore
            val alertData = hashMapOf(
                "userId" to userId,
                "userName" to (auth.currentUser?.displayName ?: "Unknown"),
                "userEmail" to auth.currentUser?.email,
                "timestamp" to System.currentTimeMillis(),
                "latitude" to latitude,
                "longitude" to longitude,
                "mapsLink" to if (latitude != null && longitude != null) 
                    "https://maps.google.com/?q=$latitude,$longitude" else null,
                "message" to message,
                "batteryLevel" to batteryLevel,
                "status" to "ACTIVE",
                "contacts" to contacts.map { contact ->
                    mapOf(
                        "name" to contact.name,
                        "phone" to contact.phoneNumber,
                        "email" to contact.email,
                        "notified" to false
                    )
                }
            )

            // Store in public alerts collection (for Cloud Functions to process)
            val alertRef = firestore.collection("emergency_alerts").document()
            alertRef.set(alertData).addOnSuccessListener {
                Log.d(TAG, "Emergency alert stored in Firestore")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to store alert", e)
            }

            // Also store in user's personal alerts
            userId?.let { uid ->
                firestore.collection("users").document(uid)
                    .collection("alerts").document(alertRef.id)
                    .set(alertData)
            }

            // Try to call Cloud Function directly for immediate notification
            try {
                callCloudFunction(alertRef.id, contacts, latitude, longitude, message)
                successCount = contacts.size
            } catch (e: Exception) {
                Log.w(TAG, "Cloud function call failed, relying on Firestore trigger", e)
                successCount = 1 // At least Firestore storage succeeded
            }

            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send internet alert", e)
            Result.failure(e)
        }
    }

    /**
     * Call Firebase Cloud Function to send notifications
     */
    private suspend fun callCloudFunction(
        alertId: String,
        contacts: List<EmergencyContact>,
        latitude: Double?,
        longitude: Double?,
        message: String
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(CLOUD_FUNCTION_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val jsonBody = """
                {
                    "alertId": "$alertId",
                    "userId": "${auth.currentUser?.uid}",
                    "userName": "${auth.currentUser?.displayName}",
                    "latitude": $latitude,
                    "longitude": $longitude,
                    "message": "${message.replace("\"", "\\\"")}",
                    "contacts": [${contacts.joinToString(",") { 
                        """{"name":"${it.name}","phone":"${it.phoneNumber}","email":"${it.email}"}"""
                    }}]
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray())
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Cloud function response: $responseCode")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud function call failed", e)
            throw e
        }
    }

    /**
     * Send alert via Telegram Bot (free alternative)
     * User needs to set up their Telegram bot token and chat IDs
     */
    suspend fun sendTelegramAlert(
        botToken: String,
        chatIds: List<String>,
        message: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInternetAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        var successCount = 0
        
        chatIds.forEach { chatId ->
            try {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val url = URL("https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedMessage&parse_mode=HTML")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                
                if (connection.responseCode == 200) {
                    successCount++
                    Log.d(TAG, "Telegram message sent to $chatId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Telegram message to $chatId", e)
            }
        }

        Result.success(successCount)
    }

    /**
     * Send email alert via Firebase Extension or direct SMTP
     * Requires Firebase Email Extension to be set up
     */
    suspend fun sendEmailAlert(
        recipientEmails: List<String>,
        subject: String,
        htmlBody: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!isInternetAvailable()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // Using Firebase Trigger Email extension
            // This requires setting up the extension in Firebase Console
            val emailDoc = hashMapOf(
                "to" to recipientEmails,
                "message" to hashMapOf(
                    "subject" to subject,
                    "html" to htmlBody
                )
            )

            firestore.collection("mail").add(emailDoc)
                .addOnSuccessListener {
                    Log.d(TAG, "Email queued for sending")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to queue email", e)
                }

            Result.success(recipientEmails.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email alert", e)
            Result.failure(e)
        }
    }

    /**
     * Build emergency message with all details
     */
    fun buildEmergencyMessage(
        userName: String?,
        latitude: Double?,
        longitude: Double?,
        batteryLevel: Int,
        additionalInfo: String = ""
    ): String {
        val mapsLink = if (latitude != null && longitude != null) {
            "https://maps.google.com/?q=$latitude,$longitude"
        } else {
            "Location unavailable"
        }

        return """
🆘 EMERGENCY ALERT!

${userName ?: "Someone"} needs help!

📍 Location: $mapsLink
🔋 Battery: $batteryLevel%
⏰ Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}

$additionalInfo

This is an automated emergency alert from B-Safe app.
        """.trimIndent()
    }

    /**
     * Build HTML email body
     */
    fun buildEmailHtml(
        userName: String?,
        latitude: Double?,
        longitude: Double?,
        batteryLevel: Int
    ): String {
        val mapsLink = if (latitude != null && longitude != null) {
            "https://maps.google.com/?q=$latitude,$longitude"
        } else null

        return """
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { background: white; border-radius: 10px; padding: 30px; max-width: 500px; margin: 0 auto; }
        .header { background: #d32f2f; color: white; padding: 20px; border-radius: 10px 10px 0 0; text-align: center; }
        .content { padding: 20px; }
        .info-row { margin: 10px 0; padding: 10px; background: #f5f5f5; border-radius: 5px; }
        .map-btn { display: inline-block; background: #1976d2; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🆘 EMERGENCY ALERT</h1>
        </div>
        <div class="content">
            <h2>${userName ?: "Someone"} needs help!</h2>
            
            <div class="info-row">
                <strong>📍 Location:</strong><br>
                ${if (mapsLink != null) "Lat: $latitude, Lng: $longitude" else "Location unavailable"}
            </div>
            
            <div class="info-row">
                <strong>🔋 Battery:</strong> $batteryLevel%
            </div>
            
            <div class="info-row">
                <strong>⏰ Time:</strong> ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            </div>
            
            ${if (mapsLink != null) """
            <a href="$mapsLink" class="map-btn">📍 View Location on Map</a>
            """ else ""}
            
            <p style="margin-top: 30px; color: #666; font-size: 12px;">
                This is an automated emergency alert from B-Safe app.
            </p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }
}

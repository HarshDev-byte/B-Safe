package com.safeguard.app.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Audio Evidence Manager - Records audio during SOS for evidence
 * Premium feature that can save lives by capturing audio evidence
 */
class AudioEvidenceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioEvidenceManager"
        private const val MAX_RECORDING_DURATION_MS = 30 * 60 * 1000L // 30 minutes max
        private const val CHUNK_DURATION_MS = 5 * 60 * 1000L // 5 minute chunks
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val recordingsDir: File by lazy {
        File(context.filesDir, "sos_recordings").apply {
            if (!exists()) mkdirs()
        }
    }
    
    data class RecordingInfo(
        val file: File,
        val startTime: Long,
        val duration: Long,
        val sosEventId: Long
    )
    
    private val recordings = mutableListOf<RecordingInfo>()
    
    fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start recording audio evidence
     */
    fun startRecording(sosEventId: Long): Boolean {
        if (!hasRecordingPermission()) {
            Log.w(TAG, "No recording permission")
            return false
        }
        
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return true
        }
        
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SOS_${sosEventId}_$timestamp.m4a"
            currentRecordingFile = File(recordingsDir, fileName)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentRecordingFile?.absolutePath)
                setMaxDuration(MAX_RECORDING_DURATION_MS.toInt())
                
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG, "Max recording duration reached")
                        stopRecording()
                    }
                }
                
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "Started recording: ${currentRecordingFile?.name}")
            
            // Track recording
            recordings.add(RecordingInfo(
                file = currentRecordingFile!!,
                startTime = System.currentTimeMillis(),
                duration = 0,
                sosEventId = sosEventId
            ))
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            false
        }
    }
    
    /**
     * Stop recording and save the file
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            val file = currentRecordingFile
            Log.d(TAG, "Stopped recording: ${file?.name}, size: ${file?.length()} bytes")
            
            currentRecordingFile = null
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanup()
            null
        }
    }
    
    /**
     * Get all recordings for an SOS event
     */
    fun getRecordingsForEvent(sosEventId: Long): List<File> {
        return recordingsDir.listFiles()
            ?.filter { it.name.contains("SOS_${sosEventId}_") }
            ?.sortedBy { it.lastModified() }
            ?: emptyList()
    }
    
    /**
     * Get all recordings
     */
    fun getAllRecordings(): List<File> {
        return recordingsDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
    
    /**
     * Delete old recordings (older than 30 days)
     */
    fun cleanupOldRecordings(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        recordingsDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
                Log.d(TAG, "Deleted old recording: ${file.name}")
            }
        }
    }
    
    /**
     * Get total storage used by recordings
     */
    fun getStorageUsed(): Long {
        return recordingsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    fun isCurrentlyRecording(): Boolean = isRecording
    
    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
        mediaRecorder = null
        isRecording = false
        currentRecordingFile = null
    }
    
    fun release() {
        recordingJob?.cancel()
        cleanup()
        scope.cancel()
    }
}

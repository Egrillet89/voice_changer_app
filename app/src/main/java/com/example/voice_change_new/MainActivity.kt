package com.example.voice_change_new

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioFile: File
    private val TAG = "VoiceChangerApp"
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())

    // Lista para mantener referencia a múltiples reproductores para efectos
    private val activeMediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Set the layout
            setContentView(R.layout.activity_main)
            // Initialize the audio file path
            audioFile = File(getExternalFilesDir(null), "recorded_audio.aac")
            // Set up button click listeners
            setupButtons()
            // Request necessary permissions
            requestAudioPermissions()
            Log.d(TAG, "MainActivity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        try {
            // Record button
            val btnRecord = findViewById<Button>(R.id.btn_record)
            btnRecord.setOnClickListener {
                if (!isRecording) {
                    startRecording()
                    btnRecord.text = "Detener Grabación"
                    isRecording = true
                } else {
                    stopRecording()
                    btnRecord.text = "Grabar"
                    isRecording = false
                }
            }
            // Play button
            findViewById<Button>(R.id.btn_play).setOnClickListener {
                playRecording(1.0f, 1.0f) // Normal playback
            }
            // Save button
            findViewById<Button>(R.id.btn_save).setOnClickListener {
                Toast.makeText(this, "Audio guardado en: ${audioFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
            // Effect buttons
            findViewById<Button>(R.id.btn_effect_robot).setOnClickListener {
                applyRobotEffect()
            }
            findViewById<Button>(R.id.btn_effect_squirrel).setOnClickListener {
                applySquirrelEffect()
            }
            findViewById<Button>(R.id.btn_effect_echo).setOnClickListener {
                applyEchoEffect()
            }
            findViewById<Button>(R.id.btn_effect_distortion).setOnClickListener {
                applyDistortionEffect()
            }
            findViewById<Button>(R.id.btn_effect_ghost).setOnClickListener {
                applyGhostEffect()
            }
            // Share button
            findViewById<Button>(R.id.btn_share).setOnClickListener {
                shareAudio()
            }
            Log.d(TAG, "Buttons set up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buttons: ${e.message}", e)
            Toast.makeText(this, "Error setting up UI: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startRecording() {
        try {
            // Stop any existing recording
            stopRecording()
            // Create a new MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100) // Alta calidad para mejores efectos
                setAudioEncodingBitRate(128000) // 128kbps para mejor calidad
                setOutputFile(audioFile.absolutePath)
                try {
                    prepare()
                    start()
                    Toast.makeText(this@MainActivity, "Grabando...", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Recording started")
                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing recorder: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error al iniciar grabación", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startRecording: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar grabación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                    Toast.makeText(this@MainActivity, "Grabación detenida", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping recorder: ${e.message}")
                    // Ignore error if not recording
                }
                release()
            }
            mediaRecorder = null
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopRecording: ${e.message}", e)
        }
    }

    private fun releaseAllPlayers() {
        // Liberar el reproductor principal
        mediaPlayer?.release()
        mediaPlayer = null

        // Liberar todos los reproductores activos
        activeMediaPlayers.forEach { player ->
            try {
                player.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing player: ${e.message}")
            }
        }
        activeMediaPlayers.clear()
    }

    private fun playRecording(speed: Float, pitch: Float) {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "No recording available to play")
                return
            }

            // Release any existing players
            releaseAllPlayers()

            // Create a new player
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(audioFile.absolutePath)
                    prepare()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val params = PlaybackParams()
                        params.speed = speed
                        params.pitch = pitch
                        playbackParams = params
                    }
                    start()

                    // Show effect info
                    val effectInfo = when {
                        speed == 1.0f && pitch == 1.0f -> "normal"
                        speed < 1.0f && pitch < 1.0f -> "lento y grave"
                        speed > 1.0f && pitch > 1.0f -> "rápido y agudo"
                        speed < 1.0f -> "lento"
                        speed > 1.0f -> "rápido"
                        pitch < 1.0f -> "grave"
                        pitch > 1.0f -> "agudo"
                        else -> "modificado"
                    }

                    Toast.makeText(this@MainActivity, "Reproduciendo: $effectInfo", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Playing recording at speed: $speed, pitch: $pitch")

                    // Reset UI when playback completes
                    setOnCompletionListener {
                        Log.d(TAG, "Playback completed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing recording: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error al reproducir", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in playRecording: ${e.message}", e)
            Toast.makeText(this, "Error al reproducir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyRobotEffect() {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(this, "Aplicando efecto robot...", Toast.LENGTH_SHORT).show()

            // Release any existing players
            releaseAllPlayers()

            // Robot effect: metallic sound with modulation
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Robot effect: normal speed but lower pitch for mechanical sound
                    val params = PlaybackParams()
                    params.speed = 1.0f
                    params.pitch = 0.6f
                    playbackParams = params
                }

                start()

                // Add robotic modulation effect
                var modulationCount = 0
                val modulationHandler = Handler(Looper.getMainLooper())
                val modulationRunnable = object : Runnable {
                    override fun run() {
                        if (isPlaying && modulationCount < 10) {
                            // Modulate volume to create robotic effect
                            if (modulationCount % 2 == 0) {
                                setVolume(1.0f, 0.5f)
                            } else {
                                setVolume(0.5f, 1.0f)
                            }
                            modulationCount++
                            modulationHandler.postDelayed(this, 150)
                        } else {
                            setVolume(1.0f, 1.0f)
                        }
                    }
                }

                modulationHandler.post(modulationRunnable)
            }

            Log.d(TAG, "Applied robot effect")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying robot effect: ${e.message}", e)
            Toast.makeText(this, "Error al aplicar efecto robot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySquirrelEffect() {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(this, "Aplicando efecto ardilla...", Toast.LENGTH_SHORT).show()

            // Release any existing players
            releaseAllPlayers()

            // Squirrel effect: faster and higher pitch
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Ardilla: rápido y agudo
                    val params = PlaybackParams()
                    params.speed = 1.5f
                    params.pitch = 2.0f
                    playbackParams = params
                }

                start()
            }

            Log.d(TAG, "Applied squirrel effect")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying squirrel effect: ${e.message}", e)
            Toast.makeText(this, "Error al aplicar efecto ardilla", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyEchoEffect() {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(this, "Aplicando eco...", Toast.LENGTH_SHORT).show()

            // Release any existing players
            releaseAllPlayers()

            // Create a new player with echo effect
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()

                // Add to active players list
                activeMediaPlayers.add(this)

                // Multiple echo simulation with decreasing volume
                for (i in 1..3) {
                    handler.postDelayed({
                        try {
                            if (!isFinishing) {
                                val echoPlayer = MediaPlayer().apply {
                                    setDataSource(audioFile.absolutePath)
                                    // Decreasing volume for each echo
                                    val volume = 0.7f / (i + 1)
                                    setVolume(volume, volume)
                                    prepare()
                                    start()

                                    // Add to active players list
                                    activeMediaPlayers.add(this)

                                    setOnCompletionListener {
                                        release()
                                        activeMediaPlayers.remove(this)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in echo effect: ${e.message}", e)
                        }
                    }, i * 300L) // Increasing delay for each echo
                }
            }

            Log.d(TAG, "Applied echo effect")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying echo effect: ${e.message}", e)
            Toast.makeText(this, "Error al aplicar eco", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyDistortionEffect() {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(this, "Aplicando distorsión...", Toast.LENGTH_SHORT).show()

            // Release any existing players
            releaseAllPlayers()

            // Create a new player with distortion effect
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()

                // Distortion effect using pitch
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = PlaybackParams()
                    params.speed = 1.0f
                    params.pitch = 1.8f
                    playbackParams = params
                }

                start()

                // Add distortion by rapidly changing volume
                val distortionHandler = Handler(Looper.getMainLooper())
                val distortionRunnable = object : Runnable {
                    override fun run() {
                        if (isPlaying) {
                            // Random volume between 0.4 and 1.0 to create distortion
                            val randomVolume = 0.4f + (Math.random() * 0.6f).toFloat()
                            setVolume(randomVolume, randomVolume)
                            distortionHandler.postDelayed(this, 50) // Fast modulation
                        } else {
                            setVolume(1.0f, 1.0f)
                        }
                    }
                }

                distortionHandler.post(distortionRunnable)

                setOnCompletionListener {
                    setVolume(1.0f, 1.0f)
                }
            }

            Log.d(TAG, "Applied distortion effect")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying distortion effect: ${e.message}", e)
            Toast.makeText(this, "Error al aplicar distorsión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyGhostEffect() {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(this, "Aplicando voz fantasma...", Toast.LENGTH_SHORT).show()

            // Release any existing players
            releaseAllPlayers()

            // Create a new player with ghost effect
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()

                // Ghost effect using slower speed and lower pitch
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = PlaybackParams()
                    params.speed = 0.8f
                    params.pitch = 0.7f
                    playbackParams = params
                }

                start()

                // Add ghost reverb effect
                handler.postDelayed({
                    try {
                        if (!isFinishing) {
                            val reverbPlayer = MediaPlayer().apply {
                                setDataSource(audioFile.absolutePath)
                                setVolume(0.3f, 0.3f) // Low volume for ghost reverb
                                prepare()

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val params = PlaybackParams()
                                    params.speed = 0.75f
                                    params.pitch = 0.65f
                                    playbackParams = params
                                }

                                start()

                                // Add to active players list
                                activeMediaPlayers.add(this)

                                setOnCompletionListener {
                                    release()
                                    activeMediaPlayers.remove(this)
                                }
                            }

                            // Add a second, even more delayed and quieter reverb
                            handler.postDelayed({
                                try {
                                    if (!isFinishing) {
                                        val secondReverbPlayer = MediaPlayer().apply {
                                            setDataSource(audioFile.absolutePath)
                                            setVolume(0.15f, 0.15f) // Very low volume
                                            prepare()

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                val params = PlaybackParams()
                                                params.speed = 0.7f
                                                params.pitch = 0.6f
                                                playbackParams = params
                                            }

                                            start()

                                            // Add to active players list
                                            activeMediaPlayers.add(this)

                                            setOnCompletionListener {
                                                release()
                                                activeMediaPlayers.remove(this)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in second ghost reverb: ${e.message}", e)
                                }
                            }, 800) // 800ms delay for second reverb
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in ghost reverb: ${e.message}", e)
                    }
                }, 400) // 400ms delay for first reverb
            }

            Log.d(TAG, "Applied ghost effect")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying ghost effect: ${e.message}", e)
            Toast.makeText(this, "Error al aplicar efecto fantasma", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAudio() {
        try {
            if (!audioFile.exists()) {
                Toast.makeText(this, "No hay grabación disponible", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                audioFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/aac"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Audio con efectos de voz")
                putExtra(Intent.EXTRA_TEXT, "Te envío mi grabación con efectos de voz 🎙️")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Compartir audio por"))
            Log.d(TAG, "Sharing audio file")
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing audio: ${e.message}", e)
            Toast.makeText(this, "Error al compartir audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de grabación concedido", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Recording permission granted")
            } else {
                Toast.makeText(this, "Se requiere permiso de grabación para usar esta app", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Recording permission denied")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopRecording()
            findViewById<Button>(R.id.btn_record).text = "Grabar"
            isRecording = false
        }
        mediaPlayer?.pause()
        Log.d(TAG, "onPause called")
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
            findViewById<Button>(R.id.btn_record).text = "Grabar"
            isRecording = false
        }
        releaseAllPlayers()
        Log.d(TAG, "onStop called, resources released")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        releaseAllPlayers()
        Log.d(TAG, "onDestroy called, resources released")
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }
}


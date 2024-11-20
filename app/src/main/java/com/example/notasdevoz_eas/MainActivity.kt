package com.example.notasdevoz_eas

import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import android.Manifest
import android.content.ContentValues
import android.widget.EditText
import android.widget.Toast
import com.example.cuestionario_20.SQLiteHelper
import java.io.File

class MainActivity : AppCompatActivity() {
    // Declaración de variables para MediaRecorder y MediaPlayer
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String = ""

    // Referencias a los elementos de la interfaz
    private lateinit var grabarButton: Button
    private lateinit var detenerGrabarButton: Button
    private lateinit var reproducirButton: Button
    private lateinit var estadoTextView: TextView
    private lateinit var nombreGrabacion: EditText
    private lateinit var borrarGrabacion : Button
    private lateinit var nombreUsuario : TextView
    private lateinit var nombreNotaTextView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa las referencias a los botones y el TextView
        grabarButton = findViewById(R.id.button)
        detenerGrabarButton = findViewById(R.id.button2)
        reproducirButton = findViewById(R.id.button3)
        estadoTextView = findViewById(R.id.tv1)
        nombreNotaTextView = findViewById(R.id.nombreNota)
        nombreGrabacion = findViewById(R.id.nombreGrab)
        borrarGrabacion = findViewById(R.id.button4)
        nombreUsuario = findViewById(R.id.user)

        val bundle = intent.extras
        if (bundle != null) {
            val usuario = bundle.getString("usuario")
            nombreUsuario.text = "$usuario"
        }

        // Configura la visibilidad de los botones iniciales
        detenerGrabarButton.isEnabled = false

        // Verifica los permisos necesarios
        solicitarPermisos()

        // Evento onClick para el botón "Grabar"
        grabarButton.setOnClickListener {
            iniciarGrabacion(nombreGrabacion)
        }
        // Evento onClick para el botón "Detener Grabación"
        detenerGrabarButton.setOnClickListener {
            detenerGrabacion()
        }
        // Evento onClick para el botón "Reproducir Grabación"
        reproducirButton.setOnClickListener {
            reproducirGrabacion(nombreGrabacion)
        }

        borrarGrabacion.setOnClickListener {
            borrarGrabacion(nombreGrabacion)
        }
    }

    fun mostrarToast(texto : String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }

    private fun generarNombreArchivo(usuario: String, nombreNota: String): String {
        return "${externalCacheDir?.absolutePath}/${usuario}${nombreNota}.3gp"
    }

    // Función para verificar y solicitar permisos
    private fun solicitarPermisos() {
        val permisos = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        // Solicita permisos si no están otorgados
        if (!permisos.all {
                ContextCompat.checkSelfPermission(this, it) ==
                        PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, permisos, 0)
        }
    }

    // Función para iniciar la grabación de audio
    private fun iniciarGrabacion(nombreGrab: EditText) {
        val usuario = nombreUsuario.text.toString()
        if (nombreGrab.text.isEmpty()) {
            mostrarToast("Introduzca el nombre de la nota de voz.")
            return
        }
        val nombreNota = nombreGrab.text.toString()
        audioFilePath = generarNombreArchivo(usuario, nombreNota)

        val admin = SQLiteHelper(this, "admin", null, 1)
        val bd = admin.writableDatabase
        val filas = bd.rawQuery("select * from UsuariosNotas where usuario = '$usuario' and nombreNota = '$nombreNota'", null)
        if (filas.moveToFirst()) {
            mostrarToast("Ya existe una nota de voz con ese nombre.")
            return
        }
        val registroNota = ContentValues().apply {
            put("usuario", usuario)
            put("nombreNota", nombreNota)
        }
        bd.insert("UsuariosNotas", null, registroNota)
        bd.close()

        //iniciamos el mediaRecorder
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) // Fuente de audio
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // Formato de salida
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // Codificador de audio
            setOutputFile(audioFilePath) // Ruta del archivo de salida

            try {
                prepare() // Prepara el MediaRecorder
                start() // Inicia la grabación
                nombreNotaTextView.text = "Nota de voz: $nombreNota"
                estadoTextView.text = "Grabando..."
                grabarButton.isEnabled = false
                detenerGrabarButton.isEnabled = true
                reproducirButton.isEnabled = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Función para detener la grabación de audio
    private fun detenerGrabacion() {
        mediaRecorder?.apply {
            stop() // Detiene la grabación
            release() // Libera los recursos
        }
        mediaRecorder = null
        nombreNotaTextView.text = "Nombre nota de voz"
        estadoTextView.text = "Grabación detenida y guardada."
        grabarButton.isEnabled = true
        detenerGrabarButton.isEnabled = false
        reproducirButton.isEnabled = true
    }

    // Función para reproducir la grabación de audio
    private fun reproducirGrabacion(nombreGrab: EditText) {
        val usuario = nombreUsuario.text.toString()
        if (nombreGrab.text.isEmpty()) {
            mostrarToast("Introduzca el nombre de la nota de voz.")
            return
        }
        val nombreNota = nombreGrab.text.toString()

        val admin = SQLiteHelper(this, "admin", null, 1)
        val bd = admin.writableDatabase
        val filas = bd.rawQuery("select * from UsuariosNotas where usuario = '$usuario' and nombreNota = '$nombreNota'", null)
        if (!filas.moveToFirst()) {
            mostrarToast("No existe una nota de voz con ese nombre.")
            return
        }
        audioFilePath = generarNombreArchivo(usuario, nombreNota)

        mediaPlayer = MediaPlayer().apply {

            try {
                setDataSource(audioFilePath) // Configura la fuente de datos (archivo de audio)
                prepare() // Prepara el MediaPlayer
                start() // Inicia la reproducción
                nombreNotaTextView.text = "Nota de voz: $nombreNota"
                estadoTextView.text = "Reproduciendo..."
                // Cambia el estado del TextView cuando finaliza la reproducción
                setOnCompletionListener {
                    estadoTextView.text = "Reproducción finalizada."
                    nombreNotaTextView.text = "Nombre nota de voz"
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun borrarGrabacion(nombreGrab: EditText) {
        val usuario = nombreUsuario.text.toString()
        if (nombreGrab.text.isEmpty()) {
            mostrarToast("Introduzca el nombre de la nota de voz.")
            return
        }
        val nombreNota = nombreGrab.text.toString()
        audioFilePath = generarNombreArchivo(usuario, nombreNota)

        val admin = SQLiteHelper(this, "admin", null, 1)
        val bd = admin.writableDatabase
        val filas = bd.rawQuery("select * from UsuariosNotas where usuario = '$usuario' and nombreNota = '$nombreNota'", null)
        if (!filas.moveToFirst()) {
            mostrarToast("No existe una nota de voz con ese nombre.")
            return
        }

        bd.delete("UsuariosNotas", "usuario = '$usuario' and nombreNota = '$nombreNota'", null)
        bd.close()

        val archivoAudio = File(audioFilePath)
        if (archivoAudio.exists()) {
            // Intentar borrar el archivo
            val eliminado = archivoAudio.delete()
            if (eliminado) {
                estadoTextView.text = "Nota de voz eliminada."
            } else {
                estadoTextView.text = "Error al eliminar la nota de voz."
            }
        }
    }
        // Función para liberar recursos cuando la actividad se destruye
        override fun onDestroy() {
            super.onDestroy()
            mediaRecorder?.release()
            mediaPlayer?.release()
        }
    }
package com.example.notasdevoz_eas

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cuestionario_20.SQLiteHelper

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraint)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textoUser = findViewById<TextView>(R.id.escUsuario)
        val textoPassword = findViewById<TextView>(R.id.escPassword)
        val registro = findViewById<Button>(R.id.registro)
        val acceder = findViewById<Button>(R.id.acceder)
        val admin = SQLiteHelper(this, "admin", null, 1)
        val actividad = Intent(this, MainActivity::class.java)

        acceder.setOnClickListener {
            if (accederCuenta(textoUser.text.toString(), textoPassword.text.toString(), admin)) {
                actividad.putExtra("usuario", textoUser.text.toString())
                textoUser.setText("")
                textoPassword.setText("")
                startActivity(actividad)
            }
            textoUser.setText("")
            textoPassword.setText("")
        }

        registro.setOnClickListener {
            if (crearCuenta(textoUser.text.toString(), textoPassword.text.toString(), admin)) {
                actividad.putExtra("usuario", textoUser.text.toString())
                textoUser.setText("")
                textoPassword.setText("")
                startActivity(actividad)
            }
            textoUser.setText("")
            textoPassword.setText("")
        }
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    fun accederCuenta(usuario: String, password: String, admin: SQLiteHelper): Boolean {
        val bd = admin.writableDatabase
        if (usuario.isNotBlank() && password.isNotBlank()) {
            val comprobarUser =
                bd.rawQuery("SELECT usuario, password FROM Usuarios WHERE usuario='${usuario}'", null)
            return try {
                if (comprobarUser.moveToFirst()) {
                    val passwordBase = comprobarUser.getString(1)
                    if (passwordBase == password) {
                        mostrarToast("Acceso exitoso")
                        true;
                    } else {
                        mostrarToast("Usuario o contraseña incorrecto")
                        false;
                    }
                } else {
                    mostrarToast("Usuario o contraseña incorrecto")
                    false;
                }
            } finally {
                comprobarUser.close()
                bd.close()
            }
        } else {
            mostrarToast("Usuario y/o contraseña no pueden estar vacio")
            return false;
        }
    }

    fun crearCuenta(usuario: String, password: String, admin: SQLiteHelper): Boolean {
        val bd = admin.writableDatabase
        if (usuario.isNotBlank() && password.isNotBlank()) {
            val comprobarUser =
                bd.rawQuery("SELECT usuario, password FROM Usuarios WHERE usuario='${usuario}'", null)
            try {
                if (comprobarUser.moveToFirst()) {
                    mostrarToast("El usuario ya existe")
                    return false
                } else {
                    val usuarioNuevo = usuario
                    val passwordNueva = password
                    val registrar = ContentValues()
                    registrar.put("usuario", usuarioNuevo)
                    registrar.put("password", passwordNueva)
                    bd.insert("Usuarios", null, registrar)
                    bd.close()
                    mostrarToast("Registro existoso")
                    return true
                }
            } finally {
                comprobarUser.close()
                bd.close()
            }
        } else {
            mostrarToast("Usuario y/o contraseña no pueden estar vacio")
            return false;
        }
    }
}
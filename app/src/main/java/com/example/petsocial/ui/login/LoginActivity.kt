package com.example.petsocial.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petsocial.R
import com.example.petsocial.data.BaseDatosAux
import com.example.petsocial.ui.main.MainActivity

open class LoginActivity : AppCompatActivity() {
    private lateinit var baseDatosLocal: BaseDatosAux
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        baseDatosLocal = BaseDatosAux(this)

        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val userId = findViewById<EditText>(R.id.userId)
        val password = findViewById<EditText>(R.id.password)

        loginButton.setOnClickListener {
            val idValue = userId.text.toString().trim()
            val passwordValue = password.text.toString().trim()

            if (idValue.isEmpty() || passwordValue.isEmpty()){
                Toast.makeText(this, "Debe Diligenciar todos los Campos.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(idValue).matches()){
                Toast.makeText(this, "El correo ingresado NO es válido.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (baseDatosLocal.buscarUsuario(idValue, passwordValue)){
                Toast.makeText(this, "Inicio de Sesión Exitoso!", Toast.LENGTH_LONG).show()
                // Abrir la siguiente activity del menú principal
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // <- esto cierra LoginActivity para que no regrese con "back"
            }else{
                Toast.makeText(this, "Usuario o contraseña incorrectos.", Toast.LENGTH_LONG).show()
            }
        }

        registerButton.setOnClickListener {
            val intentRegisterActivity = Intent(this, RegisterActivity::class.java)
            startActivity(intentRegisterActivity)
        }
    }
}
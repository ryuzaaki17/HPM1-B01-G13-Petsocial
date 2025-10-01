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

class RegisterActivity : AppCompatActivity() {
    private lateinit var baseDatosLocal: BaseDatosAux

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        baseDatosLocal = BaseDatosAux(this)
        val backButton = findViewById<Button>(R.id.backButton)
        val registerConfirmButton = findViewById<Button>(R.id.registerConfirmButton)
        val nameUser = findViewById<EditText>(R.id.userName)
        val userEmail = findViewById<EditText>(R.id.userEmail)
        val userPassword = findViewById<EditText>(R.id.userPassword)
        val userConfirmPassword = findViewById<EditText> (R.id.userConfirmPassword)

        registerConfirmButton.setOnClickListener {
            val nameValue = nameUser.text.toString().trim()
            val emailValue = userEmail.text.toString().trim()
            val passwordValue = userPassword.text.toString().trim()
            val confirmValue = userConfirmPassword.text.toString().trim()

            if (nameValue.isEmpty() || emailValue.isEmpty() || passwordValue.isEmpty() || confirmValue.isEmpty()){
                Toast.makeText(this, "Debe Diligenciar todos los Campos.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()){
                Toast.makeText(this, "El correo ingresado NO es válido.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (passwordValue != confirmValue){
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            baseDatosLocal.registrarUsuario(nameValue, emailValue, passwordValue, this)
            val successRegisterIntent = Intent(this, LoginActivity::class.java)
            startActivity(successRegisterIntent)
            finish()
        }

        backButton.setOnClickListener {
            val backIntent = Intent(this, LoginActivity::class.java)
            startActivity(backIntent)
            finish()
        }
    }
}
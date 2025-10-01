package com.example.petsocial.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class BaseDatosAux(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object{
        private const val DATABASE_NAME = "InicioAux"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USERS = "usuarios"
        private const val COLUMN_ID = "idUsuario"
        private const val COLUMN_NAME = "nombre"
        private const val COLUMN_EMAIL = "correo"
        private const val COLUMN_PASSWORD = "contrasena"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Función para registrar un usuario
    fun registrarUsuario(nombreUsuario: String, emailUsuario: String, contrasenaUsuario: String, context: Context): Boolean {
        val db = this.writableDatabase
        val datos = ContentValues().apply {
            put(COLUMN_NAME, nombreUsuario)
            put(COLUMN_EMAIL, emailUsuario)
            put(COLUMN_PASSWORD, contrasenaUsuario)
        }

        val result = db.insert(TABLE_USERS, null, datos)
        db.close()

        return if (result == -1L) {
            Toast.makeText(context, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
            false
        } else {
            Toast.makeText(context, "Registro Exitoso!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    // Función para buscar usuario registrado
    fun buscarUsuario(emailIngreso: String, contrasenaIngreso: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(emailIngreso, contrasenaIngreso))

        val exists = cursor.count > 0
        cursor.close()
        db.close()

        return exists
    }


}

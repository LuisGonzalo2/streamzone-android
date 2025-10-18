package com.universidad.streamzone

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseUsersActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var tvTitle: TextView

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_list)

        tvTitle = findViewById(R.id.tv_title)
        listView = findViewById(R.id.list_users)
        tvTitle.text = "Usuarios (Firebase)"

        // Asegurar inicialización de Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // ya inicializado o fallo
        }

        ensureSignedInAndFetch()
    }

    private fun ensureSignedInAndFetch() {
        val user = auth.currentUser
        if (user != null) {
            fetchUsersFromFirestore()
            return
        }

        // Intentar iniciar sesión anónima para poder leer según reglas que permitan authenticated reads
        auth.signInAnonymously()
            .addOnSuccessListener {
                fetchUsersFromFirestore()
            }
            .addOnFailureListener { e ->
                // Si falla la autenticación, mostrar el error que suele indicar permisos
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Error al autenticar: ${e.message}"))
                listView.adapter = adapter
                Toast.makeText(this, "No se pudo autenticar contra Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchUsersFromFirestore() {
        // Suponemos que los usuarios están en la colección "users" con campos fullName y email
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<String>()
                for (doc in result) {
                    val name = doc.getString("fullName") ?: "(sin nombre)"
                    val email = doc.getString("email") ?: "(sin email)"
                    list.add("$name — $email")
                }
                if (list.isEmpty()) list.add("No se encontraron usuarios en Firebase")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
                listView.adapter = adapter
            }
            .addOnFailureListener { e ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Error al leer Firebase: ${e.message}"))
                listView.adapter = adapter
            }
    }
}

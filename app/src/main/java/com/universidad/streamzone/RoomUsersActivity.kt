package com.universidad.streamzone

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RoomUsersActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_list)

        tvTitle = findViewById(R.id.tv_title)
        listView = findViewById(R.id.list_users)
        tvTitle.text = "Usuarios (local)"

        try {
            val db = AppDatabase.getInstance(this)
            val users = db.userDao().getAll()
            val list = if (users.isEmpty()) listOf("No hay usuarios locales") else users.map { "${it.fullName} — ${it.email}" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
            listView.adapter = adapter
        } catch (e: Exception) {
            e.printStackTrace()
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Error leyendo la DB local: ${e.message}"))
            listView.adapter = adapter
        }
    }
}


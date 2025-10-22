package com.universidad.streamzone


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView (R.layout.activity_home)



        val btnVerRegistros =
            findViewById<Button>(R.id.btnVerRegistros)


        btnVerRegistros . setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ListaUsuariosActivity::class.java
                )
            )
        }

    }
}
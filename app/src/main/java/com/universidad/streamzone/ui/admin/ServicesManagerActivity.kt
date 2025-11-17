package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import com.universidad.streamzone.R
import com.universidad.streamzone.util.PermissionManager

class ServicesManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_SERVICES

    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_message)
    }

    override fun onPermissionGranted() {
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        Toast.makeText(this, "✅ Gestión de Servicios - Funcional", Toast.LENGTH_SHORT).show()
    }
}
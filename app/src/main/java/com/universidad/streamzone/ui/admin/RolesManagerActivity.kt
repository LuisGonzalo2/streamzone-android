package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import com.universidad.streamzone.R
import com.universidad.streamzone.util.PermissionManager

/**
 * Menú principal de gestión de roles
 * Permite acceder a:
 * - Ver/Crear/Editar/Eliminar roles
 * - Asignar roles a usuarios
 */
class RolesManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_ROLES

    private lateinit var btnBack: ImageButton
    private lateinit var cardManageRoles: CardView
    private lateinit var cardAssignRoles: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roles_manager)
    }

    override fun onPermissionGranted() {
        initViews()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        cardManageRoles = findViewById(R.id.cardManageRoles)
        cardAssignRoles = findViewById(R.id.cardAssignRoles)

        btnBack.setOnClickListener { finish() }

        // Opción 1: Ver y gestionar roles (crear, editar, eliminar)
        cardManageRoles.setOnClickListener {
            val intent = Intent(this, RoleListActivity::class.java)
            startActivity(intent)
        }

        // Opción 2: Asignar roles a usuarios
        cardAssignRoles.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("SELECT_FOR_ROLES", true)
            startActivity(intent)
        }
    }
}
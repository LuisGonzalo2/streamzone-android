package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import com.universidad.streamzone.R

class AdminMenuActivity : BaseAdminActivity() {

    // AdminMenuActivity solo requiere ser admin (no permiso específico)
    override val requiredPermission: String? = null

    private lateinit var btnBack: ImageButton
    private lateinit var cardManagePurchases: CardView
    private lateinit var cardManageOffers: CardView
    private lateinit var cardManageUsers: CardView
    private lateinit var cardManageRoles: CardView
    private lateinit var cardManageServices: CardView
    private lateinit var cardManageCategories: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        cardManagePurchases = findViewById(R.id.cardManagePurchases)
        cardManageOffers = findViewById(R.id.cardManageOffers)
        cardManageUsers = findViewById(R.id.cardManageUsers)
        cardManageRoles = findViewById(R.id.cardManageRoles)
        cardManageServices = findViewById(R.id.cardManageServices)
        cardManageCategories = findViewById(R.id.cardManageCategories)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        cardManagePurchases.setOnClickListener {
            val intent = Intent(this, PendingPurchasesActivity::class.java)
            startActivity(intent)
        }

        cardManageOffers.setOnClickListener {
            val intent = Intent(this, OffersManagerActivity::class.java)
            startActivity(intent)
        }

        cardManageUsers.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            startActivity(intent)
        }

        cardManageRoles.setOnClickListener {
            val intent = Intent(this, RolesManagerActivity::class.java)
            startActivity(intent)
        }

        cardManageServices.setOnClickListener {
            val intent = Intent(this, ServicesManagerActivity::class.java)
            startActivity(intent)
        }

        cardManageCategories.setOnClickListener {
            val intent = Intent(this, CategoriesManagerActivity::class.java)
            startActivity(intent)
        }
    }
}
package com.universidad.streamzone.ui.purchases

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.profile.adapter.PurchaseCardAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PurchaseHistoryActivity : AppCompatActivity() {

    private lateinit var navbarManager: NavbarManager
    private lateinit var rvPurchases: RecyclerView
    private lateinit var emptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_history)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Aplicar padding superior al contenedor principal
        val mainContainer = findViewById<View>(R.id.purchase_history_main_container)
        mainContainer?.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(
                view.paddingLeft,
                systemBars.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Configurar navbar
        navbarManager = NavbarManager(this, NavbarManager.Screen.PURCHASES)

        initViews()
        loadPurchases()
    }

    private fun initViews() {
        rvPurchases = findViewById(R.id.rv_all_purchases)
        emptyState = findViewById(R.id.empty_state)
    }

    private fun loadPurchases() {
        val sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            showEmptyState()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@PurchaseHistoryActivity).purchaseDao()

                dao.obtenerComprasPorUsuario(userEmail).collectLatest { purchases ->
                    runOnUiThread {
                        if (purchases.isEmpty()) {
                            showEmptyState()
                        } else {
                            showPurchases(purchases)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PurchaseHistory", "Error al cargar compras", e)
                runOnUiThread {
                    showEmptyState()
                }
            }
        }
    }

    private fun showPurchases(purchases: List<com.universidad.streamzone.data.model.PurchaseEntity>) {
        rvPurchases.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        rvPurchases.layoutManager = LinearLayoutManager(this)
        val adapter = PurchaseCardAdapter(purchases) { purchase ->
            Toast.makeText(this, "Compra: ${purchase.serviceName}", Toast.LENGTH_SHORT).show()
        }
        rvPurchases.adapter = adapter
    }

    private fun showEmptyState() {
        rvPurchases.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}
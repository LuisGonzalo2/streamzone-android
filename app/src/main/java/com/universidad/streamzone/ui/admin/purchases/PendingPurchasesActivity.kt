package com.universidad.streamzone.ui.admin.purchases

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.ui.admin.purchases.adapter.AdminPurchaseAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PendingPurchasesActivity : AppCompatActivity() {

    private lateinit var rvPendingPurchases: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: MaterialButton
    private lateinit var adapter: AdminPurchaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_purchases)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val mainContainer = findViewById<View>(R.id.pending_purchases_container)
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

        initViews()
        setupRecyclerView()
        loadPendingPurchases()
    }

    private fun initViews() {
        rvPendingPurchases = findViewById(R.id.rv_pending_purchases)
        emptyState = findViewById(R.id.empty_state_pending)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminPurchaseAdapter { purchase ->
            showAssignCredentialsDialog(purchase)
        }

        rvPendingPurchases.layoutManager = LinearLayoutManager(this)
        rvPendingPurchases.adapter = adapter
    }

    private fun loadPendingPurchases() {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@PendingPurchasesActivity).purchaseDao()

                dao.obtenerComprasPendientes().collectLatest { purchases ->
                    runOnUiThread {
                        if (purchases.isEmpty()) {
                            showEmptyState()
                        } else {
                            showPurchases(purchases)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PendingPurchases", "Error al cargar compras pendientes", e)
                runOnUiThread {
                    showEmptyState()
                    Toast.makeText(
                        this@PendingPurchasesActivity,
                        "Error al cargar compras: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showPurchases(purchases: List<PurchaseEntity>) {
        rvPendingPurchases.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        adapter.submitList(purchases)
    }

    private fun showEmptyState() {
        rvPendingPurchases.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun showAssignCredentialsDialog(purchase: PurchaseEntity) {
        val dialog = AssignCredentialsDialogFragment.newInstance(purchase.id)
        dialog.show(supportFragmentManager, "assignCredentials")
    }
}
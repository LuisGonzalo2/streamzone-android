package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.data.remote.FirebaseService
import com.universidad.streamzone.ui.admin.adapter.AdminPurchaseAdapter
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

class PendingPurchasesActivity : BaseAdminActivity() {

    // Requiere permiso de gestionar compras
    override val requiredPermission: String = PermissionManager.MANAGE_PURCHASES

    private lateinit var btnBack: ImageButton
    private lateinit var tvPendingCount: TextView
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterActive: Button
    private lateinit var btnFilterAll: Button
    private lateinit var rvPurchases: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var purchaseAdapter: AdminPurchaseAdapter

    private var currentFilter = "pending"
    private var purchasesListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "PendingPurchases"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_purchases)

        // BaseAdminActivity ya valida permisos
        // Solo inicializamos si se llama a onPermissionGranted
    }

    override fun onPermissionGranted() {
        initViews()
        setupRecyclerView()
        setupFilterButtons()
        loadPurchases()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnFilterActive = findViewById(R.id.btnFilterActive)
        btnFilterAll = findViewById(R.id.btnFilterAll)
        rvPurchases = findViewById(R.id.rvPurchases)
        llEmptyState = findViewById(R.id.llEmptyState)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        purchaseAdapter = AdminPurchaseAdapter(
            purchases = emptyList(),
            onAssignCredentials = { purchase ->
                showAssignCredentialsDialog(purchase)
            }
        )

        rvPurchases.layoutManager = LinearLayoutManager(this)
        rvPurchases.adapter = purchaseAdapter
    }

    private fun setupFilterButtons() {
        btnFilterPending.setOnClickListener {
            currentFilter = "pending"
            updateFilterButtons()
            loadPurchases()
        }

        btnFilterActive.setOnClickListener {
            currentFilter = "active"
            updateFilterButtons()
            loadPurchases()
        }

        btnFilterAll.setOnClickListener {
            currentFilter = "all"
            updateFilterButtons()
            loadPurchases()
        }
    }

    private fun updateFilterButtons() {
        // Reset todos
        btnFilterPending.setBackgroundColor(getColor(R.color.bg_card))
        btnFilterActive.setBackgroundColor(getColor(R.color.bg_card))
        btnFilterAll.setBackgroundColor(getColor(R.color.bg_card))

        // Activar el seleccionado
        when (currentFilter) {
            "pending" -> btnFilterPending.setBackgroundColor(0xFFF59E0B.toInt())
            "active" -> btnFilterActive.setBackgroundColor(0xFF10B981.toInt())
            "all" -> btnFilterAll.setBackgroundColor(0xFF6366F1.toInt())
        }
    }

    private fun loadPurchases() {
        Log.d(TAG, "Iniciando listener de compras en tiempo real")

        // Limpiar listener anterior si existe
        purchasesListener?.remove()

        // Iniciar listener en tiempo real de Firebase
        purchasesListener = FirebaseService.escucharTodasLasCompras { purchasesFromFirebase ->
            Log.d(TAG, "Compras recibidas de Firebase: ${purchasesFromFirebase.size}")

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@PendingPurchasesActivity)
                    val purchaseDao = db.purchaseDao()

                    // Sincronizar compras de Firebase a Room
                    purchasesFromFirebase.forEach { firebasePurchase ->
                        // Buscar si ya existe en Room por firebaseId
                        val existingPurchase = purchaseDao.getAll()
                            .find { it.firebaseId == firebasePurchase.firebaseId }

                        if (existingPurchase != null) {
                            // Actualizar compra existente
                            val updated = existingPurchase.copy(
                                email = firebasePurchase.email,
                                password = firebasePurchase.password,
                                status = firebasePurchase.status,
                                sincronizado = true
                            )
                            purchaseDao.update(updated)
                        } else {
                            // Insertar nueva compra
                            purchaseDao.insertar(firebasePurchase)
                        }
                    }

                    // Obtener todas las compras actualizadas de Room
                    val allPurchases = purchaseDao.getAll()

                    // Filtrar según el filtro actual
                    val filteredPurchases = when (currentFilter) {
                        "pending" -> allPurchases.filter { it.status == "pending" }
                        "active" -> allPurchases.filter { it.status == "active" }
                        else -> allPurchases
                    }

                    // Contar pendientes
                    val pendingCount = allPurchases.count { it.status == "pending" }

                    runOnUiThread {
                        tvPendingCount.text = "$pendingCount compras pendientes"

                        if (filteredPurchases.isEmpty()) {
                            rvPurchases.visibility = View.GONE
                            llEmptyState.visibility = View.VISIBLE
                        } else {
                            rvPurchases.visibility = View.VISIBLE
                            llEmptyState.visibility = View.GONE
                            purchaseAdapter.updatePurchases(filteredPurchases)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error al sincronizar compras", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@PendingPurchasesActivity,
                            "Error al cargar compras: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showAssignCredentialsDialog(purchase: PurchaseEntity) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_assign_credentials, null)

        val tvPurchaseInfo = dialogView.findViewById<TextView>(R.id.tvPurchaseInfo)
        val etCredentialEmail = dialogView.findViewById<EditText>(R.id.etCredentialEmail)
        val etCredentialPassword = dialogView.findViewById<EditText>(R.id.etCredentialPassword)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvPurchaseInfo.text = "Compra de ${purchase.serviceName} por ${purchase.userName}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val email = etCredentialEmail.text.toString().trim()
            val password = etCredentialPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa el email de la cuenta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            assignCredentials(purchase, email, password)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun assignCredentials(purchase: PurchaseEntity, email: String, password: String) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@PendingPurchasesActivity)
                val purchaseDao = db.purchaseDao()

                // Actualizar compra con credenciales y cambiar estado a active
                val updatedPurchase = purchase.copy(
                    email = email,
                    password = password,
                    status = "active"
                )

                // Actualizar en Room
                purchaseDao.update(updatedPurchase)

                // Actualizar en Firebase si tiene firebaseId
                if (!purchase.firebaseId.isNullOrEmpty()) {
                    FirebaseService.actualizarCompra(
                        firebaseId = purchase.firebaseId!!,
                        email = email,
                        password = password,
                        status = "active",
                        onSuccess = {
                            Log.d(TAG, "Compra actualizada en Firebase")
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error al actualizar en Firebase", e)
                        }
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@PendingPurchasesActivity,
                        "Credenciales asignadas correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    // No es necesario llamar a loadPurchases() porque el listener
                    // detectará automáticamente el cambio en Firebase
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al asignar credenciales", e)
                runOnUiThread {
                    Toast.makeText(
                        this@PendingPurchasesActivity,
                        "Error al asignar credenciales: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listener de Firebase al destruir la actividad
        purchasesListener?.remove()
        Log.d(TAG, "Listener de compras removido")
    }
}
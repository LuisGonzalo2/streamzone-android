package com.universidad.streamzone.ui.admin.catalog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.data.model.ServiceEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ServicesManagerActivity : AppCompatActivity() {

    private lateinit var rvServices: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAddService: FloatingActionButton
    private lateinit var btnBack: MaterialButton
    private lateinit var spinnerCategory: Spinner
    private lateinit var adapter: ServiceAdapter

    private var allCategories = listOf<CategoryEntity>()
    private var selectedCategoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services_manager)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val mainContainer = findViewById<View>(R.id.services_main_container)
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
        loadCategories()
    }

    private fun initViews() {
        rvServices = findViewById(R.id.rv_services)
        emptyState = findViewById(R.id.empty_state_services)
        fabAddService = findViewById(R.id.fab_add_service)
        btnBack = findViewById(R.id.btn_back)
        spinnerCategory = findViewById(R.id.spinner_category_filter)

        fabAddService.setOnClickListener {
            val intent = Intent(this, ServiceFormActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ServiceAdapter(
            onEdit = { service ->
                val intent = Intent(this, ServiceFormActivity::class.java)
                intent.putExtra("SERVICE_ID", service.id)
                startActivity(intent)
            },
            onDelete = { service ->
                showDeleteConfirmation(service)
            },
            onToggleActive = { service ->
                toggleServiceStatus(service)
            }
        )

        rvServices.layoutManager = LinearLayoutManager(this)
        rvServices.adapter = adapter
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@ServicesManagerActivity).categoryDao()

            dao.obtenerTodas().collectLatest { categories ->
                allCategories = categories

                // Crear lista para spinner (todas + filtro especial)
                val spinnerItems = mutableListOf("Todas las categorías")
                spinnerItems.addAll(categories.map { it.name })

                val spinnerAdapter = ArrayAdapter(
                    this@ServicesManagerActivity,
                    R.layout.spinner_item,
                    spinnerItems
                )
                spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerCategory.adapter = spinnerAdapter

                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedCategoryId = if (position == 0) null else allCategories[position - 1].id
                        loadServices()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                loadServices()
            }
        }
    }

    private fun loadServices() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@ServicesManagerActivity).serviceDao()

            val servicesFlow = if (selectedCategoryId == null) {
                dao.obtenerTodos()
            } else {
                dao.obtenerServiciosPorCategoria(selectedCategoryId!!)
            }

            servicesFlow.collectLatest { services ->
                if (services.isEmpty()) {
                    showEmptyState()
                } else {
                    showServices(services)
                }
            }
        }
    }

    private fun showServices(services: List<ServiceEntity>) {
        rvServices.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        adapter.submitList(services)
    }

    private fun showEmptyState() {
        rvServices.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun showDeleteConfirmation(service: ServiceEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Servicio")
            .setMessage("¿Estás seguro de eliminar '${service.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteService(service)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteService(service: ServiceEntity) {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@ServicesManagerActivity).serviceDao()
                dao.eliminar(service)

                // TODO: Eliminar imagen de Firebase Storage si existe
                // TODO: Sincronizar con Firebase

            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this@ServicesManagerActivity)
                        .setTitle("Error")
                        .setMessage("No se pudo eliminar el servicio: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun toggleServiceStatus(service: ServiceEntity) {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@ServicesManagerActivity).serviceDao()
                val updated = service.copy(isActive = !service.isActive)
                dao.actualizar(updated)



            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this@ServicesManagerActivity)
                        .setTitle("Error")
                        .setMessage("No se pudo actualizar el estado: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadServices()
    }
}
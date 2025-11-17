package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.data.model.ServiceEntity
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

// Sealed class para items de la lista (header o servicio)
sealed class ServiceListItem {
    data class Header(val category: CategoryEntity, val serviceCount: Int) : ServiceListItem()
    data class ServiceItem(val service: ServiceEntity) : ServiceListItem()
}

class ServicesManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_SERVICES

    private lateinit var btnBack: ImageButton
    private lateinit var rvServices: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabAddService: FloatingActionButton
    private lateinit var serviceAdapter: ServiceAdminAdapter

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SERVICE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_list)
    }

    override fun onPermissionGranted() {
        initViews()
        setupRecyclerView()
        loadServices()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rvServices = findViewById(R.id.rvServices)
        llEmptyState = findViewById(R.id.llEmptyState)
        fabAddService = findViewById(R.id.fabAddService)

        btnBack.setOnClickListener { finish() }

        fabAddService.setOnClickListener {
            val intent = Intent(this, CreateEditServiceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        serviceAdapter = ServiceAdminAdapter(
            items = emptyList(),
            onEditClick = { service ->
                val intent = Intent(this, CreateEditServiceActivity::class.java)
                intent.putExtra("SERVICE_ID", service.id.toLong())
                startActivity(intent)
            },
            onToggleClick = { service ->
                toggleServiceStatus(service)
            }
        )

        rvServices.layoutManager = LinearLayoutManager(this)
        rvServices.adapter = serviceAdapter
    }

    override fun onResume() {
        super.onResume()
        loadServices()
    }

    private fun loadServices() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@ServicesManagerActivity)
                val serviceDao = db.serviceDao()
                val categoryDao = db.categoryDao()

                val services = serviceDao.getAll()
                val categories = categoryDao.obtenerTodasSync()

                // Agrupar servicios por categoría
                val groupedItems = mutableListOf<ServiceListItem>()

                for (category in categories) {
                    val servicesInCategory = services.filter { it.categoryId == category.id }
                    if (servicesInCategory.isNotEmpty()) {
                        // Agregar header de categoría
                        groupedItems.add(ServiceListItem.Header(category, servicesInCategory.size))
                        // Agregar servicios de esta categoría
                        servicesInCategory.forEach { service ->
                            groupedItems.add(ServiceListItem.ServiceItem(service))
                        }
                    }
                }

                runOnUiThread {
                    if (groupedItems.isEmpty()) {
                        rvServices.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvServices.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        serviceAdapter.updateItems(groupedItems)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ServicesManagerActivity,
                        "Error al cargar servicios: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun toggleServiceStatus(service: ServiceEntity) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@ServicesManagerActivity)
                val serviceDao = db.serviceDao()

                val updatedService = service.copy(isActive = !service.isActive)
                serviceDao.actualizar(updatedService)

                runOnUiThread {
                    Toast.makeText(
                        this@ServicesManagerActivity,
                        if (updatedService.isActive) "Servicio activado" else "Servicio desactivado",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadServices()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ServicesManagerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Adapter interno
    inner class ServiceAdminAdapter(
        private var items: List<ServiceListItem>,
        private val onEditClick: (ServiceEntity) -> Unit,
        private val onToggleClick: (ServiceEntity) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        fun updateItems(newItems: List<ServiceListItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is ServiceListItem.Header -> VIEW_TYPE_HEADER
                is ServiceListItem.ServiceItem -> VIEW_TYPE_SERVICE
            }
        }

        inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvHeaderIcon: TextView = view.findViewById(R.id.tvHeaderIcon)
            val tvHeaderName: TextView = view.findViewById(R.id.tvHeaderName)
            val tvHeaderCount: TextView = view.findViewById(R.id.tvHeaderCount)
        }

        inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvServiceName: TextView = view.findViewById(R.id.tvServiceName)
            val tvServicePrice: TextView = view.findViewById(R.id.tvServicePrice)
            val tvServiceCategory: TextView = view.findViewById(R.id.tvServiceCategory)
            val tvServiceStatus: TextView = view.findViewById(R.id.tvServiceStatus)
            val btnEditService: TextView = view.findViewById(R.id.btnEditService)
            val btnToggleService: TextView = view.findViewById(R.id.btnToggleService)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_category_header_admin, parent, false)
                    HeaderViewHolder(view)
                }
                VIEW_TYPE_SERVICE -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_service_admin, parent, false)
                    ServiceViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is ServiceListItem.Header -> {
                    val headerHolder = holder as HeaderViewHolder
                    headerHolder.tvHeaderIcon.text = item.category.icon
                    headerHolder.tvHeaderName.text = item.category.name
                    headerHolder.tvHeaderCount.text = "${item.serviceCount} servicio${if (item.serviceCount != 1) "s" else ""}"
                }
                is ServiceListItem.ServiceItem -> {
                    val serviceHolder = holder as ServiceViewHolder
                    val service = item.service

                    serviceHolder.tvServiceName.text = service.name
                    serviceHolder.tvServicePrice.text = service.price
                    serviceHolder.tvServiceCategory.visibility = View.GONE // Ya no necesitamos mostrar la categoría

                    // Estado
                    if (service.isActive) {
                        serviceHolder.tvServiceStatus.text = "✓ Activo"
                        serviceHolder.tvServiceStatus.setTextColor(0xFF10B981.toInt())
                        serviceHolder.btnToggleService.text = "Desactivar"
                    } else {
                        serviceHolder.tvServiceStatus.text = "✗ Inactivo"
                        serviceHolder.tvServiceStatus.setTextColor(0xFFEF4444.toInt())
                        serviceHolder.btnToggleService.text = "Activar"
                    }

                    serviceHolder.btnEditService.setOnClickListener {
                        onEditClick(service)
                    }

                    serviceHolder.btnToggleService.setOnClickListener {
                        onToggleClick(service)
                    }
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
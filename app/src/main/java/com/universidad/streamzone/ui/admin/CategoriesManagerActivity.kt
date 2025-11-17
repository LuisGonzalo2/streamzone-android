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
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_CATEGORIES

    private lateinit var btnBack: ImageButton
    private lateinit var rvCategories: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var categoryAdapter: CategoryAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list_admin)
    }

    override fun onPermissionGranted() {
        initViews()
        setupRecyclerView()
        loadCategories()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rvCategories = findViewById(R.id.rvCategories)
        llEmptyState = findViewById(R.id.llEmptyState)
        fabAddCategory = findViewById(R.id.fabAddCategory)

        btnBack.setOnClickListener { finish() }

        fabAddCategory.setOnClickListener {
            val intent = Intent(this, CreateEditCategoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdminAdapter(
            categories = emptyList(),
            onEditClick = { category ->
                val intent = Intent(this, CreateEditCategoryActivity::class.java)
                intent.putExtra("CATEGORY_ID", category.id.toLong())
                startActivity(intent)
            },
            onToggleClick = { category ->
                toggleCategoryStatus(category)
            }
        )

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CategoriesManagerActivity)
                val categoryDao = db.categoryDao()

                // Obtener TODAS las categorías (activas e inactivas) de forma síncrona
                val allCategories = categoryDao.obtenerTodasSync()

                withContext(Dispatchers.Main) {
                    if (allCategories.isEmpty()) {
                        rvCategories.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvCategories.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        categoryAdapter.updateCategories(allCategories)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //Toast.makeText(
                       // this@CategoriesManagerActivity,
                       // "Error al cargar categorías: ${e.message}",
                       // Toast.LENGTH_SHORT
                    //).show()
                }
            }
        }
    }

    private fun toggleCategoryStatus(category: CategoryEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CategoriesManagerActivity)
                val categoryDao = db.categoryDao()

                val updatedCategory = category.copy(isActive = !category.isActive)
                categoryDao.actualizar(updatedCategory)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CategoriesManagerActivity,
                        if (updatedCategory.isActive) "Categoría activada" else "Categoría desactivada",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadCategories()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CategoriesManagerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Adapter interno
    inner class CategoryAdminAdapter(
        private var categories: List<CategoryEntity>,
        private val onEditClick: (CategoryEntity) -> Unit,
        private val onToggleClick: (CategoryEntity) -> Unit
    ) : RecyclerView.Adapter<CategoryAdminAdapter.CategoryViewHolder>() {

        fun updateCategories(newCategories: List<CategoryEntity>) {
            categories = newCategories
            notifyDataSetChanged()
        }

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
            val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
            val tvCategoryDescription: TextView = view.findViewById(R.id.tvCategoryDescription)
            val tvCategoryStatus: TextView = view.findViewById(R.id.tvCategoryStatus)
            val btnEditCategory: TextView = view.findViewById(R.id.btnEditCategory)
            val btnToggleCategory: TextView = view.findViewById(R.id.btnToggleCategory)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_admin, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]

            holder.tvCategoryIcon.text = category.icon
            holder.tvCategoryName.text = category.name
            holder.tvCategoryDescription.text = category.description

            // Estado
            if (category.isActive) {
                holder.tvCategoryStatus.text = "✓ Activo"
                holder.tvCategoryStatus.setTextColor(0xFF10B981.toInt())
                holder.btnToggleCategory.text = "Desactivar"
            } else {
                holder.tvCategoryStatus.text = "✗ Inactivo"
                holder.tvCategoryStatus.setTextColor(0xFFEF4444.toInt())
                holder.btnToggleCategory.text = "Activar"
            }

            holder.btnEditCategory.setOnClickListener {
                onEditClick(category)
            }

            holder.btnToggleCategory.setOnClickListener {
                onToggleClick(category)
            }
        }

        override fun getItemCount() = categories.size
    }
}
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
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.CategoryRepository
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.util.PermissionManager
import com.universidad.streamzone.util.toCategoryEntityList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_CATEGORIES

    // Firebase Repository
    private val categoryRepository = CategoryRepository()

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
                intent.putExtra("CATEGORY_ID", category.firebaseId ?: "")
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
                // Obtener TODAS las categorías desde Firebase
                val firebaseCategories = categoryRepository.getAllSync()

                // Convertir a CategoryEntity para la UI
                val categoryEntities = firebaseCategories.toCategoryEntityList()

                withContext(Dispatchers.Main) {
                    if (categoryEntities.isEmpty()) {
                        rvCategories.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvCategories.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        categoryAdapter.updateCategories(categoryEntities)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("CategoriesManager", "❌ Error al cargar categorías", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CategoriesManagerActivity,
                        "Error al cargar categorías: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun toggleCategoryStatus(category: CategoryEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (category.firebaseId != null) {
                    // Obtener la categoría de Firebase
                    val firebaseCategory = categoryRepository.findById(category.firebaseId!!)
                    if (firebaseCategory != null) {
                        // Actualizar el estado en Firebase
                        val updatedCategory = firebaseCategory.copy(
                            isActive = !firebaseCategory.isActive,
                            updatedAt = Timestamp.now()
                        )
                        categoryRepository.update(updatedCategory)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@CategoriesManagerActivity,
                                if (updatedCategory.isActive) "Categoría activada" else "Categoría desactivada",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadCategories()
                        }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("CategoriesManager", "❌ Error al actualizar categoría", e)
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
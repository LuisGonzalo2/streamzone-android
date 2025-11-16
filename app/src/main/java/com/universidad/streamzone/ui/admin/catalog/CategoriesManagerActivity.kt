package com.universidad.streamzone.ui.admin.catalog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoriesManagerActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var btnBack: MaterialButton
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories_manager)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val mainContainer = findViewById<View>(R.id.categories_main_container)
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
        rvCategories = findViewById(R.id.rv_categories)
        emptyState = findViewById(R.id.empty_state_categories)
        fabAddCategory = findViewById(R.id.fab_add_category)
        btnBack = findViewById(R.id.btn_back)

        fabAddCategory.setOnClickListener {
            val intent = Intent(this, CategoryFormActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onEdit = { category ->
                val intent = Intent(this, CategoryFormActivity::class.java)
                intent.putExtra("CATEGORY_ID", category.id)
                startActivity(intent)
            },
            onDelete = { category ->
                showDeleteConfirmation(category)
            },
            onToggleActive = { category ->
                toggleCategoryStatus(category)
            }
        )

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = adapter
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@CategoriesManagerActivity).categoryDao()

            dao.obtenerTodas().collectLatest { categories ->
                if (categories.isEmpty()) {
                    showEmptyState()
                } else {
                    showCategories(categories)
                }
            }
        }
    }

    private fun showCategories(categories: List<CategoryEntity>) {
        rvCategories.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        adapter.submitList(categories)
    }

    private fun showEmptyState() {
        rvCategories.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun showDeleteConfirmation(category: CategoryEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Categoría")
            .setMessage("¿Estás seguro de eliminar '${category.name}'?\n\nEsto también eliminará todos los servicios asociados.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCategory(category: CategoryEntity) {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@CategoriesManagerActivity).categoryDao()
                dao.eliminar(category)

                // TODO: Sincronizar con Firebase
                // TODO: Eliminar servicios asociados

            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this@CategoriesManagerActivity)
                        .setTitle("Error")
                        .setMessage("No se pudo eliminar la categoría: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun toggleCategoryStatus(category: CategoryEntity) {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@CategoriesManagerActivity).categoryDao()
                val updated = category.copy(isActive = !category.isActive)
                dao.actualizar(updated)

                // TODO: Sincronizar con Firebase

            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this@CategoriesManagerActivity)
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
        // Recargar al volver de la pantalla de creación/edición
        loadCategories()
    }
}
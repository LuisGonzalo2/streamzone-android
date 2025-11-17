package com.universidad.streamzone.ui.admin.catalog

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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.ui.admin.catalog.adapter.CategoryAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoriesManagerActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: MaterialButton
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories_manager)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val mainContainer = findViewById<View>(R.id.categories_manager_container)
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
        btnBack = findViewById(R.id.btn_back)
        fabAddCategory = findViewById(R.id.fab_add_category)

        btnBack.setOnClickListener {
            finish()
        }

        fabAddCategory.setOnClickListener {
            openCreateCategoryDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onEditClick = { category -> openEditCategoryDialog(category) },
            onDeleteClick = { category -> confirmDeleteCategory(category) }
        )

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = adapter
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@CategoriesManagerActivity).categoryDao()

                dao.obtenerTodas().collectLatest { categories ->
                    runOnUiThread {
                        if (categories.isEmpty()) {
                            showEmptyState()
                        } else {
                            showCategories(categories)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CategoriesManager", "Error al cargar categorías", e)
                runOnUiThread {
                    showEmptyState()
                    Toast.makeText(
                        this@CategoriesManagerActivity,
                        "Error al cargar categorías: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun openCreateCategoryDialog() {
        val dialog = CategoryFormDialogFragment.newInstance()
        dialog.show(supportFragmentManager, "createCategory")
    }

    private fun openEditCategoryDialog(category: CategoryEntity) {
        val dialog = CategoryFormDialogFragment.newInstance(category)
        dialog.show(supportFragmentManager, "editCategory")
    }

    private fun confirmDeleteCategory(category: CategoryEntity) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Categoría")
            .setMessage("¿Estás seguro de eliminar '${category.name}'?")
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

                runOnUiThread {
                    Toast.makeText(
                        this@CategoriesManagerActivity,
                        "✅ Categoría eliminada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("CategoriesManager", "Error al eliminar categoría", e)
                runOnUiThread {
                    Toast.makeText(
                        this@CategoriesManagerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
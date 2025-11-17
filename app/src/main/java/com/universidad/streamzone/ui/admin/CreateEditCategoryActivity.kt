package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateEditCategoryActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_CATEGORIES

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etCategoryIcon: TextInputEditText
    private lateinit var etCategoryName: TextInputEditText
    private lateinit var etCategoryDescription: TextInputEditText
    private lateinit var etGradientStart: TextInputEditText
    private lateinit var etGradientEnd: TextInputEditText
    private lateinit var switchIsActive: SwitchCompat
    private lateinit var btnSave: MaterialButton

    private var categoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_edit_category)
    }

    override fun onPermissionGranted() {
        initViews()

        // Verificar si es edición
        val categoryIdLong = intent.getLongExtra("CATEGORY_ID", -1L)
        if (categoryIdLong != -1L) {
            categoryId = categoryIdLong.toInt()
            tvTitle.text = "Editar Categoría"
            loadCategoryData(categoryId!!)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        etCategoryIcon = findViewById(R.id.etCategoryIcon)
        etCategoryName = findViewById(R.id.etCategoryName)
        etCategoryDescription = findViewById(R.id.etCategoryDescription)
        etGradientStart = findViewById(R.id.etGradientStart)
        etGradientEnd = findViewById(R.id.etGradientEnd)
        switchIsActive = findViewById(R.id.switchIsActive)
        btnSave = findViewById(R.id.btnSave)

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveCategory() }
    }

    private fun loadCategoryData(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CreateEditCategoryActivity)
                val categoryDao = db.categoryDao()
                val category = categoryDao.obtenerPorId(id)

                withContext(Dispatchers.Main) {
                    if (category != null) {
                        etCategoryIcon.setText(category.icon)
                        etCategoryName.setText(category.name)
                        etCategoryDescription.setText(category.description)
                        etGradientStart.setText(category.gradientStart)
                        etGradientEnd.setText(category.gradientEnd)
                        switchIsActive.isChecked = category.isActive
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditCategoryActivity,
                        "Error al cargar categoría: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveCategory() {
        val icon = etCategoryIcon.text?.toString()?.trim()
        val name = etCategoryName.text?.toString()?.trim()
        val description = etCategoryDescription.text?.toString()?.trim()
        val gradientStart = etGradientStart.text?.toString()?.trim()
        val gradientEnd = etGradientEnd.text?.toString()?.trim()

        // Validaciones
        if (icon.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa un icono (emoji)", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el nombre de la categoría", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa la descripción", Toast.LENGTH_SHORT).show()
            return
        }

        if (gradientStart.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el color inicial", Toast.LENGTH_SHORT).show()
            return
        }

        if (gradientEnd.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el color final", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar formato de colores
        if (!gradientStart.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            Toast.makeText(this, "Color inicial debe tener formato #RRGGBB", Toast.LENGTH_SHORT).show()
            return
        }

        if (!gradientEnd.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            Toast.makeText(this, "Color final debe tener formato #RRGGBB", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@CreateEditCategoryActivity)
                val categoryDao = db.categoryDao()

                val category = CategoryEntity(
                    id = categoryId ?: 0,
                    categoryId = name.lowercase().replace(" ", "_"),
                    name = name,
                    icon = icon,
                    description = description,
                    gradientStart = gradientStart,
                    gradientEnd = gradientEnd,
                    isActive = switchIsActive.isChecked
                )

                if (categoryId != null) {
                    // Actualizar categoría existente
                    categoryDao.actualizar(category)
                } else {
                    // Insertar nueva categoría
                    categoryDao.insertar(category)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditCategoryActivity,
                        if (categoryId != null) "Categoría actualizada" else "Categoría creada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditCategoryActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
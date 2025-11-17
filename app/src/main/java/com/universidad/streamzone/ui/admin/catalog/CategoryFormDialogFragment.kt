package com.universidad.streamzone.ui.admin.catalog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import kotlinx.coroutines.launch

class CategoryFormDialogFragment : DialogFragment() {

    private lateinit var etCategoryId: EditText
    private lateinit var etCategoryName: EditText
    private lateinit var etCategoryIcon: EditText
    private lateinit var etDescription: EditText
    private lateinit var etGradientStart: EditText
    private lateinit var etGradientEnd: EditText
    private lateinit var switchActive: Switch
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnClose: ImageButton
    private lateinit var tvTitle: TextView

    private var categoryToEdit: CategoryEntity? = null

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: CategoryEntity? = null): CategoryFormDialogFragment {
            val fragment = CategoryFormDialogFragment()
            val args = Bundle()
            category?.let { args.putSerializable(ARG_CATEGORY, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog_Alert)

        arguments?.let {
            @Suppress("DEPRECATION")
            categoryToEdit = it.getSerializable(ARG_CATEGORY) as? CategoryEntity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_category_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()

        categoryToEdit?.let { loadCategoryData(it) }
    }

    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tv_dialog_title)
        etCategoryId = view.findViewById(R.id.et_category_id)
        etCategoryName = view.findViewById(R.id.et_category_name)
        etCategoryIcon = view.findViewById(R.id.et_category_icon)
        etDescription = view.findViewById(R.id.et_category_description)
        etGradientStart = view.findViewById(R.id.et_gradient_start)
        etGradientEnd = view.findViewById(R.id.et_gradient_end)
        switchActive = view.findViewById(R.id.switch_category_active)
        btnSave = view.findViewById(R.id.btn_save_category)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnClose = view.findViewById(R.id.btn_close)

        tvTitle.text = if (categoryToEdit != null) "✏️ Editar Categoría" else "➕ Crear Categoría"
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveCategory()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadCategoryData(category: CategoryEntity) {
        etCategoryId.setText(category.categoryId)
        etCategoryName.setText(category.name)
        etCategoryIcon.setText(category.icon)
        etDescription.setText(category.description)
        etGradientStart.setText(category.gradientStart)
        etGradientEnd.setText(category.gradientEnd)
        switchActive.isChecked = category.isActive

        // Deshabilitar edición de ID si estamos editando
        etCategoryId.isEnabled = false
    }

    private fun saveCategory() {
        val categoryId = etCategoryId.text.toString().trim()
        val name = etCategoryName.text.toString().trim()
        val icon = etCategoryIcon.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val gradientStart = etGradientStart.text.toString().trim()
        val gradientEnd = etGradientEnd.text.toString().trim()
        val isActive = switchActive.isChecked

        // Validaciones
        if (categoryId.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el ID de la categoría", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el nombre", Toast.LENGTH_SHORT).show()
            return
        }

        if (icon.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa un emoji", Toast.LENGTH_SHORT).show()
            return
        }

        if (gradientStart.isEmpty() || gradientEnd.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa los colores del gradiente", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar formato de color
        if (!isValidHexColor(gradientStart) || !isValidHexColor(gradientEnd)) {
            Toast.makeText(requireContext(), "Formato de color inválido (usa #RRGGBB)", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(requireContext()).categoryDao()

                if (categoryToEdit != null) {
                    // Actualizar categoría existente
                    val updatedCategory = categoryToEdit!!.copy(
                        name = name,
                        icon = icon,
                        description = description,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        isActive = isActive
                    )
                    dao.actualizar(updatedCategory)

                    runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "✅ Categoría actualizada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Crear nueva categoría
                    val newCategory = CategoryEntity(
                        categoryId = categoryId,
                        name = name,
                        icon = icon,
                        description = description,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        isActive = isActive
                    )
                    dao.insertar(newCategory)

                    runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "✅ Categoría creada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                dismiss()

            } catch (e: Exception) {
                Log.e("CategoryForm", "Error al guardar categoría", e)
                runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    }

    private fun runOnUiThread(action: () -> Unit) {
        activity?.runOnUiThread(action)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
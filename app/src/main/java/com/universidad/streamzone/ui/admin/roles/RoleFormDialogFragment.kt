package com.universidad.streamzone.ui.admin.roles

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
import com.universidad.streamzone.data.model.RoleEntity
import kotlinx.coroutines.launch

class RoleFormDialogFragment : DialogFragment() {

    private lateinit var etRoleName: EditText
    private lateinit var etRoleDescription: EditText
    private lateinit var switchActive: Switch
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnClose: ImageButton
    private lateinit var tvTitle: TextView

    private var roleToEdit: RoleEntity? = null

    companion object {
        private const val ARG_ROLE = "role"

        fun newInstance(role: RoleEntity? = null): RoleFormDialogFragment {
            val fragment = RoleFormDialogFragment()
            val args = Bundle()
            role?.let { args.putSerializable(ARG_ROLE, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog_Alert)

        arguments?.let {
            @Suppress("DEPRECATION")
            roleToEdit = it.getSerializable(ARG_ROLE) as? RoleEntity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_role_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()

        roleToEdit?.let { loadRoleData(it) }
    }

    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tv_dialog_title)
        etRoleName = view.findViewById(R.id.et_role_name)
        etRoleDescription = view.findViewById(R.id.et_role_description)
        switchActive = view.findViewById(R.id.switch_role_active)
        btnSave = view.findViewById(R.id.btn_save_role)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnClose = view.findViewById(R.id.btn_close)

        // Cambiar título según modo
        tvTitle.text = if (roleToEdit != null) "✏️ Editar Rol" else "➕ Crear Rol"
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveRole()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadRoleData(role: RoleEntity) {
        etRoleName.setText(role.name)
        etRoleDescription.setText(role.description)
        switchActive.isChecked = role.isActive
    }

    private fun saveRole() {
        val name = etRoleName.text.toString().trim()
        val description = etRoleDescription.text.toString().trim()
        val isActive = switchActive.isChecked

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el nombre del rol", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa la descripción", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(requireContext()).roleDao()

                if (roleToEdit != null) {
                    // Actualizar rol existente
                    val updatedRole = roleToEdit!!.copy(
                        name = name,
                        description = description,
                        isActive = isActive
                    )
                    dao.actualizar(updatedRole)

                    runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "✅ Rol actualizado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Crear nuevo rol
                    val newRole = RoleEntity(
                        name = name,
                        description = description,
                        isActive = isActive
                    )
                    dao.insertar(newRole)

                    runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "✅ Rol creado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                dismiss()

            } catch (e: Exception) {
                Log.e("RoleForm", "Error al guardar rol", e)
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
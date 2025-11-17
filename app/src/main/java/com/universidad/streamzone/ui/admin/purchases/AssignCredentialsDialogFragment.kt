package com.universidad.streamzone.ui.admin.purchases

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import kotlinx.coroutines.launch
import kotlin.random.Random

class AssignCredentialsDialogFragment : DialogFragment() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnGenerate: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnClose: ImageButton

    private var purchaseId: Int = 0

    companion object {
        private const val ARG_PURCHASE_ID = "purchase_id"

        fun newInstance(purchaseId: Int): AssignCredentialsDialogFragment {
            val fragment = AssignCredentialsDialogFragment()
            val args = Bundle()
            args.putInt(ARG_PURCHASE_ID, purchaseId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog_Alert)

        arguments?.let {
            purchaseId = it.getInt(ARG_PURCHASE_ID, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_assign_credentials, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
    }

    private fun initViews(view: View) {
        etEmail = view.findViewById(R.id.et_credential_email)
        etPassword = view.findViewById(R.id.et_credential_password)
        btnGenerate = view.findViewById(R.id.btn_generate_password)
        btnSave = view.findViewById(R.id.btn_save_credentials)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnClose = view.findViewById(R.id.btn_close)
    }

    private fun setupListeners() {
        btnGenerate.setOnClickListener {
            generateRandomPassword()
        }

        btnSave.setOnClickListener {
            saveCredentials()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun generateRandomPassword() {
        val password = generatePassword(12)
        etPassword.setText(password)
        Toast.makeText(requireContext(), "Contraseña generada", Toast.LENGTH_SHORT).show()
    }

    private fun generatePassword(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun saveCredentials() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(requireContext()).purchaseDao()

                // Asignar credenciales y cambiar estado a active
                dao.asignarCredenciales(purchaseId, email, password)

                Log.d("AssignCredentials", "Credenciales asignadas a compra ID: $purchaseId")

                runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "✅ Credenciales asignadas correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }

                // TODO: Aquí podrías enviar una notificación al cliente
                // sendNotificationToUser(purchaseId)

            } catch (e: Exception) {
                Log.e("AssignCredentials", "Error al asignar credenciales", e)
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
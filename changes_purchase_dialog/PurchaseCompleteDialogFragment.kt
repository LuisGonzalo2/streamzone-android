package com.universidad.streamzone.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.universidad.streamzone.R

class PurchaseCompleteDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_SERVICE = "arg_service"
        private const val ARG_TOTAL = "arg_total"
        private const val ARG_USER = "arg_user"

        fun newInstance(service: String, total: String, user: String): PurchaseCompleteDialogFragment {
            val f = PurchaseCompleteDialogFragment()
            val b = Bundle().apply {
                putString(ARG_SERVICE, service)
                putString(ARG_TOTAL, total)
                putString(ARG_USER, user)
            }
            f.arguments = b
            return f
        }
    }

    private var service: String = ""
    private var total: String = ""
    private var user: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            service = it.getString(ARG_SERVICE, "")
            total = it.getString(ARG_TOTAL, "")
            user = it.getString(ARG_USER, "")
        }
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog_Alert)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.dialog_purchase_complete, container, false)

        val btnClose = root.findViewById<ImageButton>(R.id.btnCloseComplete)
        val btnAgent = root.findViewById<Button>(R.id.btnAgent)
        val tvTitle = root.findViewById<TextView>(R.id.tvCompleteTitle)

        // Personalizar título si se necesita (mantener el por defecto si no)
        if (service.isNotBlank()) {
            tvTitle.text = getString(R.string.app_name) + " - ¡Compra Registrada! 🎉"
        }

        // Asignar texto del botón desde resources (formato con número visible)
        try {
            val numberDisplay = getString(R.string.whatsapp_number_display)
            val agentButtonText = getString(R.string.agent_button_template, numberDisplay)
            btnAgent.text = agentButtonText
        } catch (_: Exception) {
            // fallback ya definido en layout (vacío) — no crítico
        }

        btnClose.setOnClickListener { dismiss() }

        btnAgent.setOnClickListener {
            // número desde recursos (sin espacios): ej. 593984280334
            val phone = try {
                getString(R.string.whatsapp_number)
            } catch (ex: Exception) {
                "593984280334"
            }

            // mensaje usando plantilla de strings, pasando servicio y total
            val message = try {
                getString(R.string.whatsapp_message_template, if (service.isBlank()) "-" else service, if (total.isBlank()) "-" else total)
            } catch (ex: Exception) {
                "Hola 👋, he realizado una compra. Por favor, Agente acepte mi solicitud. Gracias 🙏✨"
            }

            try {
                val url = "https://wa.me/${phone}?text=${Uri.encode(message)}"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }
}


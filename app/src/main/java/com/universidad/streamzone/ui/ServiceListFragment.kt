package com.universidad.streamzone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.model.ServiceItem

class ServiceListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_service_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerServices)
        // Grid with 2 columns; ajusta según diseño (en tablet puedes usar 3)
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)

        val items = buildServiceList()
        val adapter = ServiceAdapter(items) { item ->
            // Acción de ejemplo: mostrar Toast; reemplaza con navegación/pago
            Toast.makeText(requireContext(), "Comprar: ${'$'}{item.title}", Toast.LENGTH_SHORT).show()
        }

        recycler.adapter = adapter
    }

    private fun buildServiceList(): List<ServiceItem> {
        return listOf(
            // Mensuales
            ServiceItem("netflix", "Netflix", "US$ 4,00 /mes", iconText = "N", colorHex = "#E50914" ),
            ServiceItem("disney_premium", "Disney+ Premium (D+)", "US$ 3,75 /mes", iconText = "D+", colorHex = "#113CCF" ),
            ServiceItem("disney_standard", "Disney+ Standard (D+)", "US$ 3,25 /mes", iconText = "D+", colorHex = "#1E90FF" ),
            ServiceItem("max", "Max (MAX)", "US$ 3,00 /mes", iconText = "MAX", colorHex = "#5B2BE0" ),
            ServiceItem("vix", "ViX", "US$ 2,50 /mes", iconText = "VIX", colorHex = "#FF7F00" ),
            ServiceItem("prime", "Prime Video (PV)", "US$ 3,00 /mes", iconText = "PV", colorHex = "#00A8E1" ),
            ServiceItem("yt", "YouTube Premium (YT)", "US$ 3,35 /mes", iconText = "YT", colorHex = "#FF0000" ),
            ServiceItem("paramount", "Paramount+ (P+)", "US$ 2,75 /mes", iconText = "P+", colorHex = "#0050FF" ),
            ServiceItem("chatgpt", "ChatGPT (GPT)", "US$ 4,00 /mes", iconText = "GPT", colorHex = "#10A37F" ),
            ServiceItem("crunchyroll", "Crunchyroll (CR)", "US$ 2,50 /mes", iconText = "CR", colorHex = "#F47521" ),
            ServiceItem("spotify", "Spotify (SP)", "US$ 3,50 /mes", iconText = "SP", colorHex = "#1DB954" ),
            ServiceItem("deezer", "Deezer (DZ)", "US$ 3,00 /mes", iconText = "DZ", colorHex = "#F47521" ),
            ServiceItem("appletv", "Apple TV+ (ATV)", "US$ 3,50 /mes", iconText = "ATV", colorHex = "#0A84FF" ),
            ServiceItem("canva", "Canva Pro (C)", "US$ 2,00 /mes", iconText = "C", colorHex = "#8C52FF" ),

            // Licencias anuales (flag isAnnual = true)
            ServiceItem("canva_year", "Canva Pro (1 año)", "US$ 17,50 /año", iconText = "C", colorHex = "#8C52FF", isAnnual = true),
            ServiceItem("m365_year", "Microsoft 365 (M365)", "US$ 15,00 /año", iconText = "M365", colorHex = "#0078D4", isAnnual = true),
            ServiceItem("autodesk_year", "Autodesk (AD)", "US$ 12,50 /año", iconText = "AD", colorHex = "#00B0F0", isAnnual = true),
            ServiceItem("office365_year", "Office 365 (O365)", "US$ 15,00 /año", iconText = "O365", colorHex = "#EA4335", isAnnual = true)
        )
    }
}


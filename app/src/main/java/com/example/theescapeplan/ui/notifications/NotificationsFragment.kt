package com.example.theescapeplan.ui.notifications

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.theescapeplan.R
import com.example.theescapeplan.databinding.FragmentNotificationsBinding
import com.example.theescapeplan.PlayerRepository

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val imageRes: Int,
    val type: String   // "skin", "trail", "glow", "accessory"
)

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val shopItems = listOf(
        ShopItem("trail1", "Blue Trail", 10, R.drawable.trail_blue, "trail"),
        ShopItem("trail2", "Red Trail", 10, R.drawable.trail_red, "trail"),
        ShopItem("glow1", "Yellow Glow", 5, R.drawable.coin, "glow"),
        ShopItem("glow2", "Cyan Glow", 5, R.drawable.coin, "glow"),
        ShopItem("acc1", "Cool Hat", 15, R.drawable.coin, "accessory"),
        ShopItem("acc2", "Red Scarf", 15, R.drawable.coin, "accessory")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        PlayerRepository.init(requireContext())
        updateCoinDisplay()
        buildShop()
        return root
    }

    private fun addSectionLabel(title: String) {
        val label = TextView(requireContext()).apply {
            text = title
            setTextColor(Color.CYAN)
            textSize = 20f
            setPadding(16, 24, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        binding.shopContainer.addView(label)
    }

    private fun addShopItem(item: ShopItem) {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        val image = ImageView(requireContext()).apply {
            setImageResource(item.imageRes)
            layoutParams = LinearLayout.LayoutParams(150, 150)
        }

        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(24, 0, 0, 0)
        }

        val nameText = TextView(requireContext()).apply {
            text = item.name
            setTextColor(Color.WHITE)
            textSize = 18f
        }

        val priceText = TextView(requireContext()).apply {
            text = "Price: ${item.price}"
            setTextColor(Color.YELLOW)
            textSize = 16f
        }

        val buyButton = Button(requireContext()).apply {
            text = "Buy"
            setOnClickListener { buyItem(item) }
        }

        textContainer.addView(nameText)
        textContainer.addView(priceText)
        itemLayout.addView(image)
        itemLayout.addView(textContainer)
        itemLayout.addView(buyButton)

        binding.shopContainer.addView(itemLayout)
    }

    private fun buyItem(item: ShopItem) {
        val success = when (item.type) {
            "trail" -> PlayerRepository.buyTrail(item.id, item.price)
            "glow" -> PlayerRepository.buyGlow(item.id, item.price)
            "accessory" -> PlayerRepository.buyAccessory(item.id, item.price)
            else -> false
        }
        if (success) {
            updateCoinDisplay()
            Toast.makeText(requireContext(), "Purchased ${item.name}!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Not enough coins!", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCoinDisplay() {
        binding.tvCoins.text = "Coins: ${PlayerRepository.currentPlayer.coins}"
    }

    private fun buildShop() {
        binding.shopContainer.removeAllViews()

        addSectionLabel("Trails")
        shopItems.filter { it.type == "trail" }.forEach { addShopItem(it) }

        addSectionLabel("Glows")
        shopItems.filter { it.type == "glow" }.forEach { addShopItem(it) }

        addSectionLabel("Accessories")
        shopItems.filter { it.type == "accessory" }.forEach { addShopItem(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

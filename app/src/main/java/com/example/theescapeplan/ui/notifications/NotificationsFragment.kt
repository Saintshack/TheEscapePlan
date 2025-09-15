package com.example.theescapeplan.ui.notifications

import android.content.Context
import android.content.SharedPreferences
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
import androidx.lifecycle.ViewModelProvider
import com.example.theescapeplan.R
import com.example.theescapeplan.databinding.FragmentNotificationsBinding

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val imageRes: Int
)

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences
    private var coins: Int = 0

    private val shopItems = listOf(
        ShopItem("skin1", "Cool Skin", 100, R.drawable.coin),
        ShopItem("skin2", "Neon Skin", 200, R.drawable.coin),
        ShopItem("trail1", "Blue Trail", 150, R.drawable.coin),
        ShopItem("trail2", "Red Trail", 180, R.drawable.coin)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        notificationsViewModel.text.observe(viewLifecycleOwner) { }

        prefs = requireContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        coins = prefs.getInt("coins", 0)
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
        if (coins >= item.price) {
            coins -= item.price
            prefs.edit().putInt("coins", coins).apply()
            prefs.edit().putBoolean("${item.id}_owned", true).apply()
            updateCoinDisplay()
            Toast.makeText(requireContext(), "Purchased ${item.name}!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Not enough coins!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCoinDisplay() {
        binding.tvCoins.text = "Coins: $coins"
    }

    private fun buildShop() {
        binding.shopContainer.removeAllViews()
        addSectionLabel("Skins")
        shopItems.filter { it.id.startsWith("skin") }.forEach { addShopItem(it) }
        addSectionLabel("Trails")
        shopItems.filter { it.id.startsWith("trail") }.forEach { addShopItem(it) }
        println(shopItems.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

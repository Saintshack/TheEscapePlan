package com.example.theescapeplan.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.theescapeplan.PlayerRepository
import com.example.theescapeplan.R
import com.example.theescapeplan.databinding.FragmentHomeBinding
import androidx.core.content.edit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedTrailId: String? = null
    private var selectedGlowId: String? = null

    private val prefs by lazy {
        requireContext().getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
    }

    private val trailImageMap = mapOf(
        "Blue Trail" to R.drawable.trail_blue,
        "Red Trail" to R.drawable.trail_red
    )

    private val glowImageMap = mapOf(
        "Yellow Glow" to R.drawable.yellow,
        "Green Glow" to R.drawable.green
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PlayerRepository.init(requireContext())
        binding.tvPlayerName.text = PlayerRepository.currentPlayer.name
        binding.tvCoins.text = "Coins: ${PlayerRepository.currentPlayer.coins}"
        binding.imgAvatar.setImageResource(resolveAvatarRes(PlayerRepository.currentPlayer.avatarRes))
        setupTrailsList(PlayerRepository.currentPlayer.ownedTrails)
        setupGlowsList(PlayerRepository.currentPlayer.ownedGlows)
        selectedTrailId = PlayerRepository.currentPlayer.equippedTrail.takeIf { it != "None" }
        selectedGlowId = PlayerRepository.currentPlayer.equippedGlow.takeIf { it != "None" }
        refreshPreviewPanel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun resolveAvatarRes(avatarResPath: String): Int {
        // TODO: replace with a mapping function if you store resource names (e.g. "player_blue")
        return try {
            R.drawable.player
        } catch (e: Exception) {
            R.drawable.player
        }
    }

    private fun setupTrailsList(ownedTrails: MutableList<String>) {
        if (ownedTrails.isEmpty()) {
            val empty = listOf("No Trails owned")
            binding.listOwnedTrails.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, empty)
            binding.listOwnedTrails.isEnabled = false
            return
        }

        val adapter = ItemImageAdapter(requireContext(), ownedTrails, ItemType.TRAIL)
        binding.listOwnedTrails.adapter = adapter

        binding.listOwnedTrails.setOnItemClickListener { _, _, position, _ ->
            if (isTrailClicked()) {
                val id = ownedTrails[position]
                selectedTrailId = id
                PlayerRepository.equipTrail(id)
                PlayerRepository.savePlayer()
                setTrailClicked(false)
                refreshPreviewPanel()
                adapter.notifyDataSetChanged()
            }
            else{
                selectedTrailId = "None"
                PlayerRepository.equipTrail("None")
                PlayerRepository.savePlayer()
                setTrailClicked(true)
                refreshPreviewPanel()
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupGlowsList(ownedGlows: MutableList<String>) {
        if (ownedGlows.isEmpty()) {
            val empty = listOf("No Glows owned")
            binding.listOwnedGlows.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, empty)
            binding.listOwnedGlows.isEnabled = false
            return
        }

        val adapter = ItemImageAdapter(requireContext(), ownedGlows, ItemType.GLOW)
        binding.listOwnedGlows.adapter = adapter

        binding.listOwnedGlows.setOnItemClickListener { _, _, position, _ ->
            if (isGlowClicked()) {
                val id = ownedGlows[position]
                selectedGlowId = id
                PlayerRepository.equipGlow(id)
                PlayerRepository.savePlayer()
                println(PlayerRepository.currentPlayer.equippedGlow)
                setGlowClicked(false)
                refreshPreviewPanel()
                adapter.notifyDataSetChanged()
            }
            else{
                selectedGlowId = "None"
                PlayerRepository.equipGlow("None")
                PlayerRepository.savePlayer()
                println(PlayerRepository.currentPlayer.equippedGlow)
                setGlowClicked(true)
                refreshPreviewPanel()
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun refreshPreviewPanel() {
        val previewRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatarThumb = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(160, 160)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(resolveAvatarRes(PlayerRepository.currentPlayer.avatarRes))
        }
        previewRow.addView(avatarThumb)

        selectedTrailId?.let { id ->
            val res = trailImageMap[id] ?: R.drawable.coin
            val trailThumb = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(16, 0, 0, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(res)
            }
            previewRow.addView(trailThumb)
        }

        selectedGlowId?.let { id ->
            val res = glowImageMap[id] ?: R.drawable.coin
            val glowThumb = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(12, 0, 0, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(res)
            }
            previewRow.addView(glowThumb)
        }
    }

    private inner class ItemImageAdapter(
        val ctx: Context,
        val items: List<String>,
        val type: ItemType
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val id = items[position]
            val root = convertView ?: LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12, 12, 12, 12)
                layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
            }

            val imageView: ImageView
            val textView: TextView
            if (root.tag == null) {
                imageView = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(120, 120)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                textView = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 0, 0, 0)
                    }
                    setTextColor(Color.WHITE)
                    textSize = 16f
                }
                (root as LinearLayout).addView(imageView)
                root.addView(textView)
                root.tag = Pair(imageView, textView)
            } else {
                val pair = root.tag as Pair<*, *>
                imageView = pair.first as ImageView
                textView = pair.second as TextView
            }

            val drawableRes = when (type) {
                ItemType.TRAIL -> trailImageMap[id] ?: R.drawable.coin
                ItemType.GLOW -> glowImageMap[id] ?: R.drawable.coin
            }
            imageView.setImageResource(drawableRes)
            textView.text = prettifyId(id)

            val isSelected = when (type) {
                ItemType.TRAIL -> id == selectedTrailId
                ItemType.GLOW -> id == selectedGlowId
            }
            root.setBackgroundColor(if (isSelected) Color.parseColor("#3344FF44") else Color.TRANSPARENT)

            return root
        }
    }

    override fun onPause() {
        super.onPause()
        PlayerRepository.savePlayer()
    }

    private fun prettifyId(id: String): String {
        return id.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun isTrailClicked(): Boolean {
        val defaultValue = PlayerRepository.currentPlayer.equippedTrail != "None"
        return prefs.getBoolean("trailClicked", defaultValue)
    }

    private fun setTrailClicked(value: Boolean) {
        prefs.edit { putBoolean("trailClicked", value) }
    }

    private fun isGlowClicked(): Boolean {
        val defaultValue = PlayerRepository.currentPlayer.equippedGlow != "None"
        return prefs.getBoolean("glowClicked", defaultValue)
    }

    private fun setGlowClicked(value: Boolean) {
        prefs.edit { putBoolean("glowClicked", value) }
    }


    private enum class ItemType { TRAIL, GLOW}
}

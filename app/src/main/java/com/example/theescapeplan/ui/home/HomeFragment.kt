package com.example.theescapeplan.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.theescapeplan.Player
import com.example.theescapeplan.PlayerRepository
import com.example.theescapeplan.R
import com.example.theescapeplan.databinding.FragmentHomeBinding

/** HomeFragment: shows player info + owned items and allows selecting them for preview. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // currently-loaded player (from PlayerRepository)

    // temporary selection (preview) ids
    private var selectedTrailId: String? = null
    private var selectedGlowId: String? = null
    private var selectedAccessoryId: String? = null

    // --- TODO: map your item ids (stored in player.ownedTrails / ownedGlows / ownedAccessories)
    // to actual drawable resource ids. Update these maps to match your real drawable names.
    private val trailImageMap = mapOf(
        "trail1" to R.drawable.trail_blue,
        "trail2" to R.drawable.trail_red
    )

    private val glowImageMap = mapOf(
        "glow1" to R.drawable.coin,   // replace with actual glow drawables
        "glow2" to R.drawable.coin
    )

    private val accessoryImageMap = mapOf(
        "acc1" to R.drawable.coin,    // replace with actual accessory drawables (hat/scarf)
        "acc2" to R.drawable.coin
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

        // Ensure PlayerRepository is initialized and has the current player
        PlayerRepository.init(requireContext())

        // Basic player UI
        binding.tvPlayerName.text = PlayerRepository.currentPlayer.name
        binding.tvCoins.text = "Coins: ${PlayerRepository.currentPlayer.coins}"

        // Show base avatar (player.avatarRes is a string path in your model — we fallback to R.drawable.player)
        binding.imgAvatar.setImageResource(resolveAvatarRes(PlayerRepository.currentPlayer.avatarRes))

        // Populate lists (owned items)
        setupTrailsList(PlayerRepository.currentPlayer.ownedTrails)
        setupGlowsList(PlayerRepository.currentPlayer.ownedGlows)
        setupAccessoriesList(PlayerRepository.currentPlayer.ownedAccessories)

        // Show any currently equipped items from player object as the initial preview
        selectedTrailId = PlayerRepository.currentPlayer.equippedTrail.takeIf { it != "None" }
        selectedGlowId = PlayerRepository.currentPlayer.equippedGlow.takeIf { it != "None" }
        // accessories may be multiple; if player has one equipped you may track it similarly
        selectedAccessoryId = PlayerRepository.currentPlayer.ownedAccessories.firstOrNull() // optional initial preview
        refreshPreviewPanel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------- Helpers ----------

    private fun resolveAvatarRes(avatarResPath: String): Int {
        // If Player.avatarRes stores a resource name or path, map it to a drawable id.
        // For simple fallback behavior, return player default drawable.
        // TODO: replace with a mapping function if you store resource names (e.g. "player_blue")
        return try {
            // if avatarResPath is like "res/drawable/player.png", fallback:
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
            val id = ownedTrails[position]
            selectedTrailId = id
            refreshPreviewPanel()
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupGlowsList(ownedGlows: MutableList<String>) {
        // NOTE: XML 'listOwnedSkins' is repurposed to show glows per your earlier request
        if (ownedGlows.isEmpty()) {
            val empty = listOf("No Glows owned")
            binding.listOwnedGlows.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, empty)
            binding.listOwnedGlows.isEnabled = false
            return
        }

        val adapter = ItemImageAdapter(requireContext(), ownedGlows, ItemType.GLOW)
        binding.listOwnedGlows.adapter = adapter

        binding.listOwnedGlows.setOnItemClickListener { _, _, position, _ ->
            val id = ownedGlows[position]
            selectedGlowId = id
            refreshPreviewPanel()
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupAccessoriesList(ownedAccessories: MutableList<String>) {
        binding.glowContainer.removeAllViews()

        if (ownedAccessories.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No Accessories owned"
                setTextColor(Color.LTGRAY)
                setPadding(8, 8, 8, 8)
            }
            binding.glowContainer.addView(tv)
            return
        }

        // Horizontal thumbnails
        val params = LinearLayout.LayoutParams(160, 160).apply { setMargins(8, 8, 8, 8) }

        ownedAccessories.forEach { id ->
            val iv = ImageView(requireContext()).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = params
                val res = accessoryImageMap[id] ?: R.drawable.coin
                setImageResource(res)
                setPadding(6, 6, 6, 6)
                setBackgroundColor(Color.parseColor("#222222"))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedAccessoryId = id
                    refreshPreviewPanel()
                    // small visual feedback
                    highlightAccessorySelection(id)
                }
            }
            binding.glowContainer.addView(iv)
        }
    }

    private fun highlightAccessorySelection(selectedId: String) {
        // Simple highlight: iterate children and set border color
        for (i in 0 until binding.glowContainer.childCount) {
            val child = binding.glowContainer.getChildAt(i)
            if (child is ImageView) {
                val tagRes = (child.tag as? String)
                // We didn't tag children with id; instead compare drawable — fallback: set unselected style
                child.alpha = if (isAccessoryImageMatching(child, selectedId)) 1.0f else 0.6f
            }
        }
    }

    private fun isAccessoryImageMatching(iv: ImageView, accessoryId: String): Boolean {
        // Best-effort: compare tag if present. (We didn't set tags above — you can set tag=id if you prefer)
        // For now, always return false (no tag), so highlightAccessorySelection is a no-op unless you set tags.
        return false
    }

    private fun refreshPreviewPanel() {
        // Clear any existing preview area (we'll reuse glowContainer to show a preview row)
        val previewRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Base avatar thumbnail
        val avatarThumb = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(160, 160)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(resolveAvatarRes(PlayerRepository.currentPlayer.avatarRes))
        }
        previewRow.addView(avatarThumb)

        // If a trail is selected show it
        selectedTrailId?.let { id ->
            val res = trailImageMap[id] ?: R.drawable.coin
            val trailThumb = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(16, 0, 0, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(res)
            }
            previewRow.addView(trailThumb)
        }

        // If a glow is selected show it
        selectedGlowId?.let { id ->
            val res = glowImageMap[id] ?: R.drawable.coin
            val glowThumb = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(12, 0, 0, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(res)
            }
            previewRow.addView(glowThumb)
        }

        // accessory preview
        selectedAccessoryId?.let { id ->
            val res = accessoryImageMap[id] ?: R.drawable.coin
            val accThumb = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(12, 0, 0, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(res)
            }
            previewRow.addView(accThumb)
        }

        // Place previewRow above the other controls (replace children of glowContainer's first view)
        // We'll show preview in a small container above the accessory list to avoid modifying layout xml.
        // Simplest: remove all currently in glowContainer and add preview + accessory row underneath.
        binding.glowContainer.removeAllViews()
        binding.glowContainer.addView(previewRow)

        // Re-add accessory thumbnails below (so user can still pick)
        PlayerRepository.currentPlayer.ownedAccessories.forEach { id ->
            val iv = ImageView(requireContext()).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(160, 160).apply { setMargins(8, 8, 8, 8) }
                val res = accessoryImageMap[id] ?: R.drawable.coin
                setImageResource(res)
                setOnClickListener {
                    selectedAccessoryId = id
                    refreshPreviewPanel()
                }
            }
            binding.glowContainer.addView(iv)
        }
    }

    // ---------- small adapter for lists that show image + text ----------
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

            // Ensure we have an ImageView + TextView inside root
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

            // Set image and text depending on type
            val drawableRes = when (type) {
                ItemType.TRAIL -> trailImageMap[id] ?: R.drawable.coin
                ItemType.GLOW -> glowImageMap[id] ?: R.drawable.coin
                ItemType.ACCESSORY -> accessoryImageMap[id] ?: R.drawable.coin
            }
            imageView.setImageResource(drawableRes)
            textView.text = prettifyId(id)

            // highlight selected item
            val isSelected = when (type) {
                ItemType.TRAIL -> id == selectedTrailId
                ItemType.GLOW -> id == selectedGlowId
                ItemType.ACCESSORY -> id == selectedAccessoryId
            }
            root.setBackgroundColor(if (isSelected) Color.parseColor("#3344FF44") else Color.TRANSPARENT)

            return root
        }
    }

    private fun prettifyId(id: String): String {
        // convert "trail1" -> "Trail 1" etc. Customize as needed.
        return id.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private enum class ItemType { TRAIL, GLOW, ACCESSORY }
}

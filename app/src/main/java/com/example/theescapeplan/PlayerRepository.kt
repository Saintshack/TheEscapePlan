package com.example.theescapeplan
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import androidx.core.content.edit

object PlayerRepository {
    private const val PREFS_NAME = "GamePrefs"
    private const val PLAYER_KEY = "player_data"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    var currentPlayer: Player = Player("1", "Player1", "res/drawable/player.png", "None", "None", 0)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadPlayer()
    }

    private fun loadPlayer() {
        val json = prefs.getString(PLAYER_KEY, null)
        currentPlayer = if (json != null) {
            try {
                gson.fromJson(json, Player::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                Player("1", "Player1", "res/drawable/player.png", "None", "", 0) // fallback to default
            }
        } else {
            Player("1", "Player1", "res/drawable/player.png", "None", "", 0) // first time user
        }
    }

    fun savePlayer() {
        val json = gson.toJson(currentPlayer)
        prefs.edit { putString(PLAYER_KEY, json) }
    }

    fun addCoins(amount: Int) {
        currentPlayer.coins += amount
        savePlayer()
    }

    fun spendCoins(amount: Int): Boolean {
        return if (currentPlayer.coins >= amount) {
            currentPlayer.coins -= amount
            savePlayer()
            true
        } else false
    }

    fun buyTrail(trailId: String, cost: Int): Boolean {
        if (spendCoins(cost)) {
            currentPlayer.ownedTrails.add(trailId)
            savePlayer()
            return true
        }
        return false
    }

    fun buyGlow(glowId: String, cost: Int): Boolean {
        if (spendCoins(cost)) {
            currentPlayer.ownedGlows.add(glowId)
            savePlayer()
            return true
        }
        return false
    }

    fun buyAccessory(accId: String, cost: Int): Boolean {
        if (spendCoins(cost)) {
            currentPlayer.ownedAccessories.add(accId)
            savePlayer()
            return true
        }
        return false
    }


    fun equipTrail(trailId: String) {
        if (currentPlayer.ownedTrails.contains(trailId)) {
            currentPlayer.equippedTrail = trailId
            savePlayer()
        }
    }

}
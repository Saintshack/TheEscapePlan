package com.example.theescapeplan

data class Player(
    val id: String,
    var name: String,
    var avatarRes: String,  // Path to currently equipped skin
    var equippedTrail: String,
    var equippedGlow: String,
    var coins: Int,
    var ownedTrails: MutableList<String> = mutableListOf(),
    var ownedGlows: MutableList<String> = mutableListOf(),
)
package com.example.theescapeplan.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _distance = MutableLiveData(0)
    val distance: LiveData<Int> = _distance

    enum class GameState { PLAYING, GAME_OVER }
    private val _gameState = MutableLiveData(GameState.PLAYING)
    val gameState: LiveData<GameState> = _gameState

    fun addScore(points: Int) {
        _score.value = (_score.value ?: 0) + points
    }

    fun addDistance(amount: Int) {
        _distance.value = (_distance.value ?: 0) + amount
    }

    fun setGameOver() {
        _gameState.value = GameState.GAME_OVER
    }

    fun resetGame() {
        _score.value = 0
        _distance.value = 0
        _gameState.value = GameState.PLAYING
    }
}
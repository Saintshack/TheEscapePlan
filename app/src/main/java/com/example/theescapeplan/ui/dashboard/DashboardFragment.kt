package com.example.theescapeplan.ui.dashboard
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.theescapeplan.GameView

class DashboardFragment : Fragment() {

    private lateinit var gameView: GameView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create GameView programmatically
        gameView = GameView(requireContext())

        val prefs = requireContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        gameView.loadCoins(prefs)

        // GestureDetector to handle swipes
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Horizontal swipe
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) gameView.moveRight()
                        else gameView.moveLeft()
                        gameView.update()
                        return true
                    }
                } else {
                    // Vertical swipe
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) gameView.moveDown()
                        else gameView.moveUp()
                        gameView.update()
                        return true
                    }
                }
                return false
            }
        })

        // Intercept touch events and pass to gestureDetector
        gameView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        return gameView
    }

    override fun onResume() {
        super.onResume()
        gameView.startGame()
    }

    override fun onPause() {
        super.onPause()
        gameView.stopGame() // safe stop
    }
}

package com.example.theescapeplan
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

// Obstacle data class
data class Obstacle(
    var x: Float,
    var y: Float,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
)

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), Runnable {

    private var gameThread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    // -----------------------------
    // Background
    // -----------------------------
    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
    private var bgY: Float = 0f
    private val bgSpeed = 15f

    // -----------------------------
    // Player
    // -----------------------------
    private val playerFrames: Array<Bitmap> = arrayOf(
        BitmapFactory.decodeResource(resources, R.drawable.player_run1),
        BitmapFactory.decodeResource(resources, R.drawable.player_run2)
    )
    private var frameCounter = 0
    private var playerX = 0f
    private var playerY = 0f

    // Lane tracking
    private val lanePositions = floatArrayOf(100f, 400f, 700f) // X positions for 3 lanes
    private var currentLaneIndex = 1 // start in middle lane

    // -----------------------------
    // Score + Distance
    // -----------------------------
    private var score = 0
    private var distanceTraveled = 0

    // -----------------------------
    // Game State
    // -----------------------------
    private enum class GameState { PLAYING, GAME_OVER }
    private var gameState = GameState.PLAYING

    // -----------------------------
    // Obstacles
    // -----------------------------
    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.obstacle_image)
    private val obstacleSpacing = 600f
    private var lastObstacleY = 0f
    private val laneWidth = 300 // used for obstacle positioning

    // -----------------------------
    // Initialization
    // -----------------------------
    init {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startGame()
                playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
                playerY = height - 400f
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopGame()
            }
        })
    }

    // -----------------------------
    // Game Loop
    // -----------------------------
    override fun run() {
        while (isPlaying) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        if (gameState != GameState.PLAYING) return

        // Background scroll
        bgY += bgSpeed
        distanceTraveled += bgSpeed.toInt()
        if (bgY >= height) bgY = 0f

        // Score increase
        if (distanceTraveled % 100 == 0) score += 1

        // Spawn obstacles
        if (lastObstacleY - distanceTraveled >= -obstacleSpacing) {
            spawnObstacleRow()
            lastObstacleY = distanceTraveled.toFloat()
        }

        // Move obstacles
        obstacles.forEach { it.y += bgSpeed }
        obstacles.removeAll { it.y > height }

        // Update playerX from current lane
        playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2

        frameCounter++

        // Example game over condition (replace with collisions later)
        if (distanceTraveled > 5000) gameState = GameState.GAME_OVER
    }

    // -----------------------------
    // Draw Function
    // -----------------------------
    private fun draw() {
        if (!surfaceHolder.surface.isValid) return
        val canvas = surfaceHolder.lockCanvas()

        // Background
        canvas.drawBitmap(background, 0f, bgY - height, paint)
        canvas.drawBitmap(background, 0f, bgY, paint)

        if (gameState == GameState.PLAYING) {
            // Obstacles
            obstacles.forEach {
                canvas.drawBitmap(it.bitmap, it.x, it.y, paint)
            }

            // Player animation
            val frameIndex = (frameCounter / 10) % playerFrames.size
            val playerBitmap = playerFrames[frameIndex]
            val flashPaint = Paint()
            flashPaint.colorFilter =
                if ((frameCounter / 20) % 2 == 0) PorterDuffColorFilter(Color.CYAN, PorterDuff.Mode.ADD)
                else null
            canvas.drawBitmap(playerBitmap, playerX, playerY, flashPaint)

            // HUD
            paint.color = Color.WHITE
            paint.textSize = 60f
            canvas.drawText("Score: $score", 50f, 100f, paint)
            canvas.drawText("Distance: ${distanceTraveled/100}m", 50f, 180f, paint)

        } else if (gameState == GameState.GAME_OVER) {
            // Dark overlay
            paint.color = Color.argb(180, 0,0,0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Flashing overlay
            val alpha = (Math.abs(Math.sin(frameCounter * 0.1)) * 255).toInt()
            paint.color = Color.argb(alpha, 255, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Pulsing text
            val pulse = (70 + 20 * Math.sin(frameCounter * 0.2)).toFloat()
            paint.color = Color.RED
            paint.textSize = 120f + pulse
            canvas.drawText("GAME OVER", width/4f, height/2f, paint)

            // Final Score & Restart
            paint.color = Color.WHITE
            paint.textSize = 70f
            canvas.drawText("Score: $score", width/3f, height/2f + 120f, paint)
            canvas.drawText("Tap to Restart", width/4f, height/2f + 250f, paint)

            frameCounter++
        }

        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    private fun control() { Thread.sleep(17) }

    fun startGame() {
        isPlaying = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    fun stopGame() {
        isPlaying = false
        gameThread?.join()
    }

    // -----------------------------
    // Movement Functions
    // -----------------------------
    fun moveLeft() {
        if (currentLaneIndex > 0) currentLaneIndex--
    }

    fun moveRight() {
        if (currentLaneIndex < lanePositions.size - 1) currentLaneIndex++
    }

    fun moveUp() {
        playerY -= 200f
        if (playerY < 0f) playerY = 0f
    }

    fun moveDown() {
        playerY += 200f
        val maxY = height - 150f
        if (playerY > maxY) playerY = maxY
    }

    // -----------------------------
    // Touch / Reset
    // -----------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.GAME_OVER && event.action == MotionEvent.ACTION_DOWN) {
            resetGame()
        }
        return true
    }

    private fun resetGame() {
        score = 0
        distanceTraveled = 0
        bgY = 0f
        frameCounter = 0
        obstacles.clear()
        lastObstacleY = 0f
        currentLaneIndex = 1
        playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
        playerY = height - 400f
        gameState = GameState.PLAYING
    }

    // -----------------------------
    // Spawn Obstacles
    // -----------------------------
    private fun spawnObstacleRow() {
        val lanesArray = arrayOf(0,1,2)
        val emptyLanesCount = Random.nextInt(0,2)
        val emptyLanes = lanesArray.toList().shuffled().take(emptyLanesCount)

        lanesArray.forEach { lane ->
            if (!emptyLanes.contains(lane)) {
                val x = lane * laneWidth.toFloat()
                val y = -obstacleBitmap.height.toFloat()
                obstacles.add(Obstacle(x, y, obstacleBitmap, obstacleBitmap.width, obstacleBitmap.height))
            }
        }
    }
}


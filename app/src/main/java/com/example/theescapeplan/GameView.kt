package com.example.theescapeplan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random
import androidx.core.graphics.scale

data class Obstacle(
    var x: Float,
    var y: Float,
    val bitmap: Bitmap,
    var width: Int,
    var height: Int,
    var scale: Float = 0.2f,
    val laneIndex: Int
)

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), Runnable {

    private var gameThread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    private val screenWidth = 1100
    private val screenHeight = 2000

    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        .scale(screenWidth, screenHeight)
    private val playerFrames: Array<Bitmap> = arrayOf(
        BitmapFactory.decodeResource(resources, R.drawable.player).scale(230, 600),
        BitmapFactory.decodeResource(resources, R.drawable.player).scale(230, 600)
    )
    private var frameCounter = 0
    private var playerX = 0f
    private var playerY = 0f
    private val lanePositions = floatArrayOf(300f, 550f, 900f)
    private var currentLaneIndex = 1

    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.obstacle)
    private val obstacleSpacing = 600f
    private var lastObstacleY = 0f
    private var score = 0
    private var distanceTraveled = 0

    private enum class GameState { PLAYING, GAME_OVER }
    private var gameState = GameState.PLAYING

    init {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startGame()
                playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
                playerY = screenHeight - 700f
                spawnObstacleRow()
                lastObstacleY = distanceTraveled.toFloat()
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) { stopGame() }
        })
    }

    override fun run() {
        while (isPlaying) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        if (gameState != GameState.PLAYING) return

        val obstacleSpeed = 15f
        distanceTraveled += obstacleSpeed.toInt()
        if (distanceTraveled % 100 == 0) score += 1

        // Spawn obstacles at fixed intervals
        if (distanceTraveled - lastObstacleY >= obstacleSpacing) {
            spawnObstacleRow()
            lastObstacleY = distanceTraveled.toFloat()
        }

        val startY = 800  // start above screen
        val endY = screenHeight - 400f                      // ground offset

        // Update all obstacles efficiently
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.y += obstacleSpeed

            // Scale obstacle as it approaches the player
            val t = ((obstacle.y - startY) / (endY - startY)).coerceIn(0f, 1f)
            obstacle.scale = 0.2f + 0.8f * t
            obstacle.width = (obstacle.bitmap.width * obstacle.scale).toInt()
            obstacle.height = (obstacle.bitmap.height * obstacle.scale).toInt()

            // Lane alignment
            obstacle.x = lanePositions[obstacle.laneIndex] - obstacle.width / 2

            // Remove off-screen obstacles
            if (obstacle.y - obstacle.height > screenHeight) {
                iterator.remove()
            }
        }

        // Update player X in current lane
        playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
        checkCollisions()
        frameCounter++
    }

    private fun draw() {
        if (!surfaceHolder.surface.isValid) return
        val canvas = surfaceHolder.lockCanvas()
        canvas.drawBitmap(background, 0f, 0f, paint)

        if (gameState == GameState.PLAYING) {
            // Draw obstacles using pre-scaled dimensions
            obstacles.toList().forEach { obstacle ->
                val matrix = Matrix()
                matrix.postScale(obstacle.scale, obstacle.scale)
                matrix.postTranslate(obstacle.x, obstacle.y)
                canvas.drawBitmap(obstacle.bitmap, matrix, paint)
            }

            // Draw player with flashing effect
            val frameIndex = (frameCounter / 10) % playerFrames.size
            val playerBitmap = playerFrames[frameIndex]
            val flashPaint = Paint()
            flashPaint.colorFilter =
                if ((frameCounter / 20) % 2 == 0) PorterDuffColorFilter(Color.CYAN, PorterDuff.Mode.ADD)
                else null
            canvas.drawBitmap(playerBitmap, playerX, playerY, flashPaint)

            // Draw HUD
            paint.color = Color.WHITE
            paint.textSize = 60f
            canvas.drawText("Score: $score", 50f, 100f, paint)
            canvas.drawText("Distance: ${distanceTraveled / 100}m", 50f, 180f, paint)

        } else if (gameState == GameState.GAME_OVER) {
            paint.color = Color.argb(180, 0,0,0)
            canvas.drawRect(0f,0f,screenWidth.toFloat(),screenHeight.toFloat(),paint)
            val alpha = (Math.abs(Math.sin(frameCounter * 0.1)) * 255).toInt()
            paint.color = Color.argb(alpha, 255,0,0)
            canvas.drawRect(0f,0f,screenWidth.toFloat(),screenHeight.toFloat(),paint)
            val pulse = (70 + 20 * Math.sin(frameCounter * 0.2)).toFloat()
            paint.color = Color.RED
            paint.textSize = 120f + pulse
            canvas.drawText("GAME OVER", screenWidth / 4f, screenHeight / 2f, paint)
            paint.color = Color.WHITE
            paint.textSize = 70f
            canvas.drawText("Score: $score", screenWidth / 3f, screenHeight / 2f + 120f, paint)
            canvas.drawText("Tap to Restart", screenWidth / 4f, screenHeight / 2f + 250f, paint)
            frameCounter++
        }

        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    private fun control() { Thread.sleep(17) }

    fun moveLeft() { if (currentLaneIndex > 0) currentLaneIndex-- }
    fun moveRight() { if (currentLaneIndex < lanePositions.size-1) currentLaneIndex++ }
    fun moveUp() { playerY = (playerY - 200f).coerceAtLeast(0f) }
    fun moveDown() { playerY = (playerY + 200f).coerceAtMost(screenHeight - 150f) }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        if (gameState == GameState.GAME_OVER && event?.action == android.view.MotionEvent.ACTION_DOWN) resetGame()
        return true
    }

    private fun resetGame() {
        score = 0
        distanceTraveled = 0
        obstacles.clear()
        lastObstacleY = 0f
        currentLaneIndex = 1
        playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
        playerY = screenHeight - 400f
        gameState = GameState.PLAYING
        spawnObstacleRow() // ensure at least one obstacle exists at start
    }

    private fun spawnObstacleRow() {
        val lanesArray = arrayOf(0, 1, 2)
        val emptyLanesCount = Random.nextInt(0,2)
        val emptyLanes = lanesArray.toList().shuffled().take(emptyLanesCount)
        lanesArray.forEach { lane ->
            if (!emptyLanes.contains(lane)) {
                val y = -obstacleBitmap.height.toFloat() * 2  // slightly above screen
                val scale = 0.4f  // initial visible size
                obstacles.add(
                    Obstacle(
                        x = lanePositions[lane] - obstacleBitmap.width * scale / 2,
                        y = y,
                        bitmap = obstacleBitmap,
                        width = (obstacleBitmap.width * scale).toInt(),
                        height = (obstacleBitmap.height * scale).toInt(),
                        scale = scale,
                        laneIndex = lane
                    )
                )
            }
        }
    }

    fun startGame() {
        isPlaying = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    fun stopGame() {
        isPlaying = false
        gameThread = null
    }

    private fun checkCollisions() {
        // Loop through all obstacles
        for (obstacle in obstacles) {
            // Define player rectangle
            val playerRect = RectF(
                playerX,
                playerY,
                playerX + playerFrames[0].width,
                playerY + playerFrames[0].height
            )

            // Define obstacle rectangle
            val obstacleRect = RectF(
                obstacle.x,
                obstacle.y,
                obstacle.x + obstacle.width,
                obstacle.y + obstacle.height
            )

            // Check intersection
            if (RectF.intersects(playerRect, obstacleRect)) {
                gameState = GameState.GAME_OVER
                break // no need to check further
            }
        }
    }
}

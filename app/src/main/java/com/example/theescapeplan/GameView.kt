package com.example.theescapeplan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null)
    : SurfaceView(context, attrs), Runnable {

    // -------------------------------
    // Surface and drawing setup
    // -------------------------------
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()
    private var gameThread: Thread? = null
    private var isPlaying = false
    private var screenHeight = 0
    private var screenWidth = 0

    // -------------------------------
    // Single background scrolling
    // -------------------------------
    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
    private var bgY1 = 0f
    private var bgY2 = -background.height.toFloat()
    private var bgSpeed = 12f // pixels per frame

    // -------------------------------
    // Player
    // -------------------------------
    private val player: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.player)
    private var laneX = 0f
    private var laneY = 1200f
    private var velocityY = 0f
    private var isJumping = false

    // Lane system: left, center, right
    private val lanesX = listOf(150f, 400f, 650f)
    private var currentLane = 1 // start in middle lane

    // -------------------------------
    // Obstacles
    // -------------------------------
    private val obstacle: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.obstacle)
    private val obstacles = mutableListOf<Pair<Float, Float>>()
    private val obstacleSpeed = bgSpeed

    // -------------------------------
    // Score
    // -------------------------------
    private var score = 0

    // -------------------------------
    // Initialize SurfaceHolder
    // -------------------------------
    init {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                screenHeight = height
                screenWidth = width

                // Place player in middle lane
                laneX = lanesX[currentLane]

                startGame()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) { stopGame() }
        })
    }

    // -------------------------------
    // Game loop
    // -------------------------------
    override fun run() {
        while (isPlaying) {
            update()
            draw()
            control()
        }
    }

    // -------------------------------
    // Update game state
    // -------------------------------
    private fun update() {
        // --- Update background ---
        bgY1 += bgSpeed
        bgY2 += bgSpeed

        // Reset when off-screen
        if (bgY1 >= screenHeight) bgY1 = bgY2 - background.height
        if (bgY2 >= screenHeight) bgY2 = bgY1 - background.height

        // --- Player jump physics ---
        if (isJumping) {
            velocityY += 1f // gravity
            laneY += velocityY
            if (laneY >= 1200f) { // landed
                laneY = 1200f
                isJumping = false
                velocityY = 0f
            }
        }

        // --- Update obstacles ---
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val (x, y) = iterator.next()
            val newY = y + obstacleSpeed
            if (newY > screenHeight) {
                iterator.remove()
                score += 1
            } else {
                iterator.set(Pair(x, newY))
            }
        }

        // --- Spawn obstacles randomly ---
        if (obstacles.size < 3) {
            val laneIndex = Random.nextInt(0, lanesX.size)
            obstacles.add(Pair(lanesX[laneIndex], -obstacle.height.toFloat()))
        }

        // --- Collision detection ---
        val playerRect = Rect(laneX.toInt(), laneY.toInt(),
            (laneX + player.width).toInt(),
            (laneY + player.height).toInt())

        obstacles.forEach { (x, y) ->
            val obstacleRect = Rect(x.toInt(), y.toInt(),
                (x + obstacle.width).toInt(),
                (y + obstacle.height).toInt())
            if (Rect.intersects(playerRect, obstacleRect)) {
                stopGame()
            }
        }
    }

    // -------------------------------
    // Draw everything
    // -------------------------------
    private fun draw() {
        if (!surfaceHolder.surface.isValid) return

        val canvas: Canvas = surfaceHolder.lockCanvas()
        canvas.drawColor(Color.BLACK) // clear screen

        // --- Draw background ---
        canvas.drawBitmap(background, 0f, bgY1, paint)
        canvas.drawBitmap(background, 0f, bgY2, paint)

        // --- Draw player ---
        canvas.drawBitmap(player, laneX, laneY, paint)

        // --- Draw obstacles ---
        obstacles.forEach { (x, y) ->
            canvas.drawBitmap(obstacle, x, y, paint)
        }

        // --- Draw score ---
        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("Score: $score", 50f, 100f, paint)

        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    // -------------------------------
    // Control FPS (~60fps)
    // -------------------------------
    private fun control() {
        Thread.sleep(17)
    }

    // -------------------------------
    // Start / stop game loop
    // -------------------------------
    fun startGame() {
        isPlaying = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    fun stopGame() {
        isPlaying = false
        gameThread?.join()
    }

    // -------------------------------
    // Handle touch events (jump)
    // -------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isJumping) {
                    isJumping = true
                    velocityY = -20f // jump
                }
            }
            // Later: detect swipe left/right for lane changes
        }
        return true
    }
}

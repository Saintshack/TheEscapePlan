package com.example.theescapeplan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random
import androidx.core.graphics.scale
import kotlin.Array
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CompletableDeferred

data class Obstacle(
    var x: Float,
    var y: Float,
    val bitmap: Bitmap,
    var width: Int,
    var height: Int,
    var scale: Float = 0.2f,
    val laneIndex: Int,
    val jumpOrSlide: Boolean
)

data class Coin(
    var x: Float,
    var y: Float,
    var bitmap: Bitmap,
    var width: Int,
    var height: Int,
    var scale: Float,
    var laneIndex: Int
) {
    fun update(speed: Float) {
        y += speed
    }
}

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), Runnable {

    private var gameThread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    private val trailImageMap = mapOf(
        "trail1" to R.drawable.trail_blue,
        "trail2" to R.drawable.trail_red
    )

    private val glowImageMap = mapOf(
        "glow1" to Color.YELLOW,
        "glow2" to Color.GREEN
    )

    private val accessoryImageMap = mapOf(
        "acc1" to R.drawable.hat,
        "acc2" to R.drawable.scarf
    )

    private val screenWidth = 1100
    private val screenHeight = 2000
    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        .scale(screenWidth, screenHeight)
    private val playerFrames: Array<Bitmap> = arrayOf(
        BitmapFactory.decodeResource(resources, R.drawable.player).scale(230, 400),
        BitmapFactory.decodeResource(resources, R.drawable.player2).scale(230, 400)
    )
    private var frameCounter = 0
    private var playerX = 0f
    private var playerY = 0f
    private val lanePositions = floatArrayOf(300f, 550f, 900f)
    private var currentLaneIndex = 1
    private val allCoins = mutableListOf<Coin>()

    private val obstacles = mutableListOf<Obstacle>()
    private val jumpObstaclesBitmap: Array<Bitmap> = arrayOf(
        BitmapFactory.decodeResource(resources, R.drawable.jump_obstacle_1).scale(230, 400),
        BitmapFactory.decodeResource(resources, R.drawable.jump_obstacle_2).scale(230, 400)
    )
    private val slideObstaclesBitmap: Array<Bitmap> = arrayOf(
        BitmapFactory.decodeResource(resources, R.drawable.slide_obstacle_one).scale(230, 400),
        BitmapFactory.decodeResource(resources, R.drawable.slide_obstacle_2).scale(230, 400)
    )
    private val coinBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.coin).scale(230, 400)
    private val obstacleSpacing = 600f
    private var lastObstacleY = 0f
    private var score = 0
    private var distanceTraveled = 0

    private enum class GameState { PLAYING, GAME_OVER }
    private var gameState = GameState.PLAYING

    // --- Jump/Slide Properties ---
    private var isJumping = false
    private var jumpOffsetY = 0f
    private var isSliding = false
    private var slideOffsetY = 0f
    private val jumpHeight = 300f
    private val jumpSpeed = 10f
    private val slideDepth = 100f
    private val slideSpeed = 10f
    private var jumpDescending = false   // tracks whether the player is descending during a jump
    private var slideReturning = false   // tracks whether the player is returning from a slide


    init {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startGame()
                playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
                playerY = screenHeight - 500f
                spawnObstacleRow()
                spawnCoinRow()
                PlayerRepository.init(context)
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

    fun addCoin(amount: Int = 1) {
        PlayerRepository.currentPlayer.coins += amount
        PlayerRepository.savePlayer()
    }

    fun update() {
        if (gameState != GameState.PLAYING) return

        val obstacleSpeed = 1f
        distanceTraveled += obstacleSpeed.toInt()
        if (distanceTraveled % 100 == 0) score += 1

        // Spawn obstacles at fixed intervals
        if (distanceTraveled - lastObstacleY >= obstacleSpacing) {
            spawnObstacleRow()
            if (Random.nextFloat() < 0.6f) {  // 60% chance
                spawnCoinRow()
            }
            lastObstacleY = distanceTraveled.toFloat()
        }

        val startY = 1200
        val endY = 1784

        // Update obstacles
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.y += obstacleSpeed

            val t = ((obstacle.y - startY) / (endY - startY)).coerceIn(0f, 1f)
            obstacle.scale = 0.2f + 1.2f * t
            obstacle.width = (obstacle.bitmap.width * obstacle.scale).toInt()
            obstacle.height = (obstacle.bitmap.height * obstacle.scale).toInt()

            obstacle.x = lanePositions[obstacle.laneIndex] - obstacle.width / 2

            val bottomEdge = obstacle.y + obstacle.height
            if (bottomEdge > 1568) {
                iterator.remove()
            }
        }

        val coinIterator = allCoins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            coin.update(obstacleSpeed)
            if (coin.y - coin.height > 1884) {
                coinIterator.remove()
            }
        }

        // Update player X in current lane
        playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2

        // --- Jump Logic (full motion with descending phase) ---
        if (isJumping) {
            if (!jumpDescending) { // going up
                jumpOffsetY -= jumpSpeed
                if (jumpOffsetY <= -jumpHeight) jumpDescending = true
            } else { // descending
                jumpOffsetY += jumpSpeed
                if (jumpOffsetY >= 0f) {
                    jumpOffsetY = 0f
                    isJumping = false
                    jumpDescending = false
                }
            }
        }

// --- Slide Logic (full motion with return phase) ---
        if (isSliding) {
            if (!slideReturning) { // sliding down
                slideOffsetY += slideSpeed
                if (slideOffsetY >= slideDepth) slideReturning = true
            } else { // returning to normal
                slideOffsetY -= slideSpeed
                if (slideOffsetY <= 0f) {
                    slideOffsetY = 0f
                    isSliding = false
                    slideReturning = false
                }
            }
        }


        checkCollisions()
        frameCounter++
    }

    private fun draw() {
        if (!surfaceHolder.surface.isValid) return
        val canvas = surfaceHolder.lockCanvas()

        if (gameState == GameState.PLAYING) {
            canvas.drawBitmap(background, 0f, 0f, paint)
            obstacles.toList().forEach { obstacle ->
                val matrix = Matrix()
                matrix.postScale(obstacle.scale, obstacle.scale)
                matrix.postTranslate(obstacle.x, obstacle.y)
                canvas.drawBitmap(obstacle.bitmap, matrix, paint)
            }
            allCoins.toList().forEach { coin ->
                val matrix = Matrix()
                matrix.postScale(coin.scale, coin.scale)
                matrix.postTranslate(coin.x, coin.y)
                canvas.drawBitmap(coin.bitmap, matrix, paint)
            }

            val frameIndex = (frameCounter / 10) % playerFrames.size
            val playerBitmap = playerFrames[frameIndex]

            // --- Neon Player Effect ---
            val neonPaint = Paint()
            val cycle = (Math.sin(frameCounter * 0.1) * 0.5 + 0.5).toFloat() // 0..1 range
            val neonColor = Color.argb((150 * cycle).toInt(), 255, 0, 255)   // pulsing magenta
            neonPaint.colorFilter = PorterDuffColorFilter(neonColor, PorterDuff.Mode.ADD)

            val playerDrawX = playerX
            val playerDrawY = playerY + jumpOffsetY + slideOffsetY

            val player = PlayerRepository.currentPlayer

// --- TRAIL (still bitmap if you have images) ---
            if (player.equippedTrail != "None") {
                val trailBitmap = getBitmapForItem(player.equippedTrail, "trail")
                canvas.drawBitmap(trailBitmap, playerDrawX, playerDrawY + 100f, null)
            }

// --- GLOW (now just a color) ---
            if (player.equippedGlow != "None") {
                val glowColor = when (player.equippedGlow) {
                    "Yellow" -> Color.YELLOW
                    "Green" -> Color.GREEN
                    else -> Color.MAGENTA // fallback
                }

                val glowPaint = Paint().apply {
                    color = glowColor
                    alpha = 128 // semi-transparent
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                // Draw a glowing circle behind the player
                val glowRadius = 100f // adjust size as needed
                val glowCenterX = playerDrawX + playerFrames[0].width / 2
                val glowCenterY = playerDrawY + playerFrames[0].height / 2
                canvas.drawCircle(glowCenterX, glowCenterY, glowRadius, glowPaint)
            }

// --- ACCESSORIES (still bitmaps) ---
            player.ownedAccessories.forEach { accessory ->
                if (accessory != "None") {
                    val accBitmap = getBitmapForItem(accessory, "accessory")
                    canvas.drawBitmap(accBitmap, playerDrawX, playerDrawY, null)
                }
            }

            // Draw glowing player
            canvas.drawBitmap(playerBitmap, playerDrawX, playerDrawY, neonPaint)

            paint.color = Color.WHITE
            paint.textSize = 60f
            canvas.drawText("Score: $score", 50f, 100f, paint)
            canvas.drawText("Distance: ${distanceTraveled / 100}m", 50f, 180f, paint)
            canvas.drawText("Total Coins: ${PlayerRepository.currentPlayer.coins}", 50f, 260f, paint)
        }
        else if (gameState == GameState.GAME_OVER) {
            // --- Black background ---
            canvas.drawColor(Color.BLACK)

            // --- Neon "GAME OVER" ---
            val neonPaint = Paint()
            neonPaint.textSize = 120f
            neonPaint.typeface = Typeface.DEFAULT_BOLD
            neonPaint.style = Paint.Style.FILL_AND_STROKE
            neonPaint.strokeWidth = 8f
            neonPaint.isAntiAlias = true

            val text = "GAME OVER"
            val textX = screenWidth / 4f
            val textY = screenHeight / 2f

            // Pulsing glow using sin wave
            val glowRadius = 20f + 10f * Math.abs(Math.sin(frameCounter * 0.1))
            val colors = listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW)
            colors.forEach { color ->
                neonPaint.color = color
                neonPaint.setShadowLayer(glowRadius.toFloat(), 0f, 0f, color)
                canvas.drawText(text, textX, textY, neonPaint)
            }

            // Draw score and restart text
            paint.color = Color.WHITE
            paint.textSize = 70f
            canvas.drawText("Score: $score", screenWidth / 3f, screenHeight / 2f + 120f, paint)
            canvas.drawText("Tap to Restart", screenWidth / 4f, screenHeight / 2f + 250f, paint)

            frameCounter++
        }

        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    // --- Utility function for safe bitmap loading ---
    @SuppressLint("DiscouragedApi")
    private fun loadBitmapFromResOrPath(identifier: String): Bitmap {
        return when {
            identifier == "None" -> {
                createBitmap(1, 1) // invisible fallback
            }
            identifier.startsWith("res:") -> {
                // Example: "res:drawable/glow_blue"
                val resName = identifier.removePrefix("res:")
                val resId = resources.getIdentifier(resName, null, context.packageName)
                BitmapFactory.decodeResource(resources, resId)
            }
            else -> {
                // Assume it’s a file path
                BitmapFactory.decodeFile(identifier)
            }
        }
    }


    private fun control() { Thread.sleep(17) }

    fun moveLeft() { if (currentLaneIndex > 0) currentLaneIndex-- }
    fun moveRight() { if (currentLaneIndex < lanePositions.size-1) currentLaneIndex++ }

    fun startJump() { if (!isJumping && jumpOffsetY >= 0f) isJumping = true }
    fun startSlide() { if (!isSliding && slideOffsetY <= 0f) isSliding = true }

    fun moveUp() { startJump() }
    fun moveDown() { startSlide() }

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
        spawnObstacleRow()
    }

    private fun spawnObstacleRow() {
        val lanesArray = arrayOf(0, 1, 2)
        val emptyLanesCount = Random.nextInt(0, 2) // random 0 or 1 empty lanes
        val emptyLanes = lanesArray.toList().shuffled().take(emptyLanesCount)

        lanesArray.forEach { lane ->
            if (!emptyLanes.contains(lane)) {
                // Step 1: Pick jump (true) or slide (false)
                val isJump = Random.nextBoolean()

                // Step 2: Pick a random bitmap index (0 or 1)
                val bitmapIndex = Random.nextInt(0, 2)
                val chosenBitmap = if (isJump) {
                    jumpObstaclesBitmap[bitmapIndex]
                } else {
                    slideObstaclesBitmap[bitmapIndex]
                }

                // Step 3: Now we can set scale and positions
                val scale = 0.4f
                val y = 1200.toFloat()

                obstacles.add(
                    Obstacle(
                        x = lanePositions[lane] - chosenBitmap.width * scale / 2,
                        y = y,
                        bitmap = chosenBitmap,
                        width = (chosenBitmap.width * scale).toInt(),
                        height = (chosenBitmap.height * scale).toInt(),
                        scale = scale,
                        laneIndex = lane,
                        jumpOrSlide = isJump // pass boolean here
                    )
                )
            }
        }
    }

    private fun spawnCoinRow() {
        val chosenLane = arrayOf(0, 1, 2).random()
        val scale = 0.25f
        val y = 1200f  // spawn near top, like obstacles

        val coinWidth = (coinBitmap.width * scale).toInt()
        val coinHeight = (coinBitmap.height * scale).toInt()

        allCoins.add(
            Coin(
                x = lanePositions[chosenLane] - coinWidth / 2f,
                y = y,
                bitmap = coinBitmap,
                width = coinWidth,
                height = coinHeight,
                scale = scale,
                laneIndex = chosenLane
            )
        )
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
        val horizontalMargin = 20f
        val verticalTopMargin = 50f
        val verticalBottomMargin = 20f

        val iterator = obstacles.iterator() // safe removal while iterating
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            val obstacleMargin = 10f

            // Player rectangle (adjusted by jump/slide offsets)
            val playerRect = RectF(
                playerX + horizontalMargin,
                playerY + jumpOffsetY + slideOffsetY + verticalTopMargin,
                playerX + playerFrames[0].width - horizontalMargin,
                playerY + jumpOffsetY + slideOffsetY + playerFrames[0].height - verticalBottomMargin
            )

            // Obstacle rectangle
            val obstacleRect = RectF(
                obstacle.x + obstacleMargin,
                obstacle.y + obstacleMargin,
                obstacle.x + obstacle.width - obstacleMargin,
                obstacle.y + obstacle.height - obstacleMargin
            )

            // Check intersection
            if (RectF.intersects(playerRect, obstacleRect)) {
                if (obstacle.jumpOrSlide) {
                    // Jump obstacle → must be jumping
                    if (isJumping) {
                        iterator.remove() // success: jumped over
                    } else {
                        gameState = GameState.GAME_OVER // failed
                        break
                    }
                } else {
                    // Slide obstacle → must be sliding
                    if (isSliding) {
                        iterator.remove() // success: slid under
                    } else {
                        gameState = GameState.GAME_OVER // failed
                        break
                    }
                }
            }
        }
        val coinIterator = allCoins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            val coinMargin = 10f

            val playerRect = RectF(
                playerX + horizontalMargin,
                playerY + jumpOffsetY + slideOffsetY + verticalTopMargin,
                playerX + playerFrames[0].width - horizontalMargin,
                playerY + jumpOffsetY + slideOffsetY + playerFrames[0].height - verticalBottomMargin
            )

            val coinRect = RectF(
                coin.x + coinMargin,
                coin.y + coinMargin,
                coin.x + coin.width - coinMargin,
                coin.y + coin.height - coinMargin
            )

            if (RectF.intersects(playerRect, coinRect)) {
                // Player collects the coin
                addCoin(1)
                coinIterator.remove()
            }
        }
    }
    private fun getBitmapForItem(itemId: String, type: String): Bitmap {
        val resId = when (type) {
            "trail" -> trailImageMap[itemId]
            "glow" -> glowImageMap[itemId]
            "accessory" -> accessoryImageMap[itemId]
            else -> null
        } ?: R.drawable.coin // fallback drawable if ID not found

        return BitmapFactory.decodeResource(resources, resId)
    }

}


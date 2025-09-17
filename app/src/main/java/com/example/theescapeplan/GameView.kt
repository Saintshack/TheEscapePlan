package com.example.theescapeplan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.blue
import kotlin.random.Random
import androidx.core.graphics.scale
import kotlin.Array
import androidx.core.graphics.green
import androidx.core.graphics.red

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
        "Blue Trail" to R.drawable.trail_blue,
        "Red Trail" to R.drawable.trail_red
    )

    private val glowImageMap = mapOf(
        "Yellow Glow" to Color.YELLOW,
        "Green Glow" to Color.GREEN
    )

    private val trailBitmap by lazy {
        val player = PlayerRepository.currentPlayer
        println(player.equippedTrail)
        println(player.equippedGlow)
        if (player.equippedTrail != "None") {
            getBitmapForItem(player.equippedTrail, "trail").scale(100, 100)
        } else null
    }

    data class TrailSegment(
        val x: Float,
        val y: Float,
        val createdAt: Long
    )

    private val trailSegments = mutableListOf<TrailSegment>()
    private val screenWidth = 1100
    private val screenHeight = 2000
    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        .scale(screenWidth, screenHeight)
    private val playerFrames: Array<Bitmap> = arrayOf(
        BitmapFactory.decodeResource(resources, R.drawable.player).scale(230, 400),
        BitmapFactory.decodeResource(resources, R.drawable.player2).scale(230, 400)
    )
    private var frameCounter = 0
    private var lastTrailTime = 0L
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

    enum class GameState { PLAYING, GAME_OVER }
    var gameState = GameState.PLAYING

    private var isJumping = false
    private var jumpOffsetY = 0f
    private var isSliding = false
    private var slideOffsetY = 0f
    private val jumpHeight = 100f
    private val jumpSpeed = 10f
    private val slideDepth = 100f
    private val slideSpeed = 10f
    private var jumpDescending = false
    private var slideReturning = false
    var targetX = 500f

    private val trailInterval = 10L
    val laneChangeSpeed = 40f


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

        val obstacleSpeed = 1f + (distanceTraveled/750)
        distanceTraveled += obstacleSpeed.toInt()
        if (distanceTraveled % 100 == 0) score += 1

        if (distanceTraveled - lastObstacleY >= obstacleSpacing) {
            spawnObstacleRow()
            if (Random.nextFloat() < 0.6f) {
                spawnCoinRow()
            }
            lastObstacleY = distanceTraveled.toFloat()
        }

        val startY = 1200
        val endY = 1784

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

        if (playerX < targetX) {
            playerX = minOf(playerX + laneChangeSpeed, targetX)
        } else if (playerX > targetX) {
            playerX = maxOf(playerX - laneChangeSpeed, targetX)
        }

        if (isJumping) {
            if (!jumpDescending) {
                jumpOffsetY -= jumpSpeed
                if (jumpOffsetY <= -jumpHeight) jumpDescending = true
            } else {
                jumpOffsetY += jumpSpeed
                if (jumpOffsetY >= 0f) {
                    jumpOffsetY = 0f
                    isJumping = false
                    jumpDescending = false
                }
            }
        }

        if (isSliding) {
            if (!slideReturning) {
                slideOffsetY += slideSpeed
                if (slideOffsetY >= slideDepth) slideReturning = true
            } else {
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

            val playerDrawX = playerX
            val playerDrawY = playerY + jumpOffsetY + slideOffsetY

            val player = PlayerRepository.currentPlayer

            updateTrail(playerX, playerY)
            drawTrail(canvas)

            if (player.equippedGlow != "None") {
                println(player.equippedGlow)
                val glowColor = when (player.equippedGlow) {
                    "Yellow Glow" -> Color.YELLOW
                    "Green Glow" -> Color.GREEN
                    else -> Color.TRANSPARENT
                }
                val neonPaint = Paint()
                var neonColor = Color.argb(
                    0,
                    glowColor.red,
                    glowColor.green,
                    glowColor.blue
                )
                if(glowColor != Color.TRANSPARENT) {
                    val cycle = (Math.sin(frameCounter * 0.1) * 0.5 + 0.5).toFloat()
                    neonColor = Color.argb(
                        (150 * cycle).toInt(),
                        glowColor.red,
                        glowColor.green,
                        glowColor.blue
                    )
                }
                neonPaint.colorFilter = PorterDuffColorFilter(neonColor, PorterDuff.Mode.ADD)
                canvas.drawBitmap(playerBitmap, playerDrawX, playerDrawY, neonPaint)
            }
            else {
                canvas.drawBitmap(playerBitmap, playerDrawX, playerDrawY, null)
            }

            paint.color = Color.WHITE
            paint.textSize = 60f
            canvas.drawText("Score: $score", 50f, 100f, paint)
            canvas.drawText("Distance: ${distanceTraveled / 100}m", 50f, 180f, paint)
            canvas.drawText("Total Coins: ${PlayerRepository.currentPlayer.coins}", 50f, 260f, paint)
        }
        else if (gameState == GameState.GAME_OVER) {
            canvas.drawColor(Color.BLACK)

            val neonPaint = Paint()
            neonPaint.textSize = 120f
            neonPaint.typeface = Typeface.DEFAULT_BOLD
            neonPaint.style = Paint.Style.FILL_AND_STROKE
            neonPaint.strokeWidth = 8f
            neonPaint.isAntiAlias = true
            val text = "GAME OVER"
            val textX = screenWidth / 4f
            val textY = screenHeight / 2f
            val glowRadius = 20f + 10f * Math.abs(Math.sin(frameCounter * 0.1))
            val colors = listOf(Color.CYAN, Color.MAGENTA, Color.YELLOW)
            colors.forEach { color ->
                neonPaint.color = color
                neonPaint.setShadowLayer(glowRadius.toFloat(), 0f, 0f, color)
                canvas.drawText(text, textX, textY, neonPaint)
            }
            paint.color = Color.WHITE
            paint.textSize = 70f
            canvas.drawText("Score: $score", screenWidth / 3f, screenHeight / 2f + 120f, paint)
            canvas.drawText("Tap to Restart", screenWidth / 4f, screenHeight / 2f + 250f, paint)
            frameCounter++
        }

        surfaceHolder.unlockCanvasAndPost(canvas)
    }


    private fun control() { Thread.sleep(17) }

    fun moveLeft() {
        if (currentLaneIndex > 0) currentLaneIndex--
        switchLane(currentLaneIndex)
    }
    fun moveRight() {
        if (currentLaneIndex < lanePositions.size-1) currentLaneIndex++
        switchLane(currentLaneIndex)
    }

    fun startJump() { if (!isJumping && jumpOffsetY >= 0f) isJumping = true }
    fun startSlide() { if (!isSliding && slideOffsetY <= 0f) isSliding = true }

    fun moveUp() { startJump() }
    fun moveDown() { startSlide() }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        if (gameState == GameState.GAME_OVER && event?.action == android.view.MotionEvent.ACTION_DOWN) resetGame()
        return true
    }

    fun resetGame() {
        gameState = GameState.PLAYING
        score = 0
        distanceTraveled = 0
        frameCounter = 0
        lastObstacleY = 0f
        currentLaneIndex = 1
        playerX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
        playerY = screenHeight - 500f
        targetX = playerX
        isJumping = false
        jumpOffsetY = 0f
        jumpDescending = false
        isSliding = false
        slideOffsetY = 0f
        slideReturning = false
        trailSegments.clear()
        lastTrailTime = 0L
        obstacles.clear()
        allCoins.clear()
        spawnObstacleRow()
        spawnCoinRow()
    }


    private fun spawnObstacleRow() {
        val lanesArray = arrayOf(0, 1, 2)
        val emptyLanesCount = Random.nextInt(0, 2)
        val emptyLanes = lanesArray.toList().shuffled().take(emptyLanesCount)

        lanesArray.forEach { lane ->
            if (!emptyLanes.contains(lane)) {
                val isJump = Random.nextBoolean()
                val bitmapIndex = Random.nextInt(0, 2)
                val chosenBitmap = if (isJump) {
                    jumpObstaclesBitmap[bitmapIndex]
                } else {
                    slideObstaclesBitmap[bitmapIndex]
                }
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
                        jumpOrSlide = isJump
                    )
                )
            }
        }
    }

    private fun spawnCoinRow() {
        val chosenLane = arrayOf(0, 1, 2).random()
        val scale = 0.25f
        val y = 1200f
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

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            val obstacleMargin = 10f
            val playerRect = RectF(
                playerX + horizontalMargin,
                playerY + jumpOffsetY + slideOffsetY + verticalTopMargin,
                playerX + playerFrames[0].width - horizontalMargin,
                playerY + jumpOffsetY + slideOffsetY + playerFrames[0].height - verticalBottomMargin
            )
            val obstacleRect = RectF(
                obstacle.x + obstacleMargin,
                obstacle.y + obstacleMargin,
                obstacle.x + obstacle.width - obstacleMargin,
                obstacle.y + obstacle.height - obstacleMargin
            )
            if (RectF.intersects(playerRect, obstacleRect)) {
                if (obstacle.jumpOrSlide) {
                    if (isJumping) {
                        iterator.remove()
                    } else {
                        gameState = GameState.GAME_OVER
                        break
                    }
                } else {
                    if (isSliding) {
                        iterator.remove()
                    } else {
                        gameState = GameState.GAME_OVER
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
                addCoin(1)
                coinIterator.remove()
            }
        }
    }
    private fun getBitmapForItem(itemId: String, type: String): Bitmap {
        val resId = when (type) {
            "trail" -> trailImageMap[itemId]
            "glow" -> glowImageMap[itemId]
            else -> null
        } ?: R.drawable.coin

        return BitmapFactory.decodeResource(resources, resId)
    }

    fun updateTrail(playerX: Float, playerY: Float) {
        val now = System.currentTimeMillis()
        if (now - lastTrailTime >= trailInterval) {
            trailBitmap?.let {
                trailSegments.add(TrailSegment(playerX, playerY + 200f, now))
            }
            lastTrailTime = now
        }
        trailSegments.removeAll { now - it.createdAt > 500 }
    }

    fun drawTrail(canvas: Canvas) {
        trailBitmap?.let { bitmap ->
            for (segment in trailSegments) {
                canvas.drawBitmap(bitmap, segment.x, segment.y, null)
            }
        }
    }

    fun switchLane(newLaneIndex: Int) {
        currentLaneIndex = newLaneIndex
        targetX = lanePositions[currentLaneIndex] - playerFrames[0].width / 2
    }

}


package it.gabriele.androidware.game.ranchsheeps.states.common

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.core.os.bundleOf
import it.gabriele.androidware.R
import it.gabriele.androidware.game.engine.GameContext
import it.gabriele.androidware.game.engine.audio.AudioManager
import it.gabriele.androidware.game.engine.network.NetworkManager
import it.gabriele.androidware.game.engine.network.NetworkMessage
import it.gabriele.androidware.game.engine.render.RenderUtilities
import it.gabriele.androidware.game.engine.states.GameState
import it.gabriele.androidware.game.engine.states.GameStateManager
import it.gabriele.androidware.game.math.Vector2
import it.gabriele.androidware.game.ranchsheeps.RanchSheepsGame
import it.gabriele.androidware.game.ranchsheeps.game.Poof
import it.gabriele.androidware.game.ranchsheeps.game.Selection
import it.gabriele.androidware.game.ranchsheeps.game.Sheep
import it.gabriele.androidware.game.view.StatefulNetworkGameView
import java.nio.ByteBuffer
import kotlin.math.abs

class RanchTheSheepsState(
    networkManager: NetworkManager,
    audioManager: AudioManager,
    gameStateManager: GameStateManager,
    context: Context
) : GameState(networkManager, audioManager, gameStateManager, context) {

    companion object {
        private const val TAG = "[Game]::GameState"

        const val NET_SELECTION_BEGIN =     (0x00000a).toByte()
        const val NET_SELECTION_MOVING =    (0x00000b).toByte()
        const val NET_SELECTION_END =       (0x00000c).toByte()

        const val NET_HIDE_SHEEPS =         (0x000014).toByte()
        const val NET_END_GAME =            (0x00001e).toByte()

        const val TOLERANCE = 4

        private const val SHEEP_SIZE = 220

        const val BUNDLE_SHEEPS = "CatchTheSheepState.sheeps"

        const val STATE_SHEEPS = "State.Sheeps"
    }

    private lateinit var grassTexture: Bitmap
    private lateinit var whiteSheepTexture: Bitmap
    private lateinit var darkSheepTexture: Bitmap
    private lateinit var poofTexture: Bitmap

    private var selectionEnd = false

    private lateinit var poofEffects: List<Poof>

    private val sheepTransformMatrix = Matrix()
    private var sheepsCaught = 0
    private lateinit var sheeps: ArrayList<Sheep>

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 120f
        setShadowLayer(10f, 0f, 4f, Color.BLACK)
    }

    private val playerSelection = Selection(Color.RED)
    private val opponentSelection = Selection(Color.BLUE)

    override fun onStateStart(oldState: Bundle?, args: Bundle?) {

        if (oldState == null) {
            args?.let {
                sheeps = it.getParcelableArrayList(BUNDLE_SHEEPS) ?: ArrayList<Sheep>()
                sheeps.forEach { sheep ->
                    // The sheeps positions are between [0, 1]
                    sheep.position?.let { pos ->
                        pos.x = pos.x * GameContext.LOGICAL_PORTRAIT_WIDTH * GameContext.SCALE_X
                        pos.y = pos.y * GameContext.LOGICAL_PORTRAIT_HEIGHT * GameContext.SCALE_Y
                    }
                }
            }
        }
        else {
            sheeps = oldState.getParcelableArrayList(STATE_SHEEPS) ?: ArrayList<Sheep>()
            sheepsCaught = sheeps.filter { it.visible == Sheep.INVISIBLE }.size
        }

        whiteSheepTexture = RenderUtilities.getBitmapFromCache(
            context.resources,
            R.drawable.my_sheep,
            SHEEP_SIZE,
            SHEEP_SIZE
        )
        darkSheepTexture = RenderUtilities.getBitmapFromCache(
            context.resources,
            R.drawable.my_dark_sheep,
            SHEEP_SIZE,
            SHEEP_SIZE
        )
        grassTexture = RenderUtilities.getBitmapFromCache(context.resources, R.drawable.grass, 512, 512)
        poofTexture = RenderUtilities.getBitmapFromCache(context.resources, R.drawable.poof, 1200, 1200)

        poofEffects = List(sheeps.size) {
            Poof(Vector2(), poofTexture)
        }

    }

    override fun onStateSave(state: Bundle) {
        Log.d(TAG, "onStateSave: saving sheeps state...")
        state.putParcelableArrayList(STATE_SHEEPS, sheeps)
    }

    override fun onStateEnd() {
        audioManager.releaseSoundPlayer(R.raw.sheep_cry)
    }

    override fun readInputEvent(event: MotionEvent) {

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                playerSelection.setStartingPoints(x, y)

                /* Tell to the other player that a selection has begin */
                val outgoingSelectionBuff =
                    ByteBuffer.allocate((Float.SIZE_BYTES * 2) + Int.SIZE_BYTES)
                outgoingSelectionBuff.put(NET_SELECTION_BEGIN).putFloat(x).putFloat(y)
                networkManager.send(NetworkMessage(outgoingSelectionBuff))

            }
            MotionEvent.ACTION_MOVE -> {

                val dx = abs(x - playerSelection.x2)
                val dy = abs(y - playerSelection.y2)

                playerSelection.apply {
                    clear()
                    setEndingPoint(x, y)
                    make()
                }

                if (dx >= TOLERANCE || dy >= TOLERANCE) {
                    val outgoingSelectionBuff =
                        ByteBuffer.allocate((Float.SIZE_BYTES * 2) + Int.SIZE_BYTES)
                    outgoingSelectionBuff.put(NET_SELECTION_MOVING).putFloat(x).putFloat(y)
                    networkManager.send(NetworkMessage(outgoingSelectionBuff))
                }

            }
            MotionEvent.ACTION_UP -> {

                selectionEnd = true

                /* Cleanup the path selection */
                playerSelection.path.reset()

                /* The selection rect has been drawn */
                val outgoingSelectionBuff = ByteBuffer.allocate(Int.SIZE_BYTES)
                outgoingSelectionBuff.put(NET_SELECTION_END)
                networkManager.send(NetworkMessage(outgoingSelectionBuff))

            }
        }
    }

    override fun update(deltaTime: Float) {

        networkManager.receive()?.let { incomingMsg ->

            if (incomingMsg.buffer == null) return@let

            when (incomingMsg.buffer.get()) {
                NET_SELECTION_BEGIN -> {
                    val x = incomingMsg.buffer.float
                    val y = incomingMsg.buffer.float
                    opponentSelection.setStartingPoints(x, y)
                }
                NET_SELECTION_MOVING -> {
                    val x = incomingMsg.buffer.float
                    val y = incomingMsg.buffer.float
                    opponentSelection.apply {
                        clear()
                        setEndingPoint(x, y)
                        make()
                    }
                }
                NET_SELECTION_END -> {
                    opponentSelection.path.reset()
                }
                NET_HIDE_SHEEPS -> {

                    val howManySheeps = incomingMsg.buffer.int

                    repeat(howManySheeps) {

                        val index = incomingMsg.buffer.int
                        val sheep = sheeps[index]

                        // Hide the sheep
                        sheep.visible = Sheep.INVISIBLE

                        // Show poof effect and set its position
                        poofEffects[index].apply {
                            visible = true
                            position.x = sheep.position?.x ?: 0f
                            position.y = sheep.position?.y ?: 0f
                        }

                        // Play 'Beeeh' sound
                        audioManager.playOneShootSound(R.raw.sheep_cry)
                    }

                }
                NET_END_GAME -> {
                    val colorWinner = incomingMsg.buffer.get()
                    endGame(colorWinner)
                }
                else -> {
                    Log.d(TAG, "update: unrecognized net message?")
                }
            }
        }

        if (selectionEnd) {

            val targetColor = if (networkManager.isGameMaster) Sheep.WHITE_TYPE else Sheep.DARK_TYPE
            val caughtSheeps = mutableListOf<Pair<Int, Sheep>>()

            /* Check if the are sheeps inside the selection rect */
            sheeps.forEachIndexed { index, sheep ->

                val pos = sheep.position!!

                val isContainedInX =
                    pos.x >= playerSelection.rect.left && pos.x <= playerSelection.rect.right
                val isContainedInY =
                    pos.y >= playerSelection.rect.top && pos.y <= playerSelection.rect.bottom

                if (isContainedInX && isContainedInY && sheep.visible == Sheep.VISIBLE) {
                    caughtSheeps.add(Pair(index, sheep))
                }
            }

            if (caughtSheeps.size > 0 && caughtSheeps.none { p -> p.second.color != targetColor }) {

                /* The player caught only the sheeps of the same color */
                Log.d(TAG, "readInputEvent: ${caughtSheeps.size} sheeps")
                sheepsCaught += caughtSheeps.size

                /* Hide the sheeps from the screen */
                caughtSheeps.forEach { p ->

                    // Show poof effect and set its position
                    poofEffects[p.first].apply {
                        visible = true
                        position.x = p.second.position?.x ?: 0f
                        position.y = p.second.position?.y ?: 0f
                    }

                    p.second.visible = Sheep.INVISIBLE
                    audioManager.playOneShootSound(R.raw.sheep_cry)
                }

                /* Send a network message to the other player telling which sheeps must be hidden */
                val sheepsHiddenBuff =
                    ByteBuffer.allocate(Int.SIZE_BYTES * caughtSheeps.size + (Int.SIZE_BYTES * 2))
                sheepsHiddenBuff.put(NET_HIDE_SHEEPS).putInt(caughtSheeps.size)
                caughtSheeps.forEach { p -> sheepsHiddenBuff.putInt(p.first) }

                networkManager.send(NetworkMessage(sheepsHiddenBuff))
            }

            if (sheepsCaught == (RanchSheepsGame.MAX_SHEEPS / 2)) {

                /* The player caught all the sheeps */
                val buffWinner = ByteBuffer.allocate(Int.SIZE_BYTES + Byte.SIZE_BYTES)
                buffWinner.put(NET_END_GAME).put(targetColor)
                networkManager.send(NetworkMessage(buffWinner))

                endGame(targetColor)
            }

            selectionEnd = false
        }

        /* Update visible sheeps position */
        sheeps.filter { it.visible == Sheep.VISIBLE }.forEach {

            val sheepSprite =
                if (it.color == Sheep.WHITE_TYPE) whiteSheepTexture else darkSheepTexture

            val maxX =
                ((GameContext.LOGICAL_PORTRAIT_WIDTH * GameContext.SCALE_X) - sheepSprite.width).toFloat()
            val minX = (sheepSprite.width).toFloat()

            val maxY =
                ((GameContext.LOGICAL_PORTRAIT_HEIGHT * GameContext.SCALE_Y) - sheepSprite.height).toFloat()
            val minY = (sheepSprite.height).toFloat()

            val pos = it.position!!
            val vel = it.velocity!!

            pos.x = pos.x + (vel.x * deltaTime)
            pos.y = pos.y + (vel.y * deltaTime)

            if (pos.x > maxX) {
                pos.x = maxX; vel.x *= -1
            } else if (pos.x < minX) {
                pos.x = minX; vel.x *= -1
            }

            if (pos.y > maxY) {
                pos.y = maxY; vel.y *= -1
            } else if (pos.y < minY) {
                pos.y = minY; vel.y *= -1
            }

        }

        /* Update poof effects only for the visible ones */
        poofEffects.filter { it.visible }.forEach { it.update(deltaTime) }

    }

    private fun endGame(colorWinner: Byte) {
        gameStateManager.switch(
            RanchSheepsGame.END_STATE, bundleOf(
                EndGameState.BUNDLE_COLOR_WINNER to colorWinner
            )
        )
    }

    override fun render(canvas: Canvas) {

        /* Draw the background */
        RenderUtilities.drawTiledBackground(canvas, grassTexture)

        /* Draw the Sheeps inside the scene */
        sheeps.filter { it.visible == Sheep.VISIBLE }.forEach {
            val sheepSprite =
                if (it.color == Sheep.WHITE_TYPE) whiteSheepTexture else darkSheepTexture
            sheepTransformMatrix.setTranslate(it.position?.x ?: 0f, it.position?.y ?: 0f)
            canvas.drawBitmap(sheepSprite, sheepTransformMatrix, null)
        }

        /* Draw the poof effect if visible */
        poofEffects.filter { it.visible }.forEach { it.render(canvas) }

        /* Draw the score above */
        val score = "$sheepsCaught"
        val cx = (canvas.width / 2f) - (scorePaint.measureText(score) / 2f)
        canvas.drawText(score, cx, (64 * GameContext.SCALE_Y).toFloat(), scorePaint)

        /* Draw the current selection */
        canvas.drawPath(playerSelection.path, playerSelection.paint)
        canvas.drawPath(opponentSelection.path, opponentSelection.paint)
    }

}
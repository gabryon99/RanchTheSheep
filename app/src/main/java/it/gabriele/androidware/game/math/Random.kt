package it.gabriele.androidware.game.math

import kotlin.random.Random

fun Random.sign(): Double {
    return if (this.nextBoolean()) 1.0 else -1.0
}
package dev.vmellos.airhockey.core.ads

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class AdCooldownTracker {
    private val lastAdTimestamp = AtomicLong(0L)
    private val matchesCounter = AtomicInteger(0)

    companion object {
        const val COOLDOWN_INTERVAL_MS = 180_000L // 3 minutes
        const val MINIMUM_MATCHES = 2 // Require at least 2 matches between interstitials
    }

    fun recordMatchFinished(): Int = matchesCounter.incrementAndGet()

    fun canShowInterstitial(
        isAdFreeTemporarily: Boolean,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (isAdFreeTemporarily) return false
        val matches = matchesCounter.get()
        if (matches < MINIMUM_MATCHES) return false
        val elapsed = currentTimeMs - lastAdTimestamp.get()
        return elapsed >= COOLDOWN_INTERVAL_MS
    }

    fun recordInterstitialShown(currentTimeMs: Long = System.currentTimeMillis()) {
        lastAdTimestamp.set(currentTimeMs)
        matchesCounter.set(0)
    }

    fun reset() {
        lastAdTimestamp.set(0L)
        matchesCounter.set(0)
    }
}

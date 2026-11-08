package com.htmake.reader.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * 轻量级书源健康度熔断器 (SourceCircuitBreaker)
 * 记录每个书源的连续失败次数与平均响应耗时，并在连续失败达标时触发冷却熔断。
 */
object SourceCircuitBreaker {

    data class SourceHealth(
        var consecutiveFailures: Int = 0,
        var totalRequests: Long = 0,
        var totalRtMs: Long = 0,
        var cooldownUntil: Long = 0L
    ) {
        val avgRt: Long
            get() = if (totalRequests > 0) totalRtMs / totalRequests else 9999L

        val isCoolingDown: Boolean
            get() = System.currentTimeMillis() < cooldownUntil
    }

    private val healthMap = ConcurrentHashMap<String, SourceHealth>()

    // 连续失败 3 次触发熔断
    private const val MAX_CONSECUTIVE_FAILURES = 3
    // 熔断冷却时间：30 分钟
    private const val COOLDOWN_DURATION_MS = 30 * 60 * 1000L

    /**
     * 判断书源当前是否可用（未处于冷却期）
     */
    fun isAvailable(sourceUrl: String): Boolean {
        val health = healthMap[sourceUrl] ?: return true
        if (health.isCoolingDown) {
            return false
        }
        return true
    }

    /**
     * 成功回调：重置连续失败计数，更新平均响应耗时
     */
    fun recordSuccess(sourceUrl: String, rtMs: Long) {
        healthMap.compute(sourceUrl) { _, existing ->
            val health = existing ?: SourceHealth()
            health.consecutiveFailures = 0
            health.totalRequests++
            health.totalRtMs += rtMs
            health.cooldownUntil = 0L
            health
        }
    }

    /**
     * 失败或超时回调：递增连续失败次数，达到阈值触发 30 分钟冷却
     */
    fun recordFailure(sourceUrl: String) {
        healthMap.compute(sourceUrl) { _, existing ->
            val health = existing ?: SourceHealth()
            health.consecutiveFailures++
            if (health.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                health.cooldownUntil = System.currentTimeMillis() + COOLDOWN_DURATION_MS
            }
            health
        }
    }

    /**
     * 获取书源排序权重：未冷却且 avgRt < 800ms 优先调度
     */
    fun getSortScore(sourceUrl: String): Long {
        val health = healthMap[sourceUrl] ?: return 1000L // 默认初始分
        if (health.isCoolingDown) {
            return Long.MAX_VALUE // 冷却中垫底
        }
        return health.avgRt
    }

    /**
     * 获取当前熔断书源数量
     */
    fun getCoolingDownCount(): Int {
        val now = System.currentTimeMillis()
        return healthMap.values.count { it.cooldownUntil > now }
    }
}

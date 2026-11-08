package com.htmake.reader.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 线程安全书源健康度熔断与分级调度器 (SourceCircuitBreaker)
 * 记录每个书源的连续失败次数与平均响应耗时，并在连续失败达标时触发冷却熔断。
 * 支持三级梯队调度（极速源 < 1500ms、海外正常源 1500~6000ms、慢源 > 6000ms）。
 */
object SourceCircuitBreaker {

    class SourceHealth {
        val consecutiveFailures = AtomicInteger(0)
        val totalRequests = AtomicLong(0)
        val totalRtMs = AtomicLong(0)
        val cooldownUntil = AtomicLong(0L)

        val avgRt: Long
            get() {
                val reqs = totalRequests.get()
                return if (reqs > 0) totalRtMs.get() / reqs else 1000L
            }

        val isCoolingDown: Boolean
            get() = System.currentTimeMillis() < cooldownUntil.get()
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
        return !health.isCoolingDown
    }

    /**
     * 成功回调：重置连续失败计数，更新平均响应耗时
     */
    fun recordSuccess(sourceUrl: String, rtMs: Long) {
        val health = healthMap.computeIfAbsent(sourceUrl) { SourceHealth() }
        health.consecutiveFailures.set(0)
        health.totalRequests.incrementAndGet()
        health.totalRtMs.addAndGet(rtMs)
        health.cooldownUntil.set(0L)
    }

    /**
     * 失败或超时回调：递增连续失败次数，达到阈值触发 30 分钟冷却
     */
    fun recordFailure(sourceUrl: String) {
        val health = healthMap.computeIfAbsent(sourceUrl) { SourceHealth() }
        val failures = health.consecutiveFailures.incrementAndGet()
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            health.cooldownUntil.set(System.currentTimeMillis() + COOLDOWN_DURATION_MS)
        }
    }

    /**
     * 获取书源排序权重：
     * - 冷却中：Long.MAX_VALUE 垫底
     * - 第一梯队（< 1500ms 极速国内源）：优先最先发起连接
     * - 第二梯队（1500ms ~ 6000ms 正常海外源）：居中调度
     * - 第三梯队（> 6000ms 慢源）：排在靠后位置
     */
    fun getSortScore(sourceUrl: String): Long {
        val health = healthMap[sourceUrl] ?: return 1000L
        if (health.isCoolingDown) {
            return Long.MAX_VALUE
        }
        val rt = health.avgRt
        return when {
            rt < 1500L -> rt
            rt <= 6000L -> 10000L + rt
            else -> 50000L + rt
        }
    }

    /**
     * 获取当前熔断书源数量
     */
    fun getCoolingDownCount(): Int {
        val now = System.currentTimeMillis()
        return healthMap.values.count { it.cooldownUntil.get() > now }
    }
}

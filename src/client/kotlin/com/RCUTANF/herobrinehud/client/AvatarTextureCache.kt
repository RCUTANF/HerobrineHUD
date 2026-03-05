package com.RCUTANF.herobrinehud.client

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 客户端头像纹理缓存
 *
 * 根据服务端下发的头像 URL（来自 PlayerInfo.avatar）异步下载皮肤图片，
 * 注册为 Minecraft DynamicTexture 并缓存，供 PlayerCardRenderer 渲染头部切片。
 *
 * 皮肤图片为标准 Minecraft 皮肤格式（64×64），头部底层位于 (8,8)~(16,16)，
 * 覆盖层位于 (40,8)~(48,16)。
 */
object AvatarTextureCache {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/AvatarTextureCache")

    /** 条目状态 */
    private enum class State { LOADING, READY, FAILED }

    private data class Entry(
        val state: State,
        val location: Identifier? = null
    )

    /** url -> 缓存条目 */
    private val cache = ConcurrentHashMap<String, Entry>()

    /** 后台下载线程池 */
    private val executor: ExecutorService = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "HerobrineHUD-AvatarDownloader").also { it.isDaemon = true }
    }

    /** 用于生成唯一纹理路径的计数器 */
    @Volatile
    private var idCounter = 0

    /**
     * 查询头像纹理的 Identifier。
     *
     * - 首次调用时触发异步下载，返回 null（调用方使用 Fallback）
     * - 下载成功后返回已注册的纹理 Identifier
     * - 下载失败后始终返回 null，不重试
     *
     * @param avatarUrl PlayerInfo.avatar 中的皮肤 URL
     */
    fun getTexture(avatarUrl: String): Identifier? {
        val entry = cache[avatarUrl]
        return when (entry?.state) {
            State.READY  -> entry.location
            State.FAILED -> null
            State.LOADING -> null
            else -> {
                // 首次访问：标记 LOADING 并提交下载任务
                cache[avatarUrl] = Entry(State.LOADING)
                submitDownload(avatarUrl)
                null
            }
        }
    }

    /**
     * 释放单个 URL 的纹理（断线 / 清理时调用）
     */
    fun evict(avatarUrl: String) {
        val entry = cache.remove(avatarUrl) ?: return
        if (entry.state == State.READY && entry.location != null) {
            Minecraft.getInstance().execute {
                Minecraft.getInstance().textureManager.release(entry.location)
                LOGGER.debug("已释放头像纹理: {}", avatarUrl)
            }
        }
    }

    /**
     * 清空全部缓存（断开连接时调用）
     */
    fun clear() {
        cache.keys.toList().forEach { evict(it) }
        LOGGER.debug("头像纹理缓存已清空")
    }

    // ──────────────────────────────────────────────────────────────
    //  内部实现
    // ──────────────────────────────────────────────────────────────

    private fun submitDownload(url: String) {
        executor.submit {
            try {
                LOGGER.debug("开始下载头像: {}", url)

                // 直接用 NativeImage.read 解码 PNG 字节流（与 RealmsTextureManager 相同模式）
                val nativeImage: NativeImage = URI.create(url).toURL().openStream().use { stream ->
                    NativeImage.read(stream)
                }

                // 纹理注册必须在主线程（渲染线程）执行
                Minecraft.getInstance().execute {
                    try {
                        val id = idCounter++
                        val location = Identifier.fromNamespaceAndPath("herobrinehud", "avatar/skin_$id")

                        // DynamicTexture(Supplier<String>, NativeImage) 构造函数会自动 upload()
                        val dynTex = DynamicTexture(location::toString, nativeImage)
                        Minecraft.getInstance().textureManager.register(location, dynTex)

                        cache[url] = Entry(State.READY, location)
                        LOGGER.debug("头像纹理注册成功: {} -> {}", url, location)
                    } catch (e: Exception) {
                        LOGGER.warn("注册头像纹理失败 ({}): {}", url, e.message)
                        cache[url] = Entry(State.FAILED)
                    }
                }
            } catch (e: Exception) {
                LOGGER.warn("下载头像失败 ({}): {}", url, e.message)
                cache[url] = Entry(State.FAILED)
            }
        }
    }
}

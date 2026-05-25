package com.RCUTANF.herobrinehud.data

import com.mojang.authlib.GameProfile
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * 玩家头像 URL 解析工具
 *
 * 解析优先级：
 * 1. 从 GameProfile 的 textures 属性中提取原版皮肤 URL（无需额外 HTTP 请求）
 * 2. 回退到 Crafatar API（基于 UUID）
 * 3. 离线玩家使用 Minotar API（基于名称）
 */
object AvatarResolver {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/AvatarResolver")

    /**
     * 从已填充皮肤属性的 GameProfile 中解析头像 URL
     *
     * @param profile 玩家的 GameProfile（须已由服务器填充 textures 属性）
     * @return 头像 URL，无法解析时返回基于 UUID/名称的备用 URL
     */
    fun resolve(profile: GameProfile): String {
        val uuid = profile.id?.toString()

        // 1. 尝试从 textures 属性中解析皮肤 URL
        val skinUrl = extractSkinUrl(profile)
        if (skinUrl != null) {
            LOGGER.debug("Player {} skin URL resolved: {}", profile.name, skinUrl)
            return skinUrl
        }

        // 2. 回退：使用 Crafatar（基于 UUID，自动代理 Mojang 皮肤）
        if (uuid != null && uuid.isNotBlank() && uuid != "00000000-0000-0000-0000-000000000000") {
            val crafatarUrl = "https://crafatar.com/avatars/$uuid?size=64&overlay"
            LOGGER.debug("Player {} using Crafatar avatar: {}", profile.name, crafatarUrl)
            return crafatarUrl
        }

        // 3. 离线玩家回退：使用 Minotar（基于名称）
        val minotarUrl = "https://minotar.net/helm/${profile.name}/64"
        LOGGER.debug("Player {} using Minotar avatar (offline): {}", profile.name, minotarUrl)
        return minotarUrl
    }

    /**
     * 为离线玩家（仅有名称）生成备用头像 URL
     *
     * @param playerName 玩家名称
     * @return 基于名称的头像 URL
     */
    fun resolveOffline(playerName: String): String {
        return "https://minotar.net/helm/$playerName/64"
    }

    /**
     * 从 GameProfile 的 textures 属性中解析皮肤 URL
     *
     * Minecraft 服务器在玩家登录后会自动填充 GameProfile.properties["textures"]，
     * 其值为 Base64 编码的 JSON，格式如下：
     * {
     *   "textures": {
     *     "SKIN": { "url": "http://textures.minecraft.net/texture/..." },
     *     "CAPE": { "url": "..." }
     *   }
     * }
     *
     * @return 皮肤纹理 URL，解析失败则返回 null
     */
    private fun extractSkinUrl(profile: GameProfile): String? {
        return try {
            val texturesProperty = profile.properties["textures"].firstOrNull() ?: return null
            val decoded = String(Base64.getDecoder().decode(texturesProperty.value), Charsets.UTF_8)

            // 简单字符串解析，避免引入额外 JSON 库依赖
            // 寻找 "SKIN":{"url":"..."} 模式
            val skinUrlRegex = Regex(""""SKIN"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""")
            val match = skinUrlRegex.find(decoded)
            match?.groupValues?.get(1)
        } catch (e: Exception) {
            LOGGER.debug("Failed to parse textures property: {}", e.message)
            null
        }
    }
}



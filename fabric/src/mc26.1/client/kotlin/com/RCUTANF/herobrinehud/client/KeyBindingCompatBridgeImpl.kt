package com.RCUTANF.herobrinehud.client

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping

internal class KeyBindingCompatBridgeImpl : KeyBindingCompatBridge {
    override fun register(mapping: KeyMapping): KeyMapping = KeyMappingHelper.registerKeyMapping(mapping)
}

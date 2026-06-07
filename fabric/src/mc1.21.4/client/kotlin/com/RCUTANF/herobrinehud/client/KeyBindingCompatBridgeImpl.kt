package com.RCUTANF.herobrinehud.client

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping

internal class KeyBindingCompatBridgeImpl : KeyBindingCompatBridge {
    override fun register(mapping: KeyMapping): KeyMapping = KeyBindingHelper.registerKeyBinding(mapping)
}

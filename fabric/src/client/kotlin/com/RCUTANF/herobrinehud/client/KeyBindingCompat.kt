package com.RCUTANF.herobrinehud.client

import net.minecraft.client.KeyMapping

internal interface KeyBindingCompatBridge {
    fun register(mapping: KeyMapping): KeyMapping
}

internal object KeyBindingCompat : KeyBindingCompatBridge by KeyBindingCompatBridgeImpl()

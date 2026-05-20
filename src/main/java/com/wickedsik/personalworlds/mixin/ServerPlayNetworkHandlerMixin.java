package com.wickedsik.personalworlds.mixin;

/**
 * ⚠️ DEPRECATED: This mixin has been consolidated into ChatMixin.java
 * 
 * All chat message handling logic is now in ChatMixin to avoid
 * duplicate @Mixin annotations on ServerPlayNetworkHandler.
 * 
 * If you need to add new server network handler logic,
 * extend ChatMixin or create a new mixin with a different target.
 */
@Deprecated(since = "0.7.0", forRemoval = true)
public class ServerPlayNetworkHandlerMixin {
    // This class is kept for backward compatibility only
    // It will be removed in future versions
}

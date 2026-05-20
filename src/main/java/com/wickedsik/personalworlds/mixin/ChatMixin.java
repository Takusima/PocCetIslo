package com.wickedsik.personalworlds.mixin;

import com.wickedsik.personalworlds.PersonalWorldsMod;
import com.wickedsik.personalworlds.chat.ChatCommandHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatMixin {

    @Inject(method = "onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.player;
        String message = packet.chatMessage();

        if (ChatCommandHandler.isCommand(message)) {
            PersonalWorldsMod.LOGGER.info("Intercepted chat command from {}: {}", player.getName().getString(), message);
            ci.cancel();

            String command = ChatCommandHandler.getCommand(message);
            if ("Prunus".equals(command)) {
                PersonalWorldsMod.LOGGER.info("Executing Prunus command for player: {}", player.getName().getString());
            } else if ("Malus".equals(command)) {
                PersonalWorldsMod.LOGGER.info("Executing Malus command for player: {}", player.getName().getString());
            }
        }
    }
}
package dev.limucc.brutaltyping.client.mixin;

import dev.limucc.brutaltyping.client.engine.EffectEngine;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires the send-slam when a chat message is submitted. The message still sends normally. */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "handleChatInput", at = @At("HEAD"))
    private void brutaltyping$onSend(String message, boolean addToHistory, CallbackInfo ci) {
        if (message != null && !message.isBlank()) {
            EffectEngine.INSTANCE.sendSlam(message);
        }
    }
}

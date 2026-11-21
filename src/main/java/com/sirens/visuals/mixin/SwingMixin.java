package com.sirens.visuals.mixin;

import com.sirens.visuals.Config;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SwingMixin {
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$slowSwing(CallbackInfoReturnable<Integer> cir) {
        if (Config.slowSwing && (Object) this instanceof LocalPlayer) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() * Config.swingMul)));
        }
    }
}

package archives.tater.elytrapvp.mixin;

import archives.tater.elytrapvp.ElytraPvpRebalance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

@Mixin(MaceItem.class)
public class MaceItemMixin {
    @Inject(
            method = "hurtEnemy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/MaceItem;knockback(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V")
    )
    private void refillOnSmash(ItemStack itemStack, LivingEntity livingEntity, LivingEntity livingEntity2, CallbackInfo ci) {
        if (livingEntity2 instanceof Player player)
            ElytraPvpRebalance.refillElytra(player);
    }
}

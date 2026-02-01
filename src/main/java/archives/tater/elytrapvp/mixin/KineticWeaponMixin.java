package archives.tater.elytrapvp.mixin;

import archives.tater.elytrapvp.ElytraPvpRebalance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.KineticWeapon;

@Mixin(KineticWeapon.class)
public class KineticWeaponMixin {
    @WrapOperation(
            method = "damageEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z")
    )
    private boolean refillOnCharge(LivingEntity instance, EquipmentSlot equipmentSlot, Entity entity, float f, boolean damage, boolean knockback, boolean dismount, Operation<Boolean> original) {
        if (!original.call(instance, equipmentSlot, entity, f,damage, knockback, dismount)) return false;
        if (damage && knockback && dismount && instance instanceof Player player)
            ElytraPvpRebalance.refillElytra(player);
        return true;
    }
}

package archives.tater.elytrapvp.mixin;

import archives.tater.elytrapvp.ElytraPvpRebalance;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@SuppressWarnings("UnstableApiUsage")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Shadow
	protected InterpolationHandler interpolation;

	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@WrapWithCondition(
			method = "updateFallFlying",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V")
	)
	private boolean preventDamage(ItemStack instance, int i, LivingEntity livingEntity, EquipmentSlot equipmentSlot) {
		return livingEntity.hasAttached(ElytraPvpRebalance.UNBREAKABLE_COOLDOWN);
	}

	@Inject(
			method = "tick",
			at = @At("TAIL")
	)
	private void tickGliders(CallbackInfo ci) {
		if (level() instanceof ServerLevel level)
			ElytraPvpRebalance.tickElytra((LivingEntity) (Object) this, level);
	}
}
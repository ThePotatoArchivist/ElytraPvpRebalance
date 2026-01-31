package archives.tater.elytrapvp.mixin;

import archives.tater.elytrapvp.ElytraPvpRebalance;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
	protected int fallFlyTicks;

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

	@Definition(id = "fallFlyTicks", field = "Lnet/minecraft/world/entity/LivingEntity;fallFlyTicks:I")
	@Expression("?.fallFlyTicks = 0")
	@WrapOperation(
			method = "tick",
			at = @At("MIXINEXTRAS:EXPRESSION")
	)
	private void keepFallFlyTicks(LivingEntity instance, int value, Operation<Void> original) {
		original.call(instance, fallFlyTicks % 20);
	}
}
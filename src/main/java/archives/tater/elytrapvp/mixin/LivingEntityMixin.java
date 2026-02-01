package archives.tater.elytrapvp.mixin;

import archives.tater.elytrapvp.ElytraPvpRebalance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
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
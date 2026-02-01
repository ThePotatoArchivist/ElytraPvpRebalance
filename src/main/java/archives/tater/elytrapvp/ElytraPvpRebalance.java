package archives.tater.elytrapvp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class ElytraPvpRebalance implements ModInitializer {
	public static final String MOD_ID = "elytrapvp";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final AttachmentType<Integer> UNBREAKABLE_COOLDOWN = AttachmentRegistry.createPersistent(id("unbreakable_cooldown"), Codec.INT);
	public static final AttachmentType<Integer> REPAIR_COOLDOWN = AttachmentRegistry.createPersistent(id("repair_cooldown"), Codec.INT);
	public static final AttachmentType<Integer> GLIDE_TICKS = AttachmentRegistry.createPersistent(id("glide_ticks"), Codec.INT);

	public static final GameRule<Integer> GLIDER_DURABILITY_COOLDOWN = GameRuleBuilder.forInteger(3 * 60 * 20)
			.category(GameRuleCategory.PLAYER)
			.minValue(0)
			.buildAndRegister(id("glider_durability_cooldown"));

	public static final GameRule<Integer> GLIDER_REPAIR_COOLDOWN = GameRuleBuilder.forInteger(45 * 20)
			.category(GameRuleCategory.PLAYER)
			.minValue(0)
			.buildAndRegister(id("glider_repair_cooldown"));

	public static final GameRule<Integer> GLIDER_INITIAL_REPAIR_COOLDOWN = GameRuleBuilder.forInteger(120 * 20)
			.category(GameRuleCategory.PLAYER)
			.minValue(0)
			.buildAndRegister(id("glider_initial_repair_cooldown"));

	public static final GameRule<Integer> MAX_GLIDE_TIME = GameRuleBuilder.forInteger(5 * 20)
			.category(GameRuleCategory.PLAYER)
			.minValue(0)
			.buildAndRegister(id("max_glide_time"));

	public static final TagKey<DamageType> NO_ELYTRA_DURABILITY = TagKey.create(Registries.DAMAGE_TYPE, id("no_elytra_durability"));

	private static boolean decrementOrRemove(AttachmentTarget target, AttachmentType<Integer> attachmentType) {
		var value = target.getAttached(attachmentType);
		if (value == null) return false;
		var newValue = value - 1;
        if (newValue <= 0) {
            target.removeAttached(attachmentType);
			return false;
        }
        target.setAttached(attachmentType, newValue);
        return true;
    }

	private static boolean isGlider(ItemStack stack) {
		return stack.has(DataComponents.GLIDER);
	}

	private static boolean hasGlider(Player player) {
		return EquipmentSlot.VALUES.stream().anyMatch(slot -> isGlider(player.getItemBySlot(slot)));
	}

	public static void tickElytra(LivingEntity entity, ServerLevel level) {
		var hadCooldown = entity.hasAttached(UNBREAKABLE_COOLDOWN);
		if (!(entity instanceof Player player)) return;

		if (!decrementOrRemove(entity, UNBREAKABLE_COOLDOWN)) {
			if (hadCooldown) {
				level.playSound(null, player, SoundEvents.ARMOR_EQUIP_ELYTRA.value(), player.getSoundSource(), 1f, 1f);
			}
			entity.removeAttached(GLIDE_TICKS);
			entity.removeAttached(REPAIR_COOLDOWN);
		} else {

            var glideTicks = (int) entity.getAttachedOrElse(GLIDE_TICKS, 0);

			if (player.isFallFlying() && glideTicks > 0) {
				glideTicks--;
			}

			if (!decrementOrRemove(entity, REPAIR_COOLDOWN)) {
				glideTicks = min(level.getGameRules().get(MAX_GLIDE_TIME), glideTicks + 20);
				entity.setAttached(REPAIR_COOLDOWN, level.getGameRules().get(GLIDER_REPAIR_COOLDOWN));
			}

			entity.setAttached(GLIDE_TICKS, glideTicks);

			if (hasGlider(player))
				displayElytraTime(player, glideTicks);
		}
    }

	private static void displayElytraTime(Player player, int glideTicks) {
		player.displayClientMessage(Component.literal("Elytra Time: %.1fs".formatted(glideTicks / 20f)), true);
	}

	private static void displayElytraTime(Player player) {
		displayElytraTime(player, player.getAttachedOrElse(GLIDE_TICKS, 0));
	}

	public static void refillElytra(Player player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		if (!player.hasAttached(GLIDE_TICKS)) return;
		player.setAttached(GLIDE_TICKS, level.getGameRules().get(MAX_GLIDE_TIME));
		displayElytraTime(player);
	}

	public static void makeBreakable(Player player, ServerLevel serverLevel) {
		if (!player.hasAttached(UNBREAKABLE_COOLDOWN) && hasGlider(player)) {
			serverLevel.playSound(null, player, SoundEvents.ITEM_BREAK.value(), player.getSoundSource(), 1f, 1f);
		}
		if (!player.hasAttached(GLIDE_TICKS))
			player.setAttached(GLIDE_TICKS, serverLevel.getGameRules().get(MAX_GLIDE_TIME));
		player.setAttached(UNBREAKABLE_COOLDOWN, serverLevel.getGameRules().get(GLIDER_DURABILITY_COOLDOWN));
		if (!player.hasAttached(REPAIR_COOLDOWN))
			player.setAttached(REPAIR_COOLDOWN, serverLevel.getGameRules().get(GLIDER_INITIAL_REPAIR_COOLDOWN));
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (blocked || source.is(NO_ELYTRA_DURABILITY) || !(entity.level() instanceof ServerLevel serverLevel)) return;
			if (entity instanceof Player player) {
				makeBreakable(player, serverLevel);
				if (source.getEntity() instanceof Player player2)
					makeBreakable(player2, serverLevel);
			}
		});

		EntityElytraEvents.ALLOW.register(entity ->
			!entity.hasAttached(GLIDE_TICKS) || requireNonNull(entity.getAttached(GLIDE_TICKS)) > 0
		);
	}
}
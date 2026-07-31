package com.mrbysco.armorposer.handler;

import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SyncHandler implements PluginMessageListener {

	private static final List<String> allowedKeys = List.of(
			"Invisible", "NoBasePlate", "NoGravity", "ShowArms", "Small", "CustomNameVisible", "Invulnerable",
			"Pose", "DisabledSlots", "Pose", "Scale", "Move", "Rotation"
	);

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
		if (!channel.equals("armorposer:sync_packet")) {
			return;
		}
		if (!ArmorPoserPlugin.canUse(player)) {
			return;
		}
		FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
		UUID uuid = byteBuf.readUUID();
		CompoundTag tag = byteBuf.readNbt();
		Entity entity = ArmorPoserPlugin.Plugin.getServer().getEntity(uuid);
		if (tag != null && entity instanceof ArmorStand armorStand) {
			List<String> keysToRemove = tag.keySet().stream()
					.filter(key -> !allowedKeys.contains(key))
					.toList();
			keysToRemove.forEach(tag::remove);

			if (tag.contains("Invisible"))
				armorStand.setInvisible(tag.getBooleanOr("Invisible", false));
			if (tag.contains("NoBasePlate"))
				armorStand.setBasePlate(!tag.getBooleanOr("NoBasePlate", false));
			if (tag.contains("NoGravity"))
				armorStand.setGravity(!tag.getBooleanOr("NoGravity", false));
			if (tag.contains("ShowArms"))
				armorStand.setArms(tag.getBooleanOr("ShowArms", false));
			if (tag.contains("Small"))
				armorStand.setSmall(tag.getBooleanOr("Small", false));
			if (tag.contains("CustomNameVisible"))
				armorStand.setCustomNameVisible(tag.getBooleanOr("CustomNameVisible", false));

			Optional<Vec2> rotation = tag.read("Rotation", Vec2.CODEC);
			if (rotation.isPresent()) {
				float yaw = rotation.get().x;
				armorStand.setBodyYaw(yaw);
				armorStand.setRotation(yaw, armorStand.getPitch());
			}

			if (tag.contains("DisabledSlots") && canLock(player)) {
				int disabledSlots = tag.getIntOr("DisabledSlots", 0);
				if (disabledSlots == 4144959) {
					armorStand.setDisabledSlots(EquipmentSlot.values());
					armorStand.setInvulnerable(true);
				} else {
					armorStand.removeDisabledSlots(EquipmentSlot.values());
					armorStand.setInvulnerable(false);
				}
			}

			if (tag.contains("Scale") && canResize(player)) {
				double scale = tag.getDoubleOr("Scale", 0.0D);
				if (scale >= ArmorPoserPlugin.minScale && scale <= ArmorPoserPlugin.maxScale) {
					AttributeInstance attribute = armorStand.getAttribute(Attribute.SCALE);
					if (attribute != null && scale > 0) {
						attribute.setBaseValue(scale);
					}
				} else {
					ArmorPoserPlugin.Plugin.getLogger().warning("Scale value out of bounds: " + scale);
				}
			}

			if (tag.contains("Pose")) {
				CompoundTag poseTag = tag.getCompoundOrEmpty("Pose");
				if (poseTag.isEmpty()) {
					return;
				}

				readPose(armorStand, poseTag);

				Vec3 movePos = tag.read("Move", Vec3.CODEC).orElse(Vec3.ZERO);
				double x = movePos.x();
				double y = movePos.y();
				double z = movePos.z();
				if (x != 0 || y != 0 || z != 0) {
					float oldYaw = armorStand.getYaw();
					float oldPitch = armorStand.getPitch();
					if (ArmorPoserPlugin.isFolia()) {
						armorStand.teleportAsync(new Location(armorStand.getWorld(), armorStand.getX() + x,
								armorStand.getY() + y,
								armorStand.getZ() + z), PlayerTeleportEvent.TeleportCause.PLUGIN);
					} else {
						armorStand.teleport(new Location(armorStand.getWorld(), armorStand.getX() + x,
								armorStand.getY() + y,
								armorStand.getZ() + z), PlayerTeleportEvent.TeleportCause.PLUGIN);
					}
					armorStand.setBodyYaw(oldYaw);
					armorStand.setRotation(oldYaw, oldPitch);
				}
			}
		}
	}

	private static final EulerAngle DEFAULT_HEAD_POSE = getAngle(0.0F, 0.0F, 0.0F);
	private static final EulerAngle DEFAULT_BODY_POSE = getAngle(0.0F, 0.0F, 0.0F);
	private static final EulerAngle DEFAULT_LEFT_ARM_POSE = getAngle(-10.0F, 0.0F, -10.0F);
	private static final EulerAngle DEFAULT_RIGHT_ARM_POSE = getAngle(-15.0F, 0.0F, 10.0F);
	private static final EulerAngle DEFAULT_LEFT_LEG_POSE = getAngle(-1.0F, 0.0F, -1.0F);
	private static final EulerAngle DEFAULT_RIGHT_LEG_POSE = getAngle(1.0F, 0.0F, 1.0F);

	private void readPose(ArmorStand armorStand, CompoundTag tag) {
		Rotations head = getRotation(tag, "Head");
		armorStand.setHeadPose(head == null ? DEFAULT_HEAD_POSE : getAngle(head));
		Rotations body = getRotation(tag, "Body");
		armorStand.setBodyPose(body == null ? DEFAULT_BODY_POSE : getAngle(body));
		Rotations leftArm = getRotation(tag, "LeftArm");
		armorStand.setLeftArmPose(leftArm == null ? DEFAULT_LEFT_ARM_POSE : getAngle(leftArm));
		Rotations rightArm = getRotation(tag, "RightArm");
		armorStand.setRightArmPose(rightArm == null ? DEFAULT_RIGHT_ARM_POSE : getAngle(rightArm));
		Rotations leftLeg = getRotation(tag, "LeftLeg");
		armorStand.setLeftLegPose(leftLeg == null ? DEFAULT_LEFT_LEG_POSE : getAngle(leftLeg));
		Rotations rightLeg = getRotation(tag, "RightLeg");
		armorStand.setRightLegPose(rightLeg == null ? DEFAULT_RIGHT_LEG_POSE : getAngle(rightLeg));
	}

	private Rotations getRotation(CompoundTag tag, String key) {
		return tag.read(key, Rotations.CODEC).orElse(null);
	}

	private static EulerAngle getAngle(Rotations rotations) {
		return getAngle(rotations.x(), rotations.y(), rotations.z());
	}

	private static EulerAngle getAngle(float xDeg, float yDeg, float zDeg) {
		//Euler uses Radians, so we need to convert the degrees to radians
		return new EulerAngle(Math.toRadians(xDeg), Math.toRadians(yDeg), Math.toRadians(zDeg));
	}

	public static boolean canResize(Player player) {
		if (player == null) return false;
		if (ArmorPoserPlugin.requirePermissions) {
			return player.hasPermission(ArmorPoserPlugin.RESIZE_PERMISSION);
		}
		if (ArmorPoserPlugin.restrictResizeToOP) {
			if (ArmorPoserPlugin.resizeWhitelist.contains(player.getName())) {
				return true;
			}
			return player.isOp();
		}
		return true;
	}

	public static boolean canLock(Player player) {
		if (player == null) return false;
		if (ArmorPoserPlugin.requirePermissions) {
			return player.hasPermission(ArmorPoserPlugin.LOCK_PERMISSION);
		}
		return true;
	}
}

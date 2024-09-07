package com.mrbysco.armorposer.handler;

import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SyncHandler implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
		if (!channel.equals("armorposer:sync_packet")) {
			return;
		}
//		System.out.println("Received message from " + player.getName() + " on channel " + channel);
		FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
		UUID uuid = byteBuf.readUUID();
		CompoundTag tag = byteBuf.readNbt();
		Entity entity = ArmorPoserPlugin.Plugin.getServer().getEntity(uuid);
		if (tag != null && entity instanceof ArmorStand armorStand) {
			if (tag.contains("Invisible"))
				armorStand.setInvisible(tag.getBoolean("Invisible"));
			if (tag.contains("NoBasePlate"))
				armorStand.setBasePlate(!tag.getBoolean("NoBasePlate"));
			if (tag.contains("NoGravity"))
				armorStand.setGravity(!tag.getBoolean("NoGravity"));
			if (tag.contains("ShowArms"))
				armorStand.setArms(tag.getBoolean("ShowArms"));
			if (tag.contains("Small"))
				armorStand.setSmall(tag.getBoolean("Small"));
			if (tag.contains("CustomNameVisible"))
				armorStand.setCustomNameVisible(tag.getBoolean("CustomNameVisible"));
			if (tag.contains("Rotation")) {
				ListTag tagList = tag.getList("Rotation", Tag.TAG_FLOAT);
				armorStand.setBodyYaw(tagList.getFloat(0));
			}

			if (tag.contains("DisabledSlots")) {
				int disabledSlots = tag.getInt("DisabledSlots");
				armorStand.removeDisabledSlots(EquipmentSlot.values());
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					if (isSlotDisabled(armorStand, slot, disabledSlots)) {
						armorStand.addDisabledSlots(slot);
					}
				}
			}

			if (tag.contains("Scale")) {
				double scale = tag.getDouble("Scale");
				if (scale > 0) {
					armorStand.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
				}
			}

			if (tag.contains("Pose")) {
				CompoundTag poseTag = tag.getCompound("Pose");
				readPose(armorStand, poseTag);

				ListTag tagList = tag.getList("Move", Tag.TAG_DOUBLE);
				double x = tagList.getDouble(0);
				double y = tagList.getDouble(1);
				double z = tagList.getDouble(2);
				if (x != 0 || y != 0 || z != 0)
					armorStand.teleport(new Location(armorStand.getWorld(), armorStand.getX() + x,
							armorStand.getY() + y,
							armorStand.getZ() + z), PlayerTeleportEvent.TeleportCause.PLUGIN);
			}
		}
	}

	private boolean isSlotDisabled(ArmorStand armorStand, EquipmentSlot slot, int disabledSlots) {
		int filterFlag = switch (slot) {
			case HAND -> 0;
			case OFF_HAND -> 5;
			case FEET -> 1;
			case LEGS -> 2;
			case CHEST -> 3;
			default -> 4;
		};
		return (disabledSlots & 1 << filterFlag) != 0 || (slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) && !armorStand.hasArms();
	}

	private static final EulerAngle DEFAULT_HEAD_POSE = getAngle(0.0F, 0.0F, 0.0F);
	private static final EulerAngle DEFAULT_BODY_POSE = getAngle(0.0F, 0.0F, 0.0F);
	private static final EulerAngle DEFAULT_LEFT_ARM_POSE = getAngle(-10.0F, 0.0F, -10.0F);
	private static final EulerAngle DEFAULT_RIGHT_ARM_POSE = getAngle(-15.0F, 0.0F, 10.0F);
	private static final EulerAngle DEFAULT_LEFT_LEG_POSE = getAngle(-1.0F, 0.0F, -1.0F);
	private static final EulerAngle DEFAULT_RIGHT_LEG_POSE = getAngle(1.0F, 0.0F, 1.0F);

	private void readPose(ArmorStand armorStand, CompoundTag tag) {
		ListTag head = tag.getList("Head", 5);
		armorStand.setHeadPose(head.isEmpty() ? DEFAULT_HEAD_POSE : getAngle(head));
		ListTag body = tag.getList("Body", 5);
		armorStand.setBodyPose(body.isEmpty() ? DEFAULT_BODY_POSE : getAngle(body));
		ListTag leftArm = tag.getList("LeftArm", 5);
		armorStand.setLeftArmPose(leftArm.isEmpty() ? DEFAULT_LEFT_ARM_POSE : getAngle(leftArm));
		ListTag rightArm = tag.getList("RightArm", 5);
		armorStand.setRightArmPose(rightArm.isEmpty() ? DEFAULT_RIGHT_ARM_POSE : getAngle(rightArm));
		ListTag leftLeg = tag.getList("LeftLeg", 5);
		armorStand.setLeftLegPose(leftLeg.isEmpty() ? DEFAULT_LEFT_LEG_POSE : getAngle(leftLeg));
		ListTag rightLeg = tag.getList("RightLeg", 5);
		armorStand.setRightLegPose(rightLeg.isEmpty() ? DEFAULT_RIGHT_LEG_POSE : getAngle(rightLeg));
	}

	private static EulerAngle getAngle(ListTag tag) {
		return getAngle(tag.getFloat(0), tag.getFloat(1), tag.getFloat(2));
	}

	private static EulerAngle getAngle(float xDeg, float yDeg, float zDeg) {
		//Euler uses Radians, so we need to convert the degrees to radians
		return new EulerAngle(Math.toRadians(xDeg), Math.toRadians(yDeg), Math.toRadians(zDeg));
	}
}

package com.mrbysco.armorposer.handler;

import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SwapHandler implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
		if (!channel.equals("armorposer:swap_packet")) {
			return;
		}

		FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
		UUID uuid = byteBuf.readUUID();
		Action action = byteBuf.readEnum(Action.class);
		Entity entity = ArmorPoserPlugin.Plugin.getServer().getEntity(uuid);
		if (entity instanceof ArmorStand armorStand) {
			switch (action) {
				case SWAP_HANDS:
					ItemStack offStack = armorStand.getItem(EquipmentSlot.OFF_HAND);
					armorStand.setItem(EquipmentSlot.OFF_HAND, armorStand.getItem(EquipmentSlot.HAND));
					armorStand.setItem(EquipmentSlot.HAND, offStack);
					return;
				case SWAP_WITH_HEAD:
					ItemStack headStack = armorStand.getItem(EquipmentSlot.HEAD);
					armorStand.setItem(EquipmentSlot.HEAD, armorStand.getItem(EquipmentSlot.HAND));
					armorStand.setItem(EquipmentSlot.HAND, headStack);
					return;
				default:
					throw new IllegalArgumentException("Invalid Pose action");
			}
		}
	}

	public static enum Action {
		SWAP_WITH_HEAD,
		SWAP_HANDS
	}
}

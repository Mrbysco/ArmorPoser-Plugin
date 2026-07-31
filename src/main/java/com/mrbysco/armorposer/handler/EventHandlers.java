package com.mrbysco.armorposer.handler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.serialization.Codec;
import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventHandlers implements Listener {
	private static final StreamCodec<FriendlyByteBuf, Map<UUID, List<String>>> SYNC_CODEC = ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()));

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractAtEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (entity instanceof ArmorStand armorStand && player.isSneaking() && canUseGUI(player)) {
			if (event.getHand() == EquipmentSlot.HAND) {
				// Locked
				ByteArrayDataOutput lockedOut = ByteStreams.newDataOutput();
				lockedOut.writeInt(armorStand.getEntityId());
				lockedOut.writeBoolean(armorStand.isInvulnerable());
				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:locked_packet", lockedOut.toByteArray());

				// Screen
				FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
				buffer.writeInt(armorStand.getEntityId());
				buffer.writeCollection(List.of(), ByteBufCodecs.STRING_UTF8); //TODO: Add restriction code
				buffer.writeDouble(ArmorPoserPlugin.minScale);
				buffer.writeDouble(ArmorPoserPlugin.maxScale);

				byte[] bytes = new byte[buffer.writerIndex()];
				buffer.getBytes(0, bytes);

				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:screen_packet", bytes);

				// Sync Groups
				FriendlyByteBuf groupBuffer = new FriendlyByteBuf(Unpooled.buffer());
				Map<UUID, List<String>> nearbyGroups = getNearbyGroups(player);
				SYNC_CODEC.encode(groupBuffer, nearbyGroups);

				byte[] groupBytes = new byte[groupBuffer.writerIndex()];
				groupBuffer.getBytes(0, groupBytes);
				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:sync_group_packet", groupBytes);
			}
			event.setCancelled(true);
		}
	}

	public static Map<UUID, List<String>> getNearbyGroups(Player player) {
		List<CraftArmorStand> nearbyArmorStands = player.getNearbyEntities(32, 32, 32).stream()
				.filter(entity -> entity instanceof CraftArmorStand)
				.map(entity -> (CraftArmorStand) entity)
				.toList();

		Map<UUID, List<String>> groupData = new HashMap<>();
		Codec<List<String>> codec = Codec.STRING.sizeLimitedListOf(1024);
		for (CraftArmorStand stand : nearbyArmorStands) {
			net.minecraft.world.entity.decoration.ArmorStand armorStand = stand.getHandle();
			CustomData customData = armorStand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			List<String> groups = customData.copyTag().read("armor_poser_groups", codec).orElse(List.of());
			if (!groups.isEmpty()) {
				groupData.put(armorStand.getUUID(), groups);
			}
		}
		return groupData;
	}

	private boolean canUseGUI(Player player) {
		if (!ArmorPoserPlugin.enableConfigGui) return false;
		return ArmorPoserPlugin.canUse(player);
	}

}

package com.mrbysco.armorposer.handler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mrbysco.armorposer.ArmorPoserPlugin;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class EventHandlers implements Listener {

	@EventHandler
	public void onInteract(PlayerInteractAtEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (entity instanceof ArmorStand armorStand && player.isSneaking() && canUseGUI(player)) {
			if (event.getHand() == EquipmentSlot.HAND) {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeInt(armorStand.getEntityId());
				ArmorPoserPlugin.Plugin.getServer().getMessenger().registerOutgoingPluginChannel(ArmorPoserPlugin.Plugin, "armorposer:screen_packet");
				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:screen_packet", out.toByteArray());
				ArmorPoserPlugin.Plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(ArmorPoserPlugin.Plugin, "armorposer:screen_packet");
			}
			event.setCancelled(true);
		}
	}

	private boolean canUseGUI(Player player) {
		if (!ArmorPoserPlugin.enableConfigGui) return false;
		if (!ArmorPoserPlugin.requirePermissions) return true;
		return player.hasPermission(ArmorPoserPlugin.USE_PERMISSION);
	}

}

package com.mrbysco.armorposer.handler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mrbysco.armorposer.ArmorPoserPlugin;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class EventHandlers implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractAtEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (entity instanceof ArmorStand armorStand && player.isSneaking() && canUseGUI(player)) {
			if (event.getHand() == EquipmentSlot.HAND) {
				ByteArrayDataOutput lockedOut = ByteStreams.newDataOutput();
				lockedOut.writeInt(armorStand.getEntityId());
				lockedOut.writeBoolean(armorStand.isInvulnerable());
				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:locked_packet", lockedOut.toByteArray());

				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeInt(armorStand.getEntityId());
				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:screen_packet", out.toByteArray());
			}
			event.setCancelled(true);
		}
	}

	private boolean canUseGUI(Player player) {
		if (!ArmorPoserPlugin.enableConfigGui) return false;
		return ArmorPoserPlugin.canUse(player);
	}

}

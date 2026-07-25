package com.mrbysco.armorposer.handler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

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

				FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
				buffer.writeInt(armorStand.getEntityId());
				buffer.writeCollection(List.of(), ByteBufCodecs.STRING_UTF8); //TODO: Add restriction code

				byte[] bytes = new byte[buffer.writerIndex()];
				buffer.getBytes(0, bytes);

				player.sendPluginMessage(ArmorPoserPlugin.Plugin, "armorposer:screen_packet", bytes);
			}
			event.setCancelled(true);
		}
	}

	private boolean canUseGUI(Player player) {
		if (!ArmorPoserPlugin.enableConfigGui) return false;
		return ArmorPoserPlugin.canUse(player);
	}

}

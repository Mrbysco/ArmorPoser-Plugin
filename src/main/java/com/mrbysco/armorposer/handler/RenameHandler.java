package com.mrbysco.armorposer.handler;

import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RenameHandler implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
		if (!channel.equals("armorposer:rename_packet")) {
			return;
		}
//		System.out.println("Received message from " + player.getName() + " on channel " + channel);
		FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
		UUID uuid = byteBuf.readUUID();
		String name = byteBuf.readUtf();
		Entity entity = ArmorPoserPlugin.Plugin.getServer().getEntity(uuid);
		if (!name.isEmpty() && entity instanceof ArmorStand armorStand && (player.getLevel() >= 1 || player.getGameMode() == GameMode.CREATIVE)) {
			player.giveExpLevels(-1);
			armorStand.customName(Component.text(name));
		}
	}
}

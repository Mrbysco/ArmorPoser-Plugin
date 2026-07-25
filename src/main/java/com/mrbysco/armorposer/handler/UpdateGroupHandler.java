package com.mrbysco.armorposer.handler;

import com.mojang.serialization.Codec;
import com.mrbysco.armorposer.ArmorPoserPlugin;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdateGroupHandler implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
		if (!channel.equals("armorposer:update_group_packet")) {
			return;
		}
		FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
		UUID uuid = byteBuf.readUUID();
		List<String> groups = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(byteBuf);
		Entity entity = ArmorPoserPlugin.Plugin.getServer().getEntity(uuid);
		if (entity instanceof CraftArmorStand craftArmorStand) {
			ArmorStand armorStand = craftArmorStand.getHandle();
			CustomData customData = armorStand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CustomData changedData = customData.update((customTag) -> {
				List<String> oldGroups = customTag.read("armor_poser_groups", ArmorPoserPlugin.TAG_LIST_CODEC).orElse(new ArrayList<>());
				if (oldGroups.equals(groups)) {
					return;
				}
				customTag.store("armor_poser_groups", ArmorPoserPlugin.TAG_LIST_CODEC, groups);
			});
			if (!customData.equals(changedData)) {
				armorStand.setComponent(DataComponents.CUSTOM_DATA, changedData);
			}
		}
	}
}

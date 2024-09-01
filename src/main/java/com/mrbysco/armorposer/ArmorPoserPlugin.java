package com.mrbysco.armorposer;

import com.mrbysco.armorposer.handler.EventHandlers;
import com.mrbysco.armorposer.handler.SwapHandler;
import com.mrbysco.armorposer.handler.SyncHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArmorPoserPlugin extends JavaPlugin {
	public static Plugin Plugin;

	@Override
	public void onEnable() {
		getServer().getMessenger().registerIncomingPluginChannel(this, "armorposer:sync_packet", new SyncHandler());
		getServer().getMessenger().registerIncomingPluginChannel(this, "armorposer:swap_packet", new SwapHandler());

		getServer().getPluginManager().registerEvents(new EventHandlers(), this);

		Plugin = this;
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
}

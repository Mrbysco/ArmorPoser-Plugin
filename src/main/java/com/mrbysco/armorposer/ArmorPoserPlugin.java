package com.mrbysco.armorposer;

import com.mrbysco.armorposer.handler.EventHandlers;
import com.mrbysco.armorposer.handler.RenameHandler;
import com.mrbysco.armorposer.handler.SwapHandler;
import com.mrbysco.armorposer.handler.SyncHandler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ArmorPoserPlugin extends JavaPlugin {
	public static Plugin Plugin;
	public final FileConfiguration config = getConfig();

	public static final String USE_PERMISSION = "armorposer.use";
	public static final String RESIZE_PERMISSION = "armorposer.resize";

	public static boolean enableConfigGui;
	public static boolean requirePermissions;
	public static boolean restrictResizeToOP;
	public static List<String> resizeWhitelist = new ArrayList<>();

    public static boolean canUse(Player player) {
        if (!requirePermissions) return true;
        return player.hasPermission(ArmorPoserPlugin.USE_PERMISSION);
    }

	@Override
	public void onEnable() {
		setupConfig();

		getServer().getMessenger().registerIncomingPluginChannel(this, "armorposer:sync_packet", new SyncHandler());
		getServer().getMessenger().registerIncomingPluginChannel(this, "armorposer:swap_packet", new SwapHandler());
		getServer().getMessenger().registerIncomingPluginChannel(this, "armorposer:rename_packet", new RenameHandler());
		getServer().getMessenger().registerOutgoingPluginChannel(this, "armorposer:screen_packet");
		getServer().getMessenger().registerOutgoingPluginChannel(this, "armorposer:locked_packet");

		getServer().getPluginManager().registerEvents(new EventHandlers(), this);

		Plugin = this;
	}

	/**
	 * Setup the config file
	 */
	private void setupConfig() {
		config.addDefault("enableConfigGui", true);
		config.addDefault("requirePermissions", false);
		config.addDefault("restrictResizeToOP", false);
		config.addDefault("resizeWhitelist", List.of(""));
		config.options().copyDefaults(true);
		saveConfig();

		enableConfigGui = config.getBoolean("enableConfigGui");
		requirePermissions = config.getBoolean("requirePermissions");
		restrictResizeToOP = config.getBoolean("restrictResizeToOP");
		resizeWhitelist = config.getStringList("resizeWhitelist");
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}

	public static boolean isFolia() {
		try {
			Class.forName("io.papermc.paper.threadedregions.ThreadedRegionizer");
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}

package net.novauniverse.games.survivalgames;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.novauniverse.games.survivalgames.debug.DebugCommands;
import net.novauniverse.games.survivalgames.game.SurvivalGames;
import net.novauniverse.games.survivalgames.map.mapmodules.supplydrop.medical.MedicalSupplyDropMapModule;
import net.novauniverse.games.survivalgames.supplydrop.medical.MedicalSupplyDropManager;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.spigot.NovaCore;
import net.zeeraa.novacore.spigot.abstraction.events.VersionIndependantPlayerAchievementAwardedEvent;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.compass.CompassTracker;
import net.zeeraa.novacore.spigot.module.modules.compass.event.CompassTrackingEvent;
import net.zeeraa.novacore.spigot.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.module.modules.game.map.mapmodule.MapModuleManager;
import net.zeeraa.novacore.spigot.module.modules.game.mapselector.selectors.guivoteselector.GUIMapVote;
import net.zeeraa.novacore.spigot.module.modules.gamelobby.GameLobby;

public class NovaSurvivalGames extends JavaPlugin implements Listener {
	private static NovaSurvivalGames instance;

	public static NovaSurvivalGames getInstance() {
		return instance;
	}

	private boolean allowReconnect;
	private boolean combatTagging;
	private int reconnectTime;

	private SurvivalGames game;

	public boolean isAllowReconnect() {
		return allowReconnect;
	}

	public boolean isCombatTagging() {
		return combatTagging;
	}

	public int getReconnectTime() {
		return reconnectTime;
	}

	public SurvivalGames getGame() {
		return game;
	}

	@Override
	public void onEnable() {
		NovaSurvivalGames.instance = this;

		saveDefaultConfig();

		allowReconnect = getConfig().getBoolean("allow_reconnect");
		combatTagging = getConfig().getBoolean("combat_tagging");
		reconnectTime = getConfig().getInt("player_elimination_delay");

		File mapFolder = new File(this.getDataFolder().getPath() + File.separator + "Maps");
		File worldFolder = new File(this.getDataFolder().getPath() + File.separator + "Worlds");
		File lootTableFolder = new File(this.getDataFolder().getPath() + File.separator + "LootTables");

		GameManager.getInstance().setUseCombatTagging(combatTagging);
		
		try {
			FileUtils.forceMkdir(getDataFolder());
			FileUtils.forceMkdir(mapFolder);
			FileUtils.forceMkdir(worldFolder);
			FileUtils.forceMkdir(lootTableFolder);
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.fatal("Failed to setup data directory");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		ModuleManager.enable(GameManager.class);
		ModuleManager.enable(GameLobby.class);
		ModuleManager.enable(CompassTracker.class);

		ModuleManager.loadModule(MedicalSupplyDropManager.class);

		MapModuleManager.addMapModule("novauniverse.survivalgames.medicalsupplydrop", MedicalSupplyDropMapModule.class);

		this.game = new SurvivalGames();

		GameManager.getInstance().loadGame(game);

		GUIMapVote mapSelector = new GUIMapVote();

		GameManager.getInstance().setMapSelector(mapSelector);

		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getServer().getPluginManager().registerEvents(mapSelector, this);

		Log.info("SurvivalGames", "Loading maps from " + mapFolder.getPath());
		GameManager.getInstance().getMapReader().loadAll(mapFolder, worldFolder);

		NovaCore.getInstance().getLootTableManager().loadAll(lootTableFolder);

		new DebugCommands();
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Plugin) this);
		Bukkit.getScheduler().cancelTasks(this);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onCompassTracking(CompassTrackingEvent e) {
		boolean enabled = false;
		if (GameManager.getInstance().isEnabled()) {
			if (GameManager.getInstance().hasGame()) {
				if (GameManager.getInstance().getActiveGame().hasStarted()) {
					enabled = true;
				}
			}
		}
		e.setCancelled(!enabled);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onVersionIndependantPlayerAchievementAwarded(VersionIndependantPlayerAchievementAwardedEvent e) {
		e.setCancelled(true);
	}
}
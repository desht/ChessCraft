package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public abstract class ChessListenerBase implements Listener {
	protected final ChessCraft plugin;

	public ChessListenerBase(ChessCraft plugin) {
		this.plugin = plugin;

		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
}

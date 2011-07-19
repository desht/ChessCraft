package me.desht.chesscraft;

import java.util.logging.Level;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;

import com.iConomy.iConomy;

public class ChessServerListener extends ServerListener {
	private ChessCraft plugin;

	ChessServerListener(ChessCraft plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onPluginDisable(PluginDisableEvent event) {
		if (plugin.iConomy != null) {
			if (event.getPlugin().getDescription().getName().equals("iConomy")) {
				plugin.iConomy = null;
				plugin.log(Level.INFO, "un-hooked from iConomy");
			}
		}
	}

	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		if (plugin.iConomy == null) {
			Plugin iConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");

			if (iConomy != null) {
				if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy")) {
					plugin.iConomy = (iConomy) iConomy;
					plugin.log(Level.INFO, "hooked into iConomy");
				}
			}
		}
	}
}

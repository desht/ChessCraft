package me.desht.chesscraft;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class ChessServerListener extends ServerListener {

    //private ChessCraft plugin;

    ChessServerListener(/*ChessCraft plugin*/) {
        //this.plugin = plugin;
    }

    @Override
    public void onPluginDisable(PluginDisableEvent event) {
       Economy.pluginDisable(event.getPlugin());
    }

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
       Economy.pluginEnable(event.getPlugin());
    }
}

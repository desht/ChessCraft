package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.landslide.BlockSlideEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LandslideIntegration implements Listener {
    public LandslideIntegration(ChessCraft plugin) {
        try {
            Class.forName("me.desht.landslide.BlockSlideEvent");
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        } catch (ClassNotFoundException e) {
            LogUtils.warning("Consider installing Landslide v1.5.0 or later for better slide protection of Checkers boards");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSlide(BlockSlideEvent event) {
        if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}

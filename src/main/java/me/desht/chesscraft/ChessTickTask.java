package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.dhutils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class ChessTickTask extends BukkitRunnable {
//	private int tickTaskId;
//
//	public ChessTickTask() {
//		tickTaskId = -1;
//	}
//
//	public void start(long initialDelay) {
//		cancel();
//
//		long interval = ChessCraft.getInstance().getConfig().getInt("tick_interval", 1) * 20L;
//		tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ChessCraft.getInstance(), new Runnable() {
//			@Override
//			public void run() {
//				List<ChessGame> games = new ArrayList<ChessGame>(ChessGameManager.getManager().listGames());
//				for (ChessGame game : games) {
//					game.tick();
//					game.checkForAutoDelete();
//				}
//			}
//		}, initialDelay, interval);
//
//		Debugger.getInstance().debug("ticker task initialised: interval = " + interval + " ticks, task ID = " + tickTaskId);
//	}
//
//	public void cancel() {
//		if (tickTaskId != -1) {
//			Bukkit.getScheduler().cancelTask(tickTaskId);
//			tickTaskId = -1;
//		}
//	}

    @Override
    public void run() {
        for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
            bv.tick();
        }
    }
}

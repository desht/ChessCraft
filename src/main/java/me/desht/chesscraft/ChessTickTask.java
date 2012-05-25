package me.desht.chesscraft;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;

public class ChessTickTask {
	private int tickTaskId;

	public ChessTickTask() {
		tickTaskId = -1;
	}
	
	public void start(long initialDelay) {
		cancel();
		
		long interval = ChessConfig.getConfig().getInt("tick_interval", 1) * 20L;
		tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ChessCraft.getInstance(), new Runnable() {
			@Override
			public void run() {
				for (ChessGame game : ChessGame.listGames()) {
					game.clockTick();
					game.checkForAutoDelete();
				}
			}
		}, initialDelay, interval);
		
		LogUtils.fine("ticker task initialised: interval = " + interval + " ticks, task ID = " + tickTaskId);
	}
	
	public void cancel() {
		if (tickTaskId != -1) {
			Bukkit.getScheduler().cancelTask(tickTaskId);
			tickTaskId = -1;
		}
	}
}

package me.desht.chesscraft;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.dhutils.LogUtils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class ChessTickTask {
	private int tickTaskId;

	public ChessTickTask() {
		tickTaskId = -1;
	}

	public void start(long initialDelay) {
		cancel();

		long interval = ChessCraft.getInstance().getConfig().getInt("tick_interval", 1) * 20L;
		tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ChessCraft.getInstance(), new Runnable() {
			@Override
			public void run() {
				List<ChessGame> games = new ArrayList<ChessGame>(ChessGameManager.getManager().listGames());
				for (ChessGame game : games) {
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

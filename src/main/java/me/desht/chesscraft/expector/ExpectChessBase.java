package me.desht.chesscraft.expector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.responsehandler.ExpectBase;

public abstract class ExpectChessBase extends ExpectBase {

	/**
	 * Run a task as a sync delayed task, and report any ChessException errors back to the given player
	 * (if possible).  This allows chat response handlers (which don't run in the main thread anymore)
	 *  to safely run their responses.
	 * 
	 * @param player	The player to report errors to
	 * @param task		The task to be run
	 * @return			The Bukkit scheduler task ID
	 */
	protected int deferTask(final Player player, final Runnable task) {
		return Bukkit.getScheduler().scheduleSyncDelayedTask(ChessCraft.getInstance(), new Runnable() {

			@Override
			public void run() {
				try {
					task.run();
				} catch (ChessException e) {
					if (player != null) {
						MiscUtil.errorMessage(player, e.getMessage());
					} else {
						LogUtils.warning(e.getMessage());
					}
				}
			}

		});
	}
}

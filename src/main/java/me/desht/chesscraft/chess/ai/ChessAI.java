package me.desht.chesscraft.chess.ai;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.LogUtils;

/**
 * @author desht
 *
 */
public abstract class ChessAI implements Runnable {
	/*
	 * Special character ensures AI name cannot (easily) be faked/hacked, also
	 * adds another level of AI name visibility. Users/admins should NOT be given
	 * control of this prefix - use something else to enable changing AI name
	 * colors, if wanted.
	 */
	public static final String AI_PREFIX = ChatColor.WHITE.toString();

	private boolean active = false;
	private int aiTask = -1;
	private boolean hasFailed = false;
	private boolean pendingMove = false;
	private int pendingFrom, pendingTo;
	
	private final String name;
	private final ChessGame chessCraftGame;
	private final boolean isWhite;
	protected final ConfigurationSection params;
	protected final String gameDetails;

	ChessAI(String name, ChessGame chessCraftGame, Boolean isWhite, ConfigurationSection params) {
		this.name = name;
		this.chessCraftGame = chessCraftGame;
		this.isWhite = isWhite;
		this.params = params;
		this.gameDetails = "game [" + chessCraftGame.getName() + "] AI [" + getName() + "]: ";
	}

	/**
	 * Perform the implementation-specfic steps needed to cleanly shuto down this AI instance.
	 */
	public abstract void shutdown();

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * Called when the AI has to calculate its next move.
	 */
	public abstract void run();
	
	/**
	 * Perform the implementation-specfic steps needed to undo the AI's last move.
	 */
	public abstract void undoLastMove();
	
	/**
	 * Perform the implementation-specfic steps needed to update the AI's internal game model with
	 * the given move.  Square indices are always in Chesspresso sqi format.
	 * 
	 * @param fromSqi	Square being moved from
	 * @param toSqi		Square being move to
	 * @param otherPlayer	true if this is the other player moving, false if it's us
	 */
	protected abstract void movePiece(int fromSqi, int toSqi, boolean otherPlayer);
	
	/**
	 * Get the AI's canonical name.  This is dependent only on the internal prefix.
	 * 
	 * @return
	 */
	public String getName() {
		return ChessAI.AI_PREFIX + name;
	}
	
	/**
	 * Get the AI's displayed name.  This may vary depending on the "ai.name_format" config setting.
	 * 
	 * @return
	 */
	public String getDisplayName() {
		String fmt = ChessCraft.getInstance().getConfig().getString("ai.name_format", "[AI]<NAME>").replace("<NAME>", name);
		return ChessAI.AI_PREFIX + fmt;
	}
	
	public ChessGame getChessCraftGame() {
		return chessCraftGame;
	}

	public boolean isWhite() {
		return isWhite;
	}
	
	public boolean getPendingMove() {
		return pendingMove;
	}

	public void clearPendingMove() {
		this.pendingMove = false;
	}
	
	public int getPendingFrom() {
		return pendingFrom;
	}

	public int getPendingTo() {
		return pendingTo;
	}

	public boolean hasFailed() {
		return hasFailed;
	}

	public void setFailed(boolean failed) {
		hasFailed = failed;
	}

	/**
	 * Delete a running AI instance.  Called when a game is finished, deleted, or the plugin is disabled.
	 */
	public void delete() {
		setActive(false);
		AIFactory.instance.deleteAI(this);
		shutdown();
	}
	
	/**
	 * Set the AI-active state.  Will cause either the launch or termination of the AI calculation thread.
	 *   
	 * @param active
	 */
	public void setActive(boolean active) {
		if (active == this.active)
			return;

		this.active = active;

		LogUtils.fine(gameDetails + "active => " + active);
		
		if (active) {
			startThinking();
		} else {
			stopThinking();
		}
	}
	
	/**
	 * Inform the AI that the other player has made the given move.  We are assuming the move is legal,
	 * since it's already been validated by Chesspresso in the ChessGame object.  This also sets this AI to active,
	 * so it starts calculating the next move.
	 * 
	 * @param fromSqi	the square the other player has moved from
	 * @param toSqi		the square the other player has moved to
	 */
	public void userHasMoved(int fromSqi, int toSqi) {
		if (active) {
			LogUtils.warning(gameDetails + "userHasMoved() called while AI is active?");
			return;
		}
		
		try {
			movePiece(fromSqi, toSqi, true);
			LogUtils.fine(gameDetails + "userHasMoved: " + fromSqi + "->" + toSqi);
		} catch (Exception e) {
			// oops
			aiHasFailed(e);
		}
		
		setActive(true);
	}

	/**
	 * Replay a list of Chesspresso moves into the AI object.  Called when a game is restored
	 * from persisted data.
	 * 
	 * @param moves
	 */
	public void replayMoves(List<Short> moves) {
		active = isWhite;
		for (short move : moves) {
			int from = chesspresso.move.Move.getFromSqi(move);
			int to   = chesspresso.move.Move.getToSqi(move);
			movePiece(from, to, !active);
			active = !active;
		}
		LogUtils.fine(gameDetails + "ChessAI: replayed " + moves.size() + " moves: AI to move = " + active);
		if (active) {
			startThinking();
		}
	}
	
	/**
	 * Tell the AI to start thinking.  This will call a run() method, implemented in subclasses, 
	 * which will analyze the current board position and culminate by calling aiHasMoved() with the
	 * AI's next move.
	 */
	private void startThinking() {
		long delay = ChessCraft.getInstance().getConfig().getInt("ai.min_move_wait", 0);
		aiTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(ChessCraft.getInstance(), this, delay * 20L);
	}

	/**
	 * Tell the AI to stop thinking.
	 */
	private void stopThinking() {
		if (Bukkit.getScheduler().isCurrentlyRunning(aiTask)) {
			LogUtils.fine(gameDetails + "forcing shutdown for AI task #" + aiTask);
			Bukkit.getScheduler().cancelTask(aiTask);
		}
		aiTask = -1;
	}

	/**
	 * Called when the AI has come up with its next move.  Square indices always use the
	 * Chesspresso sqi representation.
	 * 
	 * @param fromSqi	the square the AI is moving from
	 * @param toSqi		the square the AI is moving to.
	 */
	protected void aiHasMoved(int fromSqi, int toSqi) {
		if (!active) {
			LogUtils.warning(gameDetails + "aiHasMoved() called when AI not active?");
			return;
		}

		setActive(false);
		movePiece(fromSqi, toSqi, false);
		LogUtils.fine(gameDetails + "aiHasMoved: " + fromSqi + "->" + toSqi);

		// Moving directly isn't thread-safe: we'd end up altering the Minecraft world from a separate thread,
		// which is Very Bad.  So we just note the move made now, and let the ChessGame object check for it on
		// the next clock tick.
		pendingFrom = fromSqi;
		pendingTo = toSqi;
		pendingMove = true;
	}

	/**
	 * Something has gone horribly wrong.  Need to abandon this game.
	 * 
	 * @param e
	 */
	protected void aiHasFailed(Exception e) {
		LogUtils.severe(gameDetails + "Unexpected Exception in AI");
		e.printStackTrace();
		chessCraftGame.alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
		hasFailed = true;
	}
	
	public static boolean isAIPlayer(String playerName) {
		return playerName.startsWith(AI_PREFIX);
	}

}

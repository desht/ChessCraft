/**
 * Programmer: Jacob Scott
 * Program Name: ChessAI
 * Description: class for interfacing with an AI engine
 * Date: Jul 25, 2011
 */
package me.desht.chesscraft.chess;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.enums.ChessEngine;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.free.jchecs.ai.Engine;
import fr.free.jchecs.ai.EngineFactory;
import fr.free.jchecs.core.Game;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Player;
import fr.free.jchecs.core.Square;

/**
 * @author jacob
 */
public class ChessAI {

	/**
	 * special character ensures AI name cannot (easily) be faked/hacked also
	 * adds another level of AI name visibility user/admins should NOT be given
	 * control of this variable - use something else to enable changing AI name
	 * colors, if wanted
	 */
	public final static String AI_PREFIX = ChatColor.WHITE.toString();

	private final static HashMap<String, ChessAI> runningAI = new HashMap<String, ChessAI>();
	private final static HashMap<String, AI_Def> availableAI = new HashMap<String, AI_Def>();

	private final String name;
	private final AI_Def aiSettings;
	private final ChessGame chessCraftGame;
	private final Game jChecsGame;
	private final boolean isWhiteAI;

	private boolean userToMove = true;
	private int aiTask = -1;
	private boolean hasFailed = false;
	private Move pendingMove = null;

	private ChessAI(String aiName, ChessGame chessCraftGame, boolean isWhiteAI) throws ChessException {
		aiSettings = getAIDefinition(aiName);
		if (aiSettings == null) {
			throw new ChessException(Messages.getString("ChessAI.AInotFound")); //$NON-NLS-1$
		} else if (runningAI.containsKey(aiSettings.name.toLowerCase())) {
			throw new ChessException(Messages.getString("ChessAI.AIbusy")); //$NON-NLS-1$
		}
		this.name = getAIPrefix() + aiSettings.name;
		this.chessCraftGame = chessCraftGame;
		this.isWhiteAI = isWhiteAI;
		this.jChecsGame = initGame();
	}

	/**
	 * Initialise the jChecs Game object.
	 * 
	 * @return
	 */
	private Game initGame() {
		Game jChecsGame = new fr.free.jchecs.core.Game();
		// _game.getPlayer(aiWhite)
		Player joueur = jChecsGame.getPlayer(!isWhiteAI);
		joueur.setName(Messages.getString("ChessAI.human")); //$NON-NLS-1$
		joueur.setEngine(null);
		joueur = jChecsGame.getPlayer(isWhiteAI);
		joueur.setName(Messages.getString("ChessAI.computer")); //$NON-NLS-1$
		joueur.setEngine(aiSettings.newInstance());

		return jChecsGame;
	}

	/**
	 * Get the AI's "public" name.  This is name displayed to players, and also stored in the ChessCraft
	 * ChessGame object.
	 * 
	 * @return
	 */
	public String getName() {
		return AI_PREFIX + name;
	}

	public AI_Def getAISettings() {
		return aiSettings;
	}

	public Move getPendingMove() {
		return pendingMove;
	}

	public void clearPendingMove() {
		this.pendingMove = null;
	}

	public boolean hasFailed() {
		return hasFailed;
	}

	public void setFailed(boolean failed) {
		hasFailed = failed;
	}

	/**
	 * Remove an AI object from the list of running AI's.  This should clean it up (as
	 * long as nowhere else has taken a reference to it).
	 */
	public void delete() {
		if (aiTask != -1) {
			Bukkit.getScheduler().cancelTask(aiTask);
		}
		jChecsGame.getPlayer(isWhiteAI).setEngine(null);
		runningAI.remove(aiSettings.name.toLowerCase());
	}

	/**
	 * Set whether this AI or the "user" (other player) is to move next.
	 * 
	 * @param move	true if the other player is to move, false if we (this AI) is to move
	 */
	public void setUserToMove(boolean move) {
		if (move == userToMove) {
			return;
		}
		userToMove = move;
		if (!userToMove) {
			setAIThinking();
		}
	}

	/**
	 * Start the AI thinking about its next move.
	 */
	private void setAIThinking() {
		int wait = ChessCraft.getInstance().getConfig().getInt("ai.min_move_wait"); //$NON-NLS-1$
		aiTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(ChessCraft.getInstance(), new Runnable() {
			public void run() {
				try {
					final MoveGenerator plateau = jChecsGame.getBoard();
					final Engine engine = jChecsGame.getPlayer(isWhiteAI).getEngine();
					final Move m = engine.getMoveFor(plateau);
					aiHasMoved(m);
					aiTask = -1;
				} catch (Exception e) {
					aiHasFailed(e);
				}
			}
		}, wait * 20L);
	}

	/**
	 * Replay a list of moves into the jchecsGame object.  Called when a game is restored
	 * from persisted data.
	 * 
	 * @param moves
	 */
	public void replayMoves(List<Short> moves) {
		userToMove = !isWhiteAI;
		for (short move : moves) {
			Square from = Square.valueOf(chesspresso.move.Move.getFromSqi(move));
			Square to = Square.valueOf(chesspresso.move.Move.getToSqi(move));
			jChecsGame.moveFromCurrent(new Move(jChecsGame.getBoard().getPieceAt(from), from, to));
			userToMove = !userToMove;
		}
		LogUtils.finer("ChessAI: replayMoves: loaded " + moves.size() + " moves into " + getName() + ": AI to move = " + !userToMove);
		if (!userToMove) {
			setAIThinking();
		}
	}

	void userHasMoved(int fromIndex, int toIndex) {
		if (!userToMove) {
			return;
		}
		try {
			// conveniently, Chesspresso & jChecs use the same row/column/sqi conventions
			Square from = Square.valueOf(fromIndex);
			Square to = Square.valueOf(toIndex);

			// we're assuming the move is legal (it should be - it's already been validated by Chesspresso)
			Move m = new Move(jChecsGame.getBoard().getPieceAt(from), from, to);
			LogUtils.fine("ChessAI: userHasMoved: " + m);
			jChecsGame.moveFromCurrent(m);
		} catch (Exception e) {
			aiHasFailed(e);
		}

		setUserToMove(false);
	}

	private void aiHasMoved(Move m) {
		if (userToMove) {
			return;
		}

		setUserToMove(true);
		LogUtils.fine("ChessAI: aiHasMoved: " + m);

		// Moving directly isn't thread-safe: we'd end up altering the Minecraft world from a separate thread,
		// which is Very Bad.  So we just note the move made now, and let the ChessGame object check for it on
		// the next clock tick.
		jChecsGame.moveFromCurrent(m);
		pendingMove = m;
	}

	/**
	 * Something has gone horribly wrong.  Need to abandon this game.
	 * 
	 * @param e
	 */
	private void aiHasFailed(Exception e) {
		LogUtils.severe("Unexpected Exception in AI", e);
		chessCraftGame.alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
		hasFailed = true;
	}

	/*------------------------------------- static methods --------------------------------*/

	/**
	 * Check if the given player is an AI.  Check the static (non-user-definable) prefix for this.
	 * 
	 * @param name
	 * @return
	 */
	public static boolean isAIPlayer(String name) {
		return name.startsWith(AI_PREFIX);
	}
	
	/**
	 * Get the user-definable prefix for AI players.
	 * 
	 * @return
	 */
	public static String getAIPrefix() {
		return ChessCraft.getInstance().getConfig().getString("ai.name_prefix"); //$NON-NLS-1$
	}

	/**
	 * Load the AI_settings.yml file and initialise the list of available AI's from it.
	 */
	public static void initAINames() {
		availableAI.clear();
		try {
			File aiFile = new File(DirectoryStructure.getPluginDirectory(), "AI_settings.yml"); //$NON-NLS-1$
			if (!aiFile.exists()) {
				LogUtils.severe("AI Loading Error: file not found"); //$NON-NLS-1$
				return;
			}
			YamlConfiguration config = YamlConfiguration.loadConfiguration(aiFile);
			ConfigurationSection n = config.getConfigurationSection("AI"); //$NON-NLS-1$

			if (n == null) {
				LogUtils.severe("AI Loading Error: AI definitions not found"); //$NON-NLS-1$
				return;
			}

			for (String a : n.getKeys(false)) {
				ConfigurationSection d = n.getConfigurationSection(a);
				if (n.getBoolean("enabled", true)) { //$NON-NLS-1$
					for (String name : d.getString("funName", a).split(",")) { //$NON-NLS-1$ //$NON-NLS-2$
						if ((name = name.trim()).length() > 0) {
							AI_Def def = new AI_Def(name, ChessEngine.getEngine(d.getString("engine")), //$NON-NLS-1$
							                        d.getInt("depth", 0), //$NON-NLS-1$
							                        d.getDouble("payout_multiplier", 1.0), //$NON-NLS-1$ 
							                        d.getString("comment"));
							availableAI.put(name.toLowerCase(), def);
						}
					}
				}
			}
		} catch (Exception ex) {
			LogUtils.severe(Messages.getString("ChessAI.AIloadError"), ex); //$NON-NLS-1$
		}
	}

	/**
	 * Get a new AI player.
	 * 
	 * @param callback		the ChessCraft ChessGame object
	 * @param aiName		name of the desired AI (may be null to choose a random free AI)
	 * @param forceNew		get a new AI even if the configured max number of AI's is reached
	 * @param isWhiteAI		true if the AI will play white, false if playing black
	 * @return				the AI object
	 * @throws ChessException
	 */
	public static ChessAI getNewAI(ChessGame callback, String aiName, boolean forceNew, boolean isWhiteAI) throws ChessException {
		// uses exceptions method to stop too many AI's being created
		if (!forceNew) {
			int max = ChessCraft.getInstance().getConfig().getInt("ai.max_ai_games"); //$NON-NLS-1$
			if (max == 0) {
				throw new ChessException(Messages.getString("ChessAI.AIdisabled")); //$NON-NLS-1$
			} else if (runningAI.size() >= max) {
				throw new ChessException(Messages.getString("ChessAI.noAvailableAIs", max)); //$NON-NLS-1$
			}
		}

		ChessAI ai = new ChessAI(aiName, callback, isWhiteAI);
		runningAI.put(ai.aiSettings.name.toLowerCase(), ai);

		return ai;
	}

	/**
	 * Get a list of all known AI definitions.
	 * 
	 * @param isSorted	if true, sort the list by AI name
	 * @return
	 */
	public static List<AI_Def> listAIs(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(availableAI.keySet());
			List<AI_Def> res = new ArrayList<AI_Def>();
			for (String name : sorted) {
				res.add(availableAI.get(name));
			}
			return res;
		} else {
			return new ArrayList<AI_Def>(availableAI.values());
		}
	}

	public static List<AI_Def> listAIs() {
		return listAIs(true);
	}

	/**
	 * Clear down all running AI's.  This should be called when the plugin is disabled.
	 */
	public static void clearAIs() {
		String[] ais = runningAI.keySet().toArray(new String[0]);
		for (String aiName : ais) {
			ChessAI ai = runningAI.get(aiName);
			if (ai != null) {
				ai.delete();
			}
		}
		runningAI.clear();
	}

	/**
	 * Check if the given AI is free for a game.
	 * 
	 * @param ai
	 * @return
	 */
	public static boolean isFree(AI_Def ai) {
		return ai != null && !runningAI.containsKey(ai.name.toLowerCase());
	}

	public static AI_Def getFreeAI(String aiName) {
		AI_Def ai = getAIDefinition(aiName);
		return ai != null && !runningAI.containsKey(ai.name.toLowerCase()) ? ai : null;
	}

	/**
	 * Get the AI definition for the given AI name
	 * 
	 * @param aiName
	 *            Name of the AI, either with or without the AI prefix string <br>
	 *            if null, will return a random free AI (or null, if none are
	 *            free)
	 * @return The AI definition, or null if not found
	 */
	public static AI_Def getAIDefinition(String aiName) {
		if (aiName == null) {
			// return a random free AI
			ArrayList<Integer> free = new ArrayList<Integer>();
			String ai[] = availableAI.keySet().toArray(new String[0]);
			for (int i = 0; i < ai.length; ++i) {
				if (!runningAI.containsKey(ai[i])) {
					free.add(i);
				}
			}
			if (free.size() > 0) {
				Random r = new Random();
				return availableAI.get(ai[r.nextInt(ai.length)]);
			} else {
				return null;
			}
		}
		// else, return one with a matching name
		// (if multiple, return one if its the only one free)
		aiName = ChatColor.stripColor(aiName.toLowerCase());
		if (aiName.startsWith(getAIPrefix().toLowerCase())) {
			aiName = aiName.substring(getAIPrefix().length());
		}
		if (!availableAI.containsKey(aiName)) {
			String keys[] = availableAI.keySet().toArray(new String[0]);
			String matches[] = ChessUtils.fuzzyMatch(aiName, keys, 3);
			if (matches.length == 1) {
				aiName = matches[0];
			} else if (matches.length > 0) {
				// first that is available
				int k = -1;
				for (int i = 0; i < matches.length; ++i) {
					if (!runningAI.containsKey(matches[i])) {
						if (k != -1) {
							k = -1;
							break;
						} else {
							k = i;
						}
					}
				}
				if (k != -1) {
					aiName = matches[k];
				}
			}
		}

		return availableAI.get(aiName);
	}

	public static class AI_Def {

		public String name;
		ChessEngine engine;
		int searchDepth;
		double payoutMultiplier;
		String comment;

		public AI_Def(String name, ChessEngine engine, int searchDepth, double payoutMultiplier, String comment) {
			this.name = name;
			this.engine = engine;
			this.searchDepth = searchDepth;
			this.payoutMultiplier = payoutMultiplier;
			this.comment = comment;
		}

		public String getName() {
			return name;
		}

		public String getFullAIName() {
			return ChessAI.AI_PREFIX + ChessAI.getAIPrefix() + name;
		}

		public ChessEngine getEngine() {
			return engine;
		}

		public int getSearchDepth() {
			return searchDepth;
		}

		public double getPayoutMultiplier() {
			return payoutMultiplier;
		}

		public String getComment() {
			return comment;
		}

		public Engine newInstance() {
			Engine moteur;
			if (engine == null) {
				moteur = EngineFactory.newInstance();
			} else {
				moteur = EngineFactory.newInstance(engine.toString());
				if (searchDepth >= engine.getMinDepth() && searchDepth <= engine.getMaxDepth()) {
					moteur.setSearchDepthLimit(searchDepth);
				}
			}
			moteur.setOpeningsEnabled(ChessCraft.getInstance().getConfig().getBoolean("ai.use_opening_book", false));
			return moteur;
		}
	}
} // end class ChessAI


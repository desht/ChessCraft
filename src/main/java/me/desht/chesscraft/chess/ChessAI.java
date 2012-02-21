/**
 * Programmer: Jacob Scott
 * Program Name: ChessAI
 * Description: class for interfacing with an AI engine
 * Date: Jul 25, 2011
 */
package me.desht.chesscraft.chess;

import fr.free.jchecs.ai.Engine;
import fr.free.jchecs.ai.EngineFactory;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Player;
import fr.free.jchecs.core.Square;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.enums.ChessEngine;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.Rand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

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
	
	private fr.free.jchecs.core.Game jChecsGame = null;
	private final String name;
	private ChessGame chessCraftGame = null;
	private boolean userToMove = true, isWhite = false;
	private int aiTask = -1;
	private final AI_Def aiSettings;

	private ChessAI() throws ChessException {
		aiSettings = getAI(null);
		if (aiSettings == null) {
			throw new ChessException(Messages.getString("ChessAI.noFreeAI")); //$NON-NLS-1$
		}
		name = getAIPrefix() + aiSettings.name;
	}

	private ChessAI(String aiName) throws ChessException {
		aiSettings = getAI(aiName);
		if (aiSettings == null) {
			throw new ChessException(Messages.getString("ChessAI.AInotFound")); //$NON-NLS-1$
		} else if (runningAI.containsKey(aiSettings.name.toLowerCase())) {
			throw new ChessException(Messages.getString("ChessAI.AIbusy")); //$NON-NLS-1$
		}
		name = getAIPrefix() + aiSettings.name;
	}

	private ChessAI(AI_Def ai) throws ChessException {
		if (ai == null) {
			throw new ChessException(Messages.getString("ChessAI.AInotFound")); //$NON-NLS-1$
		} else if (runningAI.containsKey(ai.name.toLowerCase())) {
			throw new ChessException(Messages.getString("ChessAI.AIbusy")); //$NON-NLS-1$
		}
		aiSettings = ai;
		name = getAIPrefix() + aiSettings.name;
	}

	public String getName() {
		return AI_PREFIX + name;
	}

	public AI_Def getAISettings() {
		return aiSettings;
	}

	public static String getAIPrefix() {
		return ChessConfig.getConfig().getString("ai.name_prefix"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void init(boolean aiWhite) {
		if (jChecsGame != null) {
			return; // only init once
		}
		jChecsGame = new fr.free.jchecs.core.Game();
		// _game.getPlayer(aiWhite)
		Player joueur = jChecsGame.getPlayer(!aiWhite);
		joueur.setName(Messages.getString("ChessAI.human")); //$NON-NLS-1$
		joueur.setEngine(null);
		joueur = jChecsGame.getPlayer(aiWhite);
		joueur.setName(Messages.getString("ChessAI.computer")); //$NON-NLS-1$
		joueur.setEngine(aiSettings.newInstance());
		isWhite = aiWhite;
	}

	public static void initAI_Names() {
		availableAI.clear();
		try {
			File aiFile = new File(DirectoryStructure.getPluginDirectory(), "AI_settings.yml"); //$NON-NLS-1$
			if (!aiFile.exists()) {
				ChessCraftLogger.log(Level.SEVERE, "AI Loading Error: file not found"); //$NON-NLS-1$
				return;
			}
			YamlConfiguration config = YamlConfiguration.loadConfiguration(aiFile);
			ConfigurationSection n = config.getConfigurationSection("AI"); //$NON-NLS-1$

			if (n == null) {
				ChessCraftLogger.log(Level.SEVERE, "AI Loading Error: AI definitions not found"); //$NON-NLS-1$
				return;
			}

			for (String a : n.getKeys(false)) {
				ConfigurationSection d = n.getConfigurationSection(a);
				if (n.getBoolean("enabled", true)) { //$NON-NLS-1$
					for (String name : d.getString("funName", a).split(",")) { //$NON-NLS-1$ //$NON-NLS-2$
						if ((name = name.trim()).length() > 0) {
							availableAI.put(
									name.toLowerCase(),
									new AI_Def(name, ChessEngine.getEngine(d.getString("engine")), //$NON-NLS-1$
									d.getInt("depth", 0), d.getDouble("payout_multiplier", 1.0), d //$NON-NLS-1$ //$NON-NLS-2$
									.getString("comment"))); //$NON-NLS-1$
						}
					}
				}
			}
		} catch (Exception ex) {
			ChessCraftLogger.log(Level.SEVERE, Messages.getString("ChessAI.AIloadError"), ex); //$NON-NLS-1$
		}
	}

	public static ChessAI getNewAI(ChessGame callback) throws ChessException {
		return getNewAI(callback, null, false);
	}

	public static ChessAI getNewAI(ChessGame callback, boolean forceNew) throws ChessException {
		return getNewAI(callback, null, forceNew);
	}

	public static ChessAI getNewAI(ChessGame callback, String aiName) throws ChessException {
		return getNewAI(callback, aiName, false);
	}

	public static ChessAI getNewAI(ChessGame callback, String aiName, boolean forceNew) throws ChessException {
		// uses exceptions method to stop too many AI
		if (!forceNew) {
			int max = ChessConfig.getConfig().getInt("ai.max_ai_games"); //$NON-NLS-1$
			if (max == 0) {
				throw new ChessException(Messages.getString("ChessAI.AIdisabled")); //$NON-NLS-1$
			} else if (runningAI.size() >= max) {
				throw new ChessException(Messages.getString("ChessAI.noAvailableAIs", max)); //$NON-NLS-1$
			}
		}

		ChessAI ai = new ChessAI(aiName);
		ai.chessCraftGame = callback;
		runningAI.put(ai.aiSettings.name.toLowerCase(), ai);

		return ai;
	}

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

	public static void clearAI() {
		String[] ais = runningAI.keySet().toArray(new String[0]);
		for (String aiName : ais) {
			ChessAI ai = runningAI.get(aiName);
			if (ai != null) {
				ai.removeAI();
			}
		}
		runningAI.clear();
	}

	public void removeAI() {
		if (aiTask != -1) {
			Bukkit.getScheduler().cancelTask(aiTask);
		}
		if (jChecsGame != null) {
			jChecsGame.getPlayer(isWhite).setEngine(null);
			jChecsGame = null;
		}
		chessCraftGame = null;
		runningAI.remove(aiSettings.name.toLowerCase());
	}

	public void loadBoard(chesspresso.game.Game game, boolean usersTurn) {
		setUserMove(usersTurn);
	}

	public void setUserMove(boolean move) {
		if (move != userToMove) {
			if (!(userToMove = move)) {
				int wait = ChessConfig.getConfig().getInt("ai.min_move_wait"); //$NON-NLS-1$
				aiTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(ChessCraft.getInstance(), new Runnable() {

					public void run() {
						final MoveGenerator plateau = jChecsGame.getBoard();
						final Engine ia = jChecsGame.getPlayer(isWhite).getEngine();
						if (ia != null) {
							Move m = ia.getMoveFor(plateau);
							aiMove(m);
							aiTask = -1;
						}
					}
				}, wait * 20);
			}
		}
	}

	public void loadmove(int fromIndex, int toIndex) {
		Square from = Square.valueOf(fromIndex), to = Square.valueOf(toIndex);
		jChecsGame.moveFromCurrent(new Move(jChecsGame.getBoard().getPieceAt(from), from, to));
		userToMove = !userToMove;
	}

	public void loadDone() {
		if (!userToMove) {
			// trick the other method into starting the ai thread
			userToMove = true;
			setUserMove(false);
		}
	}

	public void userMove(int fromIndex, int toIndex) {
		if (!userToMove) {
			return;
		}
		try {

			// System.out.println("user move: " + fromIndex + " to " + toIndex);

			Square from = Square.valueOf(fromIndex),
					to = Square.valueOf(toIndex);
			// or?
			// Square from = Square.valueOf(chesspresso.Chess.sqiToRow(fromIndex),
			// chesspresso.Chess.sqiToCol(fromIndex)),
			// to = Square.valueOf(chesspresso.Chess.sqiToRow(toIndex),
			// chesspresso.Chess.sqiToCol(toIndex));

			// assume move is legal
			jChecsGame.moveFromCurrent(new Move(jChecsGame.getBoard().getPieceAt(from), from, to));

		} catch (Exception e) {
			ChessCraftLogger.severe("Error in ChessAI", e);
		}
		setUserMove(false);
	}

	public void aiMove(Move m) {
		if (userToMove || jChecsGame == null) {
			return;
		}
		// System.out.println("ai move: " + m);

		try {
			// Moving directly isn't thread-safe: we'd end up altering the Minecraft world from a separate thread,
			// which is very bad.  So we just tell the game that the AI has moved, and it can check on the next tick.
			chessCraftGame.aiHasMoved(m.getFrom().getIndex(), m.getTo().getIndex());
			if (jChecsGame != null) { // if game not been deleted
				jChecsGame.moveFromCurrent(m);
			}
		} catch (Exception ex) {
			// TODO: need to offer the player a draw here, really
			ChessCraftLogger.log(Level.SEVERE, "Unexpected Exception in AI", ex); //$NON-NLS-1$
			chessCraftGame.alert(Messages.getString("ChessAI.AIunexpectedException", ex.getMessage())); //$NON-NLS-1$
		}

		userToMove = true;
	}

	public static boolean isFree(AI_Def ai) {
		return ai != null && !runningAI.containsKey(ai.name.toLowerCase());
	}

	public static AI_Def getFreeAI(String aiName) {
		AI_Def ai = getAI(aiName);
		return ai != null && !runningAI.containsKey(ai.name.toLowerCase()) ? ai : null;
	}

	/**
	 * Get the AI definition for the given name
	 * 
	 * @param aiName
	 *            Name of the AI, either with or without the AI prefix string <br>
	 *            if null, will return a random free AI (or null, if none are
	 *            free)
	 * @return The AI definition, or null if not found
	 */
	public static AI_Def getAI(String aiName) {
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
				return availableAI.get(ai[Rand.RandomInt(0, ai.length - 1)]);
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
			moteur.setOpeningsEnabled(ChessConfig.getConfig().getBoolean("ai.use_opening_book", false));
			return moteur;
		}
	}
} // end class ChessAI


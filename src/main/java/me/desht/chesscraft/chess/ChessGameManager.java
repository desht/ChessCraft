package me.desht.chesscraft.chess;

import chesspresso.Chess;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.player.HumanChessPlayer;
import me.desht.chesscraft.event.ChessGameCreatedEvent;
import me.desht.chesscraft.event.ChessGameDeletedEvent;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class ChessGameManager {
	private static ChessGameManager instance = null;

	private final Set<ChessGame> needToMigrate = new HashSet<ChessGame>();

	private final Map<String,ChessGame> chessGames = new HashMap<String,ChessGame>();
	private final Map<UUID,ChessGame> currentGame = new HashMap<UUID, ChessGame>();

	private ChessGameManager() {
	}

	public static synchronized ChessGameManager getManager() {
		if (instance == null) {
			instance = new ChessGameManager();
		}
		return instance;
	}

	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}


	public void registerGame(ChessGame game) {
		String gameName = game.getName();
		if (!chessGames.containsKey(gameName)) {
			chessGames.put(gameName, game);
			Bukkit.getPluginManager().callEvent(new ChessGameCreatedEvent(game));
		} else {
			throw new ChessException("trying to register duplicate game " + gameName);
		}
	}

	private void unregisterGame(String gameName) {
		ChessGame game = getGame(gameName);

		List<UUID> toRemove = new ArrayList<UUID>();
		for (UUID playerId : currentGame.keySet()) {
			if (currentGame.get(playerId) == game) {
				toRemove.add(playerId);
			}
		}
		for (UUID p : toRemove) {
			currentGame.remove(p);
		}
		chessGames.remove(gameName);
		Bukkit.getPluginManager().callEvent(new ChessGameDeletedEvent(game));
	}

	/**
	 * Delete the game of the given name.  Permanent deletion is when a game is explicitly
	 * deleted; it is cleaned up and purged from disk.  Temporary deletion occurs when the
	 * plugin is reloading games or being disabled and simply unregisters the game from the
	 * game manager.
	 *
	 * @param gameName Name of the game to delete
	 * @param permanent true if game is to be permanently deleted
	 */
	public void deleteGame(String gameName, boolean permanent) {
		ChessGame game = getGame(gameName);
		if (permanent) {
			ChessCraft.getInstance().getPersistenceHandler().unpersist(game);
		}
		game.onDeleted(permanent);
		unregisterGame(gameName);
	}

	public boolean checkGame(String gameName) {
		return chessGames.containsKey(gameName);
	}

	public Collection<ChessGame> listGamesSorted() {
		SortedSet<String> sorted = new TreeSet<String>(chessGames.keySet());
		List<ChessGame> res = new ArrayList<ChessGame>();
		for (String name : sorted) {
			res.add(chessGames.get(name));
		}
		return res;
	}

	public Collection<ChessGame> listGames() {
		return chessGames.values();
	}

	public ChessGame getGame(String name) {
		return getGame(name, true);
	}

	public ChessGame getGame(String name, boolean fuzzy) {
		if (!chessGames.containsKey(name)) {
			if (fuzzy && chessGames.size() > 0) {
				// try "fuzzy" search
				Set<String> strings = chessGames.keySet();
				String keys[] = strings.toArray(new String[strings.size()]);
				String matches[] = ChessUtils.fuzzyMatch(name, keys, 3);

				if (matches.length == 1) {
					return chessGames.get(matches[0]);
				} else {
					// partial-name search
					int k = -1, c = 0;
					name = name.toLowerCase();
					for (int i = 0; i < keys.length; ++i) {
						if (keys[i].toLowerCase().startsWith(name)) {
							k = i;
							++c;
						}
					}
					if (k >= 0 && c == 1) {
						return chessGames.get(keys[k]);
					}
				}
				// TODO: if multiple matches, check if only one is waiting for
				// more players (and return that one)
			}
			throw new ChessException(Messages.getString("Game.noSuchGame", name)); //$NON-NLS-1$
		}
		return chessGames.get(name);
	}

	public void setCurrentGame(UUID uuid, String gameName) {
		currentGame.put(uuid, getGame(gameName));
	}

	public void setCurrentGame(Player player, ChessGame game) {
		currentGame.put(player.getUniqueId(), game);
	}

	public ChessGame getCurrentGame(Player player) {
		return getCurrentGame(player, false);
	}

	public ChessGame getCurrentGame(Player player, boolean verify) {
		ChessGame game = currentGame.get(player.getUniqueId());
		if (verify && game == null) {
			throw new ChessException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
		}
		return game;
	}

	public Map<UUID, String> getCurrentGames() {
		Map<UUID, String> res = new HashMap<UUID, String>();
		for (UUID s : currentGame.keySet()) {
			ChessGame game = currentGame.get(s);
			if (game != null) {
				res.put(s, game.getName());
			}
		}
		return res;
	}

	/**
	 * Create a unique game name based on the player's name.
	 *
	 * @param playerName player's name
	 * @return a unique game name
	 */
	private String makeGameName(String playerName) {
		String res;
		int n = 1;
		do {
			res = playerName + "-" + n++; //$NON-NLS-1$
		} while (checkGame(res));

		return res;
	}

	/**
	 * Convenience method to create a new chess game.
	 *
	 * @param player		The player who is creating the game
	 * @param gameName		Name of the game - may be null, in which case a name will be generated
	 * @param boardName		Name of the board for the game - may be null, in which case a free board will be picked
	 * @return	The game object
	 * @throws ChessException	if there is any problem creating the game
	 */
	public ChessGame createGame(Player player, String gameName, String boardName, int colour) {
		BoardView bv;
		if (boardName == null) {
			bv = BoardViewManager.getManager().getFreeBoard();
		} else {
			bv = BoardViewManager.getManager().getBoardView(boardName);
		}

		return createGame(player, gameName, bv, colour);
	}

	public ChessGame createGame(Player player, String gameName, BoardView bv, int colour) {
		if (gameName == null || gameName.equals("-")) {
			gameName = makeGameName(player.getName());
		}

		ChessGame game = new ChessGame(gameName, player, bv, colour);
		registerGame(game);
		setCurrentGame(player, game);
		bv.getControlPanel().repaintControls();

		game.autoSave();

		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.gameCreated", game.getName(), game.getView().getName()));

		return game;
	}

	/**
	 * A game with an old-style player name has been loaded; add it to the list of games which will
	 * be asychronously migrated to player UUIDs.  (Must be async since we'll be contacting Mojang's
	 * API service, which could block)
	 *
	 * @param game the game to be migrated
	 */
	public void needToDoUUIDMigration(ChessGame game) {
		needToMigrate.add(game);
	}

	/**
	 * Carry out the migration of old-style player names to UUIDs.  This is done asynchronously.
	 */
	public void checkForUUIDMigration() {
		final List<String> names = new ArrayList<String>();
		final List<GameAndColour> gameAndColours = new ArrayList<GameAndColour>();
		for (ChessGame game : needToMigrate) {
			if (game.hasPlayer(Chess.WHITE) && game.getPlayer(Chess.WHITE).isHuman()) {
				HumanChessPlayer hcp = (HumanChessPlayer) game.getPlayer(Chess.WHITE);
				if (hcp.getOldStyleName() != null) {
					names.add(hcp.getOldStyleName());
					gameAndColours.add(new GameAndColour(game, hcp.getColour()));
				}
			}
			if (game.hasPlayer(Chess.BLACK) && game.getPlayer(Chess.BLACK).isHuman()) {
				HumanChessPlayer hcp = (HumanChessPlayer) game.getPlayer(Chess.BLACK);
				if (hcp.getOldStyleName() != null) {
					names.add(hcp.getOldStyleName());
					gameAndColours.add(new GameAndColour(game, hcp.getColour()));
				}
			}
		}
		if (names.size() > 0) {
			LogUtils.info("migrating " + names.size() + " player names to UUID in saved game files");
			Bukkit.getScheduler().runTaskAsynchronously(ChessCraft.getInstance(), new Runnable() {
				@Override
				public void run() {
					UUIDFetcher uf = new UUIDFetcher(names, true);
					try {
						Bukkit.getScheduler().runTask(ChessCraft.getInstance(), new SyncTask(uf.call(), gameAndColours));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	private class SyncTask implements Runnable {
		private final Map<String, UUID> map;
		private final List<GameAndColour> gameAndColours;

		public SyncTask(Map<String, UUID> map, List<GameAndColour> gameAndColours) {
			this.map = map;
			this.gameAndColours = gameAndColours;
		}

		@Override
		public void run() {
			for (GameAndColour gc : gameAndColours) {
				HumanChessPlayer hcp = (HumanChessPlayer) gc.game.getPlayer(gc.colour);
				gc.game.migratePlayer(gc.colour, hcp.getOldStyleName(), map.get(hcp.getOldStyleName()));
			}
			for (ChessGame game : listGames()) {
				game.save();
			}
			LogUtils.info("player name -> UUID migration complete");
			needToMigrate.clear();
		}
	}

	private class GameAndColour {
		private final ChessGame game;
		private final int colour;

		private GameAndColour(ChessGame game, int colour) {
			this.game = game;
			this.colour = colour;
		}
	}
}

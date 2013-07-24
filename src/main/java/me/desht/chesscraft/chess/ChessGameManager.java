package me.desht.chesscraft.chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.event.ChessGameCreatedEvent;
import me.desht.chesscraft.event.ChessGameDeletedEvent;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChessGameManager {
	private static ChessGameManager instance = null;

	private final Map<String,ChessGame> chessGames = new HashMap<String,ChessGame>();
	private final Map<String,ChessGame> currentGame = new HashMap<String, ChessGame>();

	private ChessGameManager() {

	}

	public static synchronized ChessGameManager getManager() {
		if (instance == null) {
			instance = new ChessGameManager();
		}
		return instance;
	}

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

		List<String> toRemove = new ArrayList<String>();
		for (String playerName : currentGame.keySet()) {
			if (currentGame.get(playerName) == game) {
				toRemove.add(playerName);
			}
		}
		for (String p : toRemove) {
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
			ChessCraft.getPersistenceHandler().unpersist(game);
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
				String keys[] = chessGames.keySet().toArray(new String[0]);
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

	public void setCurrentGame(String playerName, String gameName) {
		ChessGame game = getGame(gameName);
		setCurrentGame(playerName, game);
	}

	public void setCurrentGame(String playerName, ChessGame game) {
		currentGame.put(playerName, game);
	}

	public ChessGame getCurrentGame(String playerName) {
		return getCurrentGame(playerName, false);
	}

	public ChessGame getCurrentGame(String playerName, boolean verify) {
		ChessGame game = currentGame.get(playerName);
		if (verify && game == null) {
			throw new ChessException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
		}
		return game;
	}

	public Map<String, String> getCurrentGames() {
		Map<String, String> res = new HashMap<String, String>();
		for (String s : currentGame.keySet()) {
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
	 * @param playerName
	 * @return
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
		String playerName = player.getName();

		if (gameName == null || gameName.equals("-")) {
			gameName = makeGameName(playerName);
		}

		ChessGame game = new ChessGame(gameName, bv, playerName, colour);
		registerGame(game);
		setCurrentGame(playerName, game);
		bv.getControlPanel().repaintControls();

		game.autoSave();

		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.gameCreated", game.getName(), game.getView().getName())); //$NON-NLS-1$ 

		return game;
	}
}

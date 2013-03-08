package me.desht.chesscraft.chess;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.event.ChessBoardCreatedEvent;
import me.desht.chesscraft.event.ChessBoardDeletedEvent;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.TerrainBackup;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BoardViewManager {

	private static BoardViewManager instance = null;

	private final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
	private final Map<String, Set<File>> deferred = new HashMap<String, Set<File>>();
	private PersistableLocation globalTeleportOutDest = null;
	
	private BoardViewManager() {	
	}
	
	public static synchronized BoardViewManager getManager() {
		if (instance == null) {
			instance = new BoardViewManager();
		}
		return instance;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * @return the globalTeleportOutDest
	 */
	public Location getGlobalTeleportOutDest() {
		return globalTeleportOutDest == null ? null : globalTeleportOutDest.getLocation();
	}

	/**
	 * @param globalTeleportOutDest the globalTeleportOutDest to set
	 */
	public void setGlobalTeleportOutDest(Location globalTeleportOutDest) {
		this.globalTeleportOutDest = globalTeleportOutDest == null ? null : new PersistableLocation(globalTeleportOutDest);
	}

	public void registerView(BoardView view) {
		chessBoards.put(view.getName(), view);
		
		Bukkit.getPluginManager().callEvent(new ChessBoardCreatedEvent(view));
	}

	public void unregisterBoardView(String name) {
		BoardView bv;
		try {
			bv = getBoardView(name);
			chessBoards.remove(name);
			Bukkit.getPluginManager().callEvent(new ChessBoardDeletedEvent(bv));
		} catch (ChessException e) {
			LogUtils.warning("removeBoardView: unknown board name " + name);
		}
	}

	public void removeAllBoardViews() {
		for (BoardView bv : listBoardViews()) {
			Bukkit.getPluginManager().callEvent(new ChessBoardDeletedEvent(bv));
		}
		chessBoards.clear();
	}

	public boolean boardViewExists(String name) {
		return chessBoards.containsKey(name);
	}

	public BoardView getBoardView(String name) throws ChessException {
		if (!chessBoards.containsKey(name)) {
			if (chessBoards.size() > 0) {
				// try "fuzzy" search
				String keys[] = chessBoards.keySet().toArray(new String[0]);
				String matches[] = ChessUtils.fuzzyMatch(name, keys, 3);

				if (matches.length == 1) {
					return chessBoards.get(matches[0]);
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
						return chessBoards.get(keys[k]);
					}
				}
			}
			throw new ChessException(Messages.getString("BoardView.noSuchBoard", name)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return chessBoards.get(name);
	}

	public Collection<BoardView> listBoardViews() {
		return chessBoards.values();
	}

	public Collection<BoardView> listBoardViewsSorted() {
		SortedSet<String> sorted = new TreeSet<String>(chessBoards.keySet());
		List<BoardView> res = new ArrayList<BoardView>();
		for (String name : sorted) {
			res.add(chessBoards.get(name));
		}
		return res;
	}

	/**
	 * Get a board that does not have a game running.
	 * 
	 * @return the first free board found
	 * @throws ChessException if no free board was found
	 */
	public BoardView getFreeBoard() throws ChessException {
		for (BoardView bv : listBoardViews()) {
			if (bv.getGame() == null && !bv.isDesigning()) {
				return bv;
			}
		}
		throw new ChessException(Messages.getString("BoardView.noFreeBoards")); //$NON-NLS-1$
	}

	/**
	 * Check if a location is any part of any board including the frame & enclosure.
	 * 
	 * @param loc	location to check
	 * @param fudge	fudge factor - check a larger area around the board
	 * @return the boardview that matches, or null if none
	 */
	public BoardView partOfChessBoard(Location loc) {
		return partOfChessBoard(loc, 0);
	}

	public BoardView partOfChessBoard(Location loc, int fudge) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isPartOfBoard(loc, fudge)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Check if location is above a board square but below the roof
	 * 
	 * @param loc  location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView aboveChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isAboveBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Check if location is part of a board square
	 * 
	 * @param loc	location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView onChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isOnBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Teleport the player in a sensible manner, depending on where they are now.
	 * 
	 * @param player
	 * @throws ChessException
	 */
	public void teleportOut(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.teleport");

		BoardView bv = partOfChessBoard(player.getLocation(), 0);
		Location prev = ChessCraft.getInstance().getPlayerTracker().getLastPos(player);
		if (bv != null && bv.hasTeleportDestination()) {
			// board has a specific location defined
			Location loc = bv.getTeleportDestination();
			ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, loc);
		} else if (bv != null && globalTeleportOutDest != null) {
			ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, getGlobalTeleportOutDest());
		} else if (bv != null && (prev == null || partOfChessBoard(prev, 0) == bv)) {
			// try to get the player out of this board safely
			Location loc = bv.findSafeLocationOutside();
			if (loc != null) {
				ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, loc);
			} else {
				ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, player.getWorld().getSpawnLocation());
				MiscUtil.errorMessage(player, Messages.getString("ChessCommandExecutor.goingToSpawn")); //$NON-NLS-1$
			}
		} else if (prev != null) {
			// go back to previous location
			ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, prev);
		} else {
			throw new ChessException(Messages.getString("ChessCommandExecutor.notOnChessboard")); //$NON-NLS-1$
		}
	}

	/**
	 * Convenience method to create a new board and do all the associated setup tasks.
	 * 
	 * @param boardName
	 * @param loc
	 * @param style
	 * @param pieceStyle
	 * @return a fully initialised and painted board
	 */
	public BoardView createBoard(String boardName, Location loc, BoardRotation rotation, String style, String pieceStyle) {
		BoardView view = new BoardView(boardName, loc, rotation, style, pieceStyle);
		registerView(view);
		if (ChessCraft.getWorldEdit() != null) {
			TerrainBackup.save(view);
		}
		view.save();
		view.paintAll();

		return view;
	}

	/**
	 * Mark a board as deferred loading - its world wasn't available so we'll record the board
	 * file name for later.
	 * 
	 * @param worldName
	 * @param f
	 */
	public void deferLoading(String worldName, File f) {
		if (!deferred.containsKey(worldName)) {
			deferred.put(worldName, new HashSet<File>());
		}
		deferred.get(worldName).add(f);
	}

	/**
	 * Load any deferred boards for the given world.
	 * 
	 * @param worldName
	 */
	public void loadDeferred(String worldName) {
		if (!deferred.containsKey(worldName)) {
			return;
		}
		for (File f : deferred.get(worldName)) {
			ChessCraft.getPersistenceHandler().loadBoard(f);
		}
		deferred.get(worldName).clear();
	}
}

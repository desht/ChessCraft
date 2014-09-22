package me.desht.chesscraft.chess;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessPersistence;
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
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import java.io.File;
import java.util.*;

public class BoardViewManager {

	private static BoardViewManager instance = null;

	private final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
	private final Map<String, Set<File>> deferred = new HashMap<String, Set<File>>();
	private PersistableLocation globalTeleportOutDest = null;

	private final List<Cuboid> flightRegions = new ArrayList<Cuboid>();

	private BoardViewManager() {
	}

	public static synchronized BoardViewManager getManager() {
		if (instance == null) {
			instance = new BoardViewManager();
		}
		return instance;
	}

	@SuppressWarnings("CloneDoesntCallSuperClone")
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

	private void unregisterBoardView(String name) {
		BoardView bv;
		try {
			bv = getBoardView(name);
			chessBoards.remove(name);
			Bukkit.getPluginManager().callEvent(new ChessBoardDeletedEvent(bv));
		} catch (ChessException e) {
			LogUtils.warning("removeBoardView: unknown board name " + name);
		}
	}

	public void deleteBoardView(String name, boolean permanent) {
		BoardView bv = getBoardView(name);
		if (permanent) {
			if (bv.getGame() != null) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.boardCantBeDeleted", name, bv.getGame().getName()));
			}
			bv.restoreTerrain();
			ChessCraft.getInstance().getPersistenceHandler().unpersist(bv);
		} else {
			if (bv.getGame() != null) {
				ChessGameManager.getManager().deleteGame(bv.getGame().getName(), false);
			}
			bv.getChessBoard().getChessSet().syncToPosition(null, bv.getChessBoard());
		}
		unregisterBoardView(name);
	}

	public boolean boardViewExists(String name) {
		return chessBoards.containsKey(name);
	}

	public BoardView getBoardView(String name) throws ChessException {
		if (!chessBoards.containsKey(name)) {
			if (chessBoards.size() > 0) {
				// try "fuzzy" search
				Set<String> strings = chessBoards.keySet();
				String keys[] = strings.toArray(new String[strings.size()]);
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
	 * @return the boardview that matches, or null if none
	 */
	public BoardView partOfChessBoard(Location loc) {
		return partOfChessBoard(loc, 0);
	}

	/**
	 * Check if a location is any part of any board including the frame & enclosure.
	 *
	 * @param loc	location to check
	 * @param fudge	fudge factor - check a larger area around the board
	 * @return the boardview that matches, or null if none
	 */
	public BoardView partOfChessBoard(Location loc, int fudge) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isPartOfBoard(loc, fudge)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Get the flight region for the given location, if any.
	 *
	 * @param loc the location to check
	 * @return the flight region for the location, or null if not in a flight region
	 */
	public Cuboid getFlightRegion(Location loc) {
		for (Cuboid c : flightRegions) {
			if (c.contains(loc)) {
				return c;
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
	 * @param player the player to check
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
	 * @param boardName name of the new board
	 * @param loc location of the A1 centre
	 * @param style the board style name
	 * @param pieceStyle the piece style name
	 * @return a fully initialised and painted board
	 */
	public BoardView createBoard(String boardName, Location loc, BoardRotation rotation, String style, String pieceStyle) {
		BoardView view = new BoardView(boardName, loc, rotation, style, pieceStyle);
		registerView(view);
		if (ChessCraft.getInstance().getWorldEdit() != null) {
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
	 * @param worldName name if the world
	 * @param f file the board is being loaded from
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
	 * @param worldName name of the world
	 */
	public void loadDeferred(String worldName) {
		if (!deferred.containsKey(worldName)) {
			return;
		}
		LogUtils.info("loading deferred boards for " + worldName);
		for (File f : deferred.get(worldName)) {
			ChessCraft.getInstance().getPersistenceHandler().loadBoard(f);
		}
		deferred.get(worldName).clear();
	}

	/**
	 * Called when a world is unloaded.  Put any boards in that world back on the deferred list.
	 *
	 * @param worldName name of the world
	 */
	public void unloadBoardsForWorld(String worldName) {
		for (BoardView bv : new ArrayList<BoardView>(listBoardViews())) {
			if (bv.getWorldName().equals(worldName)) {
				BoardViewManager.getManager().deleteBoardView(bv.getName(), false);
				File f = new File(bv.getSaveDirectory(), ChessPersistence.makeSafeFileName(bv.getName()) + ".yml");
				deferLoading(bv.getWorldName(), f);
				LogUtils.info("unloaded board '" + bv.getName() + "' (world has been unloaded)");
			}
		}
	}

	/**
	 * Get the boardview that the given chunk is in, if any.
	 *
	 * @param chunk the chunk to check
	 * @return the boardview containing the chunk, or null
	 */
	public BoardView getBoardViewForChunk(Chunk chunk) {
		for (BoardView bv : new ArrayList<BoardView>(listBoardViews())) {
			Cuboid c = bv.getOuterBounds();
			if (!c.getWorld().equals(chunk.getWorld())) {
				continue;
			}
			for (Chunk boardChunk : c.getChunks()) {
				if (boardChunk.getX() == chunk.getX() && boardChunk.getZ() == chunk.getZ()) {
					return bv;
				}
			}
		}
		return null;
	}

	/**
	 * Cache the regions in which flight is allowed.  We do this to avoid calculation in the
	 * code which is (frequently) called from the PlayerMoveEvent handler in the flight listener.
	 */
	public void recalculateFlightRegions() {
		int above = ChessCraft.getInstance().getConfig().getInt("flying.upper_limit");
		int outside = ChessCraft.getInstance().getConfig().getInt("flying.outer_limit");

		flightRegions.clear();

		for (BoardView bv : listBoardViews()) {
			Cuboid c = bv.getOuterBounds();
			MaterialData mat = bv.getChessBoard().getBoardStyle().getEnclosureMaterial();
			if (BlockType.canPassThrough(mat.getItemTypeId())) {
				c = c.expand(CuboidDirection.Up, Math.max(5, (c.getSizeY() * above) / 100));
				c = c.outset(CuboidDirection.Horizontal, Math.max(5, (c.getSizeX() * outside) / 100));
			}
			flightRegions.add(c);
		}
	}

    public BoardView findBoardForGame(ChessGame game) {
        for (BoardView bv : listBoardViews()) {
            if (bv.getGame() != null && bv.getGame().getName().equals(game.getName())) {
                return bv;
            }
        }
        return null;
    }
}

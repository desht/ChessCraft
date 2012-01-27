package me.desht.chesscraft.chess;

import me.desht.chesscraft.chess.ChessGame;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import chesspresso.Chess;
import chesspresso.position.PositionListener;
import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessPersistable;
import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.ControlPanel;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.SMSIntegration;
import me.desht.chesscraft.blocks.TerrainBackup;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessagePager;
import me.desht.chesscraft.util.PermissionUtils;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.ChessBoard;
import me.desht.chesscraft.enums.BoardLightingMethod;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.enums.Direction;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

public class BoardView implements PositionListener, ConfigurationSerializable, ChessPersistable {
	private static final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
	private String name;
	// null indicates board not used by any game yet
	private ChessGame game = null;
	private ControlPanel controlPanel;
	private ChessBoard chessBoard = null;
	// for lighting updates
	private byte lastLevel = -1;

	public BoardView(String bName, String bStyle) throws ChessException {
		this(bName, null, bStyle, null);
	}

	public BoardView(String bName, String bStyle, String pStyle) throws ChessException {
		this(bName, null, bStyle, pStyle);
	}

	public BoardView(String bName, Location where, String bStyle, String pStyle) throws ChessException {
		this(bName, where, null, bStyle, pStyle);
	}

	public BoardView(String bName, Location where,
			BoardOrientation dir, String bStyle, String pStyle) throws ChessException {
		this.name = bName;
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}
		chessBoard = new ChessBoard(ChessConfig.getBoardStyleDirectory(),
				ChessConfig.getPieceStyleDirectory(), bStyle, pStyle);

		setA1Center(where, dir);

	}
	
	public BoardView(ConfigurationSection conf) throws ChessException {
		@SuppressWarnings("unchecked")
		List<Object> origin = conf.getList("origin"); //$NON-NLS-1$
		String bStyle = conf.getString("boardStyle"); //$NON-NLS-1$
		String pStyle = conf.getString("pieceStyle"); //$NON-NLS-1$
		BoardOrientation dir = BoardOrientation.get(conf.getString("direction")); //$NON-NLS-1$
		
		Location where = ChessPersistence.thawLocation(origin);
		
		this.name = conf.getString("name");
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}
		chessBoard = new ChessBoard(ChessConfig.getBoardStyleDirectory(),
				ChessConfig.getPieceStyleDirectory(), bStyle, pStyle);
		setA1Center(where, dir);
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", name); //$NON-NLS-1$
		result.put("game", game == null ? "" : game.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		result.put("pieceStyle", chessBoard.getPieceStyleStr()); //$NON-NLS-1$
		result.put("boardStyle", chessBoard.getBoardStyleStr()); //$NON-NLS-1$
		result.put("origin", ChessPersistence.freezeLocation(chessBoard.getA1Center())); //$NON-NLS-1$
		result.put("direction", chessBoard.getRotation().name());
		return result;
	}

	public static BoardView deserialize(Map<String, Object> map) throws ChessException {
		Configuration conf = new MemoryConfiguration();
		
		for (Entry<String, Object> e : map.entrySet()) {
			if (!conf.contains(e.getKey())) {
				conf.set(e.getKey(), e.getValue());
			}
		}
		
		return new BoardView(conf);
	}
	
	
	private final void setA1Center(Location loc, BoardOrientation d) throws ChessException {
		// only allow the board center to be set once (?)
		if (loc != null && chessBoard.getA1Center() == null) {
			chessBoard.setA1Center(loc, d == null ? BoardOrientation.NORTH : d);
			validateIntersections();
			controlPanel = new ControlPanel(this);
		}
	}

	/**
	 * Ensure this board doesn't intersect any other boards
	 * 
	 * @throws ChessException
	 *             if an intersection would occur
	 */
	private void validateIntersections() throws ChessException {
		Cuboid bounds = chessBoard.getFullBoard();

		if (bounds.getUpperSW().getBlock().getLocation().getY() >= 127) {
			throw new ChessException(Messages.getString("BoardView.boardTooHigh")); //$NON-NLS-1$
		}
		for (BoardView bv : BoardView.listBoardViews()) {
			if (bv.getA1Square().getWorld() != bounds.getWorld()) {
				continue;
			}
			for (Block b : bounds.corners()) {
				if (bv.getOuterBounds().contains(b)) {
					throw new ChessException(Messages.getString("BoardView.boardWouldIntersect", bv.getName())); //$NON-NLS-1$
				}
			}
		}
	}

	public void save() {
		ChessCraft.getInstance().getSaveDatabase().savePersistable("board", this);
	}

	public void autoSave() {
		if (ChessConfig.getConfig().getBoolean("autosave", true)) { //$NON-NLS-1$
			save();
		}
	}

	public String getName() {
		return name;
	}

	public String getBoardStyle() {
		return chessBoard.getBoardStyleStr();
	}

	public String getPieceStyle() {
		return chessBoard.getPieceStyleStr();
	}

	public ChessGame getGame() {
		return game;
	}

	public Location getA1Square() {
		return chessBoard.getA1Corner();
	}

	public int getFrameWidth() {
		return chessBoard.getBoardStyle().getFrameWidth();
	}

	public int getSquareSize() {
		return chessBoard.getBoardStyle().getSquareSize();
	}

	public int getHeight() {
		return chessBoard.getBoardStyle().getHeight();
	}

	public Boolean getIsLit() {
		return chessBoard.getBoardStyle().getIsLit();
	}

	public ControlPanel getControlPanel() {
		return controlPanel;
	}

	public MaterialWithData getBlackSquareMat() {
		return chessBoard.getBoardStyle().getBlackSquareMaterial();
	}

	public MaterialWithData getWhiteSquareMat() {
		return chessBoard.getBoardStyle().getWhiteSquareMaterial();
	}

	public MaterialWithData getFrameMat() {
		return chessBoard.getBoardStyle().getFrameMaterial();
	}

	public MaterialWithData getControlPanelMat() {
		return chessBoard.getBoardStyle().getControlPanelMaterial();
	}

	public MaterialWithData getEnclosureMat() {
		return chessBoard.getBoardStyle().getEnclosureMaterial();
	}

	public BoardOrientation getDirection() {
		return chessBoard.getRotation();
	}

	public void paintAll() {
		chessBoard.paintAll();
		if (game != null) {
			chessBoard.paintChessPieces(game.getPosition());
		}
		if (controlPanel != null) {
			controlPanel.repaint();
		}
	}

	private void paintStoneAt(int sqi, int stone) {
		int col = Chess.sqiToCol(sqi);
		int row = Chess.sqiToRow(sqi);

		chessBoard.paintChessPiece(row, col, stone);
	}

	public void doLighting() {
		doLighting(false);
	}

	public void doLighting(boolean force) {
		if (getBounds() == null || !chessBoard.getBoardStyle().getIsLit()) {
			return;
		}

//        byte level = getOuterBounds().getUpperSW().getBlock().getLightLevel();
//        byte level = getBounds().shift(Direction.Up, height/2)
//                .getUpperSW().getBlock().getLightLevel();
//        Player jas = plugin.getServer().getPlayer("jascotty2");
//        if(jas!=null && getName().contains("ter")){
//            Cuboid c = getBounds().shift(Direction.Up, 2).
//				inset(Direction.Horizontal, getFrameWidth() + getSquareSize() * 3).
//				expand(Direction.Up, getHeight() / 2);
//			c.weSelect(jas);
//        }
		if (ChessBoard.getLightingMethod() == BoardLightingMethod.GLOWSTONE) {
			byte level = getBounds().shift(Direction.Up, 2).
					inset(Direction.Horizontal, getFrameWidth() + getSquareSize() * 3).
					expand(Direction.Up, getHeight() / 2).
					averageLightLevel();

			if (!force && isBright(level) == isBright(lastLevel) && lastLevel >= 0) {
				return;
			}
			lastLevel = level;
			chessBoard.lightBoard(!isBright(level));
		} else if (ChessBoard.getLightingMethod() == BoardLightingMethod.CRAFTBUKKIT) {
			chessBoard.lightBoard(true);
		}
	}

	private boolean isBright(byte level) {
		if (level < 12) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * get the bounds of the board itself
	 * 
	 * @return the bounds of the chess board - the innermost ring of the frame
	 */
	public Cuboid getBounds() {
		return chessBoard.getBoard();
	}

	/**
	 * get the region that encloses the outer extremities of this board
	 * @return
	 */
	public Cuboid getOuterBounds() {
		return chessBoard.getFullBoard();
	}

	@Override
	public void castlesChanged(int castles) {
		// TODO Auto-generated method stub
	}

	@Override
	public void halfMoveClockChanged(int halfMoveClock) {
		controlPanel.updateHalfMoveClock(halfMoveClock);
	}

	@Override
	public void plyNumberChanged(int plyNumber) {
		controlPanel.updatePlyCount(plyNumber);
	}

	@Override
	public void sqiEPChanged(int sqiEP) {
		// TODO Auto-generated method stub
	}

	@Override
	public void squareChanged(int sqi, int stone) {
		paintStoneAt(sqi, stone);
	}

	@Override
	public void toPlayChanged(int toPlay) {
		MaterialWithData mat;
		if (toPlay == Chess.WHITE) {
			mat = getWhiteSquareMat();
		} else if (toPlay == Chess.BLACK) {
			mat = getBlackSquareMat();
		} else if (toPlay == Chess.NOBODY) {
			mat = getControlPanelMat();
		} else {
			return; // should never get here
		}
		controlPanel.updateToMoveIndicator(mat);
	}

	public void setGame(ChessGame game) {
		if (this.game != null) {
			this.game.getPosition().removePositionListener(this);
		}
		this.game = game;
		paintAll();
		chessBoard.highlightSquares(-1, -1);
	}

	public boolean isOnBoard(Location loc, int minHeight, int maxHeight) {
		Cuboid bounds = getBounds();
		if (bounds == null) {
			return false;
		}
		bounds.shift(Direction.Up, minHeight);
		bounds.expand(Direction.Up, maxHeight - minHeight);
		return bounds.contains(loc);
	}

	/**
	 * check if this is a part of the board floor
	 * 
	 * @param loc
	 *            location to check
	 * @return true if the location is part of the board itself
	 */
	public boolean isOnBoard(Location loc) {
		return isOnBoard(loc, 0, 0);
	}

	/**
	 * check if this is a space within the board bounds, and above the board
	 * 
	 * @param loc
	 *            location to check
	 * @return true if the location is above the board <br>
	 *         AND within the board's height range
	 */
	public boolean isAboveBoard(Location loc) {
		return isOnBoard(loc, 1, chessBoard.getBoardStyle().getHeight());
	}

	/**
	 * check if this is somewhere within the board bounds
	 * 
	 * @param loc
	 *            location to check
	 * @return true if the location is *anywhere* within the board <br>
	 *         including frame & enclosure
	 */
	public boolean isPartOfBoard(Location loc) {
		Cuboid o = chessBoard.getFullBoard();
		return o != null && o.contains(loc);
	}

	public boolean isControlPanel(Location loc) {
		return controlPanel.getPanelBlocks().contains(loc);
	}

	public int getSquareAt(Location loc) {
		return chessBoard.getSquareAt(loc);
	}

	public void deleteTemporary() {
		delete(false, null);
	}

	public void deletePermanently(Player p) {
		delete(true, p);
		ChessCraft.getInstance().getSaveDatabase().unpersist(this);
	}
	
	public void delete(boolean deleteBlocks, Player p) {
		if (deleteBlocks) {
			restoreTerrain(p);
		}
		BoardView.removeBoardView(getName());
	}

	private void restoreTerrain(Player player) {
		chessBoard.clearAll();
		if (ChessCraft.getWorldEdit() != null) {
			// WorldEdit should take care of changes being pushed to client
			TerrainBackup.reload(player, this);
		} else {
			// ensure client sees the changes we made
			chessBoard.getFullBoard().sendClientChanges();
		}
	}

	public Location findSafeLocationOutside() {
		final int MAX_DIST = 100;

		// search north from the board's northeast corner
		Location dest0 = chessBoard.getFullBoard().getLowerNE().clone();
		Block b;
		int dist = 0;
		do {
			dest0.add(-1.0, 0.0, 0.0);
			dist++;
			b = dest0.getWorld().getHighestBlockAt(dest0);
		} while (b.getLocation().getBlockY() >= 126 && dist < MAX_DIST);

		if (dist >= MAX_DIST) {
			return b.getWorld().getSpawnLocation();
		} else {
			return b.getLocation();
		}
	}

	/*------------------------------------------------------------------------------_*/
	public static void addBoardView(String name, BoardView view) {
		if (ChessCraft.getSMS() != null) {
			SMSIntegration.boardCreated(view);
		}

		chessBoards.put(name, view);
	}

	public static void addBoardView(BoardView view) {
		addBoardView(view.getName(), view);
	}

	public static void removeBoardView(String name) {
		if (ChessCraft.getSMS() != null) {
			BoardView bv;
			try {
				bv = getBoardView(name);
				SMSIntegration.boardDeleted(bv);
			} catch (ChessException e) {
				ChessCraftLogger.warning("removeBoardView: unknown board name " + name);
			}
		}

		chessBoards.remove(name);
	}

	public static void removeAllBoardViews() {
		if (ChessCraft.getSMS() != null) {
			for (BoardView bv : listBoardViews()) {
				SMSIntegration.boardDeleted(bv);
			}
		}

		chessBoards.clear();
	}

	public static boolean boardViewExists(String name) {
		return chessBoards.containsKey(name);
	}

	public static BoardView getBoardView(String name) throws ChessException {
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

	public static List<BoardView> listBoardViews() {
		return listBoardViews(false);
	}

	public static List<BoardView> listBoardViews(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(chessBoards.keySet());
			List<BoardView> res = new ArrayList<BoardView>();
			for (String name : sorted) {
				res.add(chessBoards.get(name));
			}
			return res;
		} else {
			return new ArrayList<BoardView>(chessBoards.values());
		}

	}

	/**
	 * get a board that does not have a game running
	 * @return the first free board found
	 * @throws ChessException if no free board was found
	 */
	public static BoardView getFreeBoard() throws ChessException {
		for (BoardView bv : listBoardViews()) {
			if (bv.getGame() == null) {
				return bv;
			}
		}
		throw new ChessException(Messages.getString("BoardView.noFreeBoards")); //$NON-NLS-1$
	}

	/**
	 * match if loc is any part of the board including the frame & enclosure
	 * 
	 * @param loc
	 *            location to check
	 * @return the boardview that matches, or null if none
	 */
	public static BoardView partOfChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isPartOfBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * match if loc is above a board square but below the roof
	 * 
	 * @param loc
	 *            location to check
	 * @return the boardview that matches, or null if none
	 */
	public static BoardView aboveChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isAboveBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * match if loc is part of a board square
	 * 
	 * @param loc
	 *            location to check
	 * @return the boardview that matches, or null if none
	 */
	public static BoardView onChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isOnBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	public void wipe() {
		chessBoard.clearAll();
		chessBoard.getFullBoard().sendClientChanges();
	}

	void highlightSquares(int fromSquare, int toSquare) {
		chessBoard.highlightSquares(fromSquare, toSquare);
	}

	public void reloadStyle() throws ChessException {
		chessBoard.reloadStyles();
	}
	
	public static void teleportOut(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.teleport");
		
		BoardView bv = partOfChessBoard(player.getLocation());
		Location prev = ChessCraft.getLastPos(player);
		if (bv != null && (prev == null || partOfChessBoard(prev) == bv)) {
			// try to get the player out of this board safely
			Location loc = bv.findSafeLocationOutside();
			if (loc != null) {
				ChessCraft.teleportPlayer(player, loc);
			} else {
				ChessCraft.teleportPlayer(player, player.getWorld().getSpawnLocation());
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.goingToSpawn")); //$NON-NLS-1$
			}
		} else if (prev != null) {
			// go back to previous location
			ChessCraft.teleportPlayer(player, prev);
		} else {
			throw new ChessException(Messages.getString("ChessCommandExecutor.notOnChessboard")); //$NON-NLS-1$
		}
	}
	
	public void showBoardDetail(Player player) {
		String bullet = ChatColor.LIGHT_PURPLE + "* " + ChatColor.AQUA; //$NON-NLS-1$
		Cuboid bounds = getOuterBounds();
		String gameName = getGame() != null ? getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$

		MessagePager pager = MessagePager.getPager(player).clear();
		pager.add(Messages.getString("ChessCommandExecutor.boardDetail.board", getName())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardExtents", //$NON-NLS-1$
		                                                      ChessUtils.formatLoc(bounds.getLowerNE()),
		                                                      ChessUtils.formatLoc(bounds.getUpperSW())));
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.game", gameName)); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardOrientation", getDirection().toString())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardStyle", getBoardStyle())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.pieceStyle", getPieceStyle())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.squareSize", getSquareSize(),  //$NON-NLS-1$
		                                                      getWhiteSquareMat(), getBlackSquareMat()));
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.frameWidth", getFrameWidth(), //$NON-NLS-1$
		                                                      getFrameMat()));
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.enclosure", getEnclosureMat())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.height", getHeight())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.isLit", getIsLit())); //$NON-NLS-1$

		pager.showPage();
	}

	@Override
	public File getSaveDirectory() {
		return ChessConfig.getBoardPersistDirectory();
	}
}

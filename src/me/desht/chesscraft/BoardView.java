package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import chesspresso.Chess;
import chesspresso.position.PositionListener;

import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.ChessBoard;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.enums.Direction;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BoardView implements PositionListener {

	private static final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
	private ChessCraft plugin;
	private String name;
	// null indicates board not used by any game yet
	private Game game = null;
	private ControlPanel controlPanel;
	private ChessBoard chessBoard = null;
	// for lighting updates
	private byte lastLevel = -1;

	public BoardView(String bName, ChessCraft plugin, String bStyle) throws ChessException {
		this(bName, plugin, null, bStyle, null);
	}

	public BoardView(String bName, ChessCraft plugin, String bStyle, String pStyle) throws ChessException {
		this(bName, plugin, null, bStyle, pStyle);
	}

	public BoardView(String bName, ChessCraft plugin, Location where, String bStyle, String pStyle) throws ChessException {
		this.plugin = plugin;
		this.name = bName;
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}
		chessBoard = new ChessBoard(ChessConfig.getBoardStyleDirectory(),
				ChessConfig.getPieceStyleDirectory(), bStyle, pStyle);

		setA1Center(where);

	}

	public final void setA1Center(Location loc) throws ChessException {
		// only allow the board center to be set once (?)
		if (loc != null && chessBoard.getA1Center() == null) {

			chessBoard.setA1Center(loc, BoardOrientation.NORTH);

			validateIntersections();

			controlPanel = new ControlPanel(plugin, this);

			//paintAll();
		}
	}

	/**
	 * Ensure this board doesn't intersect any other boards
	 * 
	 * @throws ChessException
	 *             if an intersection would occur
	 */
	private void validateIntersections() throws ChessException {
		Cuboid bounds = getBounds();

		bounds.outset(Direction.Horizontal, getFrameWidth() - 1);
		bounds.expand(Direction.Up, getHeight() + 1);

		if (bounds.getUpperSW().getBlock().getLocation().getY() >= 127) {
			throw new ChessException(Messages.getString("BoardView.boardTooHigh")); //$NON-NLS-1$
		}
		for (BoardView bv : BoardView.listBoardViews()) {
			if (bv.getA1Square().getWorld() != bounds.getWorld()) {
				continue;
			}
			for (Location l : bounds.corners()) {
				if (bv.getOuterBounds().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
					throw new ChessException(Messages.getString("BoardView.boardWouldIntersect", bv.getName())); //$NON-NLS-1$
				}
			}
		}
	}

	public Map<String, Object> freeze() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", name); //$NON-NLS-1$
		result.put("game", game == null ? "" : game.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		result.put("pieceStyle", chessBoard.getPieceStyleStr()); //$NON-NLS-1$
		result.put("boardStyle", chessBoard.getBoardStyleStr()); //$NON-NLS-1$
		result.put("origin", ChessPersistence.makeBlockList(chessBoard.getA1Center())); //$NON-NLS-1$

		return result;
	}

	public void save() {
		plugin.persistence.saveBoard(this);
	}

	public void autoSave() {
		if (plugin.getConfiguration().getBoolean("autosave", true)) { //$NON-NLS-1$
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

	public Game getGame() {
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
//        if(jas!=null && getName().contains("cave")){
//            Cuboid c = getBounds().shift(Direction.Up, 2).
//                inset(Direction.Horizontal, frameWidth + squareSize * 3)
//                .expand(Direction.Up, height / 2);
//            com.sk89q.worldedit.bukkit.selections.CuboidSelection s =
//                    new com.sk89q.worldedit.bukkit.selections.CuboidSelection(
//                    c.getUpperSW().getWorld(), c.getUpperSW(), c.getLowerNE());
//            plugin.getWorldEdit().setSelection(jas, s);
//        }
		byte level = getBounds().shift(Direction.Up, 2).
				inset(Direction.Horizontal, getFrameWidth() + getSquareSize() * 3).
				expand(Direction.Up, getHeight() / 2).
				averageLightLevel();

		if (!force && isBright(level) == isBright(lastLevel) && lastLevel >= 0) {
			return;
		}
		lastLevel = level;
		chessBoard.lightBoard(!isBright(level));
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

	public void setGame(Game game) {
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
		bounds.inset(Direction.Horizontal, 1);
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

	public void delete() {
		delete(false, null);
	}

	public void delete(boolean deleteBlocks, Player p) {
		if (deleteBlocks) {
			restoreTerrain(p);
		}
		BoardView.removeBoardView(getName());
	}

	public void restoreTerrain(Player player) {
		chessBoard.clearAll();
		if (plugin.getWorldEdit() != null) {
			TerrainBackup.reload(plugin, player, this);
		}
	}

	public Location findSafeLocationOutside() {
		Location dest0 = getA1Square().clone();

		dest0.add(getFrameWidth() + 1, 0.0, getFrameWidth() + 1);
		Location dest1 = dest0.clone().add(0.0, 1.0, 0.0);

		while (dest0.getBlock().getTypeId() != 0 && dest1.getBlock().getTypeId() != 0) {
			dest0.add(0.0, 1.0, 0.0);
			dest1.add(0.0, 1.0, 0.0);
			if (dest1.getBlockY() > 127) {
				return null;
			}
		}
		return dest0;
	}

	/*------------------------------------------------------------------------------_*/
	public static void addBoardView(String name, BoardView view) {
		chessBoards.put(name, view);
	}

	public static void addBoardView(BoardView view) {
		chessBoards.put(view.getName(), view);
	}

	public static void removeBoardView(String name) {
		chessBoards.remove(name);
	}

	public static void removeAllBoardViews() {
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
	}

	void highlightSquares(int fromSquare, int toSquare) {
		chessBoard.highlightSquares(fromSquare, toSquare);
	}

	void reloadStyle() throws ChessException {
		chessBoard.reloadStyles();
	}
}

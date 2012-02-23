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
import chesspresso.move.Move;
import chesspresso.position.ImmutablePosition;
import chesspresso.position.PositionChangeListener;
import chesspresso.position.PositionListener;
import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessPersistable;
import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.ControlPanel;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.SMSIntegration;
import me.desht.chesscraft.blocks.TerrainBackup;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessagePager;
import me.desht.chesscraft.util.NoteAlert;
import me.desht.chesscraft.util.PermissionUtils;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.ChessBoard;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.enums.Direction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

public class BoardView implements PositionListener, PositionChangeListener, ConfigurationSerializable, ChessPersistable {
	private static final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();

	private final String name;
	private final ControlPanel controlPanel;
	private final ChessBoard chessBoard;

	private ChessGame game = null;			// null indicates board not currently used by any game

	public BoardView(String bName, Location origin, String bStyle, String pStyle) throws ChessException {
		this(bName, origin, null, bStyle, pStyle);
	}

	public BoardView(String bName, Location origin,
			BoardOrientation dir, String bStyle, String pStyle) throws ChessException {
		this.name = bName;
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}
		chessBoard = new ChessBoard(origin, dir, bStyle, pStyle);
		controlPanel = new ControlPanel(this);

	}

	public BoardView(ConfigurationSection conf) throws ChessException {
		@SuppressWarnings("unchecked")
		List<Object> origin = conf.getList("origin"); //$NON-NLS-1$
		String bStyle = conf.getString("boardStyle"); //$NON-NLS-1$
		String pStyle = conf.getString("pieceStyle"); //$NON-NLS-1$
		BoardOrientation dir = BoardOrientation.get(conf.getString("direction")); //$NON-NLS-1$
		Location where = ChessPersistence.thawLocation(origin);

		this.name = conf.getString("name"); //$NON-NLS-1$
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}
		chessBoard = new ChessBoard(where, dir, bStyle, pStyle);
		controlPanel = new ControlPanel(this);

		String designerName = conf.getString("designer");
		if (designerName != null && !designerName.isEmpty()) {
			setDesigner(new PieceDesigner(this, designerName));
		}
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", name); //$NON-NLS-1$
		result.put("game", game == null ? "" : game.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		result.put("pieceStyle", chessBoard.getPieceStyleName()); //$NON-NLS-1$
		result.put("boardStyle", chessBoard.getBoardStyleName()); //$NON-NLS-1$
		result.put("origin", ChessPersistence.freezeLocation(chessBoard.getA1Center())); //$NON-NLS-1$
		result.put("direction", chessBoard.getRotation().name()); //$NON-NLS-1$
		if (isDesigning()) {
			result.put("designer", chessBoard.getDesigner().getSetName()); //$NON-NLS-1$
		} else {
			result.put("designer", "");
		}
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

	public void save() {
		ChessCraft.getInstance().getPersistenceHandler().savePersistable("board", this);
	}

	public void autoSave() {
		if (ChessConfig.getConfig().getBoolean("autosave", true)) { //$NON-NLS-1$
			save();
		}
	}

	public String getName() {
		return name;
	}

	public ChessBoard getChessBoard() {
		return chessBoard;
	}

	public File getSaveDirectory() {
		return DirectoryStructure.getBoardPersistDirectory();
	}

	public String getBoardStyleName() {
		return chessBoard.getBoardStyleName();
	}

	public String getPieceStyleName() {
		return chessBoard.getPieceStyleName();
	}

	public ChessGame getGame() {
		return game;
	}

	public void setGame(ChessGame game) {
		this.game = game;
		if (game != null) {
			game.getPosition().addPositionListener(this);
			game.getPosition().addPositionChangeListener(this);
			Move lastMove = game.getPosition().getLastMove();
			if (lastMove != null) {
				chessBoard.highlightSquares(lastMove.getFromSqi(), lastMove.getToSqi());
			}
		} else {
			chessBoard.highlightSquares(Chess.NO_ROW, Chess.NO_COL);
			chessBoard.getBoard().shift(Direction.Up, 1).expand(Direction.Up, chessBoard.getBoardStyle().getHeight() - 1).clear(true);
		}
		chessBoard.getFullBoard().sendClientChanges();
		controlPanel.repaintSignButtons();
	}

	public Location getA1Square() {
		return chessBoard.getA1Corner();
	}

	public ControlPanel getControlPanel() {
		return controlPanel;
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

	public int getLightLevel() {
		return chessBoard.getBoardStyle().getLightLevel();
	}

	public MaterialWithData getBlackSquareMaterial() {
		return chessBoard.getBoardStyle().getBlackSquareMaterial();
	}

	public MaterialWithData getWhiteSquareMaterial() {
		return chessBoard.getBoardStyle().getWhiteSquareMaterial();
	}

	public MaterialWithData getFrameMaterial() {
		return chessBoard.getBoardStyle().getFrameMaterial();
	}

	public MaterialWithData getControlPanelMaterial() {
		return chessBoard.getBoardStyle().getControlPanelMaterial();
	}

	public MaterialWithData getEnclosureMaterial() {
		return chessBoard.getBoardStyle().getEnclosureMaterial();
	}

	public MaterialWithData getStrutsMaterial() {
		return chessBoard.getBoardStyle().getStrutsMaterial();
	}

	public BoardOrientation getDirection() {
		return chessBoard.getRotation();
	}

	public boolean isDesigning() {
		return chessBoard.isDesiging();
	}

	public void setDesigner(PieceDesigner designer) {
		chessBoard.setDesigner(designer);
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

	/**
	 * Get the bounds of the board itself
	 * 
	 * @return the bounds of the chess board
	 */
	public Cuboid getBounds() {
		return chessBoard.getBoard();
	}

	/**
	 * Get the region that encloses the outer extremities of this board
	 * 
	 * @return	The outermost bounding box - outer edge of the frame
	 */
	public Cuboid getOuterBounds() {
		return chessBoard.getFullBoard();
	}

	//--------------------- Chesspresso PositionListener impl. --------------------------------

	@Override
	public void castlesChanged(int castles) {
		// Ignored
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
		// Ignored
	}

	@Override
	public void squareChanged(int sqi, int stone) {
		paintStoneAt(sqi, stone);
	}

	@Override
	public void toPlayChanged(int toPlay) {
		MaterialWithData mat;
		if (toPlay == Chess.WHITE) {
			mat = getWhiteSquareMaterial();
		} else if (toPlay == Chess.BLACK) {
			mat = getBlackSquareMaterial();
		} else if (toPlay == Chess.NOBODY) {
			mat = getControlPanelMaterial();
		} else {
			return; // should never get here
		}
		controlPanel.updateToMoveIndicator(mat);
	}

	//-----------------Chesspresso PositionChangeListener impl. ----------------------

	@Override
	public void notifyPositionChanged(ImmutablePosition position) {
		// Ignored
	}

	@Override
	public void notifyMoveDone(ImmutablePosition position, short move) {
		int fromSqi = Move.getFromSqi(move);
		int toSqi = Move.getToSqi(move);

		if (Move.isCapturing(move) && ChessConfig.getConfig().getBoolean("effects.capture_explosion")) {
			Location loc = chessBoard.getSquare(Chess.sqiToRow(toSqi), Chess.sqiToCol(toSqi)).getCenter();
			chessBoard.getA1Center().getWorld().createExplosion(loc, 0.0f);
		}

		pieceRidingCheck(fromSqi, toSqi);

		chessBoard.highlightSquares(fromSqi, toSqi);
	}

	@Override
	public void notifyMoveUndone(ImmutablePosition position) {
		// Ignored
	}

	// -------------------------------------------------------------------------------

	/**
	 * Check for players standing on the piece that is being moved, and move them with the piece.
	 */
	private void pieceRidingCheck(int fromSqi, int toSqi) {
		Cuboid cFrom = chessBoard.getPieceRegion(Chess.sqiToRow(fromSqi), Chess.sqiToCol(fromSqi));
		for (Player p : chessBoard.getA1Corner().getWorld().getPlayers()) {
			Location loc = p.getLocation();
			if (cFrom.contains(loc) && loc.getY() > cFrom.getLowerY()) {
				Cuboid cTo = chessBoard.getPieceRegion(Chess.sqiToRow(toSqi), Chess.sqiToCol(toSqi));
				int xOff = cTo.getLowerX() - cFrom.getLowerX();
				int zOff = cTo.getLowerZ() - cFrom.getLowerZ();
				loc.add(xOff, 0, zOff);
				p.teleport(loc);
			}
		}
	}

	public boolean isOnBoard(Location loc, int minHeight, int maxHeight) {
		Cuboid bounds = getBounds();
		if (bounds == null) {
			return false;
		}
		bounds = bounds.shift(Direction.Up, minHeight).expand(Direction.Up, maxHeight - minHeight);
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
	 * Check if this is somewhere within the board bounds.
	 * 
	 * @param loc		location to check
	 * @param fudge		fudge factor - check within a slightly larger area
	 * @return true if the location is *anywhere* within the board <br>
	 *         including frame & enclosure
	 */
	public boolean isPartOfBoard(Location loc) {
		return isPartOfBoard(loc, 0);
	}
	public boolean isPartOfBoard(Location loc, int fudge) {
		Cuboid o = chessBoard.getFullBoard().outset(Direction.Both, fudge);
		return o != null && o.contains(loc);
	}

	public boolean isControlPanel(Location loc) {
		return controlPanel.getPanelBlocks().contains(loc);
	}

	/**
	 * Check if the given location is OK for designing on by the given player.
	 * 
	 * @param location
	 * @return
	 */
	public boolean canDesignHere(Player player, Location location) {
		if (!isDesigning() || !PermissionUtils.isAllowedTo(player, "chesscraft.designer"))
			return false;

		int sqi = chessBoard.getSquareAt(location);
		return Chess.sqiToCol(sqi) < 5 && Chess.sqiToCol(sqi) >= 0 && Chess.sqiToRow(sqi) < 2 && Chess.sqiToRow(sqi) >= 0;
	}

	public int getSquareAt(Location loc) {
		return chessBoard.getSquareAt(loc);
	}

	public void deleteTemporary() {
		deleteCommon(false, null);
	}

	public void deletePermanently(Player p) {
		deleteCommon(true, p);
		ChessCraft.getInstance().getPersistenceHandler().unpersist(this);
	}

	private void deleteCommon(boolean deleteBlocks, Player p) {
		if (deleteBlocks) {
			restoreTerrain(p);
		}
		BoardView.removeBoardView(getName());
	}

	private void restoreTerrain(Player player) {
		boolean restored = false;

		if (ChessCraft.getWorldEdit() != null) {
			// WorldEdit will take care of changes being pushed to client
			restored = TerrainBackup.reload(player, this);
		}

		if (!restored) {
			// we couldn't restore the original terrain - just set the board to air
			chessBoard.clearAll();
			chessBoard.getFullBoard().sendClientChanges();
		}
	}

	/**
	 * Find a safe location to teleport a player out of this board.  Go to the edge
	 * of the board closest to the player's current location.
	 * 
	 * @return	The location
	 */
	private Location findSafeLocationOutside() {
		Location dest0 = chessBoard.getFullBoard().getLowerNE();
		return dest0.getWorld().getHighestBlockAt(dest0).getLocation();
	}

	public void reloadStyle() throws ChessException {
		chessBoard.reloadStyles();
	}

	public void playMovedAlert(String playerName) {
		if (ChessConfig.getConfig().getBoolean("effects.move_alert")) {
			List<Note> notes = new ArrayList<Note>();
			notes.add(new Note(16));
			audibleAlert(playerName, notes, 5L);
		}
	}

	public void playCheckAlert(String playerName) {
		if (ChessConfig.getConfig().getBoolean("effects.check_alert")) {
			List<Note> notes = new ArrayList<Note>();
			notes.add(new Note(24));
			notes.add(new Note(16));
			notes.add(new Note(24));
			notes.add(new Note(16));
			audibleAlert(playerName, notes, 5L);
		}
	}

	public void audibleAlert(String playerName, List<Note> notes, long delay) {
		Player player = Bukkit.getPlayer(playerName);
		if (player != null) {
			// put a fake note block a couple of blocks below the player
			Location loc = player.getLocation().clone().add(0, -2, 0);
			NoteAlert a = new NoteAlert(player, loc, delay, notes);
			a.start();
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
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardStyle", getBoardStyleName())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.pieceStyle", getPieceStyleName())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.squareSize", getSquareSize(),  //$NON-NLS-1$
		                                      getWhiteSquareMaterial(), getBlackSquareMaterial()));
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.frameWidth", getFrameWidth(), //$NON-NLS-1$
		                                      getFrameMaterial()));
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.enclosure", getEnclosureMaterial())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.struts", getStrutsMaterial())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.height", getHeight())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.lightLevel", getLightLevel())); //$NON-NLS-1$

		if (chessBoard.getDesigner() != null) {
			pager.add(bullet + Messages.getString("ChessCommandExecutor.designMode", chessBoard.getDesigner().getSetName()));
		}

		pager.showPage();
	}

	/*----------------------static methods--------------------------------------*/

	public static String makeBoardName() {
		String res;
		int n = 1;
		do {
			res = "board-" + n++; //$NON-NLS-1$
		} while (BoardView.boardViewExists(res));

		return res;
	}

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
			if (bv.getGame() == null && !bv.isDesigning()) {
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
		return partOfChessBoard(loc, 0);
	}

	public static BoardView partOfChessBoard(Location loc, int fudge) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isPartOfBoard(loc, fudge)) {
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

	public static void teleportOut(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.teleport");

		BoardView bv = partOfChessBoard(player.getLocation());
		Location prev = ChessCraft.getInstance().getLastPos(player);
		if (bv != null && (prev == null || partOfChessBoard(prev) == bv)) {
			// try to get the player out of this board safely
			Location loc = bv.findSafeLocationOutside();
			if (loc != null) {
				ChessCraft.getInstance().teleportPlayer(player, loc);
			} else {
				ChessCraft.getInstance().teleportPlayer(player, player.getWorld().getSpawnLocation());
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.goingToSpawn")); //$NON-NLS-1$
			}
		} else if (prev != null) {
			// go back to previous location
			ChessCraft.getInstance().teleportPlayer(player, prev);
		} else {
			throw new ChessException(Messages.getString("ChessCommandExecutor.notOnChessboard")); //$NON-NLS-1$
		}
	}

}

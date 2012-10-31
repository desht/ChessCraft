package me.desht.chesscraft.chess;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessPersistable;
import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.blocks.TerrainBackup;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.controlpanel.ControlPanel;
import me.desht.chesscraft.controlpanel.TimeControlButton;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.event.ChessBoardCreatedEvent;
import me.desht.chesscraft.event.ChessBoardDeletedEvent;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.exceptions.ChessWorldNotLoadedException;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.Move;
import chesspresso.position.ImmutablePosition;
import chesspresso.position.PositionChangeListener;
import chesspresso.position.PositionListener;

public class BoardView implements PositionListener, PositionChangeListener, ConfigurationSerializable, ChessPersistable {
	private static final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();

	private static final Map<String, Set<File>> deferred = new HashMap<String, Set<File>>();

	private final String name;
	private final ControlPanel controlPanel;
	private final ChessBoard chessBoard;

	private double defaultStake;
	private boolean lockStake;

	private String defaultTcSpec;
	private boolean lockTcSpec;

	private ChessGame game = null;			// null indicates board not currently used by any game

	private final String worldName;
	private final String savedGameName;

	public BoardView(String boardName, Location origin, String bStyle, String pStyle) throws ChessException {
		this(boardName, origin, BoardRotation.getRotation(origin), bStyle, pStyle);
	}

	public BoardView(String boardName, Location origin,
			BoardRotation rotation, String bStyle, String pStyle) throws ChessException {
		this.name = boardName;
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}
		chessBoard = new ChessBoard(origin, rotation, bStyle, pStyle);
		controlPanel = new ControlPanel(this);
		defaultStake = -1.0;
		lockStake = false;
		defaultTcSpec = "";
		lockTcSpec = false;
		worldName = chessBoard.getA1Center().getWorld().getName();
		savedGameName = "";
	}

	@SuppressWarnings("unchecked")
	public BoardView(ConfigurationSection conf) {
		List<?> origin = conf.getList("origin"); //$NON-NLS-1$
		worldName = (String) origin.get(0);
		String bStyle = conf.getString("boardStyle"); //$NON-NLS-1$
		String pStyle = conf.getString("pieceStyle"); //$NON-NLS-1$
		BoardRotation dir = BoardRotation.get(conf.getString("direction")); //$NON-NLS-1$

		this.name = conf.getString("name"); //$NON-NLS-1$
		if (BoardView.boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
		}

		String designerName = conf.getString("designer.setName");
		String designerPlayerName = conf.getString("designer.playerName");
		if (designerName != null && !designerName.isEmpty()) {
			setDesigner(new PieceDesigner(this, designerName, designerPlayerName));
		}

		defaultStake = conf.getDouble("defaultStake", -1.0);
		lockStake = conf.getBoolean("lockStake", false);

		defaultTcSpec = conf.getString("defaultTcSpec", "");
		lockTcSpec = conf.getBoolean("lockTcSpec", false);

		savedGameName = conf.getString("game", "");

		Location where;
		try {
			where = ChessPersistence.thawLocation((List<Object>) origin);
		} catch (IllegalArgumentException e) {
			chessBoard = null;
			controlPanel = null;
			return;
		}
		chessBoard = new ChessBoard(where, dir, bStyle, pStyle);
		controlPanel = new ControlPanel(this);

		setDefaultTcSpec(defaultTcSpec);
	}

	/**
	 * Get the game name from the save file.  This is set even if the game hasn't actually been loaded yet.
	 *  
	 * @return
	 */
	public String getSavedGameName() {
		return savedGameName;
	}

	/**
	 * Get the world name from the save file.  This is set even if the world is not loaded.
	 * 
	 * @return
	 */
	public String getWorldName() {
		return worldName;
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
		Map<String, Object> d = new HashMap<String, Object>();
		if (isDesigning()) {
			d.put("setName", chessBoard.getDesigner().getSetName()); //$NON-NLS-1$
			d.put("playerName", chessBoard.getDesigner().getPlayerName()); //$NON-NLS-1$
		} else {
			d.put("setName", "");
			d.put("playerName", "");
		}
		result.put("designer", d);
		result.put("defaultStake", defaultStake);
		result.put("lockStake", lockStake);
		result.put("defaultTcSpec", defaultTcSpec);
		result.put("lockTcSpec", lockTcSpec);
		return result;
	}

	public static BoardView deserialize(Map<String, Object> map) throws ChessException, ChessWorldNotLoadedException {
		Configuration conf = new MemoryConfiguration();

		for (Entry<String, Object> e : map.entrySet()) {
			if (!conf.contains(e.getKey())) {
				conf.set(e.getKey(), e.getValue());
			}
		}

		return new BoardView(conf);
	}

	public void save() {
		ChessCraft.getPersistenceHandler().savePersistable("board", this);
	}

	public void autoSave() {
		if (ChessCraft.getInstance().getConfig().getBoolean("autosave", true)) { //$NON-NLS-1$
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

	public double getDefaultStake() {
		return defaultStake >= 0.0 ? defaultStake : ChessCraft.getInstance().getConfig().getDouble("stake.default", 0.0);
	}

	public void setDefaultStake(double defaultStake) {
		this.defaultStake = defaultStake;
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
			setDefaultTcSpec(getDefaultTcSpec());
		}
		chessBoard.getFullBoard().sendClientChanges();
		controlPanel.repaintClocks();
		controlPanel.repaintControls();
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

	public BoardRotation getRotation() {
		return chessBoard.getRotation();
	}

	public boolean isDesigning() {
		return chessBoard.isDesigning();
	}

	public void setDesigner(PieceDesigner designer) {
		chessBoard.setDesigner(designer);
	}

	public String getDefaultTcSpec() {
		return defaultTcSpec.isEmpty() ? ChessCraft.getInstance().getConfig().getString("time_control.default") : defaultTcSpec;
	}

	public void setDefaultTcSpec(String spec) {
		TimeControl tc = new TimeControl(spec);		// force validation of the spec
		defaultTcSpec = tc.getSpec();
		getControlPanel().getTcDefs().addCustomSpec(defaultTcSpec);
		getControlPanel().getSignButton(TimeControlButton.class).repaint();
	}

	public void setLockTcSpec(boolean lock) {
		lockTcSpec = lock;
	}

	public boolean getLockTcSpec() {
		return lockTcSpec;
	}

	public boolean getLockStake() {
		return lockStake;
	}

	public void setLockStake(boolean lockStake) {
		this.lockStake = lockStake;
	}

	public void paintAll() {
		chessBoard.paintAll();
		controlPanel.repaintAll();
		if (game != null) {
			chessBoard.paintChessPieces(game.getPosition());
		}
	}

	private void paintChessPiece(int sqi, int stone) {
		int col = Chess.sqiToCol(sqi);
		int row = Chess.sqiToRow(sqi);

		chessBoard.paintChessPiece(row, col, stone);
	}

	/**
	 * Get the bounds of the chess board itself (not including the frame).
	 * 
	 * @return the bounds of the chess board
	 */
	public Cuboid getBounds() {
		return chessBoard.getBoard();
	}

	/**
	 * Get the region that encloses the outer extremities of this board, which 
	 * includes the frame and enclosure.
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
		paintChessPiece(sqi, stone);
	}

	@Override
	public void toPlayChanged(int toPlay) {
		controlPanel.updateToMoveIndicator(toPlay);
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

		Location loc = chessBoard.getSquare(Chess.sqiToRow(toSqi), Chess.sqiToCol(toSqi)).getCenter();
		if (Move.isCapturing(move)) {
			ChessUtils.playEffect(loc, "piece_captured");
		} else {
			getGame().getPlayer(Chess.WHITE).playEffect("piece_moved");
			getGame().getPlayer(Chess.BLACK).playEffect("piece_moved");
		}

		pieceRidingCheck(fromSqi, toSqi);

		chessBoard.highlightSquares(fromSqi, toSqi);
	}

	@Override
	public void notifyMoveUndone(ImmutablePosition position) {
		// Repaint the last-move indicators
		Move m = getGame().getChesspressoGame().getLastMove();
		if (m != null) {
			getChessBoard().highlightSquares(m.getFromSqi(), m.getToSqi());
		} else {
			getChessBoard().highlightSquares(Chess.NO_SQUARE, Chess.NO_SQUARE);
		}
		
		getControlPanel().updatePlyCount(getGame().getChesspressoGame().getCurrentPly());
	}

	// -------------------------------------------------------------------------------

	/**
	 * Check for players standing on the piece that is being moved, and move them with the piece.
	 */
	private void pieceRidingCheck(int fromSqi, int toSqi) {
		if (!ChessCraft.getInstance().getConfig().getBoolean("piece_riding")) {
			return;
		}
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
		Cuboid bounds = getBounds().shift(Direction.Up, minHeight).expand(Direction.Up, maxHeight - minHeight);
		return bounds.contains(loc);
	}

	/**
	 * Check if the location is a part of the board itself.
	 * 
	 * @param loc	location to check
	 * @return true if the location is part of the board itself
	 */
	public boolean isOnBoard(Location loc) {
		return isOnBoard(loc, 0, 0);
	}

	/**
	 * Check if the location is above the board but below the enclosure roof.
	 * 
	 * @param loc	location to check
	 * @return true if the location is above the board AND within the board's height range
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
	public boolean isPartOfBoard(Location loc, int fudge) {
		Cuboid o = chessBoard.getFullBoard().outset(Direction.Both, fudge);
		return o != null && o.contains(loc);
	}
	public boolean isPartOfBoard(Location loc) {
		return isPartOfBoard(loc, 0);
	}

	public boolean isControlPanel(Location loc) {
		// outsetting the cuboid allows the signs on the panel to be targeted too
		return controlPanel.getPanelBlocks().outset(Direction.Horizontal, 1).contains(loc);
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

	/**
	 * Get the Chesspresso square index (sqi) of the given Location.
	 * 
	 * @param loc	The Location to check	
	 * @return		The sqi of the Location or Chess.NO_SQUARE if not on the board
	 */
	public int getSquareAt(Location loc) {
		return chessBoard.getSquareAt(loc);
	}

	/**
	 * Remove the board from the global list of boards, but don't remove its data
	 * from disk or alter any terrain.  Called when the plugin is being disabled or persisted
	 * data is being reloaded - don't call otherwise, or else board materials may be 
	 * vulnerable to mining!
	 */
	public void deleteTemporary() {
		deleteCommon();
	}

	/**
	 * Permanently delete a board, purging its data from disk and restoring the terrain behind it.
	 */
	public void deletePermanently() {
		if (getGame() != null) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.boardCantBeDeleted", getName(), getGame().getName()));
		}
		deleteCommon();
		restoreTerrain();
		ChessCraft.getPersistenceHandler().unpersist(this);
	}

	private void deleteCommon() {
		BoardView.removeBoardView(getName());
	}

	private void restoreTerrain() {
		boolean restored = false;

		// signs can get dropped otherwise
		getControlPanel().removeSigns();
		
		if (ChessCraft.getWorldEdit() != null) {
			// WorldEdit will take care of changes being pushed to client
			restored = TerrainBackup.reload(this);
		}

		if (!restored) {
			// we couldn't restore the original terrain - just set the board to air
			chessBoard.clearAll();
		}
		
		chessBoard.getFullBoard().outset(Direction.Horizontal, 16).initLighting();
		chessBoard.getFullBoard().outset(Direction.Horizontal, 16).sendClientChanges();
	}

	/**
	 * Find a safe location to teleport a player out of this board.
	 * 
	 * @return	The location
	 */
	private Location findSafeLocationOutside() {
		Location dest = chessBoard.getFullBoard().getLowerNE();
		dest.add(-1, 0, 1);
		return dest.getWorld().getHighestBlockAt(dest).getLocation();
	}

	public void reloadStyle() throws ChessException {
		chessBoard.reloadStyles();
	}

	public void showBoardDetail(CommandSender sender) {
		String bullet = ChatColor.LIGHT_PURPLE + "* " + ChatColor.AQUA; //$NON-NLS-1$
		Cuboid bounds = getOuterBounds();
		String gameName = getGame() != null ? getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$

		MessagePager pager = MessagePager.getPager(sender).clear();
		pager.add(Messages.getString("ChessCommandExecutor.boardDetail.board", getName())); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardExtents", //$NON-NLS-1$
		                                      MiscUtil.formatLocation(bounds.getLowerNE()),
		                                      MiscUtil.formatLocation(bounds.getUpperSW())));
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.game", gameName)); //$NON-NLS-1$
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardOrientation", getRotation().toString())); //$NON-NLS-1$
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
		String lockStakeStr = getLockStake() ? Messages.getString("ChessCommandExecutor.boardDetail.locked") : "";
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.defaultStake", ChessUtils.formatStakeStr(getDefaultStake()), lockStakeStr)); //$NON-NLS-1$
		String lockTcStr = getLockTcSpec() ? Messages.getString("ChessCommandExecutor.boardDetail.locked") : "";
		pager.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.defaultTimeControl", getDefaultTcSpec(), lockTcStr)); //$NON-NLS-1$

		if (chessBoard.getDesigner() != null) {
			pager.add(bullet + Messages.getString("ChessCommandExecutor.designMode", chessBoard.getDesigner().getSetName()));
		}
		String setComment = getChessBoard().getChessSet().getComment();
		if (setComment != null && !setComment.isEmpty()) {
			for (String s : setComment.split("\n")) {
				pager.add(ChatColor.YELLOW + s);
			}
		}

		pager.showPage();
	}

	public void summonPlayer(Player player) {
		if (isPartOfBoard(player.getLocation())) {
			return; // already there
		}
		Location loc = getControlPanel().getTeleportLocation();
		ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, loc);
		if (game != null)
			ChessGame.setCurrentGame(player.getName(), game);
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
		chessBoards.put(name, view);
		
		Bukkit.getPluginManager().callEvent(new ChessBoardCreatedEvent(view));

	}

	public static void registerView(BoardView view) {
		addBoardView(view.getName(), view);
	}

	public static void removeBoardView(String name) {
		BoardView bv;
		try {
			bv = getBoardView(name);
			chessBoards.remove(name);
			Bukkit.getPluginManager().callEvent(new ChessBoardDeletedEvent(bv));
		} catch (ChessException e) {
			LogUtils.warning("removeBoardView: unknown board name " + name);
		}

	}

	public static void removeAllBoardViews() {
		for (BoardView bv : listBoardViews()) {
			Bukkit.getPluginManager().callEvent(new ChessBoardDeletedEvent(bv));
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
	 * Get a board that does not have a game running.
	 * 
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
	 * Check if location is any part of any board including the frame & enclosure.
	 * 
	 * @param loc	location to check
	 * @param fudge	fudge factor - check a larger area around the board
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
	 * Check if location is above a board square but below the roof
	 * 
	 * @param loc  location to check
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
	 * Check if location is part of a board square
	 * 
	 * @param loc	location to check
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

	/**
	 * Teleport the player in a sensible manner, depending on where they are now.
	 * 
	 * @param player
	 * @throws ChessException
	 */
	public static void teleportOut(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.teleport");

		BoardView bv = partOfChessBoard(player.getLocation());
		Location prev = ChessCraft.getInstance().getPlayerTracker().getLastPos(player);
		if (bv != null && (prev == null || partOfChessBoard(prev) == bv)) {
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
	public static BoardView createBoard(String boardName, Location loc, BoardRotation rotation, String style, String pieceStyle) {
		BoardView view = new BoardView(boardName, loc, rotation, style, pieceStyle);
		BoardView.registerView(view);
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
	public static void deferLoading(String worldName, File f) {
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
	public static void loadDeferred(String worldName) {
		if (!deferred.containsKey(worldName)) {
			return;
		}
		for (File f : deferred.get(worldName)) {
			ChessCraft.getPersistenceHandler().loadBoard(f);
		}
		deferred.get(worldName).clear();
	}
}

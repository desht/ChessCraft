package me.desht.chesscraft.chess;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessPersistable;
import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.dhutils.block.CraftMassBlockUpdate;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.controlpanel.ControlPanel;
import me.desht.chesscraft.controlpanel.TimeControlButton;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.exceptions.ChessWorldNotLoadedException;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.TerrainBackup;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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

	private final String name;
	private final ControlPanel controlPanel;
	private final ChessBoard chessBoard;
	private final String worldName;
	private final String savedGameName;
	
	private double defaultStake;
	private boolean lockStake;
	private String defaultTcSpec;
	private boolean lockTcSpec;
	private ChessGame game = null;			// null indicates board not currently used by any game
	private PersistableLocation teleportOutDest;

	public BoardView(String boardName, Location origin, String bStyle, String pStyle) throws ChessException {
		this(boardName, origin, BoardRotation.getRotation(origin), bStyle, pStyle);
	}

	public BoardView(String boardName, Location origin,
			BoardRotation rotation, String bStyle, String pStyle) throws ChessException {
		this.name = boardName;
		if (BoardViewManager.getManager().boardViewExists(name)) {
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
		teleportOutDest = null;
	}

	@SuppressWarnings("unchecked")
	public BoardView(ConfigurationSection conf) {
		List<?> origin = conf.getList("origin"); //$NON-NLS-1$
		worldName = (String) origin.get(0);
		String bStyle = conf.getString("boardStyle"); //$NON-NLS-1$
		String pStyle = conf.getString("pieceStyle"); //$NON-NLS-1$
		BoardRotation dir = BoardRotation.getRotation(conf.getString("direction")); //$NON-NLS-1$

		this.name = conf.getString("name"); //$NON-NLS-1$
		if (BoardViewManager.getManager().boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists")); //$NON-NLS-1$
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

		Map<String, String> m = (Map<String,String>)conf.get("designer");
		if (m != null) {
			String designerName = m.get("setName");
			String designerPlayerName = m.get("playerName");
			if (designerName != null && !designerName.isEmpty()) {
				setDesigner(new PieceDesigner(this, designerName, designerPlayerName));
			}
		}
		
		setDefaultTcSpec(defaultTcSpec);
		
		teleportOutDest = conf.contains("teleportOutDest") ? (PersistableLocation) conf.get("teleportOutDest") : null;
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
		if (teleportOutDest != null) {
			result.put("teleportOutDest", teleportOutDest);
		}
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
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(chessBoard.getBoard().getWorld());
		
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
			chessBoard.getBoard().shift(CuboidDirection.Up, 1).expand(CuboidDirection.Up, chessBoard.getBoardStyle().getHeight() - 1).fill(0, (byte)0, mbu);
			setDefaultTcSpec(getDefaultTcSpec());
		}
		mbu.notifyClients();
		controlPanel.repaintClocks();
		controlPanel.repaintControls();
	}

	public void setTeleportDestination(Location loc) {
		teleportOutDest = loc == null ? null : new PersistableLocation(loc);
	}
	
	public Location getTeleportDestination() {
		return teleportOutDest == null ? null : teleportOutDest.getLocation();
	}
	
	public boolean hasTeleportDestination() {
		return teleportOutDest != null;
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
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(getChessBoard().getBoard().getWorld());

		chessBoard.paintAll(mbu);
		controlPanel.repaintAll(mbu);
		if (game != null) {
			chessBoard.paintChessPieces(game.getPosition());
		}
		
		mbu.notifyClients();
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
			ChessCraft.getInstance().getFX().playEffect(loc, "piece_captured");
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

		getControlPanel().repaintAll(null);
	}

	// -------------------------------------------------------------------------------

	/**
	 * Check for players standing on the piece that is being moved, and move them with the piece.
	 */
	private void pieceRidingCheck(int fromSqi, int toSqi) {
		if (!ChessCraft.getInstance().getConfig().getBoolean("effects.piece_riding")) {
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
		Cuboid bounds = getBounds().shift(CuboidDirection.Up, minHeight).expand(CuboidDirection.Up, maxHeight - minHeight);
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
		Cuboid o = chessBoard.getFullBoard();
		if (fudge != 0) o = o.outset(CuboidDirection.Both, fudge);
		return o.contains(loc);
	}
	public boolean isPartOfBoard(Location loc) {
		return isPartOfBoard(loc, 0);
	}

	public boolean isControlPanel(Location loc) {
		// outsetting the cuboid allows the signs on the panel to be targeted too
		return controlPanel.getPanelBlocks().outset(CuboidDirection.Horizontal, 1).contains(loc);
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
		BoardViewManager.getManager().unregisterBoardView(getName());
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
		
//		chessBoard.getFullBoard().outset(CuboidDirection.Horizontal, 16).initLighting();
//		chessBoard.getFullBoard().outset(CuboidDirection.Horizontal, 16).sendClientChanges();
	}

	/**
	 * Find a safe location to teleport a player out of this board.
	 * 
	 * @return	The location
	 */
	public Location findSafeLocationOutside() {
		Location dest = chessBoard.getFullBoard().getLowerNE();
		dest.add(-1, 0, 1);
		return dest.getWorld().getHighestBlockAt(dest).getLocation();
	}

	public void reloadStyle() throws ChessException {
		chessBoard.reloadStyles();
	}

	public List<String> getBoardDetail() {
		List<String> res = new ArrayList<String>();
		
		String bullet = MessagePager.BULLET + ChatColor.YELLOW;
		Cuboid bounds = getOuterBounds();
		String gameName = getGame() != null ? getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$

		res.add(Messages.getString("ChessCommandExecutor.boardDetail.board", getName())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardExtents", //$NON-NLS-1$
		                                      MiscUtil.formatLocation(bounds.getLowerNE()),
		                                      MiscUtil.formatLocation(bounds.getUpperSW())));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.game", gameName)); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardOrientation", getRotation().toString())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardStyle", getBoardStyleName())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.pieceStyle", getPieceStyleName())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.squareSize", getSquareSize(),  //$NON-NLS-1$
		                                      getWhiteSquareMaterial(), getBlackSquareMaterial()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.frameWidth", getFrameWidth(), //$NON-NLS-1$
		                                      getFrameMaterial()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.enclosure", getEnclosureMaterial())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.struts", getStrutsMaterial())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.height", getHeight())); //$NON-NLS-1$
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.lightLevel", getLightLevel())); //$NON-NLS-1$
		String lockStakeStr = getLockStake() ? Messages.getString("ChessCommandExecutor.boardDetail.locked") : "";
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.defaultStake", ChessUtils.formatStakeStr(getDefaultStake()), lockStakeStr)); //$NON-NLS-1$
		String lockTcStr = getLockTcSpec() ? Messages.getString("ChessCommandExecutor.boardDetail.locked") : "";
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.defaultTimeControl", getDefaultTcSpec(), lockTcStr)); //$NON-NLS-1$
		String dest = hasTeleportDestination() ? MiscUtil.formatLocation(getTeleportDestination()) : "-";
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.teleportDest", dest));
		
		if (chessBoard.getDesigner() != null) {
			res.add(bullet + Messages.getString("ChessCommandExecutor.designMode", chessBoard.getDesigner().getSetName()));
		}
		String setComment = getChessBoard().getChessSet().getComment();
		if (setComment != null && !setComment.isEmpty()) {
			for (String s : setComment.split("\n")) {
				res.add(ChatColor.YELLOW + s);
			}
		}

		return res;
	}

	public void summonPlayer(Player player) {
		if (isPartOfBoard(player.getLocation())) {
			return; // already there
		}
		Location loc = getControlPanel().getTeleportLocation();
		ChessCraft.getInstance().getPlayerTracker().teleportPlayer(player, loc);
		if (game != null)
			ChessGameManager.getManager().setCurrentGame(player.getName(), game);
	}
	
}

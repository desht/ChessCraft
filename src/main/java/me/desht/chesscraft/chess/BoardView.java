package me.desht.chesscraft.chess;

import chesspresso.Chess;
import chesspresso.move.Move;
import chesspresso.position.ImmutablePosition;
import chesspresso.position.PositionChangeListener;
import chesspresso.position.PositionListener;
import me.desht.chesscraft.*;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.controlpanel.*;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.EconomyUtil;
import me.desht.chesscraft.util.TerrainBackup;
import me.desht.dhutils.*;
import me.desht.dhutils.block.CraftMassBlockUpdate;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class BoardView implements PositionListener, PositionChangeListener, GameListener, ConfigurationSerializable, ChessPersistable, ConfigurationListener {

	private static final String DEFAULT_STAKE = "defaultstake";
	private static final String LOCK_STAKE = "lockstake";
	private static final String DEFAULT_TC = "defaulttc";
	private static final String LOCK_TC = "locktc";
	private static final String OVERRIDE_PIECE_STYLE = "overridepiecestyle";
	private static final String BOARD_STYLE = "boardstyle";

	// map attribute names as typed in-game to the corresponding key in the save file
	// backward compatibility is such fun!
	private static final Map<String, String> attr2save = new HashMap<String, String>();
	private static final Map<String, String> save2attr = new HashMap<String, String>();
	static {
		attr2save.put(DEFAULT_STAKE, "defaultStake");
		attr2save.put(LOCK_STAKE, "lockStake");
		attr2save.put(DEFAULT_TC, "defaultTcSpec");
		attr2save.put(LOCK_TC, "lockTcSpec");
		attr2save.put(OVERRIDE_PIECE_STYLE, "pieceStyle");
		attr2save.put(BOARD_STYLE, "boardStyle");
		for (String k : attr2save.keySet()) {
			save2attr.put(attr2save.get(k), k);
		}
	}

	private final String name;
	private final ControlPanel controlPanel;
	private final ChessBoard chessBoard;
	private final String worldName;
	private final String savedGameName;

	private ChessGame game = null;			// null indicates board not currently used by any game
	private PersistableLocation teleportOutDest;
	private final AttributeCollection attributes;

	public BoardView(String boardName, Location origin, String bStyle, String pStyle) throws ChessException {
		this(boardName, origin, BoardRotation.getRotation(origin), bStyle, pStyle);
	}

	public BoardView(String boardName, Location origin, BoardRotation rotation, String bStyle, String pStyle) throws ChessException {
		this.name = boardName;
		if (BoardViewManager.getManager().boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists"));
		}
		attributes = new AttributeCollection(this);
		registerAttributes();
		attributes.set(BOARD_STYLE, bStyle);
		attributes.set(OVERRIDE_PIECE_STYLE, pStyle);
		chessBoard = new ChessBoard(origin, rotation, bStyle, pStyle);
		controlPanel = new ControlPanel(this);
		worldName = chessBoard.getA1Center().getWorld().getName();
		savedGameName = "";
		teleportOutDest = null;
	}

	private void registerAttributes() {
		attributes.registerAttribute(DEFAULT_STAKE, -1.0, "Default stake for games on this board");
		attributes.registerAttribute(LOCK_STAKE, false, "Disallow changing of stake by players");
		attributes.registerAttribute(DEFAULT_TC, "", "Default time control for games on this board");
		attributes.registerAttribute(LOCK_TC, false, "Disallow changing of time control by players");
		attributes.registerAttribute(OVERRIDE_PIECE_STYLE, "", "Overridden piece style for this board");
		attributes.registerAttribute(BOARD_STYLE, "", "Board style for this board");
	}

	@SuppressWarnings("unchecked")
	public BoardView(ConfigurationSection conf) {
		this.name = conf.getString("name");
		if (BoardViewManager.getManager().boardViewExists(name)) {
			throw new ChessException(Messages.getString("BoardView.boardExists"));
		}

		savedGameName = conf.getString("game", "");

		attributes = new AttributeCollection(this);
		registerAttributes();
		for (Entry<String, String> e : save2attr.entrySet()) {
            attributes.set(e.getValue(), conf.getString(e.getKey()));
		}

		List<?> origin = conf.getList("origin");
		worldName = (String) origin.get(0);
		Location where = ChessPersistence.thawLocation(origin);
		if (where == null) {
			// world not available
			chessBoard = null;
			controlPanel = null;
			return;
		}
		BoardRotation dir = BoardRotation.getRotation(conf.getString("direction"));
		chessBoard = new ChessBoard(where, dir, (String)attributes.get(BOARD_STYLE), (String)attributes.get(OVERRIDE_PIECE_STYLE));
		controlPanel = new ControlPanel(this);

		Map<String, String> m = (Map<String,String>)conf.get("designer");
		if (m != null) {
			String designerName = m.get("setName");
			String designerId = m.get("playerName");
			if (designerName != null && !designerName.isEmpty()) {
				setDesigner(new PieceDesigner(this, designerName, UUID.fromString(designerId)));
			}
		}

		teleportOutDest = conf.contains("teleportOutDest") ? (PersistableLocation) conf.get("teleportOutDest") : null;
	}

	public boolean isWorldAvailable() {
		return chessBoard != null;
	}

	/**
	 * Get the game name from the save file.  This is set even if the game hasn't actually been loaded yet.
	 *
	 * @return the saved game name
	 */
	public String getSavedGameName() {
		return savedGameName;
	}

	/**
	 * Get the world name from the save file.  This is set even if the world is not loaded.
	 *
	 * @return the world name
	 */
	public String getWorldName() {
		return worldName;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", name);
		result.put("game", game == null ? "" : game.getName());
		result.put("origin", ChessPersistence.freezeLocation(chessBoard.getA1Center()));
		result.put("direction", chessBoard.getRotation().name());
		Map<String, Object> d = new HashMap<String, Object>();
		if (isDesigning()) {
			d.put("setName", chessBoard.getDesigner().getSetName());
			d.put("playerName", chessBoard.getDesigner().getPlayerId().toString());
		} else {
			d.put("setName", "");
			d.put("playerName", "");
		}
		result.put("designer", d);
		for (String k : attributes.listAttributeKeys(false)) {
			result.put(attr2save.get(k), attributes.get(k));
		}
		if (teleportOutDest != null) {
			result.put("teleportOutDest", teleportOutDest);
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

    public void tick() {
        if (game != null) {
            ChessPlayer player = game.getPlayerToMove();
            if (player != null) {
                updateClock(player.getColour());
            }
            game.tick();
        }
    }

    private void updateClock(int colour) {
        if (game.getState() != GameState.RUNNING) {
            return;
        }
        TwoPlayerClock clock = game.getClock();
        clock.tick();
        getControlPanel().updateClock(colour, clock.getClockString(colour));

        ChessPlayer cp = game.getPlayer(colour);
        if (clock.getRemainingTime(colour) <= 0) {
            try {
                ChessPlayer other = game.getPlayer(Chess.otherPlayer(colour));
                game.winByDefault(other.getColour());
            } catch (ChessException e) {
                LogUtils.severe("unexpected exception: " + e.getMessage(), e);
            }
        } else {
            cp.timeControlCheck();
        }
    }

    public void save() {
		ChessCraft.getInstance().getPersistenceHandler().savePersistable("board", this);
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
		double defaultStake = (Double) attributes.get(DEFAULT_STAKE);
		return defaultStake >= 0.0 ? defaultStake : ChessCraft.getInstance().getConfig().getDouble("stake.default", 0.0);
	}

	public void setGame(ChessGame game) {
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(ChessCraft.getInstance(), chessBoard.getBoard().getWorld());

		this.game = game;
		if (game != null) {
			game.getPosition().addPositionListener(this);
			game.getPosition().addPositionChangeListener(this);
            game.addGameListener(this);
			Move lastMove = game.getPosition().getLastMove();
			if (lastMove != null) {
				chessBoard.highlightSquares(lastMove.getFromSqi(), lastMove.getToSqi());
			}
			chessBoard.getChessSet().syncToPosition(game.getPosition(), chessBoard);
            getControlPanel().getTcDefs().addCustomSpec(game.getTimeControl().getSpec());
		} else {
			chessBoard.highlightSquares(Chess.NO_ROW, Chess.NO_COL);
			chessBoard.getBoard()
                    .shift(CuboidDirection.Up, 1)
                    .expand(CuboidDirection.Up, chessBoard.getBoardStyle().getHeight() - 1)
                    .fill(new MaterialData(Material.AIR), mbu);
			chessBoard.getChessSet().syncToPosition(null, chessBoard);
			attributes.set(DEFAULT_TC, getDefaultTcSpec());
			chessBoard.getChessSet().syncToPosition(null, chessBoard);
            System.out.println("board cleared: " + this.getName());
        }
		mbu.notifyClients();
		controlPanel.repaintClocks();
		controlPanel.repaintControls();
        save();
	}

	public void setTeleportDestination(Location loc) {
		teleportOutDest = loc == null ? null : new PersistableLocation(loc);
        save();
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

	public MaterialData getBlackSquareMaterial() {
		return chessBoard.getBoardStyle().getBlackSquareMaterial();
	}

	public MaterialData getWhiteSquareMaterial() {
		return chessBoard.getBoardStyle().getWhiteSquareMaterial();
	}

	public MaterialData getFrameMaterial() {
		return chessBoard.getBoardStyle().getFrameMaterial();
	}

	public MaterialData getControlPanelMaterial() {
		return chessBoard.getBoardStyle().getControlPanelMaterial();
	}

	public MaterialData getEnclosureMaterial() {
		return chessBoard.getBoardStyle().getEnclosureMaterial();
	}

	public MaterialData getStrutsMaterial() {
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
        save();
	}

	public String getDefaultTcSpec() {
		String defaultTcSpec = (String) attributes.get(DEFAULT_TC);
		return defaultTcSpec.isEmpty() ? ChessCraft.getInstance().getConfig().getString("time_control.default") : defaultTcSpec;
	}

	public boolean getLockTcSpec() {
		return (Boolean) attributes.get(LOCK_TC);
	}

	public boolean getLockStake() {
		return (Boolean) attributes.get(LOCK_STAKE);
	}

	public void paintAll() {
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(ChessCraft.getInstance(), getChessBoard().getBoard().getWorld());

		chessBoard.paintAll(mbu);
		controlPanel.repaintAll(mbu);
		if (game != null) {
			chessBoard.paintChessPieces(game.getPosition());
		}

		mbu.notifyClients();
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
		int col = Chess.sqiToCol(sqi);
		int row = Chess.sqiToRow(sqi);
		chessBoard.paintChessPiece(row, col, stone);
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
			// TODO: handle differently for entity chess sets?
			ChessCraft.getInstance().getFX().playEffect(loc, "piece_captured");
		} else {
			getGame().getPlayer(Chess.WHITE).playEffect("piece_moved");
			getGame().getPlayer(Chess.BLACK).playEffect("piece_moved");
		}

		if (Move.isEPMove(move)) {
			// en passant capture - the piece to actually capture isn't in the square we're moving to
			int captureSqi = toSqi + (getGame().getPosition().getToPlay() == Chess.WHITE ? 8 : -8);
			chessBoard.moveChessPiece(fromSqi, toSqi, captureSqi, Chess.NO_STONE);
		} else {
			chessBoard.moveChessPiece(fromSqi, toSqi, toSqi, Move.isPromotion(move) ? position.getStone(toSqi) : Chess.NO_STONE);
		}

		if (Move.isCastle(move)) {
			// if the king has done a castling move, also move the rook to the right place
			int rookFrom, rookTo;
			switch (toSqi) {
			case 2: rookFrom = 0; rookTo = 3; break; // white, queen's side
			case 6: rookFrom = 7; rookTo = 5; break; // white, king's side
			case 58: rookFrom = 56; rookTo = 59; break; // black, queen's side
			case 62: rookFrom = 63; rookTo = 61; break; // black, king's side
			default: rookFrom = rookTo = Chess.NO_SQUARE; // should never happen
			}
			if (rookFrom != Chess.NO_SQUARE) {
				chessBoard.moveChessPiece(rookFrom, rookTo, rookTo, Chess.NO_STONE);
			}
		}

		pieceRidingCheck(fromSqi, toSqi);

		getChessBoard().setSelectedSquare(Chess.NO_SQUARE);
		getChessBoard().highlightSquares(fromSqi, toSqi);
	}

	@Override
	public void notifyMoveUndone(ImmutablePosition position) {
		// Repaint the selected & last-move indicators
		getChessBoard().setSelectedSquare(Chess.NO_SQUARE);
		Move m = getGame().getChesspressoGame().getLastMove();
		if (m != null) {
			getChessBoard().highlightSquares(m.getFromSqi(), m.getToSqi());
		} else {
			getChessBoard().highlightSquares(Chess.NO_SQUARE, Chess.NO_SQUARE);
		}

		getControlPanel().updatePlyCount(getGame().getChesspressoGame().getCurrentPly());

		getControlPanel().repaintAll(null);

		// for entity sets, we need to ensure a redraw is done
		ChessSet cs = getChessBoard().getChessSet();
		if (cs.hasMovablePieces()) {
			cs.syncToPosition(getGame().getPosition(), getChessBoard());
		}
	}

	// -------------------------------------------------------------------------------

	/**
	 * Check for players standing on the piece that is being moved, and move them with the piece.
	 */
	private void pieceRidingCheck(int fromSqi, int toSqi) {
		if (!ChessCraft.getInstance().getConfig().getBoolean("effects.piece_riding") || !chessBoard.getChessSet().canRide()) {
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
	 * @param player the player to check
	 * @param location the location to check
	 * @return true is designing is OK here
	 */
	public boolean canDesignHere(Player player, Location location) {
		if (!isDesigning() || !PermissionUtils.isAllowedTo(player, "chesscraft.designer")) {
			return false;
		}
		if (!isAboveBoard(location)) {
			return false;
		}
		int sqi = chessBoard.getSquareAt(location);
		return (sqi >= 0 && sqi <= 4) || (sqi >= 8 && sqi <= 12) || sqi == 48 || (sqi >= 56 && sqi <= 60);
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
	 * Called by the board view manager when the board is permanently deleted
	 */
	void restoreTerrain() {
		boolean restored = false;

		// signs can get dropped otherwise
		getControlPanel().removeSigns();

		if (ChessCraft.getInstance().getWorldEdit() != null) {
			// WorldEdit will take care of changes being pushed to client
			restored = TerrainBackup.reload(this);
		}

		if (!restored) {
			// we couldn't restore the original terrain - just set the board to air
			chessBoard.clearAll();
		}
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
		String gameName = getGame() != null ? getGame().getName() : Messages.getString("ChessCommandExecutor.noGame");

		res.add(Messages.getString("ChessCommandExecutor.boardDetail.board", getName()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardExtents",
		                                    MiscUtil.formatLocation(bounds.getLowerNE()),
		                                    MiscUtil.formatLocation(bounds.getUpperSW())));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.game", gameName));
		// TODO:  yeah, we lie about the direction of the board here.  Blame Mojang/Bukkit for changing the meaning of the compass points
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardOrientation", getRotation().getLeft().toString()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardStyle", getBoardStyleName()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.pieceStyle", getPieceStyleName()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.squareSize", getSquareSize(),
		                                    getWhiteSquareMaterial(), getBlackSquareMaterial()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.frameWidth", getFrameWidth(),
		                                    getFrameMaterial()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.enclosure", getEnclosureMaterial()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.struts", getStrutsMaterial()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.height", getHeight()));
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.lightLevel", getLightLevel()));
		String lockStakeStr = getLockStake() ? Messages.getString("ChessCommandExecutor.boardDetail.locked") : "";
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.defaultStake", EconomyUtil.formatStakeStr(getDefaultStake()), lockStakeStr));
		String lockTcStr = getLockTcSpec() ? Messages.getString("ChessCommandExecutor.boardDetail.locked") : "";
		res.add(bullet + Messages.getString("ChessCommandExecutor.boardDetail.defaultTimeControl", getDefaultTcSpec(), lockTcStr));
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
		if (game != null) {
			ChessGameManager.getManager().setCurrentGame(player, game);
		}
	}

	public AttributeCollection getAttributes() {
		return attributes;
	}

	@Override
	public Object onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equals(DEFAULT_TC) && !newVal.toString().isEmpty()) {
			new TimeControl(newVal.toString());		// force validation of the spec
		}
        return newVal;
    }

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equals(DEFAULT_TC) && getControlPanel() != null) {
			getControlPanel().setTimeControl(newVal.toString());
		} else if (key.equals(BOARD_STYLE) && chessBoard != null) {
			chessBoard.setBoardStyle(newVal.toString());
		} else if (key.equals(OVERRIDE_PIECE_STYLE) && chessBoard != null) {
			chessBoard.setChessSet(newVal.toString());
			chessBoard.getChessSet().syncToPosition(getGame() == null ? null : getGame().getPosition(), chessBoard);
		}
	}

	public void defaultTimeControlChanged() {
		String spec = (String) getAttributes().get(DEFAULT_TC);
		if (spec.isEmpty()) {
			getControlPanel().setTimeControl("");
		}
	}

    @Override
    public void gameStateChanged(GameState state) {
        if (state == GameState.RUNNING) {
            if (ChessCraft.getInstance().getConfig().getBoolean("auto_teleport_on_start")) {
                getGame().getPlayer(Chess.WHITE).teleport(this);
                getGame().getPlayer(Chess.BLACK).teleport(this);
            }
        }
        getControlPanel().repaintControls();
    }

    @Override
    public boolean tryTimeControlChange(String tcSpec) {
        return !getLockTcSpec();
    }

    @Override
    public void timeControlChanged(String spec) {
        ControlPanel cp = getControlPanel();
        cp.getTcDefs().addCustomSpec(spec);
        cp.getSignButton(TimeControlButton.class).repaint();
        updateClock(Chess.WHITE);
        updateClock(Chess.BLACK);
    }

    @Override
    public boolean tryStakeChange(double newStake) {
        return !getLockStake();
    }

    @Override
    public void stakeChanged(double newStake) {
        getControlPanel().getSignButton(StakeButton.class).repaint();
    }

    @Override
    public void playerAdded(ChessPlayer cp) {
        if (cp != null) {
            getControlPanel().repaintControls();
            if (ChessCraft.getInstance().getConfig().getBoolean("auto_teleport_on_join")) {
                cp.teleport(this);
            } else {
                cp.alert(Messages.getString("ChessCommandExecutor.canTeleport", game.getName()));
            }
        }
    }

    @Override
    public void gameDeleted() {
        setGame(null);
    }

    @Override
    public void promotionPieceChanged(ChessPlayer chessPlayer, int promotionPiece) {
        getControlPanel().getSignButton(chessPlayer.getColour() == Chess.WHITE ? PromoteWhiteButton.class : PromoteBlackButton.class).repaint();
    }
}

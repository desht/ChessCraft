package me.desht.chesscraft;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.Cuboid.Direction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;
import chesspresso.position.PositionListener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;

public class BoardView implements PositionListener {
	private static final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
	private static final String styleDir = ChessCraft.directory + File.separator + "board_styles";

	private ChessCraft plugin;
	private String name;
	private Game game;
	private Location a1Square;
	private Location origin;
	private int frameWidth;
	private int squareSize;
	private int height;
	private MaterialWithData blackSquareMat;
	private MaterialWithData whiteSquareMat;
	private MaterialWithData frameMat;
	private MaterialWithData enclosureMat;
	private String pieceStyle;
	private Boolean isLit;
	private Map<Integer, ChessStone> stones;
	private String boardStyle;
	private byte lastLevel;
	private ControlPanel controlPanel;

	BoardView(String bName, ChessCraft plugin, Location where, String bStyle, String pStyle) throws ChessException {
		this.plugin = plugin;
		boardStyle = bStyle;
		pieceStyle = pStyle;

		name = bName;
		game = null; // indicates board not used by any game yet
		if (boardStyle == null)
			boardStyle = "Standard";
		loadStyle(boardStyle);
		origin = where;
		a1Square = calcBaseSquare(where);
		validateIntersections();
		System.out.println("intersections validated");
		stones = createStones(pieceStyle);
		System.out.println("stones created");
		validateBoardParams();
		controlPanel = new ControlPanel(plugin, this);
		lastLevel = -1;

		System.out.println("board created");
		BoardView.addBoardView(name, this);
	}

	// Overall sanity checking on board/set parameters
	private void validateBoardParams() throws ChessException {
		if (squareSize < 2)
			throw new ChessException("Board's square size is too small (minimum 2)!");
		if (height < 3)
			throw new ChessException("Board does not have enough vertical space (minimum 3)!");
		if (frameWidth < 2)
			throw new ChessException("Frame width is too narrow (minimum 2)");

		int maxH = -1, maxV = -1;
		for (Entry<Integer, ChessStone> entry : stones.entrySet()) {
			maxH = Math.max(maxH, entry.getValue().sizeX);
			maxH = Math.max(maxH, entry.getValue().sizeZ);
			maxV = Math.max(maxV, entry.getValue().sizeY);
		}
		if (maxH > squareSize)
			throw new ChessException("Set '" + pieceStyle + "' is too wide for this board!");
		if (maxV > height)
			throw new ChessException("Set '" + pieceStyle + "' is too tall for this board!");
	}

	// Ensure this board doesn't intersect any other boards
	private void validateIntersections() throws ChessException {
		Cuboid bounds = getBounds();
		bounds.outset(Direction.Horizontal, getFrameWidth() - 1);
		bounds.expand(Direction.Up, getHeight() + 1);

		for (BoardView bv : BoardView.listBoardViews()) {
			if (bv.getA1Square().getWorld() != getA1Square().getWorld())
				continue;
			for (Location l : bounds.corners()) {
				if (bv.getOuterBounds().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
					throw new ChessException("Board would intersect existing board " + bv.getName());
				}
			}
		}
	}

	Map<String, Object> freeze() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", name);
		result.put("game", game == null ? "" : game.getName());
		result.put("pieceStyle", pieceStyle);
		result.put("boardStyle", boardStyle);
		result.put("origin", ChessPersistence.makeBlockList(origin));

		return result;
	}

	public String getName() {
		return name;
	}

	public String getBoardStyle() {
		return boardStyle;
	}

	public String getPieceStyle() {
		return pieceStyle;
	}

	public Game getGame() {
		return game;
	}

	public Location getA1Square() {
		return a1Square;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getSquareSize() {
		return squareSize;
	}

	public int getHeight() {
		return height;
	}

	public Boolean getIsLit() {
		return isLit;
	}

	ControlPanel getControlPanel() {
		return controlPanel;
	}

	MaterialWithData getBlackSquareMat() {
		return blackSquareMat;
	}

	MaterialWithData getWhiteSquareMat() {
		return whiteSquareMat;
	}

	MaterialWithData getFrameMat() {
		return frameMat;
	}

	MaterialWithData getEnclosureMat() {
		return enclosureMat;
	}

	@SuppressWarnings("unchecked")
	void loadStyle(String style) throws ChessException {
		Yaml yaml = new Yaml();

		File f = new File(styleDir, style + ".yml");
		try {
			Map<String, Object> styleMap = (Map<String, Object>) yaml.load(new FileInputStream(f));

			squareSize = (Integer) styleMap.get("square_size");
			frameWidth = (Integer) styleMap.get("frame_width");
			height = (Integer) styleMap.get("height");
			isLit = (Boolean) styleMap.get("lit");
			if (pieceStyle == null)
				pieceStyle = (String) styleMap.get("piece_style");

			blackSquareMat = MaterialWithData.parseIdAndData((String) styleMap.get("black_square"));
			whiteSquareMat = MaterialWithData.parseIdAndData((String) styleMap.get("white_square"));
			frameMat = MaterialWithData.parseIdAndData((String) styleMap.get("frame"));
			enclosureMat = MaterialWithData.parseIdAndData((String) styleMap.get("enclosure"));
		} catch (Exception e) {
			e.printStackTrace();
			plugin.log(Level.SEVERE, "can't load board style " + style + ": " + e);
			throw new ChessException("Board style '" + style + "' is not available.");
		}
	}

	// Given a board origin (the block at the centre of the A1 square),
	// calculate the southwest corner of the A1 square (which is also the
	// southwest corner of the whole board)
	private Location calcBaseSquare(Location where) {
		int xOff = squareSize / 2;
		int zOff = squareSize / 2;
		return new Location(where.getWorld(), where.getBlockX() + xOff, where.getBlockY(), where.getBlockZ() + zOff);
	}

	private Map<Integer, ChessStone> createStones(String pieceStyle) throws ChessException {
		if (!plugin.library.isChessSetLoaded(pieceStyle)) {
			plugin.library.loadChessSet(pieceStyle);
		}
		Map<Integer, ChessStone> result = new HashMap<Integer, ChessStone>();

		for (int stone = Chess.MIN_STONE; stone <= Chess.MAX_STONE; stone++) {
			if (stone != Chess.NO_STONE)
				result.put(stone, plugin.library.getStone(pieceStyle, stone));
		}
		return result;
	}

	void paintAll() {
		wipe();
		paintEnclosure();
		paintBoard();
		paintFrame();
		controlPanel.repaint();
		lastLevel = -1; // force a lighting update
		doLighting();
	}

	private void paintEnclosure() {
		Cuboid bounds = getBounds();
		int fw = frameWidth - 1;
		int x1 = bounds.getLowerNE().getBlockX() - fw;
		int z1 = bounds.getLowerNE().getBlockZ() - fw;
		int x2 = bounds.getUpperSW().getBlockX() + fw;
		int z2 = bounds.getUpperSW().getBlockZ() + fw;
		// (x1,z1) & (x2,z2) are now the outermost corners of the frame
		int y1 = a1Square.getBlockY() + 1;
		int y2 = a1Square.getBlockY() + 1 + height;
		if (y2 > 127)
			y2 = 127;
		World w = a1Square.getWorld();

		Cuboid walls[] = { new Cuboid(new Location(w, x1, y1, z2), new Location(w, x2, y2, z2)), // west
				new Cuboid(new Location(w, x1, y1, z1), new Location(w, x2, y2, z1)), // east
				new Cuboid(new Location(w, x1, y1, z1), new Location(w, x1, y2, z2)), // north
				new Cuboid(new Location(w, x2, y1, z1), new Location(w, x2, y2, z2)), // south
				new Cuboid(new Location(w, x1, y2, z1), new Location(w, x2, y2, z2)), // roof
		};
		for (Cuboid wall : walls) {
			for (Location l : wall) {
				ChessCraft.setBlock(w.getBlockAt(l), enclosureMat);
			}
		}
	}

	private void paintFrame() {
		Cuboid bounds = getBounds();

		World w = a1Square.getWorld();
		int y = a1Square.getBlockY();
		int fw = frameWidth - 1;
		int x1 = bounds.getLowerNE().getBlockX();
		int z1 = bounds.getLowerNE().getBlockZ();
		int x2 = bounds.getUpperSW().getBlockX();
		int z2 = bounds.getUpperSW().getBlockZ();

		Cuboid[] frameParts = { new Cuboid(new Location(w, x1 - fw, y, z1 - fw), new Location(w, x2 + fw, y, z1)), // east
				new Cuboid(new Location(w, x1 - fw, y, z2), new Location(w, x2 + fw, y, z2 + fw)), // west
				new Cuboid(new Location(w, x1 - fw, y, z1 - fw), new Location(w, x1, y, z2 + fw)), // north
				new Cuboid(new Location(w, x2, y, z1 - fw), new Location(w, x2 + fw, y, z2 + fw)), // south
		};
		for (Cuboid part : frameParts) {
			for (Location l : part) {
				ChessCraft.setBlock(w.getBlockAt(l), frameMat);
			}
		}
	}

	// Check if the control panel is present and draw it if necessary
	// (will be missing if upgrading from v0.1)
	void checkControlPanel() {
		if (controlPanel.getPanelBlocks().getUpperSW().getBlock().getTypeId() != frameMat.material) {
			controlPanel.repaint();
		}
	}

	private void paintBoard() {
		for (int i = 0; i < Chess.NUM_OF_SQUARES; i++) {
			paintSquareAt(i);
			int stone = game != null ? game.getPosition().getStone(i) : Chess.NO_STONE;
			paintStoneAt(i, stone);
		}
	}

	private void paintStoneAt(int sqi, int stone) {
		int col = Chess.sqiToCol(sqi);
		int row = Chess.sqiToRow(sqi);
		Location l = rowColToWorld(row, col, 0, 0);

		World w = a1Square.getWorld();
		if (stone == Chess.NO_STONE) {

			// first remove blocks that might pop off & leave a drop
			Block b = null;
			for (int x = 0; x < squareSize; x++) {
				for (int y = 1; y <= height; y++) {
					for (int z = 0; z < squareSize; z++) {
						b = w.getBlockAt(l.getBlockX() - x, l.getBlockY() + y, l.getBlockZ() - z);
						if (shouldPlaceLast(b.getTypeId())) {
							b.setTypeId(0);
						}
					}
				}
			}
			for (int x = 0; x < squareSize; x++) {
				for (int y = 1; y <= height; y++) {
					for (int z = 0; z < squareSize; z++) {
						w.getBlockAt(l.getBlockX() - x, l.getBlockY() + y, l.getBlockZ() - z).setTypeId(0);
					}
				}
			}
		} else {
			MaterialWithData air = new MaterialWithData(0, (byte) 0);
			ChessStone cStone = stones.get(stone);
			int xOff = (squareSize - cStone.getSizeX()) / 2;
			int zOff = (squareSize - cStone.getSizeZ()) / 2;
			boolean secondPassNeeded = false;
			for (int x = 0; x < cStone.getSizeX(); x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < cStone.getSizeZ(); z++) {
						MaterialWithData mat = y >= cStone.getSizeY() ? air : cStone.getMaterial(x, y, z);
						if (!shouldPlaceLast(mat.material))
							ChessCraft.setBlock(w.getBlockAt((l.getBlockX() - xOff) - x, l.getBlockY() + y + 1,
							                                 (l.getBlockZ() - zOff)- z), mat);
						else
							secondPassNeeded = true;
					}
				}
			}
			if (secondPassNeeded) {
				for (int x = 0; x < cStone.getSizeX(); x++) {
					for (int y = 0; y < height; y++) {
						for (int z = 0; z < cStone.getSizeZ(); z++) {
							MaterialWithData mat = y >= cStone.getSizeY() ? air : cStone.getMaterial(x, y, z);
							if (shouldPlaceLast(mat.material))
								ChessCraft.setBlock(w.getBlockAt((l.getBlockX() - xOff) - x, l.getBlockY() + y + 1,
								                                 (l.getBlockZ() - zOff) - z), mat);
						}
					}
				}
			}
		}
	}

	private void paintSquareAt(int sqi) {
		int col = Chess.sqiToCol(sqi);
		int row = Chess.sqiToRow(sqi);
		Location locNE = rowColToWorldNE(row, col);
		MaterialWithData m = new MaterialWithData(Chess.isWhiteSquare(sqi) ? whiteSquareMat : blackSquareMat);
		Cuboid square = new Cuboid(locNE, locNE);
		square.expand(Direction.South, squareSize - 1);
		square.expand(Direction.West, squareSize - 1);

		for (Location loc : square) {
			ChessCraft.setBlock(loc.getBlock(), m);
		}
	}

	void doLighting() {
		if (!isLit)
			return;

		byte level = getOuterBounds().getUpperSW().getBlock().getLightLevel();
		if (isBright(level) == isBright(lastLevel) && lastLevel >= 0)
			return;
		lastLevel = level;

		if (isBright(level)) {
			MaterialWithData white = new MaterialWithData(whiteSquareMat);
			MaterialWithData black = new MaterialWithData(blackSquareMat);
			for (int sqi = 0; sqi < Chess.NUM_OF_SQUARES; sqi++) {
				int col = Chess.sqiToCol(sqi);
				int row = Chess.sqiToRow(sqi);
				Location locNE = rowColToWorldNE(row, col);
				ChessCraft.setBlock(locNE.getBlock(), Chess.isWhiteSquare(sqi) ? white : black);
			}
			setFrameLights(frameMat);
		} else {
			for (int sqi = 0; sqi < Chess.NUM_OF_SQUARES; sqi++) {
				int col = Chess.sqiToCol(sqi);
				int row = Chess.sqiToRow(sqi);
				Location locNE = rowColToWorldNE(row, col);
				locNE.getBlock().setTypeId(89);
			}
			setFrameLights(new MaterialWithData(89, (byte) -1));
		}
	}

	private void setFrameLights(MaterialWithData mat) {
		Location l = getBounds().getLowerNE();
		l.add(squareSize / 2 + 1, 0, 0);
		int boardSize = squareSize * 8 + 1;
		// east & west sides
		for (int i = 0; i < 8; i++) {
			ChessCraft.setBlock(l.getBlock(), mat);
			l.add(0, 0, boardSize);
			ChessCraft.setBlock(l.getBlock(), mat);
			l.add(squareSize, 0, -boardSize);
		}
		// north & south sides
		l = getBounds().getLowerNE();
		l.add(0, 0, squareSize / 2 + 1);
		for (int i = 0; i < 8; i++) {
			ChessCraft.setBlock(l.getBlock(), mat);
			l.add(boardSize, 0, 0);
			ChessCraft.setBlock(l.getBlock(), mat);
			l.add(-boardSize, 0, squareSize);
		}
	}

	private boolean isBright(byte level) {
		if (level < 12) {
			return false;
		} else {
			return true;
		}
	}

	// Return the bounds of the chess board - the innermost ring of the frame
	Cuboid getBounds() {
		Location a1 = rowColToWorldSW(0, 0);
		Location h8 = rowColToWorldNE(7, 7);

		int x1 = h8.getBlockX(), z2 = h8.getBlockZ();
		int x2 = a1.getBlockX(), z1 = a1.getBlockZ();

		World w = a1Square.getWorld();
		int y = a1Square.getBlockY();
		return new Cuboid(new Location(w, x1, y, z1), new Location(w, x2, y, z2)).outset(Direction.Horizontal, 1);
	}

	Cuboid getOuterBounds() {
		Cuboid res = getBounds();
		res.outset(Direction.Horizontal, getFrameWidth() - 1);
		res.expand(Direction.Up, getHeight() + 1);
		return res;
	}

	// given a Chess row & col, get the location in world coords of that
	// square's NE block (smallest X & Z)
	Location rowColToWorldNE(int row, int col) {
		return rowColToWorld(row, col, squareSize - 1, squareSize - 1);
	}

	// given a Chess row & col, get the location in world coords of that
	// square's SW block (largest X & Z)
	Location rowColToWorldSW(int row, int col) {
		return rowColToWorld(row, col, 0, 0);
	}

	Location rowColToWorld(int row, int col, int xOff, int zOff) {
		Location a1 = a1Square;
		xOff += row * squareSize;
		zOff += col * squareSize;
		return new Location(a1.getWorld(), a1.getX() - xOff, a1.getY(), a1.getZ() - zOff);
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
			mat = whiteSquareMat;
		} else if (toPlay == Chess.BLACK) {
			mat = blackSquareMat;
		} else if (toPlay == Chess.NOBODY) {
			mat = frameMat;
		} else {
			return; // should never get here
		}
		controlPanel.updateToMoveIndicator(mat);
	}

	void setGame(Game game) {
		this.game = game;
	}

	boolean isOnBoard(Location loc, int minHeight, int maxHeight) {
		Cuboid bounds = getBounds();
		bounds.inset(Direction.Horizontal, 1);
		bounds.shift(Direction.Up, minHeight);
		bounds.expand(Direction.Up, maxHeight - minHeight);
		return bounds.contains(loc);
	}

	// true if the location is part of the board itself
	boolean isOnBoard(Location loc) {
		return isOnBoard(loc, 0, 0);
	}

	// true if the location is above the board AND within the board's height
	// range
	boolean isAboveBoard(Location loc) {
		return isOnBoard(loc, 1, height);
	}

	// true if the location is *anywhere* within the board, including frame &
	// enclosure
	boolean isPartOfBoard(Location loc) {
		return getOuterBounds().contains(loc);
	}

	boolean isControlPanel(Location loc) {
		return controlPanel.getPanelBlocks().contains(loc);
	}

	int getSquareAt(Location loc) {
		if (!isOnBoard(loc, 0, height))
			return Chess.NO_SQUARE;
		int row = (a1Square.getBlockX() - loc.getBlockX()) / squareSize;
		int col = (a1Square.getBlockZ() - loc.getBlockZ()) / squareSize;
		return Chess.coorToSqi(col, row);
	}

	void delete(boolean deleteBlocks, Player p) {
		if (deleteBlocks)
			restoreTerrain(p);
		BoardView.removeBoardView(getName());
	}

	void delete() {
		delete(false, null);
	}

	/**
	 * Checks whether a block can be passed through. (copied from WorldEdit ...
	 * after i'd made another, lol)
	 * 
	 * @param id
	 * @return
	 */
	public static boolean canPassThrough(int id) {
		return id == 0 // Air
				|| id == 8 // Water
				|| id == 9 // Water
				|| id == 6 // Saplings
				|| id == 27 // Powered rails
				|| id == 28 // Detector rails
				|| id == 30 // Web <- someone will hate me for this
				|| id == 31 // Long grass
				|| id == 32 // Shrub
				|| id == 37 // Yellow flower
				|| id == 38 // Red flower
				|| id == 39 // Brown mushroom
				|| id == 40 // Red mush room
				|| id == 50 // Torch
				|| id == 51 // Fire
				|| id == 55 // Redstone wire
				|| id == 59 // Crops
				|| id == 63 // Sign post
				|| id == 65 // Ladder
				|| id == 66 // Minecart tracks
				|| id == 68 // Wall sign
				|| id == 69 // Lever
				|| id == 70 // Stone pressure plate
				|| id == 72 // Wooden pressure plate
				|| id == 75 // Redstone torch (off)
				|| id == 76 // Redstone torch (on)
				|| id == 77 // Stone button
				|| id == 78 // Snow
				|| id == 83 // Reed
				|| id == 90 // Portal
				|| id == 93 // Diode (off)
				|| id == 94; // Diode (on)
	}

	/**
	 * Returns true if the block is a container block. (copied from WorldEdit)
	 * 
	 * @param id
	 * @return
	 */
	public static boolean isContainerBlock(int id) {
		return id == 23 // Dispenser
				|| id == 61 // Furnace
				|| id == 62 // Furnace
				|| id == 54; // Chest
	}

	/**
	 * Checks to see whether a block should be placed last. (also copied from
	 * WorldEdit) (paintings are not blocks, and are not included...)
	 * 
	 * @param id
	 * @return
	 */
	public static boolean shouldPlaceLast(int id) {
		return id == 6 // Saplings
				|| id == 26 // Beds
				|| id == 27 // Powered rails
				|| id == 28 // Detector rails
				|| id == 31 // Long grass
				|| id == 32 // Shrub
				|| id == 37 // Yellow flower
				|| id == 38 // Red flower
				|| id == 39 // Brown mushroom
				|| id == 40 // Red mush room
				|| id == 50 // Torch
				|| id == 51 // Fire
				|| id == 55 // Redstone wire
				|| id == 59 // Crops
				|| id == 63 // Sign post
				|| id == 64 // Wooden door
				|| id == 65 // Ladder
				|| id == 66 // Minecart tracks
				|| id == 68 // Wall sign
				|| id == 69 // Lever
				|| id == 70 // Stone pressure plate
				|| id == 71 // Iron door
				|| id == 72 // Wooden pressure plate
				|| id == 75 // Redstone torch (off)
				|| id == 76 // Redstone torch (on)
				|| id == 77 // Stone button
				|| id == 78 // Snow
				|| id == 81 // Cactus
				|| id == 83 // Reed
				|| id == 90 // Portal
				|| id == 92 // Cake
				|| id == 93 // Repeater (off)
				|| id == 94 // Repeater (on)
				|| id == 96; // Trap door
	}

	/**
	 * delete blocks in bounds, but don't allow items to drop (paintings are not
	 * blocks, and are not included...) also does not scan the faces of the
	 * region for drops when the region is cleared
	 */
	void wipe() {
		Block b = null;
		// first remove blocks that might pop off & leave a drop
		for (Location l : getOuterBounds()) {
			b = l.getBlock();
			if (shouldPlaceLast(b.getTypeId())) {
				b.setTypeId(0);
			}// also check if this is a container
			else if (isContainerBlock(b.getTypeId())) {
				BlockState state = b.getState();
				if (state instanceof org.bukkit.block.ContainerBlock) {
					org.bukkit.block.ContainerBlock chest = (org.bukkit.block.ContainerBlock) state;
					Inventory inven = chest.getInventory();
					inven.clear();
				}
			}
		}
		// now wipe all (remaining) blocks
		for (Location l : getOuterBounds()) {
			l.getBlock().setTypeId(0);
		}
	}

	void restoreTerrain(Player player) {
		if (plugin.getWorldEdit() != null) {
			TerrainBackup.reload(plugin, player, this);
		} else {
			wipe();
		}
	}

	Location findSafeLocationOutside() {
		Location dest0 = getA1Square().clone();

		dest0.add(getFrameWidth() + 1, 0.0, getFrameWidth() + 1);
		Location dest1 = dest0.clone().add(0.0, 1.0, 0.0);

		while (dest0.getBlock().getTypeId() != 0 && dest1.getBlock().getTypeId() != 0) {
			dest0.add(0.0, 1.0, 0.0);
			dest1.add(0.0, 1.0, 0.0);
			if (dest1.getBlockY() > 127)
				return null;
		}
		return dest0;
	}

	/*------------------------------------------------------------------------------_*/

	static void addBoardView(String name, BoardView view) {
		chessBoards.put(name, view);
	}

	static void removeBoardView(String name) {
		chessBoards.remove(name);
	}

	static void removeAllBoardViews() {
		chessBoards.clear();
	}

	static Boolean checkBoardView(String name) {
		return chessBoards.containsKey(name);
	}

	static BoardView getBoardView(String name) throws ChessException {
		if (!chessBoards.containsKey(name))
			throw new ChessException("No such board '" + name + "'");
		return chessBoards.get(name);
	}

	static List<BoardView> listBoardViews(boolean isSorted) {
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

	static List<BoardView> listBoardViews() {
		return listBoardViews(false);
	}

	static BoardView getFreeBoard() throws ChessException {
		for (BoardView bv : listBoardViews()) {
			if (bv.getGame() == null)
				return bv;
		}
		throw new ChessException("There are no free boards to create a game on.");
	}

	// match if loc is any part of the board including the frame & enclosure
	static BoardView partOfChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isPartOfBoard(loc))
				return bv;
		}
		return null;
	}

	// match if loc is above a board square but below the roof
	static BoardView aboveChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isAboveBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	// match if loc is part of a board square
	static BoardView onChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.isOnBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

}

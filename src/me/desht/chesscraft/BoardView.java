package me.desht.chesscraft;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.Cuboid.Direction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.World;
import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;
import chesspresso.position.PositionListener;

public class BoardView implements PositionListener {
	
	private static final String styleDir =
		ChessCraft.directory + File.separator + "board_styles";
	
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
	private Map<Integer,ChessStone> stones;
	private String boardStyle;
	private byte lastLevel;
	
	BoardView(String bName, ChessCraft plugin, Location where, String bStyle) throws ChessException {
		this.plugin = plugin;
		boardStyle = bStyle;

		name = bName;
		game = null;	// indicates board not used by any game yet
		if (boardStyle == null) boardStyle = "Standard";
		loadStyle(boardStyle);
		origin = where;
		a1Square = calcBaseSquare(where);
		validateIntersections();
		stones = createStones(pieceStyle);
		validateBoardParams();
		lastLevel = -1;
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
		for (Entry<Integer,ChessStone> entry : stones.entrySet()) {
			maxH = Math.max(maxH, entry.getValue().sizeX);
			maxH = Math.max(maxH, entry.getValue().sizeZ);
			maxV = Math.max(maxV, entry.getValue().sizeY);
		}
		if (maxH >= squareSize)
			throw new ChessException("Set '" + pieceStyle + "' is too wide for this board!"); 
		if (maxV > height)
			throw new ChessException("Set '" + pieceStyle + "' is too tall for this board!"); 
	}

	// Ensure this board doesn't intersect any other boards
	private void validateIntersections() throws ChessException {
		Cuboid bounds = getBounds();
		bounds.outset(Direction.Horizontal, getFrameWidth() - 1);
		bounds.expand(Direction.Up, getHeight() + 1);

		for (BoardView bv : plugin.listBoardViews()) {
			if (bv.getA1Square().getWorld() != getA1Square().getWorld())
				continue;
			for (Location l : bounds.corners()) {
				if (bv.getOuterBounds().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
					throw new ChessException("Board would intersect existing board " + bv.getName());
				}
			}
		}
	}

	Map<String,Object> freeze() {
		Map<String,Object> result = new HashMap<String,Object>();
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

	public Map<Integer, ChessStone> getStones() {
		return stones;
	}

	@SuppressWarnings("unchecked")
	void loadStyle(String style) throws ChessException {
		Yaml yaml = new Yaml();

		File f = new File(styleDir, style + ".yml");
		try {        	
        	Map<String,Object> styleMap = 
        		(Map<String,Object>) yaml.load(new FileInputStream(f));
        	
        	squareSize = (Integer)styleMap.get("square_size");
        	frameWidth = (Integer)styleMap.get("frame_width");
        	height     = (Integer)styleMap.get("height");
        	isLit      = (Boolean)styleMap.get("lit");
        	pieceStyle = (String)styleMap.get("piece_style");
        	
        	blackSquareMat = MaterialWithData.parseIdAndData((String)styleMap.get("black_square"));
        	whiteSquareMat = MaterialWithData.parseIdAndData((String)styleMap.get("white_square"));
        	frameMat       = MaterialWithData.parseIdAndData((String)styleMap.get("frame"));
        	enclosureMat   = MaterialWithData.parseIdAndData((String)styleMap.get("enclosure"));
		} catch (Exception e) {
			e.printStackTrace();
			plugin.log(Level.SEVERE, "can't load board style " + style + ": " + e);
			throw new ChessException("Board style '" + style + "' is not available.");
		}
	}
	
	// Given a board origin (the block at the centre of the A1 square),
	// calculate the southwest corner of the A1 square (which is also the southwest
	// corner of the whole board)
	private Location calcBaseSquare(Location where) {
		int xOff = squareSize / 2;
		int zOff = squareSize / 2;
		return new Location(where.getWorld(), where.getBlockX() + xOff, where.getBlockY(), where.getBlockZ() + zOff);
	}

	private Map<Integer, ChessStone> createStones(String pieceStyle) throws ChessException {
		if (!plugin.library.isChessSetLoaded(pieceStyle)) {
			plugin.library.loadChessSet(pieceStyle);
		}
		Map<Integer,ChessStone> result = new HashMap<Integer,ChessStone>();
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
		lastLevel = -1;	// force a lighting update
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
		if (y2 > 127) y2 = 127;
		World w = a1Square.getWorld();

		Cuboid walls[] = {
				new Cuboid(new Location(w, x1, y1, z2), new Location(w, x2, y2, z2)),	// west
				new Cuboid(new Location(w, x1, y1, z1), new Location(w, x2, y2, z1)),	// east
				new Cuboid(new Location(w, x1, y1, z1), new Location(w, x1, y2, z2)),	// north
				new Cuboid(new Location(w, x2, y1, z1), new Location(w, x2, y2, z2)),	// south
				new Cuboid(new Location(w, x1, y2, z1), new Location(w, x2, y2, z2)),	// roof
		};
		for (Cuboid wall : walls) {
			for (Location l: wall) {
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

		Cuboid[] frameParts = {
				new Cuboid(new Location(w, x1 - fw, y, z1 - fw), new Location(w, x2 + fw, y, z1)),	// east side
				new Cuboid(new Location(w, x1 - fw, y, z2), new Location(w, x2 + fw, y, z2 + fw)),	// west side
				new Cuboid(new Location(w, x1 - fw, y, z1 - fw), new Location(w, x1, y, z2 + fw)),	// north side
				new Cuboid(new Location(w, x2, y, z1 - fw), new Location(w, x2 + fw, y, z2 + fw)),	// south side
		};
		for (Cuboid part : frameParts) {
			for (Location l: part) {
				ChessCraft.setBlock(w.getBlockAt(l), frameMat);
			}
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
		Location l = rowColToWorld(row, col, 0 , 0);
		
		World w = a1Square.getWorld();
		if (stone == Chess.NO_STONE) {
			for (int x = 0; x < squareSize; x++) {
				for (int y = 1; y <= height; y++) {
					for (int z = 0; z < squareSize; z++) {
						w.getBlockAt(l.getBlockX() - x, l.getBlockY() + y, l.getBlockZ() - z).setTypeId(0); 
					}
				}
			}
		} else {
			MaterialWithData air = new MaterialWithData(0, (byte)-1);
			ChessStone cStone = stones.get(stone);
			int xOff = (squareSize - cStone.getSizeX()) / 2;
			int zOff = (squareSize - cStone.getSizeZ()) / 2;
			for (int x = 0; x < cStone.getSizeX(); x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < cStone.getSizeZ(); z++) {
						MaterialWithData mat = y >= cStone.getSizeY() ? air : cStone.getMaterial(x, y, z);
						ChessCraft.setBlock(w.getBlockAt((l.getBlockX() - xOff) - x, l.getBlockY() + y + 1, (l.getBlockZ() - zOff) - z), mat);
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
			setFrameLights(new MaterialWithData(89, (byte)-1));
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

	// given a Chess row & col, get the location in world coords of that square's NE block (smallest X & Z)
	Location rowColToWorldNE(int row, int col) {
		return rowColToWorld(row, col, squareSize - 1, squareSize - 1);
	}
	// given a Chess row & col, get the location in world coords of that square's SW block (largest X & Z)
	Location rowColToWorldSW(int row, int col) {
		return rowColToWorld(row, col, 0 , 0);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void plyNumberChanged(int plyNumber) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sqiEPChanged(int sqiEP) {
		// TODO Auto-generated method stub

	}

	@Override
	public void squareChanged(int sqi, int stone) {
//		System.out.println("square changed: " + sqi + " -> " + stone);
		paintStoneAt(sqi, stone);
	}

	@Override
	public void toPlayChanged(int toPlay) {
		// TODO Auto-generated method stub

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
	
	// true if the location is above the board AND within the board's height range
	boolean isAboveBoard(Location loc) {
		return isOnBoard(loc, 1, height - 1);
	}
	
	// true if the location is *anywhere* within the board, including frame & enclosure
	boolean isPartOfBoard(Location loc) {
		return getOuterBounds().contains(loc);
	}
	
	int getSquareAt(Location loc) {
		if (!isOnBoard(loc, 0, height - 1))
			return Chess.NO_SQUARE;
		int row = (a1Square.getBlockX() - loc.getBlockX()) / squareSize;
		int col = (a1Square.getBlockZ() - loc.getBlockZ()) / squareSize;		
		return Chess.coorToSqi(col, row);
	}

	// Wipe the board's contents - generally called just before the board is deleted 
	void wipe() {
		for (Location l : getOuterBounds()) {
			// TODO: restore to previous terrain, not air
			l.getBlock().setTypeId(0);
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
}

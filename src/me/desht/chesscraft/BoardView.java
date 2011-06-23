package me.desht.chesscraft;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.World;
import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;
import chesspresso.position.ImmutablePosition;
import chesspresso.position.PositionChangeListener;
import chesspresso.position.PositionListener;

public class BoardView implements PositionListener, PositionChangeListener {
	
	private static final String styleDir =
		ChessCraft.directory + File.separator + "board_styles";
	
	private ChessCraft plugin;
	private String name;
	private Game game;
	private Location a1Square;
	private int frameWidth;
	private int squareSize;
	private int height;
	private MaterialWithData blackSquareId;
	private MaterialWithData whiteSquareId;
	private MaterialWithData frameId;
	private MaterialWithData enclosureId;
	private String pieceStyle;
	private Boolean isLit;
	private Map<Integer,ChessStone> stones;
	
	BoardView(String bName, ChessCraft plugin, Location where, String style) throws ChessException {
		this.plugin = plugin;

		name = bName;
		game = null;	// indicates board not used by any game yet
		if (style == null) style = "Standard";		
		loadStyle(style);
		a1Square = calcBaseSquare(where);
		stones = createStones(pieceStyle);
	}
	
	public String getName() {
		return name;
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
        	
        	blackSquareId = ChessCraft.parseIdAndData((String)styleMap.get("black_square"));
        	whiteSquareId = ChessCraft.parseIdAndData((String)styleMap.get("white_square"));
        	frameId       = ChessCraft.parseIdAndData((String)styleMap.get("frame"));
        	enclosureId   = ChessCraft.parseIdAndData((String)styleMap.get("enclosure"));
		} catch (Exception e) {
			e.printStackTrace();
			plugin.log(Level.SEVERE, "can't load board style " + style + ": " + e);
			throw new ChessException("Board style '" + style + "' is not available.");
		}
	}
	
	private Location calcBaseSquare(Location where) {
		int xOff = squareSize / 2;
		int zOff = squareSize / 2;
		Location res = new Location(where.getWorld(), where.getX() + xOff, where.getY() - 1, where.getZ() + zOff);

		System.out.println("standing at " + where);
		System.out.println("board origin " + res);
		return res;
	}

	private Map<Integer, ChessStone> createStones(String pieceStyle) throws ChessException {
		if (!plugin.library.isSetLoaded(pieceStyle))
			throw new ChessException("No such chess set " + pieceStyle);
		Map<Integer,ChessStone> result = new HashMap<Integer,ChessStone>();
		for (int stone = Chess.MIN_STONE; stone <= Chess.MAX_STONE; stone++) {
			if (stone != Chess.NO_STONE) 
				result.put(stone, plugin.library.getStone(pieceStyle, stone));
		}
		return result;
	}

	void paintAll() {
		paintBoard();
		paintFrame();
		paintEnclosure();
	}
	
	private void paintEnclosure() {
		int[][] bounds = getBounds();
		int fw = frameWidth - 1;
		int x1 = bounds[0][0] - fw, x2 = bounds[1][0] + fw;
		int z1 = bounds[0][1] - fw, z2 = bounds[1][1] + fw;
		// (x1,z1) & (x2,z2) are the outermost ring of the frame
		int y1 = a1Square.getBlockY() + 1;
		int y2 = a1Square.getBlockY() + 1 + height;
		World w = a1Square.getWorld();

		// draw enclosure walls
		for (int y = y1; y < y2 && y < 127; y++) {
			for (int x = x1; x <= x2; x++) {
				w.getBlockAt(x, y, z1).setTypeIdAndData(enclosureId.material, enclosureId.data, false);
				w.getBlockAt(x, y, z2).setTypeIdAndData(enclosureId.material, enclosureId.data, false);
			}
			for (int z = z1; z <= z2; z++) {
				w.getBlockAt(x1, y, z).setTypeIdAndData(enclosureId.material, enclosureId.data, false);
				w.getBlockAt(x2, y, z).setTypeIdAndData(enclosureId.material, enclosureId.data, false);
			}
		}
		// draw enclosure roof, with possible lighting
		if (y2 > 127) return;
		for (int x = 0; x <= x2 - x1; x++) {
			for (int z = 0; z <= z2 - z1; z++) {
				if (isLit && height < 7 && x % squareSize == squareSize / 2 && z % squareSize == squareSize / 2) {
					w.getBlockAt(x1 + x, y2, z1 + z).setTypeId(89);
				} else {
					w.getBlockAt(x1 + x, y2, z1 + z).setTypeIdAndData(enclosureId.material, enclosureId.data, false);
				}
			}
		}
	}

	private void paintFrame() {
		int[][] bounds = getBounds();	

		World w = a1Square.getWorld();
		int y = a1Square.getBlockY();
		int x1 = bounds[0][0], x2 = bounds[1][0];
		int z1 = bounds[0][1], z2 = bounds[1][1];
		System.out.println("bounds: (" + x1 + "," + z1 + "), (" + x2 + "," + z2 +")");
		
		for (int f = 0; f < frameWidth; f++) {
			int fId = isLit && f == 0 ? 89 : frameId.material;
			for (int x = x1 - f; x <= x2 + f; x++) {
				w.getBlockAt(x, y, z1 - f).setTypeIdAndData(fId, frameId.data, false);
				w.getBlockAt(x, y, z2 + f).setTypeId(fId);
			}
			for (int z = z1 - f; z <= z2 + f; z++) {
				w.getBlockAt(x1 - f, y, z).setTypeId(fId);
				w.getBlockAt(x2 + f, y, z).setTypeId(fId);
			}
		}
	}

	private void paintBoard() {
		for (int i = 0; i < Chess.NUM_OF_SQUARES; i++) {
			paintSquareAt(i);
			paintPieceAt(i, Chess.NO_STONE);
		}
	}

	private void paintPieceAt(int sqi, int stone) {
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
			ChessStone cStone = stones.get(stone);
			int xOff = (squareSize - cStone.getSizeX()) / 2;
			int zOff = (squareSize - cStone.getSizeZ()) / 2;
			for (int x = 0; x < cStone.getSizeX(); x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < cStone.getSizeZ(); z++) {
						int mId = y >= cStone.getSizeY() ? 0 : cStone.getMaterial(x, y, z);
						w.getBlockAt((l.getBlockX() - xOff) - x, l.getBlockY() + y + 1, (l.getBlockZ() - zOff) - z).setTypeId(mId);
					}
				}
			}
		}
	}

	private void paintSquareAt(int sqi) {
		int col = Chess.sqiToCol(sqi);
		int row = Chess.sqiToRow(sqi);
		Location l = rowColToWorldNE(row, col);
		int matId = Chess.isWhiteSquare(sqi) ? whiteSquareId.material : blackSquareId.material;
		int matData = Chess.isWhiteSquare(sqi) ? whiteSquareId.data : blackSquareId.data;
		int cx = l.getBlockX();
		int cz = l.getBlockZ();
		World w = a1Square.getWorld();
		for (int x = 0; x < squareSize; x++) {
			for (int z = 0; z < squareSize; z++) {
				w.getBlockAt(cx + x, l.getBlockY(), cz + z).setTypeIdAndData(matId, (byte)matData, false);
			}
		}
		if (isLit && height > 6) {
			w.getBlockAt(cx + squareSize / 2, l.getBlockY(), cz + squareSize / 2).setTypeId(89);
		}
		
	}

	// Return the bounds of the chessboard - the innermost ring of the frame
	int[][] getBounds() {
		Location a1 = rowColToWorldCenter(0, 0);
		Location h8 = rowColToWorldCenter(7, 7);
	
		int x1 = a1.getBlockX(), z1 = a1.getBlockZ();
		int x2 = h8.getBlockX(), z2 = h8.getBlockZ();
		
		int tmp = 0;
		if (x1 > x2) { tmp = x1; x1 = x2; x2 = tmp; }
		if (z1 > z2) { tmp = z1; z1 = z2; z2 = tmp; }
		
		x1 -= squareSize / 2 + 1; x2 += squareSize / 2 + 1;
		z1 -= squareSize / 2 + 1; z2 += squareSize / 2 + 1;
		
		int res[][] = new int[2][2];
		res[0][0] = x1;	res[0][1] = z1;
		res[1][0] = x2;	res[1][1] = z2;
		
		return res;
	}

	// given a Chess row & col, get the location in world coords of that square's NE point (smallest X & Z)
	private Location rowColToWorldNE(int row, int col) {
		return rowColToWorld(row, col, squareSize - 1, squareSize - 1);
	}
	
	private Location rowColToWorldCenter(int row, int col) {
		return rowColToWorld(row, col, squareSize / 2, squareSize / 2);
	}
	
	private Location rowColToWorld(int row, int col, int xOff, int zOff) {
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
		paintPieceAt(sqi, stone);
	}

	@Override
	public void toPlayChanged(int toPlay) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyMoveDone(ImmutablePosition position, short move) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyMoveUndone(ImmutablePosition position) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyPositionChanged(ImmutablePosition position) {
		// TODO Auto-generated method stub

	}

	void setGame(Game game) {
		this.game = game;		
	}


	boolean isOnBoard(Location loc, int minHeight, int maxHeight) {
		if (loc.getBlockY() >= a1Square.getBlockY() + minHeight && loc.getBlockY() <= a1Square.getBlockY() + maxHeight &&
				loc.getBlockX() <= a1Square.getBlockX() && loc.getBlockX() > a1Square.getBlockX() - squareSize * 8 &&
				loc.getBlockZ() <= a1Square.getBlockZ() && loc.getBlockZ() > a1Square.getBlockZ() - squareSize * 8)
			return true;
		else 
			return false;
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
		int[][] bounds = getBounds();
		int fw = getFrameWidth() - 1;
		int x1 = bounds[0][0] - fw;
		int x2 = bounds[1][0] + fw;
		int z1 = bounds[0][1] - fw; 
		int z2 = bounds[1][1] + fw;
		int y1 = getA1Square().getBlockY();
		int y2 = y1 + getHeight() + 1;
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
	}
	
	int getSquareAt(Location loc) {
		if (!isOnBoard(loc, 0, height - 1)) return Chess.NO_SQUARE;
		
		int row = (a1Square.getBlockX() - loc.getBlockX()) / squareSize;
		int col = (a1Square.getBlockZ() - loc.getBlockZ()) / squareSize;
		
		return Chess.coorToSqi(col, row);
	}

	// Wipe the board's contents - generally called just before the board is deleted 
	void wipe() {			
		int[][] bounds = getBounds();
		int fw = frameWidth - 1;
		int x1 = bounds[0][0] - fw, x2 = bounds[1][0] + fw;
		int z1 = bounds[0][1] - fw, z2 = bounds[1][1] + fw;
		// (x1,z1) & (x2,z2) are the outermost ring of the frame
		int y1 = a1Square.getBlockY();
		int y2 = a1Square.getBlockY() + 1 + height;
		
		World w = a1Square.getWorld();

		// TODO: restore to previous terrain, not air
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					w.getBlockAt(x, y, z).setTypeId(0);
				}
			}
		}
	}
}

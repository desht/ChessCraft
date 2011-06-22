package me.desht.chesscraft;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.config.Configuration;

import chesspresso.Chess;
import chesspresso.position.ImmutablePosition;
import chesspresso.position.PositionChangeListener;
import chesspresso.position.PositionListener;

public class BoardView implements PositionListener, PositionChangeListener {
	
	private ChessCraft plugin;
	private String name;
	private Game game;
	private Location a1Square;
	private int frameWidth;
	private int squareSize;
	private int height;
	private Material blackSquareMat;
	private Material whiteSquareMat;
	private Material frameMat;
	private Material enclosureMat;
	private Boolean isLit;
	private Map<Integer,ChessStone> stones;
	
	BoardView(String name, ChessCraft plugin, Location where) {
		this.plugin = plugin;
		
		Configuration c = plugin.getConfiguration();
		
		this.name = name;
		game = null;	// indicates board not used by any game yet
		squareSize = c.getInt("board.square_size", 5);
		frameWidth = c.getInt("board.frame_width", 3);
		height = c.getInt("board.height", 6);
		blackSquareMat = Material.getMaterial(c.getInt("board.black_square", 49));
		whiteSquareMat = Material.getMaterial(c.getInt("board.white_square", 24));
		frameMat = Material.getMaterial(c.getInt("board.frame", 5));
		enclosureMat = Material.getMaterial(c.getInt("board.enclosure", 20));
		isLit = c.getBoolean("board.lit", false);
		a1Square = calcBaseSquare(where);
		
		stones = createStones(c.getString("board.piece_style", "Standard"));
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

	public Material getBlackSquareMat() {
		return blackSquareMat;
	}

	public Material getWhiteSquareMat() {
		return whiteSquareMat;
	}

	public Material getFrameMat() {
		return frameMat;
	}

	public Material getEnclosureMat() {
		return enclosureMat;
	}

	public Boolean getIsLit() {
		return isLit;
	}

	public Map<Integer, ChessStone> getStones() {
		return stones;
	}
	private Location calcBaseSquare(Location where) {
		int xOff = squareSize / 2;
		int zOff = squareSize / 2;
		Location res = new Location(where.getWorld(), where.getX() + xOff, where.getY() - 1, where.getZ() + zOff);

		System.out.println("standing at " + where);
		System.out.println("board origin " + res);
		return res;
	}

	private Map<Integer, ChessStone> createStones(String style) {
		Map<Integer,ChessStone> result = new HashMap<Integer,ChessStone>();
		for (int stone = Chess.MIN_STONE; stone <= Chess.MAX_STONE; stone++) {
			if (stone != Chess.NO_STONE) 
				result.put(stone, plugin.library.getStone(style, stone));
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
		int eId = enclosureMat.getId();
		World w = a1Square.getWorld();

		System.out.println("y1=" + y1 + "y2=" + y2 + "(" + x1 + "," + z1 + "), (" + x2 + "," + z2 + ")");
		// draw enclosure walls
		for (int y = y1; y < y2 && y < 127; y++) {
			for (int x = x1; x <= x2; x++) {
				w.getBlockAt(x, y, z1).setTypeId(eId);
				w.getBlockAt(x, y, z2).setTypeId(eId);
			}
			for (int z = z1; z <= z2; z++) {
				w.getBlockAt(x1, y, z).setTypeId(eId);
				w.getBlockAt(x2, y, z).setTypeId(eId);
			}
		}
		// draw enclosure roof
		if (y2 > 127) return;
		for (int x = 0; x <= x2 - x1; x++) {
			for (int z = 0; z <= z2 - z1; z++) {
				int id = isLit && height < 7 && x % squareSize == squareSize / 2 && z % squareSize == squareSize / 2 ? 89 : eId;
				w.getBlockAt(x1 + x, y2, z1 + z).setTypeId(id);
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
			int fId = isLit && f == 0 ? 89 : frameMat.getId();
			for (int x = x1 - f; x <= x2 + f; x++) {
				w.getBlockAt(x, y, z1 - f).setTypeId(fId);
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
//			paintPieceAt(i);
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
		int mId = Chess.isWhiteSquare(sqi) ? whiteSquareMat.getId() : blackSquareMat.getId();
		int cx = l.getBlockX();
		int cz = l.getBlockZ();
		World w = a1Square.getWorld();
		for (int x = 0; x < squareSize; x++) {
			for (int z = 0; z < squareSize; z++) {
				w.getBlockAt(cx + x, l.getBlockY(), cz + z).setTypeId(mId);
			}
		}
		if (isLit && height > 6) {
			w.getBlockAt(cx + squareSize / 2, l.getBlockY(), cz + squareSize / 2).setTypeId(89);
		}
		
	}

	// Return the bounds of the chessboard - the innermost ring of the frame
	private int[][] getBounds() {
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
		return rowColToWorld(row, col, 4, 4);
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
	
	int getSquareAt(Location loc) {
		if (!isOnBoard(loc, 0, height - 1)) return Chess.NO_SQUARE;
		
		int row = (a1Square.getBlockX() - loc.getBlockX()) / squareSize;
		int col = (a1Square.getBlockZ() - loc.getBlockZ()) / squareSize;
		
		return Chess.coorToSqi(col, row);
	}

	// Wipe the board's contents - generally called just before the board is deleted 
	void wipe() {
		// TODO: should really restore to previous terrain
	}
}

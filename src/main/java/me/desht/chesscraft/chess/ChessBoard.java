/**
 * Programmer: Jacob Scott
 * Program Name: ChessBoard
 * Description: for handling the chess board
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess;

import chesspresso.Chess;
import chesspresso.position.Position;

import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.pieces.ChessStone;
import me.desht.chesscraft.enums.BoardLightingMethod;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.regions.Cuboid;
import net.minecraft.server.EnumSkyBlock;
import net.minecraft.server.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;

public class ChessBoard {
	private static BoardLightingMethod lightingMethod = BoardLightingMethod.CRAFTBUKKIT;

	public static final String DEFAULT_PIECE_STYLE = "Standard";
	public static final String DEFAULT_BOARD_STYLE = "Standard";

	// region that defines the board itself - just the squares
	private Cuboid board;
	// area above the board squares
	private Cuboid areaBoard;
	// region outset by the frame
	private Cuboid frameBoard;
	 // area <i>above</i> the board
	private Cuboid aboveFullBoard;
	//the full board region (board, frame, and area above)
	private Cuboid fullBoard;
	// if highlight_last_move, what squares (indices) are highlighted
	private int fromSquare = -1, toSquare = -1;
	// if the last lighting update is active
	private boolean isLighted = false;
	// settings related to how the board is drawn
	private BoardStyle boardStyle = null;
	//the set of chess pieces that go with this board
	private ChessSet chessPieceSet = null;
	//this is the direction white faces
	private BoardOrientation rotation = BoardOrientation.NORTH;
	// the center of the A1 square (lower-left on the board)
	private Location a1Center = null;
	// the lower-left-most part (outer corner) of the a1 square (depends on rotation)
	private Location a1Corner = null;
	//the upper-right-most part (outer corner) of the h8 square (depends on rotation)
	private Location h8Corner = null;
	// are we in designer mode?
	private boolean designing = false;
	
	//	/**
	//	 * if a chess board has been drawn, this is a save for paintAll()
	//	 */
	//	protected Position chessGameCallback = null;
	// </editor-fold>

	public ChessBoard(String boardStyleStr, String pieceStyleStr) throws ChessException {
		//		this.boardStyleStr = boardStyleStr;
		//		this.pieceStyleStr = pieceStyleStr;
		setBoardStyle(boardStyleStr);
		setPieceStyle(pieceStyleStr != null ? pieceStyleStr : boardStyle.pieceStyleName);
	}

	// <editor-fold defaultstate="collapsed" desc="Accessors">
	public Location getA1Center() {
		return a1Center == null ? null : a1Center.clone();
	}

	/**
	 * @return the outer-most corner of the A1 square
	 */
	public Location getA1Corner() {
		return a1Corner == null ? null : a1Corner.clone();
	}

	/**
	 * @return the outer-most corner of the H8 square
	 */
	public Location getH8Corner() {
		return h8Corner == null ? null : h8Corner.clone();
	}

	/**
	 * @return the region that defines the board itself - just the squares
	 */
	public Cuboid getBoard() {
		return board != null ? board.clone() : null;
	}

	/**
	 * @return the region outset by the frame
	 */
	public Cuboid getFrameBoard() {
		return frameBoard != null ? frameBoard.clone() : null;
	}

	/**
	 * @return the the full board region (board, frame, and area above)
	 */
	public Cuboid getFullBoard() {
		return fullBoard != null ? fullBoard.clone() : null;
	}

	/**
	 * @return the name of the board style used
	 */
	public String getBoardStyleName() {
		return boardStyle != null ? boardStyle.getName() : null;
	}

	/**
	 * @return the name of the piece style being used
	 */
	public String getPieceStyleName() {
		return chessPieceSet != null ? chessPieceSet.getName() : null;
	}

	/**
	 * @return the BoardStyle object associated with this chessboard
	 */
	public BoardStyle getBoardStyle() {
		return boardStyle;
	}

	/**
	 * @return the ChessSet object associated with this chessboard
	 */
	public ChessSet getChessSet() {
		return chessPieceSet;
	}

	/**
	 * @return the direction of the board (from the white to black sides of the board)
	 */
	public BoardOrientation getRotation() {
		return rotation;
	}

	public boolean isDesigning() {
		return designing;
	}

	public void setDesigning(boolean designing) {
		this.designing = designing;
	}

	public final void setPieceStyle(String pieceStyle) throws ChessException {
		if (boardStyle == null) {
			return;
		}

		ChessSet newChessSet = ChessSet.getChessSet(pieceStyle);
		// ensure the new chess set actually fits this board
		if (newChessSet.getMaxWidth() > boardStyle.squareSize || newChessSet.getMaxHeight() > boardStyle.height) {
			throw new ChessException("Set '" + newChessSet.getName() + "' is too large for this board!");
		}

		chessPieceSet = newChessSet;
	}

	public final void setBoardStyle(String boardStyleName) throws ChessException {
		BoardStyle newStyle = BoardStyle.loadNewStyle(boardStyleName == null ? DEFAULT_BOARD_STYLE : boardStyleName);
		
		// we don't allow any changes to the board's dimensions, only changes to the appearance of the board
		if (boardStyle != null &&
				(boardStyle.frameWidth != newStyle.frameWidth ||
				boardStyle.squareSize != newStyle.squareSize ||
				boardStyle.height != newStyle.height)) {
			throw new ChessException("New board style dimensions do not match the current board dimensions");
		}
		
		boardStyle = newStyle;
	}

	/**
	 * Reload the board and piece styles in-use
	 * 
	 * @throws ChessException if board or piece style cannot be loaded
	 */
	void reloadStyles() throws ChessException {
		if (boardStyle != null) {
			setBoardStyle(boardStyle.getName());
		}
		if (chessPieceSet != null) {
			setPieceStyle(chessPieceSet.getName());
		}
	}

	// TODO: rotation & locations untested
	void setA1Center(Location a1, BoardOrientation rotation) {
		this.rotation = rotation;
		if (a1 == null) {
			// clear existing location / region data
			a1Center = null;
			a1Corner = null;
			board = null;
			areaBoard = null;
			frameBoard = null;
			aboveFullBoard = null;
			fullBoard = null;
		} else {
			a1Center = a1.clone();
			int xOff = boardStyle.squareSize / 2, zOff = xOff;
			if (rotation == BoardOrientation.NORTH) {
				// N = +, +
				a1Corner = new Location(a1.getWorld(), a1.getBlockX() + xOff,
				                        a1.getBlockY(), a1.getBlockZ() + zOff);
				h8Corner = new Location(a1.getWorld(), a1Corner.getBlockX() - boardStyle.squareSize * 8 + 1,
				                        a1Corner.getBlockY(), a1Corner.getBlockZ() - boardStyle.squareSize * 8 + 1);
			} else if (rotation == BoardOrientation.EAST) {
				// E = -, +
				a1Corner = new Location(a1.getWorld(), a1.getBlockX() - xOff,
				                        a1.getBlockY(), a1.getBlockZ() + zOff);
				h8Corner = new Location(a1.getWorld(), a1Corner.getBlockX() + boardStyle.squareSize * 8 - 1,
				                        a1Corner.getBlockY(), a1Corner.getBlockZ() - boardStyle.squareSize * 8 + 1);
			} else if (rotation == BoardOrientation.SOUTH) {
				// S = -, -
				a1Corner = new Location(a1.getWorld(), a1.getBlockX() - xOff,
				                        a1.getBlockY(), a1.getBlockZ() - zOff);
				h8Corner = new Location(a1.getWorld(), a1Corner.getBlockX() + boardStyle.squareSize * 8 - 1,
				                        a1Corner.getBlockY(), a1Corner.getBlockZ() + boardStyle.squareSize * 8 - 1);
			} else { // if (rotation == BoardOrientation.WEST) {
				// W = +, -
				a1Corner = new Location(a1.getWorld(), a1.getBlockX() + xOff,
				                        a1.getBlockY(), a1.getBlockZ() - zOff);
				h8Corner = new Location(a1.getWorld(), a1Corner.getBlockX() - boardStyle.squareSize * 8 + 1,
				                        a1Corner.getBlockY(), a1Corner.getBlockZ() + boardStyle.squareSize * 8 - 1);
			}
			board = new Cuboid(a1Corner, h8Corner);
			areaBoard = board.clone().expand(Direction.Up, boardStyle.height);
			frameBoard = (new Cuboid(a1Corner, h8Corner)).outset(Direction.Horizontal, boardStyle.frameWidth);
			aboveFullBoard = frameBoard.clone().shift(Direction.Up, 1).expand(Direction.Up, boardStyle.height - 1);
			fullBoard = frameBoard.clone().expand(Direction.Up, boardStyle.height + 1);
		}
	}

	static BoardLightingMethod getLightingMethod() {
		return lightingMethod;
	}

	static void setLightingMethod(BoardLightingMethod lightingMethod) {
		ChessBoard.lightingMethod = lightingMethod;
	}

	// </editor-fold>
	/**
	 * Paint everything!
	 * (board, frame, enclosure, control panel, lighting)
	 */
	void paintAll() {
		if (board != null) {
			if (!designing) {
				fullBoard.clear(true);
			}
			paintEnclosure();
			paintFrame();
			paintBoard();
			if (designing) {
				paintDesignIndicators();
			}
			if (fromSquare >= 0 || toSquare >= 0) {
				highlightSquares(fromSquare, toSquare);
			} else {
				forceLightUpdate();
			}
			fullBoard.initLighting();
			fullBoard.sendClientChanges();
		}
	}

	private void paintEnclosure() {
		aboveFullBoard.setWalls(boardStyle.enclosureMat.getMaterial(), boardStyle.enclosureMat.getData());

		Cuboid roof = new Cuboid(frameBoard).shift(Direction.Up, boardStyle.height + 1);
		boardStyle.enclosureMat.applyToCuboid(roof);

		if (!boardStyle.enclosureMat.equals(boardStyle.strutsMat)) {
			paintStruts();
		}
	}

	private void paintStruts() {
		// vertical struts at the frame corners
		Cuboid c = new Cuboid(frameBoard.getLowerNE());
		c.shift(Direction.Up, 1).expand(Direction.Up, boardStyle.height);
		c.set(boardStyle.strutsMat, true);
		c.shift(Direction.South, frameBoard.getSizeX() - 1);
		c.set(boardStyle.strutsMat, true);
		c.shift(Direction.West, frameBoard.getSizeZ() - 1);
		c.set(boardStyle.strutsMat, true);
		c.shift(Direction.North, frameBoard.getSizeZ() - 1);
		c.set(boardStyle.strutsMat, true);

		// horizontal struts along roof edge
		Cuboid roof = new Cuboid(frameBoard).shift(Direction.Up, boardStyle.height + 1);
		for (Block b : roof.walls()) {
			boardStyle.strutsMat.applyToBlockFast(b);
		}
	}

	private void paintFrame() {
		// TODO this paints the entire square within the frame extents, including the part that will
		// be overwritten by the actual squares.  Should really only paint the area round the outside.
		boardStyle.frameMat.applyToCuboid(frameBoard);
	}

	private void paintBoard() {
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				Cuboid sq = getSquare(row, col);
				((col + (row % 2)) % 2 == 0
						? boardStyle.blackSquareMat : boardStyle.whiteSquareMat).applyToCuboid(sq);
			}
		}
	}

	private void paintBoardSquare(int i) {
		paintBoardSquare(i / 8, i % 8);
	}

	private void paintBoardSquare(int row, int col) {
		if (board == null) {
			return;
		}
		Cuboid sq = getSquare(row, col);
		((col + (row % 2)) % 2 == 0
				? boardStyle.blackSquareMat : boardStyle.whiteSquareMat).applyToCuboid(sq);
	}

	private void highlightBoardSquare(int sqi, boolean highlight) {
		highlightBoardSquare(sqi / 8, sqi % 8, highlight);
	}

	private void highlightBoardSquare(int row, int col, boolean highlight) {
		if (board == null) {
			return;
		}
		if (!highlight) {
			paintBoardSquare(row, col);
		} else {
			Cuboid sq = getSquare(row, col);
			MaterialWithData squareHighlightColor =
					boardStyle.getHighlightMaterial(col + (row % 2) % 2 == 1);
			switch (boardStyle.getHighlightStyle()) {
			case EDGES:
				for (Block b : sq.walls()) {
					squareHighlightColor.applyToBlock(b);
				}
				break;
			case CORNERS:
				for (Block b : sq.corners()) {
					squareHighlightColor.applyToBlock(b);
				}
				break;
			case CHECKERED:
			case CHEQUERED:
				for (Block b : sq) {
					if ((b.getLocation().getBlockX() - b.getLocation().getBlockZ()) % 2 == 0) {
						squareHighlightColor.applyToBlock(b.getLocation().getBlock());
					}
				}
				break;
			}
		}
	}

	/**
	 * Paint all chess pieces according to the given ChessPresso Position.
	 * 
	 * @param chessGame
	 */
	void paintChessPieces(Position chessGame) {
		if (board == null) {
			return;
		}
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				paintChessPiece(row, col, chessGame.getStone(row * 8 + col));
			}
		}
	}

	/**
	 * Draw the chess piece represented by stone into the given row and column.
	 * 
	 * @param row
	 * @param col
	 * @param stone
	 */
	void paintChessPiece(int row, int col, int stone) {
		if (board == null) {
			return;
		}
		Cuboid p = getPieceRegion(row, col);
		p.clear(true);

		if (stone != Chess.NO_STONE) {
			ChessStone cStone = chessPieceSet.getPiece(stone);
			if (cStone != null) {
				cStone.paintInto(p, rotation);
			} else {
				ChessCraftLogger.severe("unknown piece: " + stone);
			}
		}
		
		p.sendClientChanges();
	}

	/**
	 * Board is in designer mode - paint some markers on unused squares
	 */
	private void paintDesignIndicators() {
		MaterialWithData marker = new MaterialWithData("wool:red");	// configurable?
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				if (row < 2 && col < 5) {
					continue;
				}
				Cuboid sq = getSquare(row, col).shift(Direction.Up, 1).inset(Direction.Horizontal, 1);
				sq.set(marker, true);
			}
		}
	}

	void lightBoard(boolean light) {
		lightBoard(light, false);
	}

	/**
	 * Apply lighting to the board, overriding the boardStyle's preference
	 * 
	 * @param light if the board is lit up
	 * @param force force lighting to be redone even it doesn't seem to have changed
	 */
	void lightBoard(boolean light, boolean force) {
		if (board == null) {
			return;
		}

		if (lightingMethod == BoardLightingMethod.CRAFTBUKKIT) {
			// Since calling craftbukkit methods directly is prone to failure, we'll catch
			// any possible exceptions/errors and fall back to the slower but safer method
			// of placing glowstone on the chessboard
			if (isLighted == light && force == false) {
				return;
			}
			lightArea(frameBoard);
			isLighted = true;
		}

		if (lightingMethod == BoardLightingMethod.GLOWSTONE) {
			if (isLighted == light && force == false) {
				return;
			}
			isLighted = light;
			MaterialWithData mat = new MaterialWithData(89);

			// light the NE edges of all of the squares
			Location ne = board.getLowerNE();
			int ix = 0, iz = 0, dx = boardStyle.squareSize, dz = dx,
					y = ne.getBlockY();
			switch (rotation) {
			case NORTH:
				dx = -dx;
				dz = -dz;
				ix = board.getUpperX();
				iz = board.getUpperZ();
				break;
			case EAST:
				dz = -dz;
				ix = board.getLowerX();
				iz = board.getUpperZ();
				break;
			case SOUTH:
				ix = board.getLowerX();
				iz = board.getLowerZ();
				break;
			case WEST:
				dx = -dx;
				ix = board.getUpperX();
				iz = board.getLowerZ();
			}
			// the board lights
			for (int r = 0, x = ix; r < 8; ++r, x += dx) {
				for (int c = 0, z = iz; c < 8; ++c, z += dz) {
					(isLighted ? mat
							: ((c + (r % 2)) % 2 == 0
							? boardStyle.blackSquareMat : boardStyle.whiteSquareMat)).applyToBlock(ne.getWorld().getBlockAt(x, y, z));
				}
			}
			// now for the frame
			if (!isLighted) {
				mat = boardStyle.frameMat;
			}
			Cuboid frameLight = board.clone();
			frameLight.outset(Direction.Horizontal, boardStyle.frameWidth / 2);
			int i = boardStyle.frameWidth / 2;
			for (Block b : frameLight.walls()) {
				if (i++ % boardStyle.squareSize == 0) {
					mat.applyToBlock(b);
				}
			}
		}
	}

	/**
	 * Use NMS methods to fake the lighting over the given Cuboid area.
	 * 
	 * @param area
	 */
	private void lightArea(Cuboid area) {
		try {
			//			long start = System.nanoTime();
			World w = ((CraftWorld) area.getWorld()).getHandle();
			for (Block b : area) {
				int x = b.getLocation().getBlockX();
				int y = b.getLocation().getBlockY() + 1;
				int z = b.getLocation().getBlockZ();
				while (b.getWorld().getBlockAt(x, y, z).getTypeId() > 0 && y < 128) {
					y++;
				}
				w.a(EnumSkyBlock.BLOCK, x, y, z, boardStyle.lightLevel);
			}
			//			ChessCraftLogger.info("relit area in " + (System.nanoTime() - start) + " ns");
		} catch (Throwable t) {
			ChessCraftLogger.warning("CraftBukkit-style lighting failed, falling back to glowstone: " + t.getMessage());
			lightingMethod = BoardLightingMethod.GLOWSTONE;
		}
	}

	/**
	 * Force a lighting update - if lighting is on for the board, force
	 * all lights to be redrawn.  This would be done after any operation
	 * that overwrote squares on the board (e.g. full repaint, square
	 * highlight repaint...)
	 */
	private void forceLightUpdate() {
		if (isLighted) {
			// force a lighting update
			lightBoard(true, true);
		}
	}

	/**
	 * Highlight two squares on the chessboard, erasing previous highlight, if any.
	 * Use the highlight square colors per-square color, if set,
	 * or just the global highlight block otherwise.
	 * 
	 * @param from	square index of the first square
	 * @param to	square index of the second square
	 */
	void highlightSquares(int from, int to) {
		if (board == null || boardStyle.highlightStyle == HighlightStyle.NONE) {
			return;
		}
		// erase the old highlight, if any
		if (fromSquare >= 0 || toSquare >= 0) {
			if (boardStyle.highlightStyle == HighlightStyle.LINE) {
				drawHighlightLine(fromSquare, toSquare, false);
			} else {
				paintBoardSquare(fromSquare);
				paintBoardSquare(toSquare);
			}
		}
		fromSquare = from;
		toSquare = to;

		forceLightUpdate();

		// draw the new highlight
		if (from >= 0 || to >= 0) {
			if (boardStyle.highlightStyle == HighlightStyle.LINE) {
				drawHighlightLine(fromSquare, toSquare, true);
			} else {
				highlightBoardSquare(fromSquare, true);
				highlightBoardSquare(toSquare, true);
			}
		}
	}

	/**
	 * Use Bresenham's algorithm to draw line between two squares on the board
	 *
	 * @param from	Square index of the first square
	 * @param to	Square index of the second square
	 * @param isHighlighting	True if drawing a highlight, false if erasing it
	 */
	private void drawHighlightLine(int from, int to, boolean isHighlighting) {
		if (board == null || from < 0 || to < 0 || from >= 64 || to >= 64) {
			return;
		}
		Cuboid s1 = getSquare(Chess.sqiToRow(from), Chess.sqiToCol(from));
		Cuboid s2 = getSquare(Chess.sqiToRow(to), Chess.sqiToCol(to));
		//TODO: need to differentiate rotation here, too...
		Location loc1 = s1.getRelativeBlock(s1.getSizeX() / 2, 0, s1.getSizeZ() / 2).getLocation();
		Location loc2 = s2.getRelativeBlock(s2.getSizeX() / 2, 0, s2.getSizeZ() / 2).getLocation();

		int dx = Math.abs(loc1.getBlockX() - loc2.getBlockX());
		int dz = Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
		int sx = loc1.getBlockX() < loc2.getBlockX() ? 1 : -1;
		int sz = loc1.getBlockZ() < loc2.getBlockZ() ? 1 : -1;
		int err = dx - dz;

		while (loc1.getBlockX() != loc2.getBlockX() || loc1.getBlockZ() != loc2.getBlockZ()) {
			int sqi = getSquareAt(loc1);
			MaterialWithData m = isHighlighting ? boardStyle.getHighlightMaterial(Chess.isWhiteSquare(sqi))
					: (Chess.isWhiteSquare(sqi) ? boardStyle.whiteSquareMat : boardStyle.blackSquareMat);
			m.applyToBlock(loc1.getBlock());
			int e2 = 2 * err;
			if (e2 > -dz) {
				err -= dz;
				loc1.add(sx, 0, 0);
			}
			if (e2 < dx) {
				err += dx;
				loc1.add(0, 0, sz);
			}
		}
	}

	/**
	 * Clear full area associated with this board
	 */
	void clearAll() {
		if (fullBoard != null) {
			fullBoard.clear(true);
		}
	}

	/**
	 * Get the Cuboid region for this square <i>of the chessboard itself</i>
	 * 
	 * @param row
	 * @param col
	 * @return a Cuboid representing the square
	 */
	private Cuboid getSquare(int row, int col) {
		if (board == null || !(row >= 0 && col >= 0 && row < 8 && col < 8)) {
			return null;
		}
		Cuboid sq = null;
		switch (rotation) {
		case NORTH:
			sq = new Cuboid(a1Corner.clone().add(
			                                     boardStyle.getSquareSize() * -row, 0,
			                                     boardStyle.getSquareSize() * -col));
			sq.expand(Direction.North, boardStyle.getSquareSize() - 1);
			sq.expand(Direction.East, boardStyle.getSquareSize() - 1);
			break;
		case EAST:
			sq = new Cuboid(a1Corner.clone().add(
			                                     boardStyle.getSquareSize() * col, 0,
			                                     boardStyle.getSquareSize() * -row));
			sq.expand(Direction.East, boardStyle.getSquareSize() - 1);
			sq.expand(Direction.South, boardStyle.getSquareSize() - 1);
			break;
		case SOUTH:
			sq = new Cuboid(a1Corner.clone().add(
			                                     boardStyle.getSquareSize() * row, 0,
			                                     boardStyle.getSquareSize() * col));
			sq.expand(Direction.South, boardStyle.getSquareSize() - 1);
			sq.expand(Direction.West, boardStyle.getSquareSize() - 1);
			break;
		case WEST:
			sq = new Cuboid(a1Corner.clone().add(
			                                     boardStyle.getSquareSize() * -col, 0,
			                                     boardStyle.getSquareSize() * row));
			sq.expand(Direction.West, boardStyle.getSquareSize() - 1);
			sq.expand(Direction.North, boardStyle.getSquareSize() - 1);
		}
		return sq;
	}

	/**
	 * Get the region above a square into which a chesspiece gets drawn
	 * 
	 * @param row	the board row
	 * @param col	the board column
	 * @return		a Cuboid representing the drawing space
	 */
	private Cuboid getPieceRegion(int row, int col) {
		if (board == null) {
			return null;
		}

		Cuboid sq = getSquare(row, col);
		sq.expand(Direction.Up, boardStyle.height - 1).shift(Direction.Up, 1);

		return sq;
	}

	/**
	 * Get the Chesspresso index of the square clicked
	 * 
	 * @param loc location that was clicked
	 * @return the square index, or Chess.NO_SQUARE if not on the board
	 */
	int getSquareAt(Location loc) {
		if (board == null || !areaBoard.contains(loc)) {
			return Chess.NO_SQUARE;
		}
		int row = 0, col = 0;
		switch (rotation) {
		case NORTH:
			row = 7 - ((loc.getBlockX() - areaBoard.getLowerX()) / boardStyle.squareSize);
			col = 7 - ((loc.getBlockZ() - areaBoard.getLowerZ()) / boardStyle.squareSize);
			break;
		case EAST:
			row = 7 - ((loc.getBlockZ() - areaBoard.getLowerZ()) / boardStyle.squareSize);
			col = -((areaBoard.getLowerX() - loc.getBlockX()) / boardStyle.squareSize);
			break;
		case SOUTH:
			row = -((areaBoard.getLowerX() - loc.getBlockX()) / boardStyle.squareSize);
			col = -((areaBoard.getLowerZ() - loc.getBlockZ()) / boardStyle.squareSize);
			break;
		case WEST:
			row = -((areaBoard.getLowerZ() - loc.getBlockZ()) / boardStyle.squareSize);
			col = 7 - ((loc.getBlockX() - areaBoard.getLowerX()) / boardStyle.squareSize);
			break;
		}
		//		System.out.println(rotation + ": " + row + " " + col);
		return row * 8 + col;
	}
} // end class ChessBoard


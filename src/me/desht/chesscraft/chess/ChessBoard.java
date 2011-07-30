/**
 * Programmer: Jacob Scott
 * Program Name: ChessBoard
 * Description: for handling the chess board
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess;

import chesspresso.Chess;
import chesspresso.position.Position;
import java.io.File;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.pieces.ChessPieceLibrary;
import me.desht.chesscraft.chess.pieces.ChessStone;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.Location;

public class ChessBoard {

	public static final String DEFAULT_PIECE_STYLE = "Standard",
			DEFAULT_BOARD_STYLE = "Standard";
// <editor-fold defaultstate="collapsed" desc="Variables">
	//cuboid regions of areas on the board
	/**
	 * region that defines the board itself - just the squares
	 */
	private Cuboid board;
	/**
	 * area above the board squares
	 */
	private Cuboid areaBoard;
	/**
	 * region outset by the frame
	 */
	private Cuboid frameBoard;
	/**
	 * area <i>above</i> the board
	 */
	private Cuboid aboveFullBoard;
	/**
	 * the full board region (board, frame, and area above)
	 */
	private Cuboid fullBoard;
	/**
	 * local save of the style names
	 */
	private String boardStyleStr, pieceStyleStr;
	/**
	 * if highlight_last_move, what squares (indices) are highlighted
	 */
	private int fromSquare = -1, toSquare = -1;
	/**
	 * if the last lighting update is active
	 */
	private boolean isLighted = false;
	/**
	 * settings related to how the board is drawn
	 */
	private BoardStyle boardStyle = null;
	/**
	 * the set of chess pieces that go with this board
	 */
	private ChessSet chessPieceSet = null;
	/**
	 * this is which side white is on
	 */
	private BoardOrientation rotation = BoardOrientation.NORTH;
	/**
	 * the center of the A1 square (lower-left on the board)
	 */
	private Location a1Center = null;
	/**
	 * the lower-left-most part (outer corner) of the a1 square (depends on rotation)
	 */
	private Location a1Corner = null;
	/**
	 * the upper-right-most part (outer corner) of the h8 square (depends on rotation)
	 */
	private Location h8Corner = null;
	// </editor-fold>

	public ChessBoard(File boardStyleFolder, File pieceStyleFolder,
			String boardStyleStr, String pieceStyleStr) throws ChessException {
		this.boardStyleStr = boardStyleStr;
		this.pieceStyleStr = pieceStyleStr;
		setBoardStyle(boardStyleFolder, boardStyleStr);
		setPieceStyle(pieceStyleFolder,
				pieceStyleStr != null ? pieceStyleStr : boardStyle.pieceStyleStr);
	}

	// <editor-fold defaultstate="collapsed" desc="Accessors">
	public Location getA1Center() {
		return a1Center;
	}

	public Location getA1Corner() {
		return a1Corner;
	}

	public Location getH8Corner() {
		return h8Corner;
	}

	public Cuboid getBoard() {
		return board != null ? board.clone() : null;
	}

	public Cuboid getFrameBoard() {
		return frameBoard != null ? frameBoard.clone() : null;
	}

	public Cuboid getFullBoard() {
		return fullBoard != null ? fullBoard.clone() : null;
	}

	public String getBoardStyleStr() {
		return boardStyleStr;
	}

	public String getPieceStyleStr() {
		return pieceStyleStr;
	}

	public BoardStyle getBoardStyle() {
		return boardStyle;
	}

	public ChessSet getChessSet() {
		return chessPieceSet;
	}

	public BoardOrientation getRotation() {
		return rotation;
	}

	// </editor-fold>
	// <editor-fold defaultstate="collapsed" desc="Modifiers">
	public final void setPieceStyle(File pieceStyleFolder, String pieceStyle) throws ChessException {
		this.pieceStyleStr = pieceStyle == null ? DEFAULT_PIECE_STYLE : pieceStyle;
		chessPieceSet = ChessPieceLibrary.getChessSet(pieceStyleStr);
		if (chessPieceSet == null) {
			chessPieceSet = ChessPieceLibrary.loadChessSet(pieceStyleFolder, pieceStyle);
		}
		if (boardStyle != null) {
			/**
			 * Overall sanity checking on board parameters
			 * @throws ChessException if any of the piece sizes are too big
			 */
			int maxH = -1, maxV = -1;
			for (ChessStone c : chessPieceSet) {
				maxH = Math.max(maxH, c.getSizeX());
				maxH = Math.max(maxH, c.getSizeZ());
				maxV = Math.max(maxV, c.getSizeY());
			}

			if (maxH > boardStyle.squareSize) {
				throw new ChessException("Set '" + pieceStyle + "' is too wide for this board!");
			}
			if (maxV > boardStyle.height) {
				throw new ChessException("Set '" + pieceStyle + "' is too tall for this board!");
			}

		}
	}

	public final void setBoardStyle(File boardStyleFolder, String boardStyle) throws ChessException {
		this.boardStyleStr = boardStyle == null ? DEFAULT_BOARD_STYLE : boardStyle;
		try {
			this.boardStyle = BoardStyle.loadNewStyle(boardStyleFolder, boardStyle);
		} catch (Exception e) {
			ChessCraftLogger.severe("can't load board style " + boardStyle, e);
			throw new ChessException("Board style '" + boardStyle + "' is not available.");
		}
	}

	public void reloadStyles(File boardStyleFolder, File pieceStyleFolder) throws ChessException {
		setBoardStyle(boardStyleFolder, boardStyleStr);
		setPieceStyle(pieceStyleFolder,
				pieceStyleStr != null ? pieceStyleStr : boardStyle.pieceStyleStr);
	}

	public void setA1Center(Location a1) {
		setA1Center(a1, this.rotation);
	}

	// TODO: rotation & locations untested
	public void setA1Center(Location a1, BoardOrientation rotation) {
		if (a1 != null) {
			// apply new rotation to the pieces
			int pieceRotate = 0;
			if (this.rotation != rotation) {

				if ((this.rotation == BoardOrientation.NORTH && rotation == BoardOrientation.EAST)
						|| (this.rotation == BoardOrientation.EAST && rotation == BoardOrientation.SOUTH)
						|| (this.rotation == BoardOrientation.SOUTH && rotation == BoardOrientation.WEST)
						|| (this.rotation == BoardOrientation.WEST && rotation == BoardOrientation.NORTH)) {
					pieceRotate = 90;
				} else if ((this.rotation == BoardOrientation.NORTH && rotation == BoardOrientation.SOUTH)
						|| (this.rotation == BoardOrientation.EAST && rotation == BoardOrientation.WEST)
						|| (this.rotation == BoardOrientation.SOUTH && rotation == BoardOrientation.NORTH)
						|| (this.rotation == BoardOrientation.WEST && rotation == BoardOrientation.EAST)) {
					pieceRotate = 180;
				} else if ((this.rotation == BoardOrientation.NORTH && rotation == BoardOrientation.WEST)
						|| (this.rotation == BoardOrientation.EAST && rotation == BoardOrientation.NORTH)
						|| (this.rotation == BoardOrientation.SOUTH && rotation == BoardOrientation.EAST)
						|| (this.rotation == BoardOrientation.WEST && rotation == BoardOrientation.SOUTH)) {
					pieceRotate = 270;
				}
				for (ChessStone c : chessPieceSet) {
					c.rotate(pieceRotate);
				}
				this.rotation = rotation;
			}
		}
		a1Center = a1.clone();

		if (a1 == null) {
			// clear existing location / region data
			a1Corner = null;
			board = null;
			areaBoard = null;
			frameBoard = null;
			aboveFullBoard = null;
			fullBoard = null;
		} else {
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
			areaBoard = board.clone().expand(Direction.Up, boardStyle.height - 1);
			frameBoard = (new Cuboid(a1Corner, h8Corner)).outset(Direction.Horizontal, boardStyle.frameWidth - 1);
			aboveFullBoard = frameBoard.clone().shift(Direction.Up, 1).expand(Direction.Up, boardStyle.height - 1);
			fullBoard = frameBoard.clone().expand(Direction.Up, boardStyle.height - 1);
		}
	}

	// </editor-fold>
	/**
	 * paint whole board
	 * (board, frame, enclosure, control panel, lighting)
	 */
	public void paintAll() {
		if (board != null) {
			clearAll();
			paintEnclosure();
			paintBoard();
			paintFrame();
			if (fromSquare >= 0 || toSquare >= 0) {
				highlightSquares(fromSquare, toSquare);
			} else {
				forceLightUpdate();
			}
		}
	}

	public void paintEnclosure() {
		if (board == null) {
			return;
		}
		aboveFullBoard.setWalls(boardStyle.enclosureMat.getMaterial(), boardStyle.enclosureMat.getData());

		Cuboid roof = new Cuboid(aboveFullBoard.getUpperSW(),
				new Location(aboveFullBoard.getWorld(), aboveFullBoard.getLowerNE().getBlockX(),
				aboveFullBoard.getUpperSW().getBlockY(), aboveFullBoard.getLowerNE().getBlockZ()));
		for (Location l : roof) {
			boardStyle.enclosureMat.applyToBlock(l.getBlock());
		}
	}

	public void paintFrame() {
		if (board == null) {
			return;
		}
		for (Location l : frameBoard) {
			if (!board.contains(l)) {
				boardStyle.frameMat.applyToBlock(l.getBlock());
			}
		}
	}

	public void paintBoard() {
		if (board == null) {
			return;
		}
		// TODO? time methods for most optimal
//		for (int row = 0; row < 8; ++row) {
//			int firstBlack = row % 2;
//			for (int r = 0; r < boardStyle.squareSize; ++r) {
//				for (int col = 0; col < 8; ++col) {
//					MaterialWithData m = (col + firstBlack) % 2 == 0
//							? boardStyle.blackSquareMat : boardStyle.whiteSquareMat;
//					for (int c = 0; c < boardStyle.squareSize; ++c) {
//						//TODO: chessboard square direction determined by rotation
//					}
//				}
//			}
//		}
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				Cuboid sq = getSquare(row, col);
				((col + (row % 2)) % 2 == 0
						? boardStyle.blackSquareMat : boardStyle.whiteSquareMat).applyToCuboid(sq);
			}
		}
	}

	public void paintBoardSquare(int i) {
		paintBoardSquare(i / 8, i % 8);
	}

	public void paintBoardSquare(int row, int col) {
		if (board == null) {
			return;
		}
		Cuboid sq = getSquare(row, col);
		((col + (row % 2)) % 2 == 0
				? boardStyle.blackSquareMat : boardStyle.whiteSquareMat).applyToCuboid(sq);
	}

	public void highlightBoardSquare(int i, boolean highlight) {
		highlightBoardSquare(i / 8, i % 8, highlight);
	}

	public void highlightBoardSquare(int row, int col, boolean highlight) {
		if (board == null) {
			return;
		}
		if (!highlight) {
			paintBoardSquare(row, col);
		} else {
			Cuboid sq = getSquare(row, col);
			MaterialWithData squareHighlightColor =
					boardStyle.getHighlightMaterial(col + (row % 2) % 2 == 1);
			switch (boardStyle.highlightStyle) {
				case EDGES:
					for (Location loc : sq.walls()) {
						squareHighlightColor.applyToBlock(loc.getBlock());
					}
					break;
				case CORNERS:
					for (Location loc : sq.corners()) {
						squareHighlightColor.applyToBlock(loc.getBlock());
					}
					break;
				case CHECKERED:
				case CHEQUERED:
					for (Location loc : sq) {
						if ((loc.getBlockX() - loc.getBlockZ()) % 2 == 0) {
							squareHighlightColor.applyToBlock(loc.getBlock());
						}
					}
					break;
			}
		}
	}

	public void paintChessPieces(Position chessGame) {
		if (board == null) {
			return;
		}
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				paintChessPiece(row, col, chessGame.getStone(row * 8 + col));
			}
		}
	}

	public void paintChessPiece(int row, int col, int stone) {
		if (board == null) {
			return;
		}
		Cuboid p = getPieceRegion(row, col);
		p.clear();

		if (stone != Chess.NO_STONE) {
			ChessStone cStone = chessPieceSet.getPiece(stone);
			if (cStone != null) {
				cStone.paintInto(p);
			} else {
				ChessCraftLogger.severe("unknown piece: " + stone);
			}
		}
	}

	/**
	 * applies lighting to the board <br>
	 * - overrides the boardStyle's preference
	 * @param light if the board is lit up
	 */
	public void lightBoard(boolean light) {
		if (isLighted == light || board == null) {
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
		for (Location l : frameLight.walls()) {
			if (i++ % boardStyle.squareSize == 0) {
				mat.applyToBlock(l.getBlock());
			}
		}
	}

	public void forceLightUpdate() {
		if (isLighted) {
			// force a lighting update
			lightBoard(isLighted = false);
		}
	}
//	
//	private void setFrameLights(MaterialWithData mat) {
//		Location l = getBounds().getLowerNE();
//		l.add(squareSize / 2 + 1, 0, 0);
//		int boardSize = squareSize * 8 + 1;
//		// east & west sides
//		for (int i = 0; i < 8; ++i) {
//			mat.applyToBlock(l.getBlock());
//			l.add(0, 0, boardSize);
//			mat.applyToBlock(l.getBlock());
//			l.add(squareSize, 0, -boardSize);
//		}
//		// north & south sides
//		l = getBounds().getLowerNE();
//		l.add(0, 0, squareSize / 2 + 1);
//		for (int i = 0; i < 8; ++i) {
//			mat.applyToBlock(l.getBlock());
//			l.add(boardSize, 0, 0);
//			mat.applyToBlock(l.getBlock());
//			l.add(-boardSize, 0, squareSize);
//		}
//	}

	public void highlightSquares(int from, int to) {
		if (board == null || boardStyle.highlightStyle == HighlightStyle.NONE) {
			return;
		}
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

		if (boardStyle.highlightStyle == HighlightStyle.LINE) {
			drawHighlightLine(fromSquare, toSquare, true);
		} else {
			highlightBoardSquare(fromSquare, true);
			highlightBoardSquare(toSquare, true);
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
		//TODO: need to diferintiate rotation here, too...
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
	 * clear the board of pieces
	 */
	public void clearBoard() {
		if (board != null) {
			Cuboid toClear = board.clone();
			toClear.shift(Direction.Up, 1);
			toClear.expand(Direction.Up, boardStyle.height - 1);
			toClear.clear();
		}
	}

	/**
	 * clear full area associated with this board
	 */
	public void clearAll() {
		if (fullBoard != null) {
			fullBoard.clear();
		}
	}

	/**
	 * get the cuboid region for this square
	 * @param row
	 * @param col
	 * @return the region associated with this square
	 */
	public Cuboid getSquare(int row, int col) {
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
	 * get the region above a square where a chesspiece gets put
	 * @param row
	 * @param col
	 * @return
	 */
	public Cuboid getPieceRegion(int row, int col) {
		if (board == null) {
			return null;
		}
		// copy-paste of above, but with height
		Cuboid sq = null;
		switch (rotation) {
			case NORTH:
				sq = new Cuboid(a1Corner.clone().add(
						boardStyle.getSquareSize() * -row, boardStyle.height - 1,
						boardStyle.getSquareSize() * -col));
				sq.expand(Direction.North, boardStyle.getSquareSize() - 1);
				sq.expand(Direction.East, boardStyle.getSquareSize() - 1);
				break;
			case EAST:
				sq = new Cuboid(a1Corner.clone().add(
						boardStyle.getSquareSize() * col, boardStyle.height - 1,
						boardStyle.getSquareSize() * -row));
				sq.expand(Direction.East, boardStyle.getSquareSize() - 1);
				sq.expand(Direction.South, boardStyle.getSquareSize() - 1);
				break;
			case SOUTH:
				sq = new Cuboid(a1Corner.clone().add(
						boardStyle.getSquareSize() * row, boardStyle.height - 1,
						boardStyle.getSquareSize() * col));
				sq.expand(Direction.South, boardStyle.getSquareSize() - 1);
				sq.expand(Direction.West, boardStyle.getSquareSize() - 1);
				break;
			case WEST:
				sq = new Cuboid(a1Corner.clone().add(
						boardStyle.getSquareSize() * -col, boardStyle.height - 1,
						boardStyle.getSquareSize() * row));
				sq.expand(Direction.West, boardStyle.getSquareSize() - 1);
				sq.expand(Direction.North, boardStyle.getSquareSize() - 1);
		}
		sq.shift(Direction.Up, 1);
		return sq;
	}

	/**
	 * gets the index of the square clicked
	 * @param loc location to check
	 * @return the square index, or -1 if not on the board
	 */
	public int getSquareAt(Location loc) {
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
				col = 7 - ((areaBoard.getLowerX() - loc.getBlockX()) / boardStyle.squareSize);
				break;
			case SOUTH:
				row = 7 - ((areaBoard.getLowerX() - loc.getBlockX()) / boardStyle.squareSize);
				col = 7 - ((areaBoard.getLowerZ() - loc.getBlockZ()) / boardStyle.squareSize);
				break;
			case WEST:
				row = 7 - ((areaBoard.getLowerZ() - loc.getBlockZ()) / boardStyle.squareSize);
				col = 7 - ((loc.getBlockX() - areaBoard.getLowerX()) / boardStyle.squareSize);
				break;
		}
		//System.out.println(rotation + ": " + row + " " + col);
		return row * 8 + col;
	}
} // end class ChessBoard


/**
 * Programmer: Jacob Scott
 * Program Name: ChessBoard
 * Description: for handling the chess board
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import chesspresso.Chess;
import chesspresso.position.Position;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.chess.pieces.ChessStone;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class ChessBoard {

	// the center of the A1 square (lower-left on the board)
	private final Location a1Center;
	// the lower-left-most part (outer corner) of the a1 square (depends on rotation)
	private final Location a1Corner;
	// the upper-right-most part (outer corner) of the h8 square (depends on rotation)
	private final Location h8Corner;
	// region that defines the board itself - just the squares
	private final Cuboid board;
	// area above the board squares
	private final Cuboid areaBoard;
	// region outset by the frame
	private final Cuboid frameBoard;
	// area <i>above</i> the board
	private final Cuboid aboveFullBoard;
	// the full board region (board, frame, and area above)
	private final Cuboid fullBoard;
	// this is the direction white faces
	private final BoardRotation rotation;

	// if highlight_last_move, what squares (indices) are highlighted
	private int fromSquare = -1, toSquare = -1;
	// settings related to how the board is drawn
	private BoardStyle boardStyle = null;
	// the set of chess pieces that go with this board
	private ChessSet chessPieceSet = null;
	// are we in designer mode?
	private PieceDesigner designer = null;

	/**
	 * Board constructor.
	 * 
	 * @param origin
	 * @param rotation
	 * @param boardStyleName
	 * @param pieceStyleName
	 * @throws ChessException
	 */
	public ChessBoard(Location origin, BoardRotation rotation, String boardStyleName, String pieceStyleName) throws ChessException {
		setBoardStyle(boardStyleName);
		setPieceStyle(pieceStyleName != null ? pieceStyleName : boardStyle.getPieceStyleName());
		this.rotation = rotation;
		a1Center = origin;
		a1Corner = initA1Corner(origin, rotation);
		h8Corner = initH8Corner(a1Corner);
		board = new Cuboid(a1Corner, h8Corner);
		areaBoard = board.expand(Direction.Up, boardStyle.getHeight());
		frameBoard = board.outset(Direction.Horizontal, boardStyle.getFrameWidth());
		aboveFullBoard = frameBoard.shift(Direction.Up, 1).expand(Direction.Up, boardStyle.getHeight() - 1);
		fullBoard = frameBoard.expand(Direction.Up, boardStyle.getHeight() + 1);
		validateBoardPosition();
	}

	private Location initA1Corner(Location origin, BoardRotation rotation) {
		Location a1 = new Location(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
		int offset = boardStyle.getSquareSize() / 2;
		switch (rotation) {
		case NORTH:
			a1.add(offset, 0, offset); break;
		case EAST:
			a1.add(-offset, 0, offset); break;
		case SOUTH:
			a1.add(-offset, 0, -offset); break;
		case WEST:
			a1.add(offset, 0, -offset); break;
		}
		return a1;
	}

	private Location initH8Corner(Location a1) {
		Location h8 = new Location(a1.getWorld(), a1.getBlockX(), a1.getBlockY(), a1.getBlockZ());
		int size = boardStyle.getSquareSize();
		switch (rotation) {
		case NORTH:
			h8.add(-size * 8 + 1, 0, -size * 8 + 1); break;
		case EAST:
			h8.add(size * 8 - 1, 0, -size * 8 + 1); break;
		case SOUTH:
			h8.add(size * 8 - 1, 0, size * 8 - 1); break;
		case WEST:
			h8.add(-size * 8 + 1, 0, size * 8 - 1); break;
		}
		return h8;
	}

	/**
	 * Ensure this board isn't built too high and doesn't intersect any other boards
	 * 
	 * @throws ChessException if an intersection would occur
	 */
	private void validateBoardPosition() throws ChessException {
		Cuboid bounds = getFullBoard();

		if (bounds.getUpperSW().getBlock().getLocation().getY() > bounds.getUpperSW().getWorld().getMaxHeight()) {
			throw new ChessException(Messages.getString("BoardView.boardTooHigh")); //$NON-NLS-1$
		}
		for (BoardView bv : BoardView.listBoardViews()) {
			if (bv.getA1Square().getWorld() != bounds.getWorld()) {
				continue;
			}
			for (Block b : bounds.corners()) {
				if (bv.getOuterBounds().contains(b)) {
					throw new ChessException(Messages.getString("BoardView.boardWouldIntersect", bv.getName())); //$NON-NLS-1$
				}
			}
		}
	}

	public Location getA1Center() {
		return a1Center.clone();
	}

	/**
	 * @return the outer-most corner of the A1 square
	 */
	public Location getA1Corner() {
		return a1Corner.clone();
	}

	/**
	 * @return the outer-most corner of the H8 square
	 */
	public Location getH8Corner() {
		return h8Corner.clone();
	}

	/**
	 * @return the region that defines the board itself - just the squares
	 */
	public Cuboid getBoard() {
		return board;
	}

	/**
	 * @return the region outset by the frame
	 */
	public Cuboid getFrameBoard() {
		return frameBoard;
	}

	/**
	 * @return the the full board region (board, frame, and area above)
	 */
	public Cuboid getFullBoard() {
		return fullBoard;
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
	 * @return the direction of the board (from the white to black sides of the
	 *         board)
	 */
	public BoardRotation getRotation() {
		return rotation;
	}

	public boolean isDesiging() {
		return designer != null;
	}

	public PieceDesigner getDesigner() {
		return designer;
	}

	public void setDesigner(PieceDesigner designer) {
		this.designer = designer;
	}

	public final void setPieceStyle(String pieceStyle) throws ChessException {
		if (boardStyle == null) {
			return;
		}

		ChessSet newChessSet = ChessSet.getChessSet(pieceStyle);
		boardStyle.verifyCompatibility(newChessSet);

		chessPieceSet = newChessSet;
	}

	public final void setBoardStyle(String boardStyleName) throws ChessException {
		BoardStyle newStyle = BoardStyle.loadStyle(boardStyleName);

		// We don't allow any changes to the board's dimensions, only changes to
		// the appearance of the board.
		if (boardStyle != null &&
				(boardStyle.getFrameWidth() != newStyle.getFrameWidth() ||
				boardStyle.getSquareSize() != newStyle.getSquareSize() ||
				boardStyle.getHeight() != newStyle.getHeight())) {
			throw new ChessException("New board style dimensions do not match the current board dimensions");
		}

		boardStyle = newStyle;
		chessPieceSet = ChessSet.getChessSet(boardStyle.getPieceStyleName());
	}

	/**
	 * Reload the board and piece styles in-use
	 * 
	 * @throws ChessException
	 *             if board or piece style cannot be loaded
	 */
	void reloadStyles() throws ChessException {
		if (boardStyle != null) {
			setBoardStyle(boardStyle.getName());
		}
		if (chessPieceSet != null) {
			setPieceStyle(chessPieceSet.getName());
		}
	}

	/**
	 * Paint everything! (board, frame, enclosure, control panel, lighting)
	 */
	void paintAll() {
		if (designer == null) {
			fullBoard.clear(true);
		}
		paintEnclosure();
		paintFrame();
		paintBoard();
		if (designer != null) {
			paintDesignIndicators();
		}
		if (fromSquare >= 0 || toSquare >= 0) {
			highlightSquares(fromSquare, toSquare);
		}
		fullBoard.forceLightLevel(boardStyle.getLightLevel());
		fullBoard.sendClientChanges();
	}

	private void paintEnclosure() {
		aboveFullBoard.getFace(Direction.North).setFast(boardStyle.getEnclosureMaterial());
		aboveFullBoard.getFace(Direction.East).setFast(boardStyle.getEnclosureMaterial());
		aboveFullBoard.getFace(Direction.South).setFast(boardStyle.getEnclosureMaterial());
		aboveFullBoard.getFace(Direction.West).setFast(boardStyle.getEnclosureMaterial());

		fullBoard.getFace(Direction.Up).setFast(boardStyle.getEnclosureMaterial());

		if (!boardStyle.getEnclosureMaterial().equals(boardStyle.getStrutsMaterial())) {
			paintStruts();
		}
	}

	private void paintStruts() {
		MaterialWithData struts = boardStyle.getStrutsMaterial();

		// vertical struts at the frame corners
		Cuboid c = new Cuboid(frameBoard.getLowerNE()).shift(Direction.Up, 1).expand(Direction.Up, boardStyle.getHeight());
		c.setFast(struts);
		c = c.shift(Direction.South, frameBoard.getSizeX() - 1);
		c.setFast(struts);
		c = c.shift(Direction.West, frameBoard.getSizeZ() - 1);
		c.setFast(struts);
		c = c.shift(Direction.North, frameBoard.getSizeZ() - 1);
		c.setFast(struts);

		// horizontal struts along roof edge
		Cuboid roof = frameBoard.shift(Direction.Up, boardStyle.getHeight() + 1);
		roof.getFace(Direction.East).setFast(struts);
		roof.getFace(Direction.North).setFast(struts);
		roof.getFace(Direction.West).setFast(struts);
		roof.getFace(Direction.South).setFast(struts);

	}

	private void paintFrame() {
		int fw = boardStyle.getFrameWidth();
		MaterialWithData fm = boardStyle.getFrameMaterial();
		frameBoard.getFace(Direction.West).expand(Direction.East, fw - 1).setFast(fm);
		frameBoard.getFace(Direction.South).expand(Direction.North, fw - 1).setFast(fm);
		frameBoard.getFace(Direction.East).expand(Direction.West, fw - 1).setFast(fm);
		frameBoard.getFace(Direction.North).expand(Direction.South, fw - 1).setFast(fm);
	}

	private void paintBoard() {
		for (int sqi = 0; sqi < Chess.NUM_OF_SQUARES; sqi++	) {
			paintBoardSquare(sqi);
		}
	}

	private void paintBoardSquare(int sqi) {
		paintBoardSquare(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi));
	}

	private void paintBoardSquare(int row, int col) {
		Cuboid square = getSquare(row, col);
		boolean black = (col + (row % 2)) % 2 == 0;
		square.setFast(black ? boardStyle.getBlackSquareMaterial() : boardStyle.getWhiteSquareMaterial());
		square.sendClientChanges();
	}

	private void highlightBoardSquare(int sqi, boolean highlight) {
		highlightBoardSquare(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi), highlight);
	}

	private void highlightBoardSquare(int row, int col, boolean highlight) {
		if (!highlight) {
			paintBoardSquare(row, col);
		} else {
			Cuboid sq = getSquare(row, col);
			MaterialWithData squareHighlightColor = boardStyle.getHighlightMaterial(col + (row % 2) % 2 == 1);
			switch (boardStyle.getHighlightStyle()) {
			case EDGES:
				sq.getFace(Direction.East).set(squareHighlightColor);
				sq.getFace(Direction.North).set(squareHighlightColor);
				sq.getFace(Direction.West).set(squareHighlightColor);
				sq.getFace(Direction.South).set(squareHighlightColor);
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
	 * Paint all chess pieces according to the given Chesspresso Position.
	 * 
	 * @param chessGame
	 */
	void paintChessPieces(Position chessGame) {
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				paintChessPiece(row, col, chessGame.getStone(row * 8 + col));
			}
		}
	}

	/**
	 * Draw the chess piece represented by stone into the given row and column.  The actual blocks
	 * drawn depend on the board's current chess set.
	 * 
	 * @param row
	 * @param col
	 * @param stone
	 */
	public void paintChessPiece(int row, int col, int stone) {
		Cuboid region = getPieceRegion(row, col);
		region.clear(true);
		if (stone != Chess.NO_STONE) {
			ChessStone cStone = chessPieceSet.getStone(stone, getRotation());
			if (cStone != null) {
				paintChessPiece(region, cStone);
			} else {
				LogUtils.severe("unknown piece: " + stone);
			}
		}

		region.expand(Direction.Down, 1).forceLightLevel(boardStyle.getLightLevel());		
		region.sendClientChanges();
	}

	private void paintChessPiece(Cuboid region, ChessStone stone) {
		assert region.getSizeX() >= stone.getSizeX();
		assert region.getSizeZ() >= stone.getSizeZ();

		int xOff = (region.getSizeX() - stone.getSizeX()) / 2;
		int zOff = (region.getSizeZ() - stone.getSizeZ()) / 2;

		Map<Block,MaterialWithData> deferred = new HashMap<Block, MaterialWithData>();
		for (int x = 0; x < stone.getSizeX(); x++) {
			for (int y = 0; y < stone.getSizeY(); y++) {
				for (int z = 0; z < stone.getSizeZ(); z++) {
					MaterialWithData mat = stone.getMaterial(x, y, z);
					if (mat.getMaterial() == 0) {
						// the region was pre-cleared, skip placing air a second time
						continue;
					}
					Block b = region.getRelativeBlock(x + xOff, y, z + zOff);
					if (BlockType.shouldPlaceLast(mat.getMaterial())) {
						deferred.put(b, mat);
					} else {
						mat.applyToBlockFast(b);
					}
				}	
			}	
		}

		for (Entry<Block,MaterialWithData> e : deferred.entrySet()) {
			e.getValue().applyToBlockFast(e.getKey());
		}
	}

	/**
	 * Board is in designer mode - paint some markers on unused squares
	 */
	private void paintDesignIndicators() {
		MaterialWithData marker = MaterialWithData.get("wool:red"); // configurable?
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				if (row < 2 && col < 5) {
					continue;
				}
				Cuboid sq = getSquare(row, col).shift(Direction.Up, 1).inset(Direction.Horizontal, 1);
				sq.setFast(marker);
			}
		}
	}

	/**
	 * Highlight two squares on the chessboard, erasing previous highlight, if
	 * any. Use the highlight square colors per-square color, if set, or just
	 * the global highlight block otherwise.
	 * 
	 * @param from	square index of the first square
	 * @param to	square index of the second square
	 */
	void highlightSquares(int from, int to) {
		if (boardStyle.getHighlightStyle() == HighlightStyle.NONE) {
			return;
		}
		// erase the old highlight, if any
		if (fromSquare >= 0 || toSquare >= 0) {
			if (boardStyle.getHighlightStyle() == HighlightStyle.LINE) {
				drawHighlightLine(fromSquare, toSquare, false);
			} else {
				paintBoardSquare(fromSquare);
				paintBoardSquare(toSquare);
			}
		}
		fromSquare = from;
		toSquare = to;

		// draw the new highlight
		if (from >= 0 || to >= 0) {
			if (boardStyle.getHighlightStyle() == HighlightStyle.LINE) {
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
		if (from < 0 || to < 0 || from >= 64 || to >= 64) {
			return;
		}

		Cuboid s1 = getSquare(Chess.sqiToRow(from), Chess.sqiToCol(from));
		Cuboid s2 = getSquare(Chess.sqiToRow(to), Chess.sqiToCol(to));
		Location loc1 = s1.getRelativeBlock(s1.getSizeX() / 2, 0, s1.getSizeZ() / 2).getLocation();
		Location loc2 = s2.getRelativeBlock(s2.getSizeX() / 2, 0, s2.getSizeZ() / 2).getLocation();

		int dx = Math.abs(loc1.getBlockX() - loc2.getBlockX());
		int dz = Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
		int sx = loc1.getBlockX() < loc2.getBlockX() ? 1 : -1;
		int sz = loc1.getBlockZ() < loc2.getBlockZ() ? 1 : -1;
		int err = dx - dz;

		while (loc1.getBlockX() != loc2.getBlockX() || loc1.getBlockZ() != loc2.getBlockZ()) {
			int sqi = getSquareAt(loc1);
			MaterialWithData m = isHighlighting ? 
					boardStyle.getHighlightMaterial(Chess.isWhiteSquare(sqi)) :
						(Chess.isWhiteSquare(sqi) ? boardStyle.getWhiteSquareMaterial() : boardStyle.getBlackSquareMaterial());
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
		fullBoard.clear(true);
	}

	/**
	 * Get the Cuboid region for this square <i>of the chessboard itself</i>
	 * 
	 * @param row
	 * @param col
	 * @return a Cuboid representing the square
	 */
	public Cuboid getSquare(int row, int col) {
		if (row < 0 || col < 0 || row > 7 || col > 7) {
			LogUtils.warning("ChessBoard: getSquare: bad (row, col): (" + row + "," + col + ")");
			return null;
		}
		Cuboid sq = null;
		int s = boardStyle.getSquareSize();
		switch (rotation) {
		case NORTH:
			sq = new Cuboid(a1Corner.clone().add(s * -row, 0, s * -col));
			sq = sq.expand(Direction.North, s - 1).expand(Direction.East, s - 1);
			break;
		case EAST:
			sq = new Cuboid(a1Corner.clone().add(s * col, 0, s * -row));
			sq = sq.expand(Direction.East, s - 1).expand(Direction.South, s - 1);
			break;
		case SOUTH:
			sq = new Cuboid(a1Corner.clone().add(s * row, 0, s * col));
			sq = sq.expand(Direction.South, s - 1).expand(Direction.West, s - 1);
			break;
		case WEST:
			sq = new Cuboid(a1Corner.clone().add(s * -col, 0, s * row));
			sq = sq.expand(Direction.West, s - 1).expand(Direction.North, s - 1);
		}
		return sq;
	}

	/**
	 * Get the region above a square into which a chesspiece gets drawn
	 * 
	 * @param row
	 *            the board row
	 * @param col
	 *            the board column
	 * @return a Cuboid representing the drawing space
	 */
	public Cuboid getPieceRegion(int row, int col) {
		Cuboid sq = getSquare(row, col).expand(Direction.Up, boardStyle.getHeight() - 1).shift(Direction.Up, 1);
		return sq;
	}

	/**
	 * Get the Chesspresso square index of the given location
	 * 
	 * @param loc	desired location
	 * @return the square index, or Chess.NO_SQUARE if not on the board
	 */
	int getSquareAt(Location loc) {
		if (!areaBoard.contains(loc)) {
			return Chess.NO_SQUARE;
		}
		int row = 0, col = 0;
		switch (rotation) {
		case NORTH:
			row = 7 - ((loc.getBlockX() - areaBoard.getLowerX()) / boardStyle.getSquareSize());
			col = 7 - ((loc.getBlockZ() - areaBoard.getLowerZ()) / boardStyle.getSquareSize());
			break;
		case EAST:
			row = 7 - ((loc.getBlockZ() - areaBoard.getLowerZ()) / boardStyle.getSquareSize());
			col = -((areaBoard.getLowerX() - loc.getBlockX()) / boardStyle.getSquareSize());
			break;
		case SOUTH:
			row = -((areaBoard.getLowerX() - loc.getBlockX()) / boardStyle.getSquareSize());
			col = -((areaBoard.getLowerZ() - loc.getBlockZ()) / boardStyle.getSquareSize());
			break;
		case WEST:
			row = -((areaBoard.getLowerZ() - loc.getBlockZ()) / boardStyle.getSquareSize());
			col = 7 - ((loc.getBlockX() - areaBoard.getLowerX()) / boardStyle.getSquareSize());
			break;
		}
		return Chess.coorToSqi(col, row);
	}
} // end class ChessBoard


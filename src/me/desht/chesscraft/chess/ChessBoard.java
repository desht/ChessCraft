/**
 * Programmer: Jacob Scott
 * Program Name: ChessBoard
 * Description: for wrapping up all board settings
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess;

import java.io.File;
import java.util.Map;
import me.desht.chesscraft.blocks.ChessStone;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.Location;

public class ChessBoard {

	private Cuboid board, frameBoard, fullBoard;
	private int frameWidth, squareSize, height;
	private MaterialWithData blackSquareMat, whiteSquareMat;
	private MaterialWithData enclosureMat, frameMat, controlPanelMat;
	private MaterialWithData highlightMat, highlightWhiteSquareMat, highlightBlackSquareMat;
	private HighlightStyle highlightStyle;
	private String boardStyle, pieceStyle;
	private Boolean isLit;
	/**
	 * the set of chess pieces that go with this board
	 */
	private Map<Integer, ChessStone> stones;
	/**
	 * this is which side white is on
	 */
	private BoardOrientation rotation = BoardOrientation.NORTH;
	/**
	 * the center of the A1 square (lower-left on the board)
	 */
	private Location a1Center = null;

	public ChessBoard(File boardStyleFolder, File pieceStyleFolder, String boardStyle, String pieceStyle) {
		this.boardStyle = boardStyle;
		this.pieceStyle = pieceStyle;
	}

	public Location getA1Center() {
		return a1Center;
	}

	public MaterialWithData getBlackSquareMat() {
		return blackSquareMat;
	}

	public Cuboid getBoard() {
		return board;
	}

	public String getBoardStyle() {
		return boardStyle;
	}

	public MaterialWithData getControlPanelMat() {
		return controlPanelMat;
	}

	public MaterialWithData getEnclosureMat() {
		return enclosureMat;
	}

	public Cuboid getFrameBoard() {
		return frameBoard;
	}

	public MaterialWithData getFrameMat() {
		return frameMat;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public Cuboid getFullBoard() {
		return fullBoard;
	}

	public int getHeight() {
		return height;
	}

	public HighlightStyle getHighlightStyle() {
		return highlightStyle;
	}

	public Boolean getIsLit() {
		return isLit;
	}

	public String getPieceStyle() {
		return pieceStyle;
	}

	public BoardOrientation getRotation() {
		return rotation;
	}

	public int getSquareSize() {
		return squareSize;
	}

	public Map<Integer, ChessStone> getStones() {
		return stones;
	}

	public MaterialWithData getWhiteSquareMat() {
		return whiteSquareMat;
	}

	public void setBlackSquareMat(MaterialWithData blackSquareMat) {
		if (blackSquareMat != null) {
			this.blackSquareMat = blackSquareMat;
		}
	}

	public void setWhiteSquareMat(MaterialWithData whiteSquareMat) {
		if (whiteSquareMat != null) {
			this.whiteSquareMat = whiteSquareMat;
		}
	}

	public void setBoardStyle(String boardStyle) {
		this.boardStyle = boardStyle;
	}

	public void setControlPanelMat(MaterialWithData controlPanelMat) {
		this.controlPanelMat = controlPanelMat;
	}

	public void setEnclosureMat(MaterialWithData enclosureMat) {
		if (enclosureMat == null) {
			this.enclosureMat = new MaterialWithData(0);
		} else {
			this.enclosureMat = enclosureMat;
		}
	}

	public void setIsLit(Boolean isLit) {
		this.isLit = isLit;
	}

	public void setPieceStyle(File pieceStyleFolder, String pieceStyle) {
		this.pieceStyle = pieceStyle;
		
	}

	private Map<Integer, ChessStone> createStones(String pieceStyle) throws ChessException {
		if (!plugin.library.isChessSetLoaded(pieceStyle)) {
			plugin.library.loadChessSet(pieceStyle);
		}
		Map<Integer, ChessStone> result = new HashMap<Integer, ChessStone>();

		for (int stone = Chess.MIN_STONE; stone <= Chess.MAX_STONE; ++stone) {
			if (stone != Chess.NO_STONE) {
				result.put(stone, plugin.library.getStone(pieceStyle, stone));
			}
		}
		return result;
	}

} // end class ChessBoard


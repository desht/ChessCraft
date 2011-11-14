/**
 * Programmer: Jacob Scott
 * Program Name: BoardStyle
 * Description: for wrapping up all board settings
 * Date: Jul 29, 2011
 */
package me.desht.chesscraft.chess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import org.yaml.snakeyaml.Yaml;

public class BoardStyle {

	public static final int MIN_HEIGHT = 3, MIN_FRAMEWIDTH = 2, MIN_SQUARESIZE = 1;
	public static final int MAX_HEIGHT = 128, MAX_FRAMEWIDTH = 20, MAX_SQUARESIZE = 20;
	protected int frameWidth, squareSize, height;
	protected MaterialWithData blackSquareMat, whiteSquareMat;
	protected MaterialWithData enclosureMat, frameMat, controlPanelMat;
	protected MaterialWithData highlightMat, highlightWhiteSquareMat, highlightBlackSquareMat;
	protected HighlightStyle highlightStyle;
	public boolean isLit;
	protected String styleName;
	protected File boardStyleFile;
	public String pieceStyleStr;

	// protected - have to use BoardStyle.loadNewStyle to get a new one..
	protected BoardStyle() {
	}

	public String getName() {
		return styleName;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getHeight() {
		return height;
	}

	public int getSquareSize() {
		return squareSize;
	}

	public boolean getIsLit() {
		return isLit;
	}

	public MaterialWithData getBlackSquareMaterial() {
		return blackSquareMat;
	}

	public MaterialWithData getWhiteSquareMaterial() {
		return whiteSquareMat;
	}

	public MaterialWithData getControlPanelMaterial() {
		return controlPanelMat == null ? frameMat : controlPanelMat;
	}

	public MaterialWithData getEnclosureMaterial() {
		return enclosureMat;
	}

	public MaterialWithData getFrameMaterial() {
		return frameMat;
	}

	public HighlightStyle getHighlightStyle() {
		return highlightStyle;
	}

	public MaterialWithData getHighlightMaterial() {
		return highlightMat;
	}

	public MaterialWithData getHighlightMaterial(boolean isWhiteSquare) {
		return isWhiteSquare ? getWhiteSquareHighlightMaterial() : getBlackSquareHighlightMaterial();
	}

	public MaterialWithData getBlackSquareHighlightMaterial() {
		return highlightBlackSquareMat == null ? highlightMat : highlightBlackSquareMat;
	}

	public MaterialWithData getWhiteSquareHighlightMaterial() {
		return highlightWhiteSquareMat == null ? highlightMat : highlightWhiteSquareMat;
	}

	public File getBoardStyleFile() {
		return boardStyleFile;
	}

	public void setBlackSquareMaterial(MaterialWithData blackSquareMat) {
		if (blackSquareMat != null) {
			this.blackSquareMat = blackSquareMat;
		}
	}

	public void setWhiteSquareMat(MaterialWithData whiteSquareMat) {
		if (whiteSquareMat != null) {
			this.whiteSquareMat = whiteSquareMat;
		}
	}

	public void setControlPanelMaterial(MaterialWithData controlPanelMat) {
		if (controlPanelMat != null && !BlockType.canPassThrough(controlPanelMat.getMaterial())) {
			this.controlPanelMat = controlPanelMat;
		}
	}

	public void setEnclosureMaterial(MaterialWithData enclosureMat) {
		if (enclosureMat == null) {
			this.enclosureMat = new MaterialWithData(0);
		} else {
			this.enclosureMat = enclosureMat;
		}
	}

	public void setFrameMaterial(MaterialWithData frameMat) {
		if (frameMat != null) {
			this.frameMat = frameMat;
		}
	}

	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth < MIN_FRAMEWIDTH ? MIN_FRAMEWIDTH
				: (frameWidth > MAX_FRAMEWIDTH ? MAX_FRAMEWIDTH : frameWidth);
	}

	public void setHeight(int height) {
		this.height = height < MIN_HEIGHT ? MIN_HEIGHT
				: (height > MAX_HEIGHT ? MAX_HEIGHT : height);
	}

	public void setHighlightBlackSquareMaterial(MaterialWithData highlightBlackSquareMat) {
		this.highlightBlackSquareMat = highlightBlackSquareMat;
	}

	public void setHighlightMaterial(MaterialWithData highlightMat) {
		this.highlightMat = highlightMat;
	}

	public void setHighlightStyle(HighlightStyle highlightStyle) {
		this.highlightStyle = highlightStyle;
	}

	public void setHighlightWhiteSquareMaterial(MaterialWithData highlightWhiteSquareMat) {
		this.highlightWhiteSquareMat = highlightWhiteSquareMat;
	}

	public void setSquareSize(int squareSize) {
		this.squareSize = squareSize < MIN_SQUARESIZE ? MIN_SQUARESIZE
				: (squareSize > MAX_SQUARESIZE ? MAX_SQUARESIZE : squareSize);
	}

	public void loadStyle(File boardStyleFolder, String boardStyle) throws FileNotFoundException, ChessException {
		BoardStyle st = loadNewStyle(boardStyleFolder, boardStyle);

		this.frameWidth = st.frameWidth;
		this.squareSize = st.squareSize;
		this.height = st.height;
		this.blackSquareMat = st.blackSquareMat;
		this.whiteSquareMat = st.whiteSquareMat;
		this.enclosureMat = st.enclosureMat;
		this.frameMat = st.frameMat;
		this.controlPanelMat = st.controlPanelMat;
		this.highlightMat = st.highlightMat;
		this.highlightWhiteSquareMat = st.highlightWhiteSquareMat;
		this.highlightBlackSquareMat = st.highlightBlackSquareMat;
		this.highlightStyle = st.highlightStyle;
		this.isLit = st.isLit;
		this.styleName = st.styleName;
		this.pieceStyleStr = st.pieceStyleStr;
		this.boardStyleFile = st.boardStyleFile;
	}

	public static BoardStyle loadNewStyle(File boardStyleFolder, String boardStyle)
			throws FileNotFoundException, ChessException {
		return loadNewStyle(new File(boardStyleFolder, boardStyle + ".yml"));
	}
	
	public static BoardStyle loadNewStyle(File f)
			throws FileNotFoundException, ChessException {
		Yaml yaml = new Yaml();

		FileInputStream in = new FileInputStream(f);
		@SuppressWarnings("unchecked")
		Map<String, Object> styleMap = (Map<String, Object>) yaml.load(in);

		for (String k : new String[]{"square_size", "frame_width", "height",
					"lit", "black_square", "white_square", "frame", "enclosure"}) {
			if (styleMap.get(k) == null) {
				throw new ChessException("required field '" + k + "' is missing");
			}
		}
		BoardStyle style = new BoardStyle();
		style.boardStyleFile = f;
		style.styleName = f.getName().replaceFirst("\\.yml$", "");

		style.setSquareSize((Integer) styleMap.get("square_size"));
		style.setFrameWidth((Integer) styleMap.get("frame_width"));
		style.setHeight((Integer) styleMap.get("height"));
		style.isLit = (Boolean) styleMap.get("lit");
		style.pieceStyleStr = (String) styleMap.get("piece_style");

		style.blackSquareMat = new MaterialWithData((String) styleMap.get("black_square"));
		style.whiteSquareMat = new MaterialWithData((String) styleMap.get("white_square"));
		style.frameMat = new MaterialWithData((String) styleMap.get("frame"));
		style.enclosureMat = new MaterialWithData((String) styleMap.get("enclosure"));

		/**************  added optional parameters  **************/
		if (styleMap.get("panel") != null) {
			style.controlPanelMat = new MaterialWithData((String) styleMap.get("panel"));
		}
		if (styleMap.get("highlight") != null) {
			style.highlightMat = new MaterialWithData((String) styleMap.get("highlight"));
		} else {
			style.highlightMat = new MaterialWithData(89);
		}

		if (styleMap.get("highlight_white_square") != null) {
			style.highlightWhiteSquareMat = new MaterialWithData(
					(String) styleMap.get("highlight_white_square"));
		} else {
			style.highlightWhiteSquareMat = null;
		}

		if (styleMap.get("highlight_black_square") != null) {
			style.highlightBlackSquareMat = new MaterialWithData(
					(String) styleMap.get("highlight_black_square"));
		} else {
			style.highlightBlackSquareMat = null;
		}

		style.highlightStyle = HighlightStyle.getStyle((String) styleMap.get("highlight_style"));
		if (style.highlightStyle == null) {
			style.highlightStyle = HighlightStyle.CORNERS;
		}

		return style;
	}
}

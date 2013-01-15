/**
 * Programmer: Jacob Scott
 * Program Name: BoardStyle
 * Description: for wrapping up all board settings
 * Date: Jul 29, 2011
 */
package me.desht.chesscraft.chess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class BoardStyle implements Comparable<BoardStyle> {
	public static final String DEFAULT_BOARD_STYLE = "standard";
	
	public static final int MIN_HEIGHT = 3, MIN_FRAMEWIDTH = 2, MIN_SQUARESIZE = 1;
	public static final int MAX_HEIGHT = 128, MAX_FRAMEWIDTH = 10, MAX_SQUARESIZE = 30;
	
	private final boolean isCustom;
	private final int frameWidth, squareSize, height;
	private final String styleName;
	
	private MaterialWithData blackSquareMat, whiteSquareMat;
	private MaterialWithData enclosureMat, frameMat, controlPanelMat;
	private MaterialWithData highlightMat, highlightWhiteSquareMat, highlightBlackSquareMat;
	private MaterialWithData strutsMat;
	private HighlightStyle highlightStyle;
	private int lightLevel;
	
	private String pieceStyleName;

	/**
	 * Private constructor.  Use BoardStyle.loadNewStyle() to get new BoardStyle objects.
	 * 
	 * @param styleName
	 * @param c
	 * @param isCustom
	 * @throws ChessException
	 */
	private BoardStyle(String styleName, Configuration c, boolean isCustom) throws ChessException {
		this.styleName = styleName;
		this.isCustom = isCustom;
		
		for (String k : new String[] {
				"square_size", "frame_width", "height",
				"black_square", "white_square", "frame", "enclosure"}) {
			ChessPersistence.requireSection(c, k);
		}
		if (!c.contains("lit") && !c.contains("light_level")) {
			throw new ChessException("board style must have at least one of 'lit' or 'light_level'");
		}
		
		squareSize = c.getInt("square_size");
		frameWidth = c.getInt("frame_width");
		height = c.getInt("height");
		
		this.pieceStyleName = c.getString("piece_style", "standard");

		if (c.contains("lit")) {
			this.lightLevel = 15;
		} else {
			this.lightLevel = c.getInt("light_level");
		}

		this.blackSquareMat = MaterialWithData.get(c.getString("black_square"));
		this.whiteSquareMat = MaterialWithData.get(c.getString("white_square"));
		this.frameMat = MaterialWithData.get(c.getString("frame"));
		this.enclosureMat = MaterialWithData.get(c.getString("enclosure"));

		/************** optional parameters  **************/		
		this.controlPanelMat = MaterialWithData.get(c.getString("panel", this.frameMat.toString()));
		this.strutsMat = MaterialWithData.get(c.getString("struts", this.enclosureMat.toString()));
		this.highlightMat = MaterialWithData.get(c.getString("highlight", "glowstone"));
		this.highlightWhiteSquareMat = MaterialWithData.get(c.getString("highlight_white_square", this.highlightMat.toString()));
		this.highlightBlackSquareMat = MaterialWithData.get(c.getString("highlight_black_square", this.highlightMat.toString()));
		try {
			this.highlightStyle = HighlightStyle.getStyle(c.getString("highlight_style", "corners"));
		} catch (IllegalArgumentException e) {
			throw new ChessException(e.getMessage());
		}
	}

	public String getName() {
		return styleName;
	}
	
	public boolean isCustom() {
		return isCustom;
	}
	
	public String getPieceStyleName() {
		return pieceStyleName;
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

	public int getLightLevel() {
		return lightLevel;
	}

	public void setLightLevel(int lightLevel) {
		if (lightLevel >= 0 && lightLevel <= 15) {
			this.lightLevel = lightLevel;
		}
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

	public MaterialWithData getStrutsMaterial() {
		return strutsMat;
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

	public void setPieceStyleName(String pieceStyleName) {
		this.pieceStyleName = pieceStyleName;
	}

	public void setBlackSquareMaterial(MaterialWithData blackSquareMat) {
		if (blackSquareMat != null) {
			this.blackSquareMat = blackSquareMat;
		}
	}

	public void setWhiteSquareMaterial(MaterialWithData whiteSquareMat) {
		if (whiteSquareMat != null) {
			this.whiteSquareMat = whiteSquareMat;
		}
	}

	public void setControlPanelMaterial(MaterialWithData controlPanelMat) {
		if (controlPanelMat != null && !BlockType.canPassThrough(controlPanelMat.getId())) {
			this.controlPanelMat = controlPanelMat;
		}
	}

	public void setEnclosureMaterial(MaterialWithData enclosureMat) {
		if (enclosureMat == null) {
			this.enclosureMat = MaterialWithData.get("air");
		} else {
			this.enclosureMat = enclosureMat;
		}
	}

	public void setStrutsMaterial(MaterialWithData strutsMat) {
		this.strutsMat = strutsMat;
	}

	public void setFrameMaterial(MaterialWithData frameMat) {
		if (frameMat != null) {
			this.frameMat = frameMat;
		}
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

	public void verifyCompatibility(ChessSet pieceStyle) throws ChessException {
		// ensure the new chess set actually fits this board
		if (pieceStyle.getMaxWidth() > squareSize || pieceStyle.getMaxHeight() > height) {
			throw new ChessException("Set '" + pieceStyle.getName() + "' is too large for this board!");
		}
	}
	
	public BoardStyle saveStyle(String newStyleName) throws ChessException {
		File f = DirectoryStructure.getResourceFileForSave(DirectoryStructure.getBoardStyleDirectory(), newStyleName);

		// It would be nice to use the configuration API to save this, but I want comments!
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("# Chess board style definition\n\n");
			out.write("# NOTE: all materials must be quoted, even if they're just integers, or\n");
			out.write("# you will get a java.lang.ClassCastException when the style is loaded.\n\n");
			out.write("# width/length of the board squares, in blocks\n");
			out.write("square_size: " + squareSize + "\n");
			out.write("# width in blocks of the frame surrounding the board\n");
			out.write("frame_width: " + frameWidth + "\n");
			out.write("# height of the board - number of squares of clear air between board and enclosure roof\n");
			out.write("height: " + height + "\n");
			out.write("# material/data for the white squares\n");
			out.write("white_square: '" + whiteSquareMat + "'\n");
			out.write("# material/data for the black squares\n");
			out.write("black_square: '" + blackSquareMat + "'\n");
			out.write("# material/data for the frame\n");
			out.write("frame: '" + frameMat + "'\n");
			out.write("# material/data for the enclosure\n");
			out.write("enclosure: '" + enclosureMat + "'\n");
			out.write("# material/data for the enclosure struts (default: 'enclosure' setting)\n");
			out.write("struts: '" + strutsMat + "'\n");
			out.write("# board lighting level (0-15)\n");
			out.write("light_level: " + lightLevel + "\n");
			out.write("# style of chess set to use (see ../pieces/*.yml)\n");
			out.write("# the style chosen must fit within the square_size specified above\n");
			out.write("piece_style: " + pieceStyleName + "\n");
			out.write("# material/data for the control panel (default: 'frame' setting)\n");
			out.write("panel: '" + controlPanelMat + "'\n");
			out.write("# highlighting style (one of NONE, CORNERS, EDGES, LINE, CHECKERED)\n");
			out.write("highlight_style: " + highlightStyle + "\n");
			out.write("# highlighting material (default: glowstone)\n");
			out.write("highlight: '" + highlightMat + "'\n");
			out.write("# highlighting material on white squares (default: 'highlight' setting)\n");
			out.write("highlight_white_square: '" + highlightWhiteSquareMat + "'\n");
			out.write("# highlighting material on black squares (default: 'highlight' setting)\n");
			out.write("highlight_black_square: '" + highlightBlackSquareMat + "'\n");
			out.close();
			
			return loadStyle(newStyleName);
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}
	
	/**
	 * Load a new style.
	 * 
	 * @param styleName		Name of the style to load
	 * @return				The new loaded style
	 * @throws ChessException
	 */
	public static BoardStyle loadStyle(String styleName) throws ChessException {
		if (styleName == null) styleName = DEFAULT_BOARD_STYLE;
		
		try {
			File f = DirectoryStructure.getResourceFileForLoad(DirectoryStructure.getBoardStyleDirectory(), styleName);
			Configuration c = MiscUtil.loadYamlUTF8(f);
			return new BoardStyle(styleName, c, DirectoryStructure.isCustom(f));
		} catch (Exception e) {
			throw new ChessException(e.getMessage());
		}
	}
	
	/**
	 * Ensure the given piece style will fit on the given board style.
	 * 
	 * @param boardStyleName	name of the board style
	 * @param pieceStyleName	name of the piece style (null means to use board's default piece style)
	 * @throws ChessException	if the piece style will not fit
	 */
	public static void verifyCompatibility(String boardStyleName, String pieceStyleName) throws ChessException {
		BoardStyle b = BoardStyle.loadStyle(boardStyleName);
		ChessSet cs = ChessSet.getChessSet(pieceStyleName == null ? b.getPieceStyleName() : pieceStyleName);
		b.verifyCompatibility(cs);
	}

	@Override
	public int compareTo(BoardStyle o) {
		return getName().compareTo(o.getName());
	}
}

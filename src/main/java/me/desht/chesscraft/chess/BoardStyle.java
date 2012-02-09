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

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class BoardStyle {

	public static final int MIN_HEIGHT = 3, MIN_FRAMEWIDTH = 2, MIN_SQUARESIZE = 1;
	public static final int MAX_HEIGHT = 128, MAX_FRAMEWIDTH = 20, MAX_SQUARESIZE = 20;
	
	int frameWidth, squareSize, height;
	MaterialWithData blackSquareMat, whiteSquareMat;
	MaterialWithData enclosureMat, frameMat, controlPanelMat;
	MaterialWithData highlightMat, highlightWhiteSquareMat, highlightBlackSquareMat;
	MaterialWithData strutsMat;
	HighlightStyle highlightStyle;
	int lightLevel;
	String styleName;
	String pieceStyleName;

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
		if (controlPanelMat != null && !BlockType.canPassThrough(controlPanelMat.getMaterial())) {
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

	private void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth < MIN_FRAMEWIDTH ? MIN_FRAMEWIDTH
				: (frameWidth > MAX_FRAMEWIDTH ? MAX_FRAMEWIDTH : frameWidth);
	}

	private void setHeight(int height) {
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

	private void setSquareSize(int squareSize) {
		this.squareSize = squareSize < MIN_SQUARESIZE ? MIN_SQUARESIZE
				: (squareSize > MAX_SQUARESIZE ? MAX_SQUARESIZE : squareSize);
	}

	public void saveStyle(String newStyleName) throws ChessException {
		File f = new File(ChessConfig.getBoardStyleDirectory(), "custom" + File.separator + newStyleName.toLowerCase() + ".yml");
		
		// TODO: disallow overwriting a built-in style name?
		
		// It would be nice to use the configuration API to save this, but I want comments
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
			out.write("# material/data for the enclosure (if you don't use glass or air, then light the board!)\n");
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
			out.write("# highlighting style (NONE, CORNERS, EDGES, LINE, CHECKERED)\n");
			out.write("highlight_style: " + highlightStyle + "\n");
			out.write("# highlighting material (default: glowstone)\n");
			out.write("highlight: '" + highlightMat + "'\n");
			out.write("# highlighting material on white squares (default: 'highlight' setting)\n");
			out.write("highlight_white_square: '" + highlightWhiteSquareMat + "'\n");
			out.write("# highlighting material on black squares (default: 'highlight' setting)\n");
			out.write("highlight_black_square: '" + highlightBlackSquareMat + "'\n");
			out.close();
			
			styleName = newStyleName;
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}

	public static BoardStyle loadNewStyle(String boardStyle) throws ChessException {
		File f = ChessConfig.getResourceFile(ChessConfig.getBoardStyleDirectory(), boardStyle);
		
		Configuration c = YamlConfiguration.loadConfiguration(f);

		for (String k : new String[] {
				"square_size", "frame_width", "height",
				"black_square", "white_square", "frame", "enclosure"}) {
			if (!c.contains(k)) {
				throw new ChessException("board style is missing required field '" + k + "'");
			}
		}
		if (!c.contains("lit") && !c.contains("light_level")) {
			throw new ChessException("board style must have at least one of 'lit' or 'light_level'");
		}
		
		BoardStyle style = new BoardStyle();
		style.styleName = boardStyle;

		style.setSquareSize(c.getInt("square_size"));
		style.setFrameWidth(c.getInt("frame_width"));
		style.setHeight(c.getInt("height"));
		style.pieceStyleName = c.getString("piece_style");

		if (c.contains("lit")) {
			style.lightLevel = 15;
		} else {
			style.lightLevel = c.getInt("light_level");
		}

		style.blackSquareMat = MaterialWithData.get(c.getString("black_square"));
		style.whiteSquareMat = MaterialWithData.get(c.getString("white_square"));
		style.frameMat = MaterialWithData.get(c.getString("frame"));
		style.enclosureMat = MaterialWithData.get(c.getString("enclosure"));

		/************** optional parameters  **************/		
		style.controlPanelMat = MaterialWithData.get(c.getString("panel", style.frameMat.toString()));
		style.strutsMat = MaterialWithData.get(c.getString("struts", style.enclosureMat.toString()));
		style.highlightMat = MaterialWithData.get(c.getString("highlight", "glowstone"));
		style.highlightWhiteSquareMat = MaterialWithData.get(c.getString("highlight_white_square", style.highlightMat.toString()));
		style.highlightBlackSquareMat = MaterialWithData.get(c.getString("highlight_black_square", style.highlightMat.toString()));
		style.highlightStyle = HighlightStyle.getStyle(c.getString("highlight_style", "corners"));

		return style;
	}
}

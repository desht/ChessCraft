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
import me.desht.chesscraft.ChessValidate;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.chess.pieces.ChessSetFactory;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.AttributeCollection;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.block.MaterialWithData;

import org.bukkit.configuration.Configuration;

public class BoardStyle implements Comparable<BoardStyle>, ConfigurationListener {
	public static final String DEFAULT_BOARD_STYLE = "standard";

	public static final int MIN_HEIGHT = 3, MIN_FRAMEWIDTH = 2, MIN_SQUARESIZE = 1;
	public static final int MAX_HEIGHT = 128, MAX_FRAMEWIDTH = 10, MAX_SQUARESIZE = 30;

	private static final String WHITE_SQUARE = "white_square";
	private static final String BLACK_SQUARE = "black_square";
	private static final String FRAME = "frame";
	private static final String ENCLOSURE = "enclosure";
	private static final String STRUTS = "struts";
	private static final String PANEL = "panel";
	private static final String HIGHLIGHT_SQUARE = "highlight_square";
	private static final String HIGHLIGHT_WHITE_SQUARE = "highlight_white_square";
	private static final String HIGHLIGHT_BLACK_SQUARE = "highlight_black_square";
	private static final String HIGHLIGHT_STYLE = "highlight_style";
	private static final String LIGHT_LEVEL = "light_level";
	private static final String PIECE_STYLE = "piece_style";

	private final boolean isCustom;
	private final int frameWidth, squareSize, height;
	private final String styleName;

	private final AttributeCollection attributes;

	/**
	 * Private constructor.  Use {@link #loadStyle(String)} to get new BoardStyle objects.
	 * 
	 * @param styleName name of this board style
	 * @param c configuration object which stores this board style
	 * @param isCustom true if this is a custom-created board style, false if it is a stock style
	 * @throws ChessException
	 */
	private BoardStyle(String styleName, Configuration c, boolean isCustom) throws ChessException {
		for (String k : new String[] {
				"square_size", "frame_width", "height",
				"black_square", "white_square", "frame", "enclosure"}) {
			ChessPersistence.requireSection(c, k);
		}
		this.attributes = new AttributeCollection(this);
		registerAttributes();
		this.styleName = styleName;
		this.isCustom = isCustom;
		this.squareSize = c.getInt("square_size");
		this.frameWidth = c.getInt("frame_width");
		this.height = c.getInt("height");
		ChessValidate.isTrue(c.contains("lit") || c.contains("light_level"), "board style must have at least one of 'lit' or 'light_level'");

		for (String k : c.getKeys(false)) {
			if (attributes.contains(k)) {
				attributes.set(k, c.getString(k));
			}
		}
	}

	private void registerAttributes() {
		attributes.registerAttribute(LIGHT_LEVEL, 15, "Lighting level (0-15) for the board");
		attributes.registerAttribute(PIECE_STYLE, "Standard", "Default piece style for the board");
		attributes.registerAttribute(WHITE_SQUARE, MaterialWithData.get("wool:white"), "Block for white board square");
		attributes.registerAttribute(BLACK_SQUARE, MaterialWithData.get("wool:black"), "Block for black board square");
		attributes.registerAttribute(FRAME, MaterialWithData.get("wood"), "Block for outer board frame");
		attributes.registerAttribute(ENCLOSURE, MaterialWithData.get("air"), "Block for board enclosure");
		attributes.registerAttribute(STRUTS, MaterialWithData.get("wood"), "Block for board edge struts");
		attributes.registerAttribute(PANEL, MaterialWithData.get("wood"), "Block for control panel");
		attributes.registerAttribute(HIGHLIGHT_SQUARE, MaterialWithData.get("wool:red"), "Block for last-move highlight");
		attributes.registerAttribute(HIGHLIGHT_WHITE_SQUARE, MaterialWithData.get("wool:red"), "Block for last-move highlight (white squares)");
		attributes.registerAttribute(HIGHLIGHT_BLACK_SQUARE, MaterialWithData.get("wool:red"), "Block for last-move highlight (black squares)");
		attributes.registerAttribute(HIGHLIGHT_STYLE, HighlightStyle.CORNERS, "Style of last-move highlighting");
	}

	public String getName() {
		return styleName;
	}

	public boolean isCustom() {
		return isCustom;
	}

	public String getPieceStyleName() {
		return (String) attributes.get(PIECE_STYLE);
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
		return (Integer) attributes.get(LIGHT_LEVEL);
	}

	public MaterialWithData getBlackSquareMaterial() {
		return (MaterialWithData) attributes.get(BLACK_SQUARE);
	}

	public MaterialWithData getWhiteSquareMaterial() {
		return (MaterialWithData) attributes.get(WHITE_SQUARE);
	}

	public MaterialWithData getControlPanelMaterial() {
		return (MaterialWithData) attributes.get(PANEL);
	}

	public MaterialWithData getEnclosureMaterial() {
		return (MaterialWithData) attributes.get(ENCLOSURE);
	}

	public MaterialWithData getFrameMaterial() {
		return (MaterialWithData) attributes.get(FRAME);
	}

	public HighlightStyle getHighlightStyle() {
		return (HighlightStyle) attributes.get(HIGHLIGHT_STYLE);
	}

	public MaterialWithData getHighlightMaterial() {
		return (MaterialWithData) attributes.get(HIGHLIGHT_SQUARE);
	}

	public MaterialWithData getStrutsMaterial() {
		return (MaterialWithData) attributes.get(STRUTS);
	}

	public MaterialWithData getHighlightMaterial(boolean isWhiteSquare) {
		return isWhiteSquare ? getWhiteSquareHighlightMaterial() : getBlackSquareHighlightMaterial();
	}

	public MaterialWithData getBlackSquareHighlightMaterial() {
		return (MaterialWithData) attributes.get(HIGHLIGHT_BLACK_SQUARE);
	}

	public MaterialWithData getWhiteSquareHighlightMaterial() {
		return (MaterialWithData) attributes.get(HIGHLIGHT_BLACK_SQUARE);
	}

	private void validateIsBlock(MaterialWithData mat, String tag) {
		ChessValidate.isTrue(mat.getBukkitMaterial().isBlock(), tag + ": " + mat + " is not a block material!");
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
			out.write("square_size: " + getSquareSize() + "\n");
			out.write("# width in blocks of the frame surrounding the board\n");
			out.write("frame_width: " + getFrameWidth() + "\n");
			out.write("# height of the board - number of squares of clear air between board and enclosure roof\n");
			out.write("height: " + getHeight() + "\n");
			out.write("# material/data for the white squares\n");
			out.write("white_square: '" + getWhiteSquareMaterial() + "'\n");
			out.write("# material/data for the black squares\n");
			out.write("black_square: '" + getBlackSquareMaterial() + "'\n");
			out.write("# material/data for the frame\n");
			out.write("frame: '" + getFrameMaterial() + "'\n");
			out.write("# material/data for the enclosure\n");
			out.write("enclosure: '" + getEnclosureMaterial() + "'\n");
			out.write("# material/data for the enclosure struts (default: 'enclosure' setting)\n");
			out.write("struts: '" + getStrutsMaterial() + "'\n");
			out.write("# board lighting level (0-15)\n");
			out.write("light_level: " + getLightLevel() + "\n");
			out.write("# style of chess set to use (see ../pieces/*.yml)\n");
			out.write("# the style chosen must fit within the square_size specified above\n");
			out.write("piece_style: " + getPieceStyleName() + "\n");
			out.write("# material/data for the control panel (default: 'frame' setting)\n");
			out.write("panel: '" + getControlPanelMaterial() + "'\n");
			out.write("# highlighting style (one of NONE, CORNERS, EDGES, LINE, CHECKERED)\n");
			out.write("highlight_style: " + getHighlightStyle() + "\n");
			out.write("# highlighting material (default: glowstone)\n");
			out.write("highlight: '" + getHighlightMaterial() + "'\n");
			out.write("# highlighting material on white squares (default: 'highlight' setting)\n");
			out.write("highlight_white_square: '" + getWhiteSquareHighlightMaterial() + "'\n");
			out.write("# highlighting material on black squares (default: 'highlight' setting)\n");
			out.write("highlight_black_square: '" + getBlackSquareHighlightMaterial() + "'\n");
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
		ChessSet cs = ChessSetFactory.getChessSet(pieceStyleName.isEmpty() ? b.getPieceStyleName() : pieceStyleName);
		b.verifyCompatibility(cs);
	}

	@Override
	public int compareTo(BoardStyle o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equals(LIGHT_LEVEL)) {
			int level = (Integer) newVal;
			ChessValidate.isTrue(level >= 0 && level <= 15, "Light level must be in range 0-15");
		} else if (key.equals(PIECE_STYLE)) {
			verifyCompatibility(ChessSetFactory.getChessSet((String) newVal));
		} else if (newVal instanceof MaterialWithData) {
			validateIsBlock((MaterialWithData) newVal, key);
		}
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		// nothing to do here
	}

	public AttributeCollection getAttributes() {
		return attributes;
	}
}

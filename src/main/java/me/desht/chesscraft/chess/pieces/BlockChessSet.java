/**
 * Programmer: Jacob Scott
 * Program Name: ChessSet
 * Description: wrapper for all of the chess sets
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess.pieces;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.chess.ChessBoard;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MaterialWithData;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import chesspresso.Chess;
import chesspresso.position.Position;

import com.google.common.base.Joiner;

public class BlockChessSet extends ChessSet {

	private static final String[] CHESS_SET_HEADER_LINES = new String[] {
		"ChessCraft block piece style definition file",
		"See http://dev.bukkit.org/server-mods/chesscraft/pages/piece-styles",
		"",
		"'name' is the name for this set, and should match the filename",
		"",
		"'comment' is a freeform comment about the set (can be multi-line)",
		"",
		"'materials.white' & 'materials.black' are lists of materials used in this set",
		" Can be specified as plain integer (e.g. '0' - air), material name (e.g. iron_block)",
		" or material plus data (e.g. 35:0, wool:white)",
		" If you use plain integers, they must be quoted, or the set will not load!",
		" If you use material names, they must match the org.bukkit.Material definitions",
		" - see http://jd.bukkit.org/apidocs/org/bukkit/Material.html",
		"",
		"'pieces.<X>' defines the template for a chess piece, where <X> is one of P,R,N,B,Q,K",
		" Each piece definition is a list of list of strings such that:",
		" - definition[0] is the lowest layer on the Y-axis",
		" - definition[0][0] is the northmost row on the lowest layer",
		" - each string runs from west to east and consists of materials defined above",
		"",
		"There is also support for separate templates for white and black pieces.  In",
		"this case, the templates are stored as 'pieces.white.<X>' and pieces.black.<X>",
	};

	// map a character to a material
	private final MaterialMap materialMapWhite, materialMapBlack;
	// map a Chesspresso piece number to a PieceTemplate object
	private final ChessPieceTemplate[] templates;
	// where white & black pieces have different templates, this will be non-null
	private final ChessPieceTemplate[] templatesBlack;
	// cache of instantiated chess stones
	private final Map<String, ChessStone> stoneCache = new HashMap<String, ChessStone>();

	/**
	 * Package-protected constructor.  Initialise a chess set from saved data.
	 *
	 * @param c		The Configuration object loaded from file.
	 * @throws ChessException if there is any problem loading the set.
	 */
	BlockChessSet(Configuration c, boolean isCustom) throws ChessException {
		super(c, isCustom);

		ChessPersistence.requireSection(c, "pieces");
		ChessPersistence.requireSection(c, "materials.white");
		ChessPersistence.requireSection(c, "materials.black");

		if (c.contains("pieces.white")) {
			ChessPersistence.requireSection(c, "pieces.black");
			templates = loadTemplates(c.getConfigurationSection("pieces.white"));
			templatesBlack = loadTemplates(c.getConfigurationSection("pieces.black"));
		} else {
			templates = loadTemplates(c.getConfigurationSection("pieces"));
			templatesBlack = null;
		}
		setupMaxDimensions();

		try {
			materialMapWhite = initMaterialMap(c, "white");
			materialMapBlack = initMaterialMap(c, "black");
		} catch (IllegalArgumentException e) {
			throw new ChessException("Can't load piece style '" + getName() + "': " + e.getMessage());
		}
	}

	/**
	 * Package protected constructor. Initialise a chess set from template and material map information.
	 * This constructor is used to create a new set from piece designer information.
	 *
	 * @param name the chess set name
	 * @param templates the templates to copy in for the white (or both) pieces
	 * @param materialMaps the material maps for the white and black pieces
	 * @param description a free-form comment about the set
	 */
	BlockChessSet(String name, ChessPieceTemplate[][] templates, MaterialMap[] materialMaps, String description) {
		super(name, description);

		this.materialMapWhite = materialMaps[Chess.WHITE];
		this.materialMapBlack = materialMaps[Chess.BLACK];

		this.templates = new ChessPieceTemplate[Chess.MAX_PIECE + 1];
		System.arraycopy(templates[Chess.WHITE], Chess.MIN_PIECE + 1, this.templates, Chess.MIN_PIECE + 1, Chess.MAX_PIECE);

		if (templates[Chess.BLACK] != null) {
			this.templatesBlack = new ChessPieceTemplate[Chess.MAX_PIECE + 1];
			System.arraycopy(templates[Chess.BLACK], Chess.MIN_PIECE + 1, this.templatesBlack, Chess.MIN_PIECE + 1, Chess.MAX_PIECE);
		} else {
			this.templatesBlack = null;
		}
		setupMaxDimensions();
	}

	/**
	 * Establish the maximum width & height for the templates just loaded.
	 * This decides which boards this set will fit on.
	 */
	private void setupMaxDimensions() {
		int maxH = 0, maxW = 0;
		for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
			ChessPieceTemplate tmpl = templates[piece];
			maxW = Math.max(maxW, tmpl.getWidth());
			maxH = Math.max(maxH, tmpl.getSizeY());
		}
		if (templatesBlack != null) {
			for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
				ChessPieceTemplate tmpl = templatesBlack[piece];
				maxW = Math.max(maxW, tmpl.getWidth());
				maxH = Math.max(maxH, tmpl.getSizeY());
			}
		}
		setMaxWidth(maxW);
		setMaxHeight(maxH);
	}

	/**
	 * Load data from the config file into a templates array.
	 *
	 * @param cs configuration section to load from
	 * @return an array of templates, one for each of the six pieces
	 */
	private ChessPieceTemplate[] loadTemplates(ConfigurationSection cs) {
		ChessPieceTemplate[] result = new ChessPieceTemplate[Chess.MAX_PIECE + 1];

		for (String p : cs.getKeys(false)) {
			@SuppressWarnings("unchecked")
			List<List<String>> pieceData = (List<List<String>>) cs.getList(p);
			int piece = Chess.charToPiece(p.charAt(0));
			if (piece == Chess.NO_PIECE) {
				throw new ChessException("invalid piece letter: " + p);
			}
			result[piece] = new ChessPieceTemplate(pieceData);
		}
		return result;
	}

	private MaterialMap initMaterialMap(Configuration c, String colour) {
		MaterialMap res = new MaterialMap();
		ConfigurationSection cs = c.getConfigurationSection("materials." + colour);
		for (String k : cs.getKeys(false)) {
			res.put(k.charAt(0), MaterialWithData.get(cs.getString(k)));
		}
		return res;
	}

	/**
	 * Return a mapping of white material to black material for those materials in this set
	 * which differ for the white and black pieces.
	 *
	 * @return
	 */
	Map<String, String> getWhiteToBlack() {
		Map<String,String> res = new HashMap<String, String>();

		for (Entry<Character,MaterialWithData> e : materialMapWhite.getMap().entrySet()) {
			String w = e.getValue().toString();
			String b = materialMapBlack.get(e.getKey()).toString();
			if (!w.equals(b)) {
				LogUtils.finer("ChessSet: " + getName() + ": add white->black material map: " + w + "->" + b);
				res.put(w, b);
			}
		}

		return res;
	}

	/**
	 * Retrieve a fully instantiated chess stone, of the appropriate material for the stone's
	 * colour, and facing the right direction.
	 *
	 * @param stone		Chesspresso stone number (Chess.WHITE_PAWN etc.)
	 * @param direction		Board orientation
	 * @return
	 */
	public ChessStone getStone(int stone, BoardRotation direction) {
		int piece = Chess.stoneToPiece(stone);
		int colour = Chess.stoneToColor(stone);
		String key = String.format("%d:%d:%s", piece, colour, direction);
		if (!stoneCache.containsKey(key)) {
			MaterialMap materialMap = colour == Chess.WHITE ? materialMapWhite : materialMapBlack;
			ChessPieceTemplate template = colour == Chess.BLACK && templatesBlack != null ? templatesBlack[piece] : templates[piece];
			stoneCache.put(key, new BlockChessStone(stone, template, materialMap, direction));
		}
		return stoneCache.get(key);
	}

	@Override
	public ChessStone getStoneAt(int sqi) {
		throw new UnsupportedOperationException("Block chess sets don't track pieces by square index");
	}

	@Override
	protected void addSaveData(Configuration conf) {
		for (char c : materialMapWhite.getMap().keySet()) {
			conf.set("materials.white." + c, materialMapWhite.get(c).toString());
		}
		for (char c : materialMapBlack.getMap().keySet()) {
			conf.set("materials.black." + c, materialMapBlack.get(c).toString());
		}
		if (templatesBlack != null) {
			for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
				conf.set("pieces.white." + Chess.pieceToChar(piece), templates[piece].getPieceData());
			}
			for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
				conf.set("pieces.black." + Chess.pieceToChar(piece), templatesBlack[piece].getPieceData());
			}
		} else {
			for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
				conf.set("pieces." + Chess.pieceToChar(piece), templates[piece].getPieceData());
			}
		}
	}

	@Override
	protected String getHeaderText() {
		return Joiner.on("\n").join(CHESS_SET_HEADER_LINES);
	}

	@Override
	protected String getType() {
		return "block";
	}

	public boolean differentBlackTemplates() {
		return templatesBlack != null;
	}

	@Override
	public boolean canRide() {
		return true;
	}

	@Override
	public boolean hasMovablePieces() {
		return false;
	}

	@Override
	public void movePiece(int fromSqi, int toSqi, int captureSqi, Location to, int promoteStone) {
		// no-op
	}

	@Override
	public void syncToPosition(Position pos, ChessBoard board) {
		// no-op
	}
}


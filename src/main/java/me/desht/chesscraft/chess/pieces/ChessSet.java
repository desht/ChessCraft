/**
 * Programmer: Jacob Scott
 * Program Name: ChessSet
 * Description: wrapper for all of the chess sets
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess.pieces;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Joiner;

import chesspresso.Chess;
import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

public class ChessSet implements Iterable<ChessStone>,Comparable<ChessSet> {

	// map of all known chess sets keyed by set name
	private static final Map<String, ChessSet> allChessSets = new HashMap<String, ChessSet>();

	// note when the set was loaded so we can reload if the set file is updated
	private static final Map<String,Long> setLoadTime = new HashMap<String, Long>();

	private static final String[] CHESS_SET_HEADER_LINES = new String[] {
			"ChessCraft piece style definition file",
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
	};

	// map a character to a material
	private final MaterialMap materialMapWhite, materialMapBlack;
	// map a Chesspresso piece number to a PieceTemplate object
	private final ChessPieceTemplate[] templates = new ChessPieceTemplate[Chess.MAX_PIECE + 1];
	// cache of instantiated chess stones
	private final Map<String, ChessStone> stoneCache = new HashMap<String, ChessStone>();
	private final String name;
	private final String comment;
	private final int maxWidth;
	private final int maxHeight;
	private final boolean isCustom;

	/**
	 * Private constructor.  Initialise a chess set from saved data.
	 * 
	 * @param c		The Configuration object loaded from file.
	 * 
	 * @throws ChessException if there is any problem loading the set.
	 */
	private ChessSet(Configuration c, boolean isCustom) throws ChessException {
		ChessPersistence.requireSection(c, "name");
		ChessPersistence.requireSection(c, "pieces");
		ChessPersistence.requireSection(c, "materials.white");
		ChessPersistence.requireSection(c, "materials.black");
		
		name = c.getString("name");
		comment = c.getString("comment", "");
		this.isCustom = isCustom;
		
		ConfigurationSection pieceConf = c.getConfigurationSection("pieces");
		int maxH = 0, maxW = 0;
		for (String p : pieceConf.getKeys(false)) {
			@SuppressWarnings("unchecked")
			List<List<String>> pieceData = (List<List<String>>) pieceConf.getList(p);
			int piece = Chess.charToPiece(p.charAt(0));
			if (piece == Chess.NO_PIECE) {
				throw new ChessException("invalid piece letter: " + p);
			}
			ChessPieceTemplate tmpl = new ChessPieceTemplate(pieceData);
			maxW = Math.max(maxW, tmpl.getWidth());
			maxH = Math.max(maxH, tmpl.getSizeY());
			templates[piece] = tmpl;
		}
		maxWidth = maxW; maxHeight = maxH;
		
		try {
			materialMapWhite = initMaterialMap(c, "white");
			materialMapBlack = initMaterialMap(c, "black");
		} catch (IllegalArgumentException e) {
			throw new ChessException("Can't load piece style '" + name + "': " + e.getMessage());
		}
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
	 * Package protected constructor. Initialise a chess set from template and material map information.
	 * This constructor would be used to create a new set from piece designer information.
	 * 
	 * @param name
	 * @param templates
	 * @param materialMapWhite
	 * @param materialMapBlack
	 */
	ChessSet(String name, ChessPieceTemplate[] templates, MaterialMap materialMapWhite, MaterialMap materialMapBlack, String comment) {
		this.name = name;
		this.isCustom = true;
		this.materialMapWhite = materialMapWhite;
		this.materialMapBlack = materialMapBlack;
		this.comment = comment;
		int maxW = 0, maxH = 0;
		for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
			this.templates[piece] = templates[piece];
			maxW = Math.max(maxW, templates[piece].getWidth());
			maxH = Math.max(maxH, templates[piece].getSizeY());
		}
		maxWidth = maxW; maxHeight = maxH;
	}

	public Iterator<ChessStone> iterator() {
		return new ChessPieceIterator();
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
				LogUtils.finer("ChessSet: " + name + ": add white->black material map: " + w + "->" + b);
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
			MaterialMap matMap = colour == Chess.WHITE ? materialMapWhite : materialMapBlack;
			stoneCache.put(key, new ChessStone(stone, templates[piece], matMap, direction));
		}
		return stoneCache.get(key);
	}

	/**
	 * Get this chess set's name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the comment for this chess set.
	 * 
	 * @return
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Return the width (X or Z) of the widest piece in the set
	 *
	 * @return
	 */
	public int getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Return the height of the tallest piece in the set
	 * 
	 * @return
	 */
	public int getMaxHeight() {
		return maxHeight;
	}

	public boolean isCustom() {
		return isCustom;
	}

	/**
	 * Save this chess set to a file with the new name.
	 * 
	 * @param newName
	 * @throws ChessException
	 */
	public void save(String newName) throws ChessException {
		File f = DirectoryStructure.getResourceFileForSave(DirectoryStructure.getPieceStyleDirectory(), ChessPersistence.makeSafeFileName(newName));
		
		YamlConfiguration conf = new YamlConfiguration();
		conf.options().header(Joiner.on("\n").join(CHESS_SET_HEADER_LINES));
		try {
			conf.set("name", name);
			conf.set("comment", comment);
			for (char c : materialMapWhite.getMap().keySet()) {
				conf.set("materials.white." + c, materialMapWhite.get(c).toString());
			}
			for (char c : materialMapBlack.getMap().keySet()) {
				conf.set("materials.black." + c, materialMapBlack.get(c).toString());
			}
			for (int piece = Chess.MIN_PIECE + 1; piece <= Chess.MAX_PIECE; piece++) {
				conf.set("pieces." + Chess.pieceToChar(piece), templates[piece].getPieceData());
			}
			conf.save(f);
			LogUtils.fine("saved chess set '" + name + "' to " + f);
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}
	
	//--------------------------static methods---------------------------------
	
	/**
	 * Check if the given set is loaded.
	 * 
	 * @param setName
	 * @return
	 */
	public static boolean isLoaded(String setName) {
		return allChessSets.containsKey(setName);
	}
	
	/**
	 * Retrieve a chess set with the given name, loading it from file if necessary.
	 * 
	 * @param setName
	 * @return
	 * @throws ChessException
	 */
	public static ChessSet getChessSet(String setName) throws ChessException {
		setName = setName.toLowerCase();
		if (!isLoaded(setName) || needsReload(setName)) {
			loadChessSet(setName);
		}
		return allChessSets.get(setName);
	}

	private static boolean needsReload(String setName) throws ChessException {
		if (!setLoadTime.containsKey(setName)) {
			return true;
		}
		File f = DirectoryStructure.getResourceFileForLoad(DirectoryStructure.getPieceStyleDirectory(), setName);
		return f.lastModified() > setLoadTime.get(setName);
	}

	private static ChessSet loadChessSet(String setName) throws ChessException {
		File f = DirectoryStructure.getResourceFileForLoad(DirectoryStructure.getPieceStyleDirectory(), setName);
		
		Configuration c = YamlConfiguration.loadConfiguration(f);
		ChessSet set = new ChessSet(c, DirectoryStructure.isCustom(f));
		LogUtils.fine("loaded chess set '" + set.getName() + "' from " + f);
		
		allChessSets.put(setName, set);
		setLoadTime.put(setName, System.currentTimeMillis());
		
		return set;
	}
	
	//-------------------------------- iterator class
	
	public class ChessPieceIterator implements Iterator<ChessStone> {

		int i = 0;
		Integer keys[] = new Integer[0];
		
		public ChessPieceIterator(){
			keys = stoneCache.keySet().toArray(keys);
		}
		
		public boolean hasNext() {
			return keys.length > i;
		}

		public ChessStone next() {
			// simply iterates through values.. not through keys
			return stoneCache.get(keys[i++]);
		}

		public void remove() {
		}

	}

	@Override
	public int compareTo(ChessSet o) {
		return getName().compareTo(o.getName());
	}
} // end class ChessSet


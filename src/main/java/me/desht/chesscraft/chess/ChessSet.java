/**
 * Programmer: Jacob Scott
 * Program Name: ChessSet
 * Description: wrapper for all of the chess sets
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import chesspresso.Chess;
import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.chess.pieces.ChessStone;
import me.desht.chesscraft.chess.pieces.PieceTemplate;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ChessSet implements Iterable<ChessStone> {

	// map of all known chess sets keyed by set name
	private static final Map<String, ChessSet> templates = new HashMap<String, ChessSet>();

	// map a ChessPresso stone number to a ChessStone object
	private final Map<Integer, ChessStone> stoneCache = new HashMap<Integer, ChessStone>();
	private final String name;
	private int maxWidth = 0;
	private int maxHeight = 0;

	/**
	 * Private constructor.  Use ChessSet.getChessSet() to get a chess set.
	 * 
	 * @param setName
	 * @param stones
	 */
	private ChessSet(String setName, Map<Integer, PieceTemplate> stones) {
		name = setName;
		for (int i : stones.keySet()) {
			ChessStone stone = new ChessStone(i, stones.get(i));
			if (stone.getWidth() > maxWidth) {
				maxWidth = stone.getWidth();
			}
			if (stone.getHeight() > maxHeight) {
				maxHeight = stone.getHeight();
			}
			stoneCache.put(i, stone);
		}
	}

	public Iterator<ChessStone> iterator() {
		return new ChessPieceIterator();
	}

	public ChessStone getPiece(int stone) {
		return stoneCache.get(stone);
	}

	public String getName() {
		return name;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	//--------------------------static methods---------------------------------
	
	public static boolean loaded(String setName) {
		return templates.containsKey(setName);
	}
	
	public static ChessSet getChessSet(String setName) throws ChessException {
		if (templates.get(setName) == null) {
			templates.put(setName, loadChessSet(setName));
		}
		return templates.get(setName);
	}

	public static String[] getChessSetNames() {
		return templates.keySet().toArray(new String[0]);
	}

	public static ChessSet[] getAllChessSets() {
		return templates.values().toArray(new ChessSet[0]);
	}
	
	@SuppressWarnings("unchecked")
	public static ChessSet loadChessSet(String setFileName) throws ChessException {
		if (!setFileName.endsWith(".yml")) {
			setFileName = setFileName + ".yml";
		}
		Configuration c = YamlConfiguration.loadConfiguration(new File(ChessConfig.getPieceStyleDirectory(), setFileName));

		String setName = c.getString("name");
		Map<Integer, PieceTemplate> stoneToTemplate = new HashMap<Integer, PieceTemplate>();
		
		ConfigurationSection pieces = c.getConfigurationSection("pieces");
		for (String p : pieces.getKeys(false)) {
			List<List<String>> pieceData = pieces.getList(p);
			int piece = Chess.charToPiece(p.charAt(0));
			PieceTemplate ptw = new PieceTemplate(pieceData, c.getConfigurationSection("materials.white"));
			stoneToTemplate.put(Chess.pieceToStone(piece, Chess.WHITE), ptw);
			PieceTemplate ptb = new PieceTemplate(pieceData, c.getConfigurationSection("materials.black"));
			stoneToTemplate.put(Chess.pieceToStone(piece, Chess.BLACK), ptb);
		}
		ChessSet set = new ChessSet(setName, stoneToTemplate);
		ChessCraftLogger.log("loaded set " + setName + " OK.");
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
} // end class ChessSet


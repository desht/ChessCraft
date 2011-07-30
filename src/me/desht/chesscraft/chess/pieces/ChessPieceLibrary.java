package me.desht.chesscraft.chess.pieces;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.chesscraft.exceptions.ChessException;

import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;
import me.desht.chesscraft.chess.ChessSet;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ChessPieceLibrary {

	private static final Map<String, ChessSet> templates = new HashMap<String, ChessSet>();

	public static boolean isChessSetLoaded(String setName) {
		return templates.containsKey(setName);
	}

	public static ChessSet getChessSet(String setName) {
		return templates.get(setName);
	}

	public static String[] getChessSetNames(){
		return templates.keySet().toArray(new String[0]);
	}

	public static ChessSet[] getAllChessSets(){
		return templates.values().toArray(new ChessSet[0]);
	}
	
	public static ChessSet loadChessSet(File folder, String setName) throws ChessException {
		if (!setName.matches("\\.yml$")) {
			setName = setName + ".yml";
		}
		File f = new File(folder, setName);
		try {
			return loadChessSet(f);
		} catch (FileNotFoundException e) {
			throw new ChessException("can't load chess set " + setName + ": " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private static ChessSet loadChessSet(File f) throws FileNotFoundException {

		String setName = null;
		Yaml yaml = new Yaml();

		try {
			Map<String, Object> pieceMap = (Map<String, Object>) yaml.load(new FileInputStream(f));

			setName = (String) pieceMap.get("name");
			if(setName == null){
				setName = f.getName().replaceFirst("\\.yml$", "");
			}
			// overwrite old set
//			if (templates.get(setName) != null) {
//				throw new ChessException("Duplicate chess set name " + setName + " detected");
//			}

			Map<String, Map<String, String>> mm = (Map<String, Map<String, String>>) pieceMap.get("materials");
			Map<String, String> whiteMats = mm.get("white");
			Map<String, String> blackMats = mm.get("black");

			Map<String, Object> mp = (Map<String, Object>) pieceMap.get("pieces");
			Map<Integer, PieceTemplate> pieces = new HashMap<Integer, PieceTemplate>();
			for (Entry<String, Object> e : mp.entrySet()) {
				List<List<String>> data = (List<List<String>>) e.getValue();
				int piece = Chess.charToPiece(e.getKey().charAt(0));
				PieceTemplate ptw = new PieceTemplate(data, whiteMats);
				pieces.put(Chess.pieceToStone(piece, Chess.WHITE), ptw);

				PieceTemplate ptb = new PieceTemplate(data, blackMats);
				pieces.put(Chess.pieceToStone(piece, Chess.BLACK), ptb);
			}
			ChessSet set = new ChessSet(pieces);
			templates.put(setName, set);
			ChessCraftLogger.log("loaded set " + setName + " OK.");
			return set;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			ChessCraftLogger.severe("can't load chess set " + setName + ": " + e);
		}
		return null;
	}
//
//	/**
//	 * Return a chess stone rotated in the given direction
//	 * @param style The style to use
//	 * @param stone The Chess stone to rotate
//	 * @return A ChessStone representing the stone
//	 */
//	ChessStone getStone(String style, int stone) {
//		if (!templates.containsKey(style)) {
//			throw new IllegalArgumentException("No such style '" + style + "'");
//		}
//		if (stone < Chess.MIN_STONE || stone > Chess.MAX_STONE || stone == Chess.NO_STONE) {
//			throw new IllegalArgumentException("Bad stone index " + stone);
//		}
//		String k = style + ":" + stone;
//		if (!stoneCache.containsKey(k)) {
//			PieceTemplate tmpl = templates.get(style).get(stone);
//			stoneCache.put(k, new ChessStone(stone, tmpl));
//		}
//		return stoneCache.get(k);
//	}
}

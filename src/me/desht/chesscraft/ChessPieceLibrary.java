package me.desht.chesscraft;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;

import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;

public class ChessPieceLibrary {
	ChessCraft plugin;
	private static final String libraryDir =
		ChessCraft.directory + File.separator + "piece_styles";
	private final Map<String,Map<Integer,PieceTemplate>> templates =
		new HashMap<String,Map<Integer,PieceTemplate>>();
	
	ChessPieceLibrary(ChessCraft plugin) {
		this.plugin = plugin;
		
		try {
			loadChessSets();
		} catch (FileNotFoundException e) {
			plugin.log(Level.SEVERE, "Can't load piece libraries: " + e.getMessage());
		}
	}

	private class PieceFilter implements FileFilter {
		public boolean accept(File file) {
			return file.getName().toLowerCase().endsWith(".yml");
		}
	}
	
	private void loadChessSets() throws FileNotFoundException {
		File[] files = new File(libraryDir).listFiles(new PieceFilter());
		if (files == null) throw new FileNotFoundException("can't open folder " + libraryDir);
		for (File f : files) {
			loadChessSet(f);
		}
	}

	boolean isSetLoaded(String setName) {
		return templates.containsKey(setName);
	}
	
	@SuppressWarnings("unchecked")
	private void loadChessSet(File f) throws FileNotFoundException {

		String setName = null;
		Yaml yaml = new Yaml();

		try {        	
        	Map<String,Object> pieceMap = 
        		(Map<String,Object>) yaml.load(new FileInputStream(f));

        	setName = (String) pieceMap.get("name");
        	if (templates.get(setName) != null)
        		throw new ChessException("Duplicate chess set name " + setName + " detected");
        	
        	Map<String, Map<String,Integer>> mm = (Map<String, Map<String,Integer>>) pieceMap.get("materials");
        	Map<String, Integer> whiteMats = mm.get("white");
        	Map<String, Integer> blackMats = mm.get("black");
        	
        	Map<String, Object> mp = (Map<String,Object>) pieceMap.get("pieces");
        	Map<Integer,PieceTemplate> pieces = new HashMap<Integer,PieceTemplate>();
        	for (String s : mp.keySet()) {
        		List<List<String>> data = (List<List<String>>) mp.get(s);
        		int piece = Chess.charToPiece(s.charAt(0));
        		
        		PieceTemplate ptw = new PieceTemplate(data, whiteMats);
        		pieces.put(Chess.pieceToStone(piece, Chess.WHITE), ptw);
        		
        		PieceTemplate ptb = new PieceTemplate(data, blackMats);
        		pieces.put(Chess.pieceToStone(piece, Chess.BLACK), ptb);
        	}
        	templates.put(setName, pieces);
        	plugin.log(Level.INFO, "loaded set " + setName + " OK.");
		} catch (Exception e) {
			plugin.log(Level.SEVERE, "can't load chess set " + setName + ": " + e);
		}
	}

	// Return a chess stone rotated in the given direction
	ChessStone getStone(String style, int stone) {
		if (!templates.containsKey(style))
			throw new IllegalArgumentException("No such style '" + style + "'");
		if (stone < Chess.MIN_STONE || stone > Chess.MAX_STONE || stone == Chess.NO_STONE)
			throw new IllegalArgumentException("Bad stone index " + stone);
		PieceTemplate tmpl = templates.get(style).get(stone);
		ChessStone result = new ChessStone(stone, tmpl);
		return result;
	}
	
}

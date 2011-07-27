package me.desht.chesscraft;

import me.desht.chesscraft.blocks.PieceTemplate;
import me.desht.chesscraft.blocks.ChessStone;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;

import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;

public class ChessPieceLibrary {

    ChessCraft plugin;
    private final Map<String, Map<Integer, PieceTemplate>> templates = new HashMap<String, Map<Integer, PieceTemplate>>();
    private final Map<String, ChessStone> stoneCache = new HashMap<String, ChessStone>();

    ChessPieceLibrary(ChessCraft plugin) {
        this.plugin = plugin;
    }

    boolean isChessSetLoaded(String setName) {
        return templates.containsKey(setName);
    }

    void loadChessSet(String setName) throws ChessException {
        if (!setName.matches("\\.yml$")) {
            setName = setName + ".yml";
        }
        File f = new File(ChessConfig.getPieceStyleDirectory(), setName);
        try {
            loadChessSet(f);
        } catch (FileNotFoundException e) {
            throw new ChessException("can't load chess set " + setName + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadChessSet(File f) throws FileNotFoundException {

        String setName = null;
        Yaml yaml = new Yaml();

        try {
            Map<String, Object> pieceMap = (Map<String, Object>) yaml.load(new FileInputStream(f));

            setName = (String) pieceMap.get("name");
            if (templates.get(setName) != null) {
                throw new ChessException("Duplicate chess set name " + setName + " detected");
            }

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
            templates.put(setName, pieces);
            ChessCraft.log(Level.INFO, "loaded set " + setName + " OK.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            ChessCraft.log(Level.SEVERE, "can't load chess set " + setName + ": " + e);
        }
    }

    /**
     * Return a chess stone rotated in the given direction
     * @param style The style to use
     * @param stone The Chess stone to rotate
     * @return A ChessStone representing the stone
     */
    ChessStone getStone(String style, int stone) {
        if (!templates.containsKey(style)) {
            throw new IllegalArgumentException("No such style '" + style + "'");
        }
        if (stone < Chess.MIN_STONE || stone > Chess.MAX_STONE || stone == Chess.NO_STONE) {
            throw new IllegalArgumentException("Bad stone index " + stone);
        }
        String k = style + ":" + stone;
        if (!stoneCache.containsKey(k)) {
        	PieceTemplate tmpl = templates.get(style).get(stone);
        	stoneCache.put(k, new ChessStone(stone, tmpl));
        }
        return stoneCache.get(k);
    }
}

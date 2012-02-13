package me.desht.chesscraft.chess.pieces;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;

import chesspresso.Chess;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessSet;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.regions.Cuboid;

public class PieceDesigner {
	private BoardView view;
	private PieceTemplate[] pieces;
	private String setName;
	
	private Map<Character, String> materialMapWhite;
	private Map<Character, String> materialMapBlack;
	
	public PieceDesigner(BoardView view, String setName) throws ChessException {
		if (view.isDesigning()) {
			throw new ChessException("This board is already in design mode.");
		}
		this.view = view;
		this.setName = setName;
	}
	
	public String getSetName() {
		return setName;
	}
	
	public void setSetName(String setName) {
		this.setName = setName;
	}

	/**
	 * Scan the board view and initialise all the piece templates based on what's currently
	 * on the board
	 */
	public void scan() {
		materialMapWhite = new HashMap<Character,String>();

		pieces = new PieceTemplate[Chess.MAX_PIECE + 1];
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			// get the bounding box for the materials in this square
			Cuboid c = getPieceBox(p);
			System.out.println("piece " + Chess.pieceToChar(p) + " - cuboid: " + c);
			pieces[p] = new PieceTemplate(c.getSizeX(), c.getSizeY(), c.getSizeZ());
			pieces[p].useMaterialMap(materialMapWhite);
			
			// scan the cuboid and use the contents to populate the new template
			for (int x = 0; x < pieces[p].getSizeX(); x++) {
				for (int y = 0; y < pieces[p].getSizeY(); y++) {
					for (int z = 0; z < pieces[p].getSizeZ(); z++) {
						Block b = c.getRelativeBlock(x, y, z);
						MaterialWithData mat = MaterialWithData.get(b.getTypeId(), b.getData());
//						System.out.println("setmat " + x +","+y+","+z+" = " + mat);
						pieces[p].setMaterial(x, y, z, mat);
					}	
				}
			}
			
			// scan the template to regenerate the piece array and material map
			pieces[p].scan();
		}

		// set up the material map for black
		initBlackMaterialMap();
	}
	
	/**
	 * Load the current design's set data onto the board.
	 * 
	 * @throws ChessException if the set doesn't exist or doesn't fit the board
	 */
	public void load() throws ChessException {
		ChessSet newChessSet = ChessSet.getChessSet(setName);
		BoardStyle boardStyle = view.getChessBoard().getBoardStyle();
		// ensure the new chess set actually fits this board
		if (newChessSet.getMaxWidth() > boardStyle.getSquareSize() || newChessSet.getMaxHeight() > boardStyle.getHeight()) {
			throw new ChessException("Set '" + newChessSet.getName() + "' is too large for this board!");
		}
		
		Set<Cuboid> updated = new HashSet<Cuboid>();
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			int sqi = getSqi(p);
			int stone = Chess.pieceToStone(p, Chess.WHITE);
			Cuboid c = view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi));
			System.out.println("paint into " + c);
			ChessStone csWhite = newChessSet.getPiece(stone);
			System.out.println("stone " + csWhite.stone + " " + csWhite.getWidth() + " x " + csWhite.getHeight());
			newChessSet.getPiece(stone).paintInto(c, view.getDirection());
			updated.add(c);
		
			// now scan the set and work out which materials differ between white and black
			int stoneBlack = Chess.pieceToStone(p, Chess.BLACK);
			ChessStone csBlack = newChessSet.getPiece(stoneBlack);
			Map<Character,String> whiteMap = csWhite.getPieceTemplate(view.getDirection()).getMaterialMap();
			Map<Character,String> blackMap = csBlack.getPieceTemplate(view.getDirection()).getMaterialMap();
			Map<String,String> whiteToBlack = new HashMap<String, String>();
			for (Entry<Character,String> e : whiteMap.entrySet()) {
				if (!e.getValue().equals(blackMap.get(e.getKey()))) {
					// materials differ
					whiteToBlack.put(e.getValue(), blackMap.get(e.getKey()));
				}
			}
			
			addMapBlocks(whiteToBlack);
		}
		
		
		for (Cuboid c : updated) {
			c.sendClientChanges();
		}
	}

	private void addMapBlocks(Map<String, String> whiteToBlack) {
		
		Iterator<String> iter = whiteToBlack.keySet().iterator();
		
		for (int col = 1; col < 5; col++) {
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(Direction.Up, 1);
			c.clear(true);
			int n = 0;
			for (Block b : c) {
				if (!iter.hasNext()) 
					break;
				if (n++ % 2 == 1)
					continue;	// skip alternate squares
			}
		}
	}

	/**
	 * Save the current design.
	 * 
	 * @throws ChessException if the file can't be written
	 */
	public void save() throws ChessException {
		File f = ChessConfig.getResourceFile(ChessConfig.getPieceStyleDirectory(), setName, true);
		
		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.set("name", setName);
			for (char c : materialMapWhite.keySet()) {
				conf.set("materials.white." + c, materialMapWhite.get(c));
			}
			for (char c : materialMapBlack.keySet()) {
				conf.set("materials.black." + c, materialMapBlack.get(c));
			}
			for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
				conf.set("pieces." + Chess.pieceToChar(p), pieces[p].pieceData);
			}
			conf.save(f);
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}

	/**
	 * Clear all pieces in the design squares
	 */
	public void clear() {
		Set<Cuboid> updated = new HashSet<Cuboid>();
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			int sqi = getSqi(p);
			Cuboid c = view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi));
			c.clear(true);
			updated.add(c);
		}
		for (Cuboid c : updated) {
			c.sendClientChanges();
		}
	}

	private void initBlackMaterialMap() {
		materialMapBlack = new HashMap<Character,String>();
		Map<String,Character> reverseMap = new HashMap<String,Character>();
		
		for (Entry<Character, String> e : materialMapWhite.entrySet()) {
			materialMapBlack.put(e.getKey(), e.getValue());
			reverseMap.put(e.getValue(), e.getKey());
		}
		
		// scan just above squares B2 - B4 inclusive
		// any block found with another block on top is of interest
		for (int col = 1; col < 5; col++) {
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(Direction.Up, 1);
			for (Block b : c) {
				if (b.getTypeId() == 0) {
					continue;
				}
				Block b2 = b.getRelative(BlockFace.UP);
				if (b2.getTypeId() == 0) {
					continue;
				}
				MaterialWithData mat = MaterialWithData.get(b.getTypeId(), b.getData());
				if (reverseMap.containsKey(mat.toString())) {
					MaterialWithData mat2 = MaterialWithData.get(b2.getTypeId(), b2.getData());
					System.out.println("insert mapping " + mat.toString() + " -> " + reverseMap.get(mat.toString()) + " -> " + mat2.toString());
					materialMapBlack.put(reverseMap.get(mat.toString()), mat2.toString());
				}
			}
		}
	}

	private int getSqi(int p) {
		switch (p) {
		case Chess.PAWN:
			return Chess.A2;
		case Chess.KNIGHT:
			return Chess.B1;
		case Chess.BISHOP:
			return Chess.C1;
		case Chess.ROOK:
			return Chess.A1;
		case Chess.QUEEN:
			return Chess.D1;
		case Chess.KING:
			return Chess.E1;
		default:
			throw new IllegalArgumentException("Invalid chess piece " + p);
		}
	}
	
	private Cuboid getPieceBox(int p) {
		int sqi = getSqi(p);
		Cuboid c = view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi)).contract();
		// c is now the smallest Cuboid which fully contains the piece (with no external air)
		
		return c;
	}
}

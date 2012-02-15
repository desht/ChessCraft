package me.desht.chesscraft.chess.pieces;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import chesspresso.Chess;

import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.regions.Cuboid;

public class PieceDesigner {
	private final BoardView view;
	private String setName;	// name of the set currently being designed
	private ChessSet chessSet;	// the set currently being designed

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
	 * Scan the board and initialise a chess set based on the contents of squares A1-E1 & A2-E2.
	 */
	public void scan() {
		MaterialMap whiteMap = new MaterialMap();

		ChessPieceTemplate[] templates = new ChessPieceTemplate[Chess.MAX_PIECE + 1];

		// reverse mapping of character to material name
		Map<String,Character> reverseMap = new HashMap<String, Character>();
		char nextChar = 'A';

		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			// get the bounding box for the materials in this square
			Cuboid c = getPieceBox(p);
//			System.out.println("piece " + Chess.pieceToChar(p) + " - cuboid: " + c);
			templates[p] = new ChessPieceTemplate(c.getSizeX(), c.getSizeY(), c.getSizeZ());

			// scan the cuboid and use the contents to populate the new template
			for (int x = 0; x < templates[p].getSizeX(); x++) {
				for (int y = 0; y < templates[p].getSizeY(); y++) {
					for (int z = 0; z < templates[p].getSizeZ(); z++) {
						Block b = c.getRelativeBlock(x, y, z);
						MaterialWithData mat = MaterialWithData.get(b.getTypeId(), b.getData());
						String materialName = mat.toString();
						if (!reverseMap.containsKey(materialName)) {
							// not seen this material yet
							reverseMap.put(materialName, nextChar);
							whiteMap.put(nextChar, mat);
							nextChar++;
						}
						templates[p].put(x, y, z, reverseMap.get(materialName));
					}	
				}
			}
		}

		MaterialMap blackMap = initBlackMaterialMap(whiteMap);

		chessSet = new ChessSet(setName, templates, whiteMap, blackMap);
	}

	private MaterialMap initBlackMaterialMap(MaterialMap whiteMap) {
		Map<String,Character> reverseMap = new HashMap<String,Character>();
		MaterialMap blackMap = new MaterialMap();
		for (Entry<Character, MaterialWithData> e : whiteMap.getMap().entrySet()) {
			blackMap.put(e.getKey(), e.getValue());
			reverseMap.put(e.getValue().toString(), e.getKey());
		}

		// scan just above squares B2-E2 inclusive
		// any block found with a different block on top is of interest
		for (int col = 1; col < 5; col++) {
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(Direction.Up, 1);
			for (Block b : c) {
				Block b2 = b.getRelative(BlockFace.UP);
				if (b.getTypeId() == b2.getTypeId() && b.getData() == b2.getData()) {
					continue;
				}
				MaterialWithData mat = MaterialWithData.get(b.getTypeId(), b.getData());
				if (reverseMap.containsKey(mat.toString())) {
					MaterialWithData mat2 = MaterialWithData.get(b2.getTypeId(), b2.getData());
//					System.out.println("insert mapping " + mat.toString() + " -> " + reverseMap.get(mat.toString()) + " -> " + mat2.toString());
					blackMap.put(reverseMap.get(mat.toString()), mat2);
				}
			}
		}

		return blackMap;
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
		chessSet = newChessSet;

		Cuboid bounding = null;
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			int sqi = getSqi(p);
			Cuboid c = view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi));
			bounding = c.getBoundingCuboid(bounding);
			ChessStone whiteStone = chessSet.getStone(p, Chess.WHITE, view.getDirection());
//			System.out.println("stone " + whiteStone.getStone() + " " + whiteStone.getWidth() + " x " + whiteStone.getSizeY());
			view.getChessBoard().paintChessPiece(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi), whiteStone.getStone());
		}

		addMapBlocks(chessSet.getWhiteToBlack());

		bounding.forceLightLevel(view.getChessBoard().getBoardStyle().getLightLevel());
		bounding.sendClientChanges();
	}

	private void addMapBlocks(Map<String, String> whiteToBlack) {
		Iterator<String> iter = whiteToBlack.keySet().iterator();

		for (int col = 1; col < 5; col++) {
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(Direction.Up, 1);
			c.expand(Direction.Up, 1).clear(true);
			int n = 0;
			for (Block b : c) {
				if (!iter.hasNext()) 
					break;
				if (n++ % 2 == 1)
					continue;	// skip alternate squares
				String whiteMat = iter.next();
				String blackMat = whiteToBlack.get(whiteMat);
				MaterialWithData.get(whiteMat).applyToBlockFast(b);
				MaterialWithData.get(blackMat).applyToBlockFast(b.getRelative(BlockFace.UP));
			}
		}
	}

	/**
	 * Save the current design to a piece style file.
	 * 
	 * @throws ChessException if the file can't be written
	 */
	public void save() throws ChessException {
		if (chessSet != null) {
			chessSet.save(setName);
		}
	}

	/**
	 * Clear all pieces in the design squares (A1-E1 & A2-E2)
	 */
	public void clear() {
		Cuboid bounding = null;
		for (int row = 0; row < 2; row++) {
			for (int col = 0; col < 5; col++) {
				view.getChessBoard().paintChessPiece(row, col, Chess.NO_STONE);
//				Cuboid c = view.getChessBoard().getPieceRegion(row, col);
//				c.clear(true);
//				bounding = c.getBoundingCuboid(bounding);
			}
		}
//		bounding.forceLightLevel(view.getChessBoard().getBoardStyle().getLightLevel());
		
		// force an update for all squares in the box A1-E2
		bounding = view.getChessBoard().getPieceRegion(0, 0);
		bounding = bounding.getBoundingCuboid(view.getChessBoard().getPieceRegion(1, 4));
		bounding.sendClientChanges();

		chessSet = null;
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

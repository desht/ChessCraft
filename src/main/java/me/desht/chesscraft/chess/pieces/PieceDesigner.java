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
import me.desht.chesscraft.log.ChessCraftLogger;
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
	 * 
	 * @throws ChessException if there is any kind of problem initialising the set
	 */
	public void scan() throws ChessException {
		MaterialMap whiteMap = new MaterialMap();

		ChessPieceTemplate[] templates = new ChessPieceTemplate[Chess.MAX_PIECE + 1];

		int rotation = rotationNeeded();
		ChessCraftLogger.fine("Designer: need to rotate templates by " + rotation + " degrees");
		
		// reverse mapping of character to material name
		Map<String,Character> reverseMap = new HashMap<String, Character>();
		char nextChar = 'A';

		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			// get the bounding box for the materials in this square
			Cuboid c = getPieceBox(p);
			ChessCraftLogger.fine("Designer: scan: piece " + Chess.pieceToChar(p) + " - cuboid: " + c);

			templates[p] = createTemplate(c, rotation);
			
			// scan the cuboid and use the contents to populate the new template
			for (int x = 0; x < templates[p].getSizeX(); x++) {
				for (int y = 0; y < templates[p].getSizeY(); y++) {
					for (int z = 0; z < templates[p].getSizeZ(); z++) {
						Point rotatedPoint = rotate(x, z, templates[p].getSizeX(), templates[p].getSizeZ(), 360 - rotation);
						Block b = c.getRelativeBlock(rotatedPoint.x, y, rotatedPoint.z);
						MaterialWithData mat = MaterialWithData.get(b.getTypeId(), b.getData()).rotate(rotation);
						String materialName = mat.toString();
						if (!reverseMap.containsKey(materialName)) {
							// not seen this material yet
							reverseMap.put(materialName, nextChar);
							whiteMap.put(nextChar, mat);
							ChessCraftLogger.finer("Designer: add material mapping: " + nextChar + "->" + materialName);
							nextChar = getNextChar(nextChar);
						}
						templates[p].put(x, y, z, reverseMap.get(materialName));
					}	
				}
			}
		}

		MaterialMap blackMap = initBlackMaterialMap(whiteMap);

		chessSet = new ChessSet(setName, templates, whiteMap, blackMap);
	}

	private char getNextChar(char c) throws ChessException {
		if (c == 'Z') {
			return 'a';
		} else if (c == 'z') {
			return '0';
		} else if (c == '9') {
			throw new ChessException("material limit exceeded (maximum 62 different materials)");
		} else {
			return ++c;
		}
	}
	
	private ChessPieceTemplate createTemplate(Cuboid c, int rotation) {
		if (rotation == 0 || rotation == 180) {
			return new ChessPieceTemplate(c.getSizeX(), c.getSizeY(), c.getSizeZ());
		} else if (rotation == 90 || rotation == 270) {
			return new ChessPieceTemplate(c.getSizeZ(), c.getSizeY(), c.getSizeX());
		} else {
			return null;
		}
	}
	
	private Point rotate(int x, int z, int sizeX, int sizeZ, int rotation) {
		switch (rotation % 360) {
		case 0:
			return new Point(x, z);
		case 90:
			return new Point(z, sizeZ - x - 1);
		case 180:
			return new Point(sizeX - x - 1, sizeZ - z - 1);
		case 270:
			return new Point(sizeX - z - 1, x);
		default:
			return null;
		}
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
					ChessCraftLogger.fine("Designer: insert mapping " + mat.toString() + " -> " + reverseMap.get(mat.toString()) + " -> " + mat2.toString());
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
			ChessStone whiteStone = chessSet.getStone(Chess.pieceToStone(p,  Chess.WHITE), view.getRotation());
			ChessCraftLogger.fine("Designer: load: stone " + whiteStone.getStone() + " " + whiteStone.getWidth() + " x " + whiteStone.getSizeY());
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
			}
		}
		
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
	
	private int rotationNeeded() {
		switch (view.getRotation()) {
		case NORTH: return 0;
		case EAST: return 270;
		case SOUTH: return 180;
		case WEST: return 90;
		default: return 0;
		}
	}
	
	private class Point {
		public int x, z;
		public Point(int x, int z) {
			this.x = x; this.z = z;
		}
	}
}

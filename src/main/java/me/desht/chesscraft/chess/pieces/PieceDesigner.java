package me.desht.chesscraft.chess.pieces;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import chesspresso.Chess;

import me.desht.dhutils.block.MaterialWithData;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;

public class PieceDesigner {
	private final BoardView view;
	private final String playerName;
	private String setName;	// name of the set currently being designed
	private BlockChessSet chessSet;	// the set currently being designed

	public PieceDesigner(BoardView view, String setName, String playerName) throws ChessException {
		if (view.isDesigning()) {
			throw new ChessException("This board is already in design mode.");
		}
		this.view = view;
		this.setName = setName;
		this.playerName = playerName;
	}

	public String getSetName() {
		return setName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setSetName(String setName) {
		this.setName = setName;
	}
	
	public BlockChessSet getChessSet() {
		return chessSet;
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
		LogUtils.fine("Designer: need to rotate templates by " + rotation + " degrees");
		
		// reverse mapping of character to material name
		Map<String,Character> reverseMap = new HashMap<String, Character>();
		char nextChar = 'A';

		World world = view.getA1Square().getWorld();
		
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			// get the bounding box for the materials in this square
			Cuboid c = getPieceBox(p);
			LogUtils.fine("Designer: scan: piece " + Chess.pieceToChar(p) + " - cuboid: " + c);

			templates[p] = createTemplate(c, rotation);
			
			// scan the cuboid and use the contents to populate the new template
			for (int x = 0; x < templates[p].getSizeX(); x++) {
				for (int y = 0; y < templates[p].getSizeY(); y++) {
					for (int z = 0; z < templates[p].getSizeZ(); z++) {
						Point rotatedPoint = rotate(x, z, templates[p].getSizeZ(), templates[p].getSizeX(), rotation);
						Block b = c.getRelativeBlock(world, rotatedPoint.x, y, rotatedPoint.z);
						MaterialWithData mat = MaterialWithData.get(b).rotate(rotation);
						String materialName = mat.toString();
						if (!reverseMap.containsKey(materialName)) {
							// not seen this material yet
							reverseMap.put(materialName, nextChar);
							whiteMap.put(nextChar, mat);
							LogUtils.finer("Designer: add material mapping: " + nextChar + "->" + materialName);
							nextChar = getNextChar(nextChar);
						}
						templates[p].put(x, y, z, reverseMap.get(materialName));
					}	
				}
			}
		}

		MaterialMap blackMap = initBlackMaterialMap(whiteMap);

		chessSet = new BlockChessSet(setName, templates, whiteMap, blackMap, "Created in ChessCraft piece designer by " + playerName);
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
			return new Point(sizeZ - x - 1, sizeX - z - 1);
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
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(CuboidDirection.Up, 1);
			for (Block b : c) {
				Block b2 = b.getRelative(BlockFace.UP);
				if (b.getTypeId() == b2.getTypeId() && b.getData() == b2.getData()) {
					continue;
				}
				MaterialWithData mat = MaterialWithData.get(b);
				if (reverseMap.containsKey(mat.toString())) {
					MaterialWithData mat2 = MaterialWithData.get(b2);
					LogUtils.fine("Designer: insert mapping " + mat.toString() + " -> " + reverseMap.get(mat.toString()) + " -> " + mat2.toString());
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
		ChessSet newChessSet = ChessSetFactory.getChessSet(setName);
		if (!(newChessSet instanceof BlockChessSet)) {
			throw new ChessException("Set '" + newChessSet.getName() + "' is not a block chess set!");
		}
		
		BoardStyle boardStyle = view.getChessBoard().getBoardStyle();
		// ensure the new chess set actually fits this board
		if (newChessSet.getMaxWidth() > boardStyle.getSquareSize() || newChessSet.getMaxHeight() > boardStyle.getHeight()) {
			throw new ChessException("Set '" + newChessSet.getName() + "' is too large for this board!");
		}
		chessSet = (BlockChessSet) newChessSet;

		Cuboid bounding = null;
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			int sqi = getSqi(p);
			Cuboid c = view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi));
			bounding = c.getBoundingCuboid(bounding);
			ChessStoneBlock whiteStone = (ChessStoneBlock) chessSet.getStone(Chess.pieceToStone(p,  Chess.WHITE), view.getRotation());
			LogUtils.fine("Designer: load: stone " + whiteStone.getStone() + " " + whiteStone.getWidth() + " x " + whiteStone.getSizeY());
			view.getChessBoard().paintChessPiece(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi), whiteStone.getStone());
		}

		addMapBlocks(chessSet.getWhiteToBlack());
	}

	private void addMapBlocks(Map<String, String> whiteToBlack) {
		Iterator<String> iter = whiteToBlack.keySet().iterator();

		for (int col = 1; col < 5; col++) {
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(CuboidDirection.Up, 1);
			c.expand(CuboidDirection.Up, 1).fill(0, (byte)0);
			int n = 0;
			for (Block b : c) {
				if (!iter.hasNext()) 
					break;
				if (n++ % 2 == 1)
					continue;	// skip alternate squares
				String whiteMat = iter.next();
				String blackMat = whiteToBlack.get(whiteMat);
				MaterialWithData.get(whiteMat).applyToBlock(b);
				MaterialWithData.get(blackMat).applyToBlock(b.getRelative(BlockFace.UP));
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
			// force the new set to be re-cached
			ChessSetFactory.getChessSet(setName);
		}
	}

	/**
	 * Clear all pieces in the design squares (A1-E1 & A2-E2)
	 */
	public void clear() {
		for (int row = 0; row < 2; row++) {
			for (int col = 0; col < 5; col++) {
				view.getChessBoard().paintChessPiece(row, col, Chess.NO_STONE);
			}
		}
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

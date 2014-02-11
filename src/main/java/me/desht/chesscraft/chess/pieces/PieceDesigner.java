package me.desht.chesscraft.chess.pieces;

import chesspresso.Chess;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
		MaterialMap[] materialMaps = new MaterialMap[2];
		materialMaps[Chess.WHITE] = new MaterialMap();

		ChessPieceTemplate[][] templates = new ChessPieceTemplate[2][];
		templates[Chess.WHITE] = new ChessPieceTemplate[Chess.MAX_PIECE + 1];
		templates[Chess.BLACK] = null;

		World world = view.getA1Square().getWorld();

		for (int colour = Chess.WHITE; colour <= Chess.BLACK; colour++) {
			int rotation = rotationNeeded(colour);
			Debugger.getInstance().debug("Designer: rotate templates by " + rotation + " degrees for colour " + colour + " & board orientation " + view.getRotation());

			// reverse mapping of character to material name
			Map<String,Character> reverseMap = new HashMap<String, Character>();
			char nextChar = 'A';

			for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
				// get the bounding box for the materials in this square
				Cuboid c = getPieceBox(p, colour);
				Debugger.getInstance().debug("Designer: scan: piece " + Chess.pieceToChar(p) + ", colour " + colour + " = cuboid: " + c);

				ChessPieceTemplate template = createTemplate(c, rotation);

				// scan the cuboid and use the contents to populate the new template
				for (int x = 0; x < template.getSizeX(); x++) {
					for (int y = 0; y < template.getSizeY(); y++) {
						for (int z = 0; z < template.getSizeZ(); z++) {
							Point rotatedPoint = rotate(x, z, template.getSizeZ(), template.getSizeX(), rotation);
							Block b = c.getRelativeBlock(world, rotatedPoint.x, y, rotatedPoint.z);
							short data = b.getType() == Material.AIR ? 0 : b.getData();
							MaterialWithData mat = MaterialWithData.get(b.getTypeId(), data).rotate(rotation);
							String materialName = mat.toString();
							if (!reverseMap.containsKey(materialName)) {
								// not seen this material yet
								reverseMap.put(materialName, nextChar);
								materialMaps[colour].put(nextChar, mat);
								Debugger.getInstance().debug(2, "Designer: add material mapping: " + nextChar + "->" + materialName);
								nextChar = getNextChar(nextChar);
							}
							template.put(x, y, z, reverseMap.get(materialName));
						}
					}
				}
				templates[colour][p] = template;
			}
			if (colour == Chess.WHITE) {
				materialMaps[Chess.BLACK] = initBlackMaterialMap(materialMaps[Chess.WHITE]);
				if (materialMaps[Chess.BLACK] != null) {
					// no need to scan black pieces - we're using the same templates for white & black
					break;
				} else {
					templates[Chess.BLACK] = new ChessPieceTemplate[Chess.MAX_PIECE + 1];
					materialMaps[Chess.BLACK] = new MaterialMap();
				}
			}
		}

		chessSet = new BlockChessSet(setName, templates, materialMaps, "Created in ChessCraft piece designer by " + playerName);
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

	/**
	 * Attempt to initialise the black material map from blocks in squares B2-E2 inclusive.
	 *
	 * @param whiteMap the existing white material map
	 * @return the black material map, or null if no valid mappings found in B2-E2
	 */
	private MaterialMap initBlackMaterialMap(MaterialMap whiteMap) {
		Map<String,Character> reverseMap = new HashMap<String,Character>();
		MaterialMap blackMap = new MaterialMap();
		for (Entry<Character, MaterialWithData> e : whiteMap.getMap().entrySet()) {
			blackMap.put(e.getKey(), e.getValue());
			reverseMap.put(e.getValue().toString(), e.getKey());
		}

		boolean different = false;
		// scan just above squares B2-E2 inclusive
		// any block found with a different block on top is of interest
		for (int col = 1; col < 5; col++) {
			Cuboid c = view.getChessBoard().getSquare(1, col).shift(CuboidDirection.Up, 1);
			for (Block b : c) {
				Block b2 = b.getRelative(BlockFace.UP);
				if (b.getType() == b2.getType() && b.getData() == b2.getData()) {
					continue;
				}
				MaterialWithData mat = MaterialWithData.get(b);
				if (reverseMap.containsKey(mat.toString())) {
					MaterialWithData mat2 = MaterialWithData.get(b2);
					Debugger.getInstance().debug("Designer: insert mapping " + mat.toString() + " -> " + reverseMap.get(mat.toString()) + " -> " + mat2.toString());
					blackMap.put(reverseMap.get(mat.toString()), mat2);
					different = true;
				}
			}
		}

		return different ? blackMap : null;
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

		for (int colour = Chess.WHITE; colour <= Chess.BLACK; colour++) {
			for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
				int sqi = getSqi(p, colour);
				BlockChessStone stone = (BlockChessStone) chessSet.getStone(Chess.pieceToStone(p,  colour), view.getRotation());
				Debugger.getInstance().debug("Designer: load: stone " + stone.getStone() + " " + stone.getWidth() + " x " + stone.getSizeY());
				view.getChessBoard().paintChessPiece(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi), stone.getStone());
			}
			if (!chessSet.differentBlackTemplates()) {
				break;
			}
		}

		if (!chessSet.differentBlackTemplates()) {
			addMapBlocks(chessSet.getWhiteToBlack());
		}
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

	private int getSqi(int p, int colour) {
		switch (Chess.pieceToStone(p, colour)) {
		case Chess.WHITE_PAWN:
			return Chess.A2;
		case Chess.WHITE_KNIGHT:
			return Chess.B1;
		case Chess.WHITE_BISHOP:
			return Chess.C1;
		case Chess.WHITE_ROOK:
			return Chess.A1;
		case Chess.WHITE_QUEEN:
			return Chess.D1;
		case Chess.WHITE_KING:
			return Chess.E1;
		case Chess.BLACK_PAWN:
			return Chess.A7;
		case Chess.BLACK_KNIGHT:
			return Chess.B8;
		case Chess.BLACK_BISHOP:
			return Chess.C8;
		case Chess.BLACK_ROOK:
			return Chess.A8;
		case Chess.BLACK_QUEEN:
			return Chess.D8;
		case Chess.BLACK_KING:
			return Chess.E8;
		default:
			throw new IllegalArgumentException("Invalid chess piece " + p);
		}
	}

	private Cuboid getPieceBox(int p, int colour) {
		int sqi = getSqi(p, colour);
		// get the smallest Cuboid which fully contains the piece (with no external air)
		return view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi)).contract();
	}

	private int rotationNeeded(int colour) {
		int rot;
		switch (view.getRotation()) {
		case NORTH: rot = 0; break;
		case EAST: rot = 270; break;
		case SOUTH: rot = 180; break;
		case WEST: rot = 90; break;
		default: rot = 0; break;
		}
		if (colour == Chess.BLACK){
			rot = (rot + 180) % 360;
		}
		return rot;
	}

	private class Point {
		public final int x, z;
		public Point(int x, int z) {
			this.x = x; this.z = z;
		}
	}
}

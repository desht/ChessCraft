package me.desht.chesscraft.chess.pieces;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import chesspresso.Chess;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class ChessStone {

	int stone;
	PieceTemplate rotatedStones[] = new PieceTemplate[4];

	public ChessStone(int stone, PieceTemplate t) {
		this.stone = stone;

		// we only instantiate the north-facing piece for now
		// saves storing in memory a load of templates we might never need
		rotatedStones = new PieceTemplate[4];
		rotatedStones[0] = new PieceTemplate(t);
		if (Chess.stoneHasColor(stone, Chess.BLACK)) {
			rotatedStones[0].rotate(180);
		}
	}

	public int getStone() {
		return stone;
	}

	public int getWidth() {
		return Math.max(rotatedStones[0].getSizeX(), rotatedStones[0].getSizeZ());
	}

	public int getHeight() {
		return rotatedStones[0].getSizeY();
	}

	/**
	 * Get the rotated piece template for the given direction, instantiating it if necessary.
	 * 
	 * @param direction		the board's orientation (the direction that white faces)
	 * @return		the rotated piece
	 */
	public PieceTemplate getPieceTemplate(BoardOrientation direction) {
		int n = direction.ordinal();
		if (rotatedStones[n] == null) {
			rotatedStones[n] = new PieceTemplate(rotatedStones[0]);
			rotatedStones[n].rotate(n * 90);
		}
		return rotatedStones[n];
	}

	/**
	 * Paint this stone into the given Cuboid region, which represents the space above
	 * one square on the chessboard.  The stone will be centred in the region X & Z axes
	 * and painted at the lowest altitude.
	 *
	 * @param region the Cuboid area to fill
	 * @param direction the board's orientation (the direction that white faces)
	 */
	public void paintInto(Cuboid region, BoardOrientation direction) {
		PieceTemplate piece = getPieceTemplate(direction);

		assert region.getSizeX() >= piece.getSizeX();
		assert region.getSizeZ() >= piece.getSizeZ();

		int xOff = (region.getSizeX() - piece.getSizeX()) / 2;
		int zOff = (region.getSizeZ() - piece.getSizeZ()) / 2;

		region.clear(true);

		Map<Block,MaterialWithData> deferred = new HashMap<Block, MaterialWithData>();
		
		for (int x = 0; x < piece.getSizeX(); x++) {
			for (int y = 0; y < piece.getSizeY(); y++) {
				for (int z = 0; z < piece.getSizeZ(); z++) {
					MaterialWithData mat = piece.getMaterial(x, y, z);
					Block b = region.getRelativeBlock(x + xOff, y, z + zOff);
					if (BlockType.shouldPlaceLast(mat.getMaterial())) {
						deferred.put(b, mat);
					} else {
						mat.applyToBlockFast(region.getRelativeBlock(x + xOff, y, z + zOff));
					}
				}	
			}	
		}
		
		for (Entry<Block,MaterialWithData> e : deferred.entrySet()) {
			e.getValue().applyToBlockFast(e.getKey());
		}

		region.initLighting();
	}
}

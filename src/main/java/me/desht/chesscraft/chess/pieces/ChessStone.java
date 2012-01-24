package me.desht.chesscraft.chess.pieces;

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
	 * Paint this stone into the given Cuboid region, which represents one
	 * square on the chessboard.  The stone will be centred in
	 * the region (even if it's larger, which it shouldn't be).
	 *
	 * @param square the Cuboid area to fill
	 * @param direction the board's orientation (the direction that white faces)
	 */
	public synchronized void paintInto(Cuboid square, BoardOrientation direction) {
		PieceTemplate piece = getPieceTemplate(direction);
		
		int ibx = square.getLowerX();
		int ibz = square.getLowerZ();
		int iby = square.getLowerY();
		
		// centre the piece on the square
		if (piece.getSizeX() < square.getSizeX()) {
			ibx += (square.getSizeX() - piece.getSizeX()) / 2; // truncate to lower number
		} else if (piece.getSizeX() > square.getSizeX()) {
			// technically shouldn't reach here
			ibx -= (piece.getSizeX() - square.getSizeX()) / 2;
		}
		if (piece.getSizeZ() < square.getSizeZ()) {
			ibz += (square.getSizeZ() - piece.getSizeZ()) / 2; // truncate to lower number
		} else if (piece.getSizeZ() > square.getSizeZ()) {
			// technically shouldn't reach here
			ibz -= (piece.getSizeZ() - square.getSizeZ()) / 2;
		}
		
		//System.out.println("painting piece.. " + ibx + ", " + ibz + ", " + iby + ", [" + square.getSizeX() + ", " + square.getSizeZ() + "]");
		boolean secondPassNeeded = false;
		
		// first scan for solid blocks
		for (Block b : square) {
			Location l = b.getLocation();
			MaterialWithData mat = piece.getMaterial(l.getBlockX() - ibx, l.getBlockY() - iby, l.getBlockZ() - ibz);
			if (mat != null) {
				if (BlockType.shouldPlaceLast(mat.getMaterial())) {
					// can't safely place first, so will need to scan a second time
					secondPassNeeded = true;
				} else {
					mat.applyToBlockFast(b);
				}
			}
		}
		if (secondPassNeeded) {
			// place any blocks that we couldn't place in the first pass
			for (Block b : square) {
				Location l = b.getLocation();
				MaterialWithData mat = piece.getMaterial(l.getBlockX() - ibx, l.getBlockY() - iby, l.getBlockZ() - ibz);;
				if (mat != null) {
					if (BlockType.shouldPlaceLast(mat.getMaterial())) {
						mat.applyToBlockFast(b);
					}
				}
			}
		}
		
		square.initLighting();
	}
}

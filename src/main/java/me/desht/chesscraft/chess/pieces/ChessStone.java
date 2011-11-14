package me.desht.chesscraft.chess.pieces;

import chesspresso.Chess;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.Location;

public class ChessStone {

	int stone;
	PieceTemplate rotatedStones[];

	public ChessStone(int stone, PieceTemplate t) {
		this.stone = stone;

		int r = 0;
		if (Chess.stoneHasColor(stone, Chess.BLACK)) {
			r = 180;
		}
		
		rotatedStones = new PieceTemplate[4];
		for (int i = 0; i < 4; ++i, r += 90) {
			rotatedStones[i] = new PieceTemplate(t);
			rotatedStones[i].rotate(r);
		}
	}

	public int getStone() {
		return stone;
	}

	public PieceTemplate getPiece(BoardOrientation direction) {
		return direction == null ? null : rotatedStones[direction.ordinal()];
	}

	/**
	 * simply applies the stone template to this region <br>
	 * <b>this assumes that the area is empty</b>
	 * if the set does not fit in the center of the region,
	 * will be shifted to the north & east
	 * @param square area to fill
	 * @param direction which way white faces
	 */
	public synchronized void paintInto(Cuboid square, BoardOrientation direction) {
		PieceTemplate piece = rotatedStones[direction.ordinal()];
		
		int ibx = square.getLowerX(),
				ibz = square.getLowerZ(),
				iby = square.getLowerY();
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
		for (Location l : square) {
			MaterialWithData mat = piece.getMaterial(
					l.getBlockX() - ibx,
					l.getBlockY() - iby,
					l.getBlockZ() - ibz);
			if (mat != null) {
				// can't safely place first, so will need to scan a second time
				if (BlockType.shouldPlaceLast(mat.getMaterial())) {
					secondPassNeeded = true;
				} else {
					mat.applyToBlock(l.getBlock());
				}
			}
		}
		if (secondPassNeeded) {
			for (Location l : square) {
				MaterialWithData mat = piece.getMaterial(
						l.getBlockX() - ibx,
						l.getBlockY() - iby,
						l.getBlockZ() - ibz);
				if (mat != null) {
					// place blocks couldn't place first
					//if (BlockType.shouldPlaceLast(mat.getMaterial())) {
					mat.applyToBlock(l.getBlock());
					//}
				}
			}
		}
	}
}

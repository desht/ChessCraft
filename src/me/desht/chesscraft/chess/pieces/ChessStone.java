package me.desht.chesscraft.chess.pieces;

import chesspresso.Chess;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.Location;

public class ChessStone extends PieceTemplate {

	int stone;

	public int getStone() {
		return stone;
	}

	public ChessStone(int stone, PieceTemplate t) {
		super(t);
		this.stone = stone;

		if (Chess.stoneHasColor(stone, Chess.BLACK)) {
			rotate(180);
		}
	}

	public final void rotate(int rotation) {
		MaterialWithData[][][] newArray = new MaterialWithData[sizeX][sizeY][sizeZ];
		int maxX = sizeX - 1;
		int maxZ = sizeZ - 1;

		//TODO: allow pieces with data (signs, torches, ladders) to rotate, too

		switch (rotation % 360) {
			case 0:
				return;
			case 90:
				for (int x = 0; x < sizeX; ++x) {
					for (int y = 0; y < sizeY; ++y) {
						for (int z = 0; z < sizeZ; ++z) {
							newArray[z][y][maxX - x] = pieceArray[x][y][z];
						}
					}
				}
				break;
			case 180:
				for (int x = 0; x < sizeX; ++x) {
					for (int y = 0; y < sizeY; ++y) {
						for (int z = 0; z < sizeZ; ++z) {
							newArray[maxX - x][y][maxZ - z] = pieceArray[x][y][z];
						}
					}
				}
				break;
			case 270:
				for (int x = 0; x < sizeX; ++x) {
					for (int y = 0; y < sizeY; ++y) {
						for (int z = 0; z < sizeZ; ++z) {
							newArray[maxZ - x][y][x] = pieceArray[x][y][z];
						}
					}
				}
				break;
			default:
				throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270");
		}
		pieceArray = newArray;
	}

	/**
	 * simply applies the stone template to this region <br>
	 * <b>this assumes that the area is empty</b>
	 * if the set does not fit in the center of the region,
	 * will be shifted to the north & east
	 * @param square area to fill
	 */
	public void paintInto(Cuboid square) {
		int ibx = square.getLowerX(),
				ibz = square.getLowerZ(),
				iby = square.getLowerY();
		if (getSizeX() < square.getSizeX()) {
			ibx += (square.getSizeX() - getSizeX()) / 2; // truncate to lower number
		} else if (getSizeX() < square.getSizeX()) {
			// technically shouldn't reach here
			ibx -= -((getSizeX() - square.getSizeX()) / 2);
		}
		if (getSizeZ() < square.getSizeZ()) {
			ibz += (square.getSizeZ() - getSizeZ()) / 2; // truncate to lower number
		} else if (getSizeZ() < square.getSizeZ()) {
			// technically shouldn't reach here
			ibz -= ((getSizeZ() - square.getSizeZ()) / 2);
		}
		boolean secondPassNeeded = false;
		// first scan for solid blocks
		for (Location l : square) {
			MaterialWithData mat = getMaterial(
					ibx - l.getBlockX(),
					iby - l.getBlockY(),
					ibz - l.getBlockZ());
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
				MaterialWithData mat = getMaterial(
						ibx - l.getBlockX(),
						iby - l.getBlockY(),
						ibz - l.getBlockZ());
				if (mat != null) {
					// place blocks couldn't place first
					if (BlockType.shouldPlaceLast(mat.getMaterial())) {
						mat.applyToBlock(l.getBlock());
					}
				}
			}
		}
	}
}

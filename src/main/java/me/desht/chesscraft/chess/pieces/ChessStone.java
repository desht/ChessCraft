package me.desht.chesscraft.chess.pieces;

import chesspresso.Chess;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardOrientation;

public class ChessStone {
	private final int stone;
	private final int sizeX, sizeY, sizeZ;
	private final MaterialWithData[][][] pieceArray;

	/**
	 * Instantiate a new chess stone
	 * 
	 * @param chessPieceTemplate
	 * @param matMap
	 * @param direction
	 */
	public ChessStone(int stone, ChessPieceTemplate tmpl, MaterialMap matMap, BoardOrientation direction) {
		this.stone = stone;
		sizeX = tmpl.getWidth();
		sizeY = tmpl.getSizeY();
		sizeZ = tmpl.getWidth();

		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];

		int rotation = direction.ordinal() * 90;
		if (Chess.stoneHasColor(stone, Chess.BLACK)) {
			rotation = (rotation + 180) % 360;
		}

		switch (rotation) {
		case 0:
			for (int x = 0; x < sizeX; ++x) {
				for (int y = 0; y < sizeY; ++y) {
					for (int z = 0; z < sizeZ; ++z) {
						pieceArray[x][y][z] = matMap.get(tmpl.get(x, y, z));
					}
				}
			}
			break;
		case 90:
			for (int x = 0; x < sizeX; ++x) {
				for (int y = 0; y < sizeY; ++y) {
					for (int z = 0; z < sizeZ; ++z) {
						pieceArray[sizeZ - z - 1][y][x] = matMap.get(tmpl.get(x, y, z));
					}
				}
			}
			break;
		case 180:
			for (int x = 0; x < sizeX; ++x) {
				for (int y = 0; y < sizeY; ++y) {
					for (int z = 0; z < sizeZ; ++z) {
						pieceArray[sizeX - x - 1][y][sizeZ - z - 1] = matMap.get(tmpl.get(x, y, z));
					}
				}
			}
			break;
		case 270:
			for (int x = 0; x < sizeX; ++x) {
				for (int y = 0; y < sizeY; ++y) {
					for (int z = 0; z < sizeZ; ++z) {
						pieceArray[z][y][sizeX - x - 1] = matMap.get(tmpl.get(x, y, z));
					}
				}
			}
			break;
		default:
			throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270");
		}

		for (int x = 0; x < sizeX; ++x) {
			for (int y = 0; y < sizeY; ++y) {
				for (int z = 0; z < sizeZ; ++z) {
					if (pieceArray[x][y][z] != null) {
						pieceArray[x][y][z] = pieceArray[x][y][z].rotate(rotation);
					}
				}
			}
		}
	}

	public int getStone() {
		return stone;
	}

	public int getSizeX() {
		return sizeX;
	}

	public int getSizeY() {
		return sizeY;
	}

	public int getSizeZ() {
		return sizeZ;
	}

	public MaterialWithData getMaterial(int x, int y, int z) {
		return pieceArray[x][y][z];
	}

	public int getWidth() {
		return Math.max(getSizeX(), getSizeZ());
	}
}

package me.desht.chesscraft;

import chesspresso.Chess;

public class ChessStone extends PieceTemplate {
	int stone;

	public int getStone() {
		return stone;
	}

	ChessStone(int stone, PieceTemplate t) {
		super(t);
		this.stone = stone;

		if (Chess.stoneHasColor(stone, Chess.BLACK))
			rotate(180);
	}

	private void rotate(int rotation) {
		MaterialWithData[][][] newArray = new MaterialWithData[sizeX][sizeY][sizeZ];
		int maxX = sizeX - 1;
		int maxZ = sizeZ - 1;

		switch (rotation % 360) {
		case 0:
			return;
		case 90:
			for (int x = 0; x < sizeX; x++) {
				for (int y = 0; y < sizeY; y++) {
					for (int z = 0; z < sizeZ; z++) {
						newArray[z][y][maxX - x] = pieceArray[x][y][z];
					}
				}
			}
			break;
		case 180:
			for (int x = 0; x < sizeX; x++) {
				for (int y = 0; y < sizeY; y++) {
					for (int z = 0; z < sizeZ; z++) {
						newArray[maxX - x][y][maxZ - z] = pieceArray[x][y][z];
					}
				}
			}
			break;
		case 270:
			for (int x = 0; x < sizeX; x++) {
				for (int y = 0; y < sizeY; y++) {
					for (int z = 0; z < sizeZ; z++) {
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
}

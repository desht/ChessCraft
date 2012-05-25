package me.desht.chesscraft.chess.pieces;

import chesspresso.Chess;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.dhutils.LogUtils;

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
	public ChessStone(int stone, ChessPieceTemplate tmpl, MaterialMap matMap, BoardRotation direction) {
		this.stone = stone;

		int rotation = direction.ordinal() * 90;
		if (Chess.stoneHasColor(stone, Chess.BLACK)) {
			rotation = (rotation + 180) % 360;
		}

		int tmplX = tmpl.getSizeX();
		int tmplY = tmpl.getSizeY();
		int tmplZ = tmpl.getSizeZ();
		
		LogUtils.finest("ChessStone: stone = " + stone + " rotation = " + rotation);
		sizeY = tmplY;
		if (rotation == 90 || rotation == 270) {
			// allows for pieces with a non-square footprint
			sizeZ = tmplX;
			sizeX = tmplZ;
		} else {
			sizeX = tmplX;
			sizeZ = tmplZ;
		}
		LogUtils.finest("ChessStone: tmpl size = " + tmplX + "," + tmplY + "," + tmplZ + ", stone size = " + sizeX + "," + sizeY + "," + sizeZ);
		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];

		switch (rotation) {
		case 0:
			for (int x = 0; x < tmplX; ++x) {
				for (int y = 0; y < tmplY; ++y) {
					for (int z = 0; z < tmplZ; ++z) {
						pieceArray[x][y][z] = matMap.get(tmpl.get(x, y, z));
					}
				}
			}
			break;
		case 90:
			for (int x = 0; x < tmplX; ++x) {
				for (int y = 0; y < tmplY; ++y) {
					for (int z = 0; z < tmplZ; ++z) {
						pieceArray[sizeX - z - 1][y][x] = matMap.get(tmpl.get(x, y, z)).rotate(90);
					}
				}
			}
			break;
		case 180:
			for (int x = 0; x < tmplX; ++x) {
				for (int y = 0; y < tmplY; ++y) {
					for (int z = 0; z < tmplZ; ++z) {
						pieceArray[sizeX - x - 1][y][sizeZ - z - 1] = matMap.get(tmpl.get(x, y, z)).rotate(180);
					}
				}
			}
			break;
		case 270:
			for (int x = 0; x < tmplX; ++x) {
				for (int y = 0; y < tmplY; ++y) {
					for (int z = 0; z < tmplZ; ++z) {
						pieceArray[z][y][sizeZ - x - 1] = matMap.get(tmpl.get(x, y, z)).rotate(270);
					}
				}
			}
			break;
		default:
			throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270");
		}
		LogUtils.finer("ChessStone: instantiated stone " + stone + ", rotation " + rotation);
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

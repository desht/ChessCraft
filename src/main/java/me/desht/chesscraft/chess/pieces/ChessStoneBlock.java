package me.desht.chesscraft.chess.pieces;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.World;
import org.bukkit.block.Block;

import chesspresso.Chess;
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.dhutils.LogUtils;

public class ChessStoneBlock implements ChessStone {
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
	public ChessStoneBlock(int stone, ChessPieceTemplate tmpl, MaterialMap matMap, BoardRotation direction) {
		this.stone = stone;

		int rotation = direction.ordinal() * 90;
		if (Chess.stoneHasColor(stone, Chess.BLACK)) {
			rotation = (rotation + 180) % 360;
		}

		int tmplX = tmpl.getSizeX();
		int tmplY = tmpl.getSizeY();
		int tmplZ = tmpl.getSizeZ();
		
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
	
	@Override
	public void paint(Cuboid region, MassBlockUpdate mbu) {
		assert region.getSizeX() >= getSizeX();
		assert region.getSizeZ() >= getSizeZ();

		int xOff = (region.getSizeX() - getSizeX()) / 2;
		int zOff = (region.getSizeZ() - getSizeZ()) / 2;

		Map<Block,MaterialWithData> deferred = new HashMap<Block, MaterialWithData>();
		World world = region.getWorld();
		for (int x = 0; x < getSizeX(); x++) {
			for (int y = 0; y < getSizeY(); y++) {
				for (int z = 0; z < getSizeZ(); z++) {
					MaterialWithData mat = getMaterial(x, y, z);
					if (mat.getId() == 0) {
						// the region was pre-cleared, skip placing air a second time
						continue;
					}
					Block b = region.getRelativeBlock(world, x + xOff, y, z + zOff);
					if (BlockType.shouldPlaceLast(mat.getId())) {
						deferred.put(b, mat);
					} else {
						mat.applyToBlock(b, mbu);
					}
				}	
			}	
		}

		for (Entry<Block,MaterialWithData> e : deferred.entrySet()) {
			e.getValue().applyToBlock(e.getKey(), mbu);
		}
	}
}

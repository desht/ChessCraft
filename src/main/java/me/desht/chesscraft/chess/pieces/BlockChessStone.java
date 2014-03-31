package me.desht.chesscraft.chess.pieces;

import chesspresso.Chess;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class BlockChessStone extends ChessStone {
	private final MaterialWithData[][][] pieceArray;

	/**
	 * Instantiate a new chess stone
	 *
	 * @param stone the numeric Chesspresso stone ID
	 * @param tmpl the piece template
	 * @param matMap the material map
	 * @param direction the board orientation
	 */
	public BlockChessStone(int stone, ChessPieceTemplate tmpl, MaterialMap matMap, BoardRotation direction) {
		super(stone);

		int rotation = rotationNeeded(direction, Chess.stoneToColor(stone));

		int tmplX = tmpl.getSizeX();
		int tmplY = tmpl.getSizeY();
		int tmplZ = tmpl.getSizeZ();

		if (rotation == 90 || rotation == 270) {
			// allows for pieces with a non-square footprint
			setSize(tmplZ, tmplY, tmplX);
		} else {
			setSize(tmplX, tmplY, tmplZ);
		}
		Debugger.getInstance().debug(3, "ChessStone: tmpl size = " + tmplX + "," + tmplY + "," + tmplZ + ", stone size = " + getSizeX() + "," + getSizeY() + "," + getSizeZ());
		pieceArray = new MaterialWithData[getSizeX()][getSizeY()][getSizeZ()];

		int sx = getSizeX();
		int sz = getSizeZ();

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
						pieceArray[sx - z - 1][y][x] = matMap.get(tmpl.get(x, y, z)).rotate(90);
					}
				}
			}
			break;
		case 180:
			for (int x = 0; x < tmplX; ++x) {
				for (int y = 0; y < tmplY; ++y) {
					for (int z = 0; z < tmplZ; ++z) {
						pieceArray[sx - x - 1][y][sz - z - 1] = matMap.get(tmpl.get(x, y, z)).rotate(180);
					}
				}
			}
			break;
		case 270:
			for (int x = 0; x < tmplX; ++x) {
				for (int y = 0; y < tmplY; ++y) {
					for (int z = 0; z < tmplZ; ++z) {
						pieceArray[z][y][sz - x - 1] = matMap.get(tmpl.get(x, y, z)).rotate(270);
					}
				}
			}
			break;
		default:
			throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270");
		}
		Debugger.getInstance().debug(2, "ChessStone: instantiated stone " + stone + ", rotation " + rotation);
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
					MaterialWithData mat = pieceArray[x][y][z];
					if (mat.getId() == 0) {
						// we expect that the region was pre-cleared, skip placing air a second time
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

	@Override
	public void move(int fromSqi, int toSqi, Location to, ChessStone captured) {
		// no-op for block sets; they're not animated
//		System.out.println("moveTo: " + fromSqi + " -> " + toSqi + " to loc = " + to);
	}

	private int rotationNeeded(BoardRotation direction, int colour) {
		int rot;
		switch (direction) {
		case NORTH: rot = 0; break;
		case EAST: rot = 90; break;
		case SOUTH: rot = 180; break;
		case WEST: rot = 270; break;
		default: rot = 0; break;
		}
		if (colour == Chess.BLACK){
			rot = (rot + 180) % 360;
		}
		return rot;
	}
}

package me.desht.chesscraft.chess.pieces;

import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;

public abstract class ChessStone {
	private final int stone;
	private int sizeX, sizeY, sizeZ;
	
	protected ChessStone(int stone) {
		this.stone = stone;
	}

	protected void setSize(int sizeX, int sizeY, int sizeZ) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}
	
	public abstract void paint(Cuboid region, MassBlockUpdate mbu);
	
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
	
	public int getWidth() {
		return Math.max(getSizeX(), getSizeZ());
	}
	
}

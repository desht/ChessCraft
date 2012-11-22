package me.desht.chesscraft.enums;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public enum BoardRotation {

	NORTH(-1, 0), // north = -x
	EAST(0, -1), // east = -z
	SOUTH(1, 0), // south = +x
	WEST(0, 1);// west = +z
	
	// the increments if moving in this direction
	private final int x, z;

	private BoardRotation(int xPositive, int zPositive) {
		x = xPositive;
		z = zPositive;
	}

	public int getXadjustment() {
		return x;
	}

	public int getZadjustment() {
		return z;
	}

	public int getXadjustment(int offset) {
		return x * offset;
	}

	public int getZadjustment(int offset) {
		return z * offset;
	}
	
	/**
	 * Get the rotation to the right (clockwise) of the current rotation
	 * 
	 * @return the rotation to the right
	 */
	public BoardRotation getRight() {
		if (this.ordinal() >= BoardRotation.values().length - 1) {
			return BoardRotation.values()[0];
		} else {
			return BoardRotation.values()[this.ordinal() + 1];
		}
	}

	/**
	 * Get the rotation to the left (anti-clockwise) of the current rotation
	 * 
	 * @return the rotation to the left
	 */
	public BoardRotation getLeft() {
		if (this.ordinal() == 0) {
			return BoardRotation.values()[BoardRotation.values().length - 1];
		} else {
			return BoardRotation.values()[this.ordinal() - 1];
		}
	}

	public Direction getDirection() {
		switch (this) {
			case NORTH:
				return Direction.North;
			case EAST:
				return Direction.East;
			case SOUTH:
				return Direction.South;
			case WEST:
				return Direction.West;
		}
		return null; // should not get here..
	}

	public float getYaw() {
		switch (this) {
			case NORTH:
				return 90;
			case EAST:
				return 180;
			case SOUTH:
				return 270;
			case WEST:
				return 0;
		}
		return 0;
	}

	public static BoardRotation getRotation(Location loc) {
		double rot = (loc.getYaw() - 90) % 360;
		if (rot < 0) {
			rot += 360;
		}
		if ((0 <= rot && rot < 45) || (315 <= rot && rot < 360.0)) {
			return BoardRotation.NORTH;
		} else if (45 <= rot && rot < 135) {
			return BoardRotation.EAST;
		} else if (135 <= rot && rot < 225) {
			return BoardRotation.SOUTH;
		} else if (225 <= rot && rot < 315) {
			return BoardRotation.WEST;
		} else {
			throw new IllegalArgumentException("impossible rotation: " + rot);
		}
	}
	
	public static BoardRotation getRotation(Player p) {
		return getRotation(p.getLocation());
	}

	public static BoardRotation getRotation(String name) {
		for (BoardRotation o : values()) {
			if (o.name().equalsIgnoreCase(name)) {
				return o;
			}
		}
		return null;
	}
}

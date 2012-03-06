package me.desht.chesscraft.enums;

public enum BoardRotation {

	NORTH(-1, 0), // north = -x
	EAST(0, -1), // east = -z
	SOUTH(1, 0), // south = +x
	WEST(0, 1);// west = +z
	/**
	 * the increments if moving in this direction
	 */
	private final int x, z;

	BoardRotation(int xPositive, int zPositive) {
		x = xPositive;
		z = zPositive;
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
	}

	/**
	 * this is to the <i>right</i> of the direction
	 * @return the direction if it is turned right
	 */
	public BoardRotation getRight() {
		if (this.ordinal() >= BoardRotation.values().length - 1) {
			return BoardRotation.values()[0];
		} else {
			return BoardRotation.values()[this.ordinal() + 1];
		}
	}

	/**
	 * this is to the <i>left</i> of the direction
	 * @return the direction if it is turned left
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

	public static BoardRotation getPlayerDirection(org.bukkit.entity.Player p) {
		if (p != null) {
			// get the direction the player is facing
			double rot = (p.getLocation().getYaw() - 90) % 360;
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
			}
		}
		return null;
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

	public static BoardRotation get(String name) {
		for (BoardRotation o : values()) {
			if (o.name().equalsIgnoreCase(name)) {
				return o;
			}
		}
		return null;
	}
}

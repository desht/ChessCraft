package me.desht.chesscraft.enums;

public enum BoardOrientation {

	NORTH(-1, 0), // north = -x
	EAST(0, -1), // east = -z
	SOUTH(1, 0), // south = +x
	WEST(0, 1);// west = +z
	/**
	 * the increments if moving in this direction
	 */
	int x, z;

	BoardOrientation(int xPositive, int zPositive) {
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
	public BoardOrientation getRight() {
		if (this.ordinal() >= BoardOrientation.values().length - 1) {
			return BoardOrientation.values()[0];
		} else {
			return BoardOrientation.values()[this.ordinal() + 1];
		}
	}

	/**
	 * this is to the <i>left</i> of the direction
	 * @return the direction if it is turned left
	 */
	public BoardOrientation getLeft() {
		if (this.ordinal() == 0) {
			return BoardOrientation.values()[BoardOrientation.values().length - 1];
		} else {
			return BoardOrientation.values()[this.ordinal() - 1];
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

	public static BoardOrientation getPlayerDirection(org.bukkit.entity.Player p) {
		if (p != null) {
			// get the direction the player is facing
			double rot = (p.getLocation().getYaw() - 90) % 360;
			if (rot < 0) {
				rot += 360;
			}
			if ((0 <= rot && rot < 45) || (315 <= rot && rot < 360.0)) {
				return BoardOrientation.NORTH;
			} else if (45 <= rot && rot < 135) {
				return BoardOrientation.EAST;
			} else if (135 <= rot && rot < 225) {
				return BoardOrientation.SOUTH;
			} else if (225 <= rot && rot < 315) {
				return BoardOrientation.WEST;
			}
		}
		return null;
	}

	public float getYaw() {
		switch (this) {
			case NORTH:
				return (float) 22.5;
			case EAST:
				return (float) (45 + 22.5);
			case SOUTH:
				return (float) (135 + 22.5);
			case WEST:
				return (float) (225 + 22.5);
		}
		return 0;
	}

	public static BoardOrientation get(String name){
		for(BoardOrientation o : values()){
			if(o.name().equalsIgnoreCase(name)){
				return o;
			}
		}
		return null;
	}
}

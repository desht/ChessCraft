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
}

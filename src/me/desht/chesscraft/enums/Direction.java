package me.desht.chesscraft.enums;

public enum Direction {

	North, East, South, West, Up, Down, Horizontal, Vertical, Both, Unknown;
	public Direction opposite(){
		switch(this){
			case North:
				return South;
			case East:
				return West;
			case South:
				return North;
			case West:
				return East;
			case Horizontal:
				return Vertical;
			case Vertical:
				return Horizontal;
			default:
				return Unknown;
		}
	}
};

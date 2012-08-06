package me.desht.chesscraft.exceptions;

public class ChessWorldNotLoadedException extends ChessException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String worldName;
	
	public ChessWorldNotLoadedException(String worldName) {
		this.worldName = worldName;
	}
	
	public String getWorldName() {
		return worldName;
	}

}

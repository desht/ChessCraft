/**
 * Programmer: Jacob Scott
 * Program Name: ChessSet
 * Description: wrapper for all of the chess sets
 * Date: Jul 28, 2011
 */
package me.desht.chesscraft.chess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.desht.chesscraft.chess.pieces.ChessStone;
import me.desht.chesscraft.chess.pieces.PieceTemplate;

public class ChessSet implements Iterable<ChessStone> {

	private Map<Integer, ChessStone> stoneCache = new HashMap<Integer, ChessStone>();

	public ChessSet(Map<Integer, PieceTemplate> stones) {
		for(int i : stones.keySet()){
			stoneCache.put(i, new ChessStone(i, stones.get(i)));
		}
	} // end default constructor

	public Iterator<ChessStone> iterator() {
		return new ChessPieceIterator();
	}

	public ChessStone getPiece(int stone){
		return stoneCache.get(stone);
	}
	
	// non-static class specific to this ChessSet instance
	public class ChessPieceIterator implements Iterator<ChessStone> {

		int i = 0;
		Integer keys[] = new Integer[0];
		
		public ChessPieceIterator(){
			keys = stoneCache.keySet().toArray(keys);
		}
		
		public boolean hasNext() {
			return keys.length > i;
		}

		public ChessStone next() {
			// simply iterates through values.. not through keys
			return stoneCache.get(keys[i++]);
		}

		public void remove() {
		}

	}
} // end class ChessSet


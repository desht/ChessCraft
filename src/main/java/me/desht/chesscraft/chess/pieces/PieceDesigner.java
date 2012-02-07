package me.desht.chesscraft.chess.pieces;

import java.util.HashMap;
import java.util.Map;

import chesspresso.Chess;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.regions.Cuboid;

public class PieceDesigner {
	private BoardView view;
	private PieceTemplate[] pieces;
	
	private Map<Character, String> materialMap;
	
	public PieceDesigner(BoardView view) {
		this.view = view;
		
		materialMap = new HashMap<Character,String>();

		pieces = new PieceTemplate[Chess.MAX_PIECE + 1];
		for (int p = Chess.MIN_PIECE + 1; p <= Chess.MAX_PIECE; p++) {
			Cuboid c = getPieceBox(p);
			pieces[p] = new PieceTemplate(c.getSizeX(), c.getSizeY(), c.getSizeZ());
			pieces[p].useMaterialMap(materialMap);
		}
	}

	private Cuboid getPieceBox(int p) {
		int sqi;
		switch (p) {
		case Chess.PAWN:
			sqi = Chess.A2;
			break;
		case Chess.KNIGHT:
			sqi = Chess.B1;
			break;
		case Chess.BISHOP:
			sqi = Chess.C1;
			break;
		case Chess.ROOK:
			sqi = Chess.A1;
			break;
		case Chess.QUEEN:
			sqi = Chess.D1;
			break;
		case Chess.KING:
			sqi = Chess.E1;
			break;
		default:
			throw new IllegalArgumentException("Invalid chess piece " + p);	
		}
		Cuboid c = view.getChessBoard().getPieceRegion(Chess.sqiToRow(sqi), Chess.sqiToCol(sqi));
		
		// TODO: shrink the cuboid to the smallest bounding box that holds the piece (no air)
		
		return c;
	}
}

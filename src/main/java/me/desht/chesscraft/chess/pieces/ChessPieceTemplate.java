package me.desht.chesscraft.chess.pieces;

import java.util.ArrayList;
import java.util.List;

public class ChessPieceTemplate {
	private final int sizeX, sizeY, sizeZ;
	private final char[][][] pieceData;
	
	public ChessPieceTemplate(List<List<String>> data) {
		sizeY = data.size();
		sizeZ = data.get(0).size();
		sizeX = data.get(0).get(0).length();
		pieceData = new char[sizeX][sizeY][sizeZ];

		// scan bottom to top
		for (int y = 0; y < sizeY; ++y) {
			List<String> yRow = data.get(y);
			for (int x = sizeX - 1; x >= 0; --x) {
				String xRow = yRow.get(x);
				for (int z = sizeZ - 1; z >= 0; --z) {
					char k = xRow.charAt(z);
					pieceData[x][y][z] = k;
				}
			}
		}
	}
	
	public ChessPieceTemplate(int x, int y, int z) {
		assert(x > 0);
		assert(y > 0);
		assert(z > 0);
		this.sizeX = x;
		this.sizeY = y;
		this.sizeZ = z;
		pieceData = new char[sizeX][sizeY][sizeZ];
	}
	
	public int getSizeX() {
		return pieceData.length;
	}

	public int getSizeY() {
		return pieceData[0].length;
	}

	public int getSizeZ() {
		return pieceData[0][0].length;
	}
	
	public char get(int x, int y, int z) {
		return pieceData[x][y][z];
	}
	
	public void put(int x, int y, int z, char data) {
		pieceData[x][y][z] = data;
	}
	
	public List<List<String>> getPieceData() {
		List<List<String>> res = new ArrayList<List<String>>();
		
		res = new ArrayList<List<String>>(sizeY);

		for (int y = 0; y < sizeY; ++y) {
			res.add(new ArrayList<String>(sizeX));
			for (int x = 0; x < sizeX; ++x) {
				StringBuilder sb = new StringBuilder();
				for (int z = sizeZ - 1; z >= 0; z--) {
					char c = pieceData[x][y][z];
					sb.append(c);
				}
				res.get(y).add(sb.toString());
			}
		}
		
		return res;
	}

	public int getWidth() {
		return Math.max(getSizeX(), getSizeZ());
	}
}

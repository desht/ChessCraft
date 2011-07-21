package me.desht.chesscraft;

import java.util.List;
import java.util.Map;

import me.desht.chesscraft.exceptions.ChessException;

public class PieceTemplate {
	protected MaterialWithData[][][] pieceArray;
	protected int sizeX, sizeY, sizeZ;

	PieceTemplate(List<List<String>> data, Map<String, String> matMap) throws ChessException {
		sizeY = data.size();
		sizeZ = data.get(0).size();
		sizeX = data.get(0).get(0).length();

		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
		for (int y = 0; y < sizeY; y++) {
			List<String> yRow = data.get(y);
			for (int x = 0; x < sizeX; x++) {
				String zRow = yRow.get(sizeX - (x + 1));
				for (int z = 0; z < sizeZ; z++) {
					String k = zRow.substring(z, z + 1);
					if (!matMap.containsKey(k))
						throw new ChessException("unknown character '" + k + "' found.");
					pieceArray[x][y][z] = MaterialWithData.parseIdAndData(matMap.get(k));
				}
			}
		}
	}

	// create a blank template
	PieceTemplate(int sizeX, int sizeY, int sizeZ) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;

		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
	}
	
	// copy constructor
	PieceTemplate(PieceTemplate t) {
		sizeX = t.getSizeX();
		sizeY = t.getSizeY();
		sizeZ = t.getSizeZ();
		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
		for (int i = 0; i < pieceArray.length; i++) {
			for (int j = 0; j < pieceArray[i].length; j++) {
				for (int k = 0; k < pieceArray[i][j].length; k++) {
					pieceArray[i][j][k] = new MaterialWithData(t.getMaterial(i, j, k));
				}
			}
		}
	}

	int getSizeX() {
		return pieceArray[0][0].length;
	}

	int getSizeY() {
		return pieceArray.length;
	}

	int getSizeZ() {
		return pieceArray[0].length;
	}

	MaterialWithData getMaterial(int x, int y, int z) {
		return pieceArray[x][y][z];
	}
	
	void setMaterial(int x, int y, int z, MaterialWithData mwd) {
		pieceArray[x][y][z] = mwd;
	}

//	@Override
//	public String toString() {
//		
//	}

}

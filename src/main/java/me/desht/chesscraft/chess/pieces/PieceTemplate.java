package me.desht.chesscraft.chess.pieces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import me.desht.chesscraft.blocks.MaterialWithData;

import me.desht.chesscraft.exceptions.ChessException;

public class PieceTemplate {

	protected MaterialWithData[][][] pieceArray;
	protected final int sizeX, sizeY, sizeZ;
	List<List<String>> pieceData = null;
	Map<Character, String> pieceMaterials = null;

	public PieceTemplate(List<List<String>> data, Map<String, String> matMap) throws ChessException {
		sizeY = data.size();
		sizeZ = data.get(0).size();
		sizeX = data.get(0).get(0).length();

		Map<Character, MaterialWithData> mats = new HashMap<Character, MaterialWithData>();
		for (Entry<String, String> entry : matMap.entrySet()) {
			if (entry.getKey().length() != 1) {
				throw new ChessException("invalid key loading PieceTemplate: " + entry.getKey());
			}
			mats.put(entry.getKey().charAt(0), new MaterialWithData(entry.getValue()));
		}

		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];

		for (int y = 0; y < sizeY; ++y) {
			List<String> yRow = data.get(y);
			for (int x = sizeX - 1; x >= 0; --x) {
				String xRow = yRow.get(x);
				for (int z = sizeZ - 1; z >= 0; --z) {
					char k = xRow.charAt(z);
					if (!mats.containsKey(k)) {
						throw new ChessException("unknown character '" + k + "' found.");
					}
					pieceArray[x][y][z] = mats.get(k);
				}
			}
		}
	}

	// create a blank template
	public PieceTemplate(int sizeX, int sizeY, int sizeZ) {
		this.sizeX = sizeX < 0 ? 0 : sizeX;
		this.sizeY = sizeY < 0 ? 0 : sizeY;
		this.sizeZ = sizeZ < 0 ? 0 : sizeZ;

		pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
	}

	// copy constructor
	public PieceTemplate(PieceTemplate t) {
		if (t != null) {
			sizeX = t.getSizeX();
			sizeY = t.getSizeY();
			sizeZ = t.getSizeZ();
			pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
			MaterialWithData m = null;
			for (int x = 0; x < sizeX; ++x) {
				for (int y = 0; y < sizeY; ++y) {
					for (int z = 0; z < sizeZ; ++z) {
						m = t.getMaterial(x, y, z);
						if (m != null) {
							pieceArray[x][y][z] = m.clone();
						}
					}
				}
			}
		} else {
			sizeX = sizeY = sizeZ = 0;
			pieceArray = new MaterialWithData[0][0][0];
		}
	}

	public int getSizeX() {
		return pieceArray.length;
	}

	public int getSizeY() {
		return pieceArray[0].length;
	}

	public int getSizeZ() {
		return pieceArray[0][0].length;
	}

	public MaterialWithData getMaterial(int x, int y, int z) {
		return x >= 0 && y >= 0 && z >= 0
				&& pieceArray.length > x
				&& pieceArray[x].length > y
				&& pieceArray[x][y].length > z
				? pieceArray[x][y][z] : null;
	}

	public void setMaterial(int x, int y, int z, MaterialWithData mwd) {
		if (x >= 0 && y >= 0 && z >= 0
				&& pieceArray.length < x
				&& pieceArray[x].length < y
				&& pieceArray[x][y].length < z) {
			pieceArray[x][y][z] = mwd;
		}
	}

	public final void rotate(int rotation) {
		MaterialWithData[][][] newArray = new MaterialWithData[sizeX][sizeY][sizeZ];

		//TODO: allow pieces with data (signs, torches, ladders) to rotate, too

		switch (rotation % 360) {
			case 0:
				return;
			case 90:
				for (int x = 0; x < sizeX; ++x) {
					for (int y = 0; y < sizeY; ++y) {
						for (int z = 0; z < sizeZ; ++z) {
							newArray[sizeZ - z - 1][y][x] = pieceArray[x][y][z];
						}
					}
				}
				break;
			case 180:
				for (int x = 0; x < sizeX; ++x) {
					for (int y = 0; y < sizeY; ++y) {
						for (int z = 0; z < sizeZ; ++z) {
							newArray[sizeX - x - 1][y][sizeZ - z - 1] = pieceArray[x][y][z];
						}
					}
				}
				break;
			case 270:
				for (int x = 0; x < sizeX; ++x) {
					for (int y = 0; y < sizeY; ++y) {
						for (int z = 0; z < sizeZ; ++z) {
							newArray[z][y][sizeX - x - 1] = pieceArray[x][y][z];
						}
					}
				}
				break;
			default:
				throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270");
		}

		for (int x = 0; x < sizeX; ++x) {
			for (int y = 0; y < sizeY; ++y) {
				for (int z = 0; z < sizeZ; ++z) {
					if (newArray[x][y][z] != null) {
						newArray[x][y][z].rotate(rotation);
					}
				}
			}
		}
		
		pieceArray = newArray;
	}

	public List<List<String>> getData() {
		if (pieceData == null) {
			scan();
		}
		return pieceData;
	}

	public Map<Character, String> getMaterialMap() {
		if (pieceMaterials == null) {
			scan();
		}
		return pieceMaterials;
	}

	/**
	 * (Re)generate the piece data array and material map
	 */
	private void scan() {
		pieceData = new ArrayList<List<String>>(sizeY);
		pieceMaterials = new HashMap<Character, String>();

		Map<MaterialWithData, Character> matToStr = new HashMap<MaterialWithData, Character>();
		int i = 65; // ASCII 'A'

		for (int y = 0; y < sizeY; ++y) {
			pieceData.add(new ArrayList<String>(sizeX));
			for (int x = 0; x < sizeX; ++x) {
				StringBuilder sb = new StringBuilder();
				for (int z = 0; z < sizeZ; ++z) {
					MaterialWithData m = getMaterial(x, y, z);
					if (!matToStr.containsKey(m)) {
						Character c = (char) i++;
						matToStr.put(m, c);
						pieceMaterials.put(c, m.toString());
					}
					sb.append(matToStr.get(m));
				}
				pieceData.get(y).add(sb.toString());
			}
		}
	}
}

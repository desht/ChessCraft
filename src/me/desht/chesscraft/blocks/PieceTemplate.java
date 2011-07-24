package me.desht.chesscraft.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.chesscraft.exceptions.ChessException;

public class PieceTemplate {

    protected MaterialWithData[][][] pieceArray;
    protected final int sizeX, sizeY, sizeZ;
    List<List<String>> pieceData = null;
    Map<String, String> pieceMaterials = null;

    public PieceTemplate(List<List<String>> data, Map<String, String> matMap) throws ChessException {
        sizeY = data.size();
        sizeZ = data.get(0).size();
        sizeX = data.get(0).get(0).length();

        Map<String, MaterialWithData> mats = new HashMap<String, MaterialWithData>();
        for (Entry<String, String> entry : matMap.entrySet()) {
            mats.put(entry.getKey(), new MaterialWithData(entry.getValue()));
        }

        pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
        for (int y = 0; y < sizeY; ++y) {
            List<String> yRow = data.get(y);
            for (int x = 0; x < sizeX; ++x) {
                String zRow = yRow.get(sizeX - (x + 1));
                for (int z = 0; z < sizeZ; ++z) {
                    String k = zRow.substring(z, z + 1);
                    if (!matMap.containsKey(k)) {
                        throw new ChessException("unknown character '" + k + "' found.");
                    }
                    pieceArray[x][y][z] = mats.get(k);
                }
            }
        }
    }

    // create a blank template
    public PieceTemplate(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;

        pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];
    }

    // copy constructor
    public PieceTemplate(PieceTemplate t) {
        sizeX = t.getSizeX();
        sizeY = t.getSizeY();
        sizeZ = t.getSizeZ();
        pieceArray = new MaterialWithData[sizeX][sizeY][sizeZ];

        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                for (int z = 0; z < sizeZ; ++z) {
                    pieceArray[x][y][z] = new MaterialWithData(t.getMaterial(x, y, z));
                }
            }
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
        return pieceArray[x][y][z];
    }

    public void setMaterial(int x, int y, int z, MaterialWithData mwd) {
        pieceArray[x][y][z] = mwd;
    }

    public List<List<String>> getData() {
        if (pieceData == null) {
            scan();
        }
        return pieceData;
    }

    public Map<String, String> getMaterialMap() {
        if (pieceMaterials == null) {
            scan();
        }
        return pieceMaterials;
    }

    // (Re)generate the piece data array and material map
    private void scan() {
        pieceData = new ArrayList<List<String>>(sizeY);
        pieceMaterials = new HashMap<String, String>();

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
                        pieceMaterials.put(c.toString(), m.toString());
                    }
                    sb.append(matToStr.get(m));
                }
                pieceData.get(y).add(sb.toString());
            }
        }
    }
}

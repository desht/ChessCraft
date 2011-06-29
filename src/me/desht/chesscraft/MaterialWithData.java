package me.desht.chesscraft;

public class MaterialWithData {
	int material;
	byte data;

	MaterialWithData(int mat, byte d) {
		material = mat;
		data = d;
	}
	
	MaterialWithData(MaterialWithData m) {
		material = m.material;
		data = m.data;
	}
}


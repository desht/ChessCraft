package me.desht.chesscraft;

import org.bukkit.DyeColor;
import org.bukkit.Material;

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
	
	static MaterialWithData parseIdAndData(String string) {
		String[] items = string.split(":");
		int mat;
		byte data;
		
		if (items[0].matches("^[0-9]+$")) {
			mat = Integer.parseInt(items[0]);
		} else {
			Material m = Material.valueOf(items[0].toUpperCase());
			if (m == null) throw new IllegalArgumentException("unknown material " + items[0]);
			mat = m.getId();
		}
		if (items.length < 2) 
			return new MaterialWithData(mat, (byte)-1);
		
		if (items[1].matches("^[0-9]+$")) {
			data = Byte.parseByte(items[1]);
		} else if (mat == 35) {	// wool
			DyeColor d = DyeColor.valueOf(items[1].toUpperCase());
			if (d == null) throw new IllegalArgumentException("unknown dye colour " + items[0]);
			data = d.getData();
		} else {
			throw new IllegalArgumentException("invalid data specification " + items[1]);
		}
		return new MaterialWithData(mat, data);
	}
	
	@Override
	public String toString() {
		String s = Material.getMaterial(material).toString();
		if (material == 35)	// wool
			s = s + ":" + DyeColor.getByData(data).toString(); 
		return s;
	}
}


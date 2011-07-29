package me.desht.chesscraft.blocks;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class MaterialWithData {

	int material;
	byte data;

	public MaterialWithData(int mat, byte d) {
		material = mat;
		data = d;
	}

	public MaterialWithData(int mat) {
		material = mat;
		data = 0;
	}

	public MaterialWithData(MaterialWithData m) {
		material = m.material;
		data = m.data;
	}

	public MaterialWithData(String string) {
		String[] items = string.split(":");
		data = 0;

		if (items[0].matches("^[0-9]+$")) {
			material = Integer.parseInt(items[0]);
		} else {
			Material m = Material.valueOf(items[0].toUpperCase());
			if (m == null) {
				throw new IllegalArgumentException("unknown material " + items[0]);
			}
			material = m.getId();
		}
		if (items.length < 2) {
			return;
		}

		if (items[1].matches("^[0-9]+$")) {
			data = Byte.parseByte(items[1]);
		} else if (material == 35) { // wool
			DyeColor d = DyeColor.valueOf(items[1].toUpperCase());
			if (d == null) {
				throw new IllegalArgumentException("unknown dye colour " + items[0]);
			}
			data = d.getData();
		} else {
			throw new IllegalArgumentException("invalid data specification " + items[1]);
		}
	}

	public byte getData() {
		return data;
	}

	public int getMaterial() {
		return material;
	}

	public void applyToBlock(Block b) {
		if (b != null) {
			if (data >= 0) {
				b.setTypeIdAndData(material, data, false);
			} else {
				b.setTypeId(material);
			}
		}
	}

	@Override
	public String toString() {
		String s = Material.getMaterial(material).toString();
		if (material == 35) // wool
		{
			s = s + ":" + DyeColor.getByData(data).toString();
		} else {
			s = s + ":" + data;
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + data;
		result = prime * result + material;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MaterialWithData other = (MaterialWithData) obj;
		if (data != other.data) {
			return false;
		}
		if (material != other.material) {
			return false;
		}
		return true;
	}
}

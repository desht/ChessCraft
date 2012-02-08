package me.desht.chesscraft.blocks;

import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class MaterialWithData implements Cloneable {

	int material;
	byte data;
	String[] text;	// sign text

	public MaterialWithData(int mat, byte d) {
		material = mat;
		data = d;
	}

	public MaterialWithData(int mat) {
		material = mat;
		data = 0;
	}

	public MaterialWithData(MaterialWithData m) {
		if (m != null) {
			material = m.material;
			data = m.data;
		}
	}

	public MaterialWithData(String string) {
		String[] matAndText = string.split("=");
		String[] matAndData = matAndText[0].split(":");
		
		data = 0;
		text = matAndText.length > 1 ? makeText(matAndText[1]) : null;
		
		if (matAndData[0].matches("^[0-9]+$")) {
			material = Integer.parseInt(matAndData[0]);
		} else {
			Material m = Material.valueOf(matAndData[0].toUpperCase());
			if (m == null) {
				throw new IllegalArgumentException("unknown material " + matAndData[0]);
			}
			material = m.getId();
		}
		if (matAndData.length < 2) {
			return;
		}

		if (matAndData[1].matches("^[0-9]+$")) {
			data = Byte.parseByte(matAndData[1]);
		} else if (material == 35) { // wool
			DyeColor d = DyeColor.valueOf(matAndData[1].toUpperCase());
			if (d == null) {
				throw new IllegalArgumentException("unknown dye colour " + matAndData[0]);
			}
			data = d.getData();
		} else {
			throw new IllegalArgumentException("invalid data specification " + matAndData[1]);
		}
	}

	private String[] makeText(String input) {
		String[] t = new String[] {"", "", "", ""};
		String[] s = input.split(";");
		for (int i = 0; i < 4 && i < s.length; i++) {
			t[i] = s[i];
		}
		return t;
	}

	public Byte getData() {
		return data;
	}

	public int getMaterial() {
		return material;
	}
	
	public String[] getText() {
		return text;
	}

	public void applyToBlock(Block b) {
		if (b != null) {
//			if (data != null) {
//				b.setTypeIdAndData(material, data, false);
//			} else {
//				b.setTypeId(material);
//			}
			b.setTypeIdAndData(material, data, false);
			if (text != null && (material == 63 || material == 68)) {
				// updating a wall sign or floor sign, with text
				Sign sign = (Sign) b.getState().getData();
				for (int i = 0; i < 4; i++) {
					sign.setLine(i, text[i]);
				}
				sign.update();
			}
		}
	}
	
	/**
	 * Use direct NMS calls to apply this block.  The caller is responsible for ensuring that
	 * lighting is re-initialised afterwards.
	 * 
	 * @param b
	 */
	public void applyToBlockFast(Block b) {
		if (b != null) {
//			if (data != null) {
//				BlockUtils.setBlockFast(b, material, data);
//			} else {
//				BlockUtils.setBlockFast(b, material);
//			}
			BlockUtils.setBlockFast(b, material, data);
			if (text != null && (material == 63 || material == 68)) {
				// updating a wall sign or floor sign, with text
				Sign sign = (Sign) b.getState().getData();
				for (int i = 0; i < 4; i++) {
					sign.setLine(i, text[i]);
				}
				sign.update();
			}
		}
	}

	public void applyToCuboid(Cuboid c) {
		if (c != null) {
//			if (data != null) {
//				c.set(material, data, true);
//			} else {
//				c.set(material, true);
//			}
			c.set(material, data, true);
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(Material.getMaterial(material).toString());
		if (material == 35) // wool
		{
			s.append(":").append(DyeColor.getByData(data).toString());
//		} else if (data != null) {
//			s.append(":").append(data.toString());
		} else {
			s.append(":").append(Byte.toString(data));
		}
		return s.toString();
	}

	@Override
	public MaterialWithData clone(){
		return new MaterialWithData(this);
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
		if (material != other.material) {
			return false;
//		} else if (data == null && other.data == null) {
//			return true;
//		} else if (!data.equals(other.data)) {
//			return false;
		} else if (data != other.data) {
			return false;
		} else {
			return true;
		}
	}

	public void rotate(int rotation) {
//		if (data == null) {
//			return;
//		}
		switch(rotation){
			case 270:
				data = (byte) BlockData.rotate90Reverse(material, data);
				break;
			case 180:
				data = (byte) BlockData.rotate90(material, data);
				// 180 does twice, so don't break
			case 90:
				data = (byte) BlockData.rotate90(material, data);
		}
		
	}
}

package me.desht.chesscraft.blocks;

import java.util.HashMap;
import java.util.Map;

import me.desht.chesscraft.regions.Cuboid;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import com.google.common.base.Joiner;

public class MaterialWithData implements Cloneable {

	private final static Map<String,MaterialWithData> materialCache = new HashMap<String, MaterialWithData>();

	final int material;
	final byte data;
	final String[] metadata;	// e.g. sign text

	private MaterialWithData(int mat, byte d) {
		material = mat;
		data = d;
		metadata = null;
	}

	private MaterialWithData(int mat) {
		this(mat, (byte)0);
	}

	private MaterialWithData(MaterialWithData m) {
		material = m.material;
		data = m.data;
		metadata = m.metadata;
	}

	private MaterialWithData(String string) {
		String[] matAndText = string.split("=");
		String[] matAndData = matAndText[0].split(":");

		metadata = matAndText.length > 1 ? makeText(matAndText[1]) : null;

		if (matAndData[0].matches("^[0-9]+$")) {
			material = Integer.parseInt(matAndData[0]);
		} else {
			Material m = Material.matchMaterial(matAndData[0].toUpperCase());
			if (m == null) {
				throw new IllegalArgumentException("unknown material " + matAndData[0]);
			}
			material = m.getId();
		}
		if (matAndData.length < 2) {
			data = 0;
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
			data = 0;
			throw new IllegalArgumentException("invalid data specification " + matAndData[1]);
		}
	}

	public Byte getData() {
		return data;
	}

	public int getMaterial() {
		return material;
	}

	public String[] getText() {
		return metadata;
	}

	public static MaterialWithData get(String spec) {
		spec = spec.toLowerCase();
		if (!materialCache.containsKey(spec)) {
			MaterialWithData mat = new MaterialWithData(spec);
			materialCache.put(spec, mat);
		}
		return materialCache.get(spec);
	}
	
	public static MaterialWithData get(int id, byte data, String[] metadata) {
		String key = metadata == null ? 
				String.format("%d:%d", id, data) :
					String.format("%d:%d=%s", id, data, Joiner.on(";").join(metadata));
		return get(key);
	}
	
	public static MaterialWithData get(int id, byte data) {
		return get(String.format("%d:%d", id, data));
	}

	public static MaterialWithData get(int id) {
		return get(String.format("%d:%d", id, 0));
	}

	private String[] makeText(String input) {
		String[] t = new String[] {"", "", "", ""};
		String[] s = input.split(";");
		for (int i = 0; i < 4 && i < s.length; i++) {
			t[i] = s[i];
		}
		return t;
	}

	public void applyToBlock(Block b) {
		b.setTypeIdAndData(material, data, false);
		if (metadata != null && (material == 63 || material == 68)) {
			// updating a wall sign or floor sign, with text
			Sign sign = (Sign) b.getState().getData();
			for (int i = 0; i < 4; i++) {
				sign.setLine(i, metadata[i]);
			}
			sign.update();
		}
	}

	/**
	 * Use direct NMS calls to apply this block.  The caller is responsible for ensuring that
	 * lighting is re-initialised afterwards.
	 * 
	 * @param b
	 */
	public void applyToBlockFast(Block b) {
		BlockUtils.setBlockFast(b, material, data);
		if (metadata != null && (material == 63 || material == 68)) {
			// updating a wall sign or floor sign, with text
			Sign sign = (Sign) b.getState().getData();
			for (int i = 0; i < 4; i++) {
				sign.setLine(i, metadata[i]);
			}
			sign.update();
		}
	}

	public void applyToCuboid(Cuboid c) {
		if (c != null) {
			c.set(material, data, true);
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(Material.getMaterial(material).toString());
		if (material == 35) { // wool
			s.append(":").append(DyeColor.getByData(data).toString());
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
		} else if (data != other.data) {
			return false;
		} else {
			return true;
		}
	}

	public MaterialWithData rotate(int rotation) {
		byte newData = data;
		switch(rotation){
		case 270:
			newData = (byte) BlockData.rotate90Reverse(material, data);
			break;
		case 180:
			newData = (byte) BlockData.rotate90(material, data);
			// 180 does twice, so don't break
		case 90:
			newData = (byte) BlockData.rotate90(material, data);
		}
		return MaterialWithData.get(material, newData, metadata);
	}

}

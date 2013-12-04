package me.desht.chesscraft.chess.pieces;

import me.desht.dhutils.block.MaterialWithData;

import java.util.HashMap;
import java.util.Map;

public class MaterialMap {
	private final Map<Character,MaterialWithData> map = new HashMap<Character, MaterialWithData>();

	public void put(char c, MaterialWithData mat) {
		map.put(c, mat);
	}

	public MaterialWithData get(char c) {
		return map.get(c);
	}

	public Map<Character,MaterialWithData> getMap() {
		return map;
	}
}

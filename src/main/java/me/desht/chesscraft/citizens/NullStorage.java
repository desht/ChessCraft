package me.desht.chesscraft.citizens;

import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.api.util.Storage;

/**
 * NullStorage object which doesn't load or save anything to disk.
 *
 * ChessCraft does its own entity persistence management based on game state,
 * and we don't want Citizens2 also saving and loading NPC's.
 */
public class NullStorage implements Storage {
	@Override
	public DataKey getKey(String root) {
		return new MemoryDataKey();
	}

	@Override
	public boolean load() {
		// no-op
		return true;
	}

	@Override
	public void save() {
		// no-op
	}
}

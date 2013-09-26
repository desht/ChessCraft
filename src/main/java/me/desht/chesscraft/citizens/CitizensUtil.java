package me.desht.chesscraft.citizens;

import me.desht.chesscraft.chess.pieces.ChessPieceTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.SimpleNPCDataStore;
import net.citizensnpcs.api.trait.TraitInfo;

public class CitizensUtil {
	public static void initCitizens() {
		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ChessPieceTrait.class).withName("chesspiece"));
		CitizensAPI.createNamedNPCRegistry("chesscraft", SimpleNPCDataStore.create(new NullStorage()));
	}
}

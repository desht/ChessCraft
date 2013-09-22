package me.desht.chesscraft.chess.pieces;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import net.citizensnpcs.api.trait.Trait;

public class ChessPieceTrait extends Trait {

	public ChessPieceTrait() {
		super("chesspiece");
	}

	@EventHandler
	public void onNPCDeath(EntityDeathEvent event) {
		if (event.getEntity().getUniqueId() == getNPC().getBukkitEntity().getUniqueId()) {
			System.out.println("we died!");
		}
	}
}

package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessCraft;
import me.desht.dhutils.LogUtils;
import net.citizensnpcs.api.ai.event.NavigationCompleteEvent;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

public class ChessPieceTrait extends Trait {

	private EntityChessStone capturingTarget;

	public ChessPieceTrait() {
		super("chesspiece");
	}

//	@EventHandler
//	public void onNPCDeath(EntityDeathEvent event) {
//		if (event.getEntity().getUniqueId() == getNPC().getBukkitEntity().getUniqueId()) {
//			System.out.println("we died!");
//		}
//	}

	@EventHandler
	public void onNavigationCompleted(NavigationCompleteEvent event) {
		if (event.getNPC() == getNPC()) {
			LogUtils.fine("navigation completed for " + event.getNPC().getFullName() + ", NPC id " + event.getNPC().getId());

			if (capturingTarget != null) {
				capturingTarget.getBukkitEntity().setVelocity(new Vector(0.0, 1.7, 0.0));
				final NPC npc = capturingTarget.getNPC();
				Bukkit.getScheduler().runTaskLater(ChessCraft.getInstance(), new Runnable() {
					@Override
					public void run() {
						npc.despawn(DespawnReason.REMOVAL);
						npc.destroy();
					}
				}, 20L);
				capturingTarget = null;
			}
			getNPC().getBukkitEntity().teleport(getNPC().getNavigator().getTargetAsLocation());
		}
	}

	public void setCapturingTarget(EntityChessStone captured) {
		capturingTarget = captured;
	}
}

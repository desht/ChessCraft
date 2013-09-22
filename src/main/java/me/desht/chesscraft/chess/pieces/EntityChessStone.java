package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import chesspresso.Chess;

public class EntityChessStone extends ChessStone {

	private final NPC npc;

	protected EntityChessStone(int stone, EntityType entityType, final Location loc, float yaw) {
		super(stone);

		loc.setYaw(yaw);
		LogUtils.fine("create " + stone + "[" + entityType + "] @" + loc);

		String name = ChessUtils.getColour(Chess.stoneToColor(stone)) + " " + ChessUtils.pieceToStr(Chess.stoneToPiece(stone));
		npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
		npc.setProtected(true);
		npc.addTrait(ChessPieceTrait.class);
		npc.getNavigator().getLocalParameters().baseSpeed(0.6f);
		npc.spawn(loc);

		Bukkit.getPluginManager().registerEvents(npc.getTrait(ChessPieceTrait.class), ChessCraft.getInstance());

//		npc = mgr.createNamedEntity(entityType, loc, name, false);
//		entity.setStationary(true);
//		npc.setPushable(false);
//		npc.setYaw(yaw);
//		npc.setSpeed(0.6);
//		entity.getMind().addMovementDesire(new DesireLookAtNearest(Player.class, 4.0f), 1);
//		entity.getBukkitEntity().setRemoveWhenFarAway(false);
//		Bukkit.getScheduler().runTaskLater(ChessCraft.getInstance(), new Runnable() {
//			@Override
//			public void run() {
//				entity.getBukkitEntity().teleport(loc);
//			}
//		}, 10L);
//		entity.spawn(loc);
	}

	/**
	 * @return the entity
	 */
	public Entity getBukkitEntity() {
		return npc.getBukkitEntity();
	}

	@Override
	public void paint(Cuboid region, MassBlockUpdate mbu) {
		// no-op
	}

	@Override
	public void move(int fromSqi, int toSqi, Location to, ChessStone captured) {
		LogUtils.fine("move " + getStone() + " " + npc.getName() + " to " + to);
		if (captured != null) {
			npc.getNavigator().setTarget(((EntityChessStone)captured).getBukkitEntity(), true);
		} else {
			npc.getNavigator().setTarget(to);
		}
	}

	/**
	 * Destroy any Entity for this stone.
	 */
	public void cleanup() {
		npc.destroy();
	}

}

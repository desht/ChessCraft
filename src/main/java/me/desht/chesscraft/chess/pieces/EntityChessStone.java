package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessValidate;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import de.kumpelblase2.remoteentities.EntityManager;
import de.kumpelblase2.remoteentities.RemoteEntities;
import de.kumpelblase2.remoteentities.api.DespawnReason;
import de.kumpelblase2.remoteentities.api.RemoteEntity;
import de.kumpelblase2.remoteentities.api.RemoteEntityType;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireLookAtNearest;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireMoveToLocation;

public class EntityChessStone extends ChessStone {

	private final RemoteEntity entity;

	protected EntityChessStone(int stone, RemoteEntityType entityType, final Location loc, float yaw) {
		super(stone);

		loc.setYaw(yaw);
		LogUtils.fine("create " + stone + "[" + entityType + "] @" + loc);
		EntityManager mgr = RemoteEntities.getManagerOfPlugin("ChessCraft");
		ChessValidate.notNull(mgr, "remote entities manager is null???");

		String name = ChessUtils.getColour(Chess.stoneToColor(stone)) + " " + ChessUtils.pieceToStr(Chess.stoneToPiece(stone));
		entity = mgr.createNamedEntity(entityType, loc, name, false);
//		entity.setStationary(true);
		entity.setPushable(false);
		entity.setYaw(yaw);
		entity.setSpeed(0.7);
		entity.getMind().addMovementDesire(new DesireLookAtNearest(Player.class, 4.0f), 1);
		entity.getBukkitEntity().setRemoveWhenFarAway(false);
		Bukkit.getScheduler().runTaskLater(ChessCraft.getInstance(), new Runnable() {
			@Override
			public void run() {
				entity.getBukkitEntity().teleport(loc);
			}
		}, 10L);
//		entity.spawn(loc);
	}

	/**
	 * @return the entity
	 */
	public RemoteEntity getEntity() {
		return entity;
	}

	@Override
	public void paint(Cuboid region, MassBlockUpdate mbu) {
		// no-op
	}

	@Override
	public void move(int fromSqi, int toSqi, Location to, ChessStone captured) {
		LogUtils.fine("move " + getStone() + " " + entity.getName() + " to " + to);
		entity.getMind().addMovementDesire(new DesireMovePiece(to, captured), 100);
//		if (captured != null) {
//			entity.getMind().addMovementDesire(new DesireMovePiece(to, captured), 100);
//		} else {
//			entity.getMind().addMovementDesire(new DesireMoveToLocation(to), 100);
//		}
	}

	/**
	 * Destroy any Entity for this stone.
	 */
	public void cleanup() {
		// the manager is set to auto-remove despawned entities
		entity.despawn(DespawnReason.CUSTOM);
	}

	private class DesireMovePiece extends DesireMoveToLocation {

		private final ChessStone captured;
		private final Location realDest;

		public DesireMovePiece(Location inTargetLocation, ChessStone captured) {
			super(inTargetLocation);
			this.realDest = inTargetLocation;
			this.captured = captured;
		}

		@Override
		public void stopExecuting() {
			entity.teleport(realDest);
			if (captured != null) {
				RemoteEntity re = ((EntityChessStone)captured).getEntity();
				re.getBukkitEntity().damage(1000);
				re.despawn(DespawnReason.CUSTOM);
			}
		}
	}
}

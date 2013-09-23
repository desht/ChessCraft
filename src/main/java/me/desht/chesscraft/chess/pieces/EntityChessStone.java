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
		LogUtils.finer("create " + stone + "[" + entityType + "] @" + loc);

		String name = ChessUtils.getColour(Chess.stoneToColor(stone)) + " " + ChessUtils.pieceToStr(Chess.stoneToPiece(stone));
		npc = CitizensAPI.getNamedNPCRegistry("chesscraft").createNPC(entityType, name);
		npc.setProtected(true);
		npc.addTrait(ChessPieceTrait.class);
		npc.getNavigator().getLocalParameters().speedModifier(1.25f).distanceMargin(0.0);
		npc.spawn(loc);

		Bukkit.getPluginManager().registerEvents(npc.getTrait(ChessPieceTrait.class), ChessCraft.getInstance());
	}

	public NPC getNPC() {
		return npc;
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
		ChessPieceTrait chessTrait = npc.getTrait(ChessPieceTrait.class);
		chessTrait.setCapturingTarget((EntityChessStone)captured);
		npc.getNavigator().setTarget(to);
	}

	/**
	 * Destroy any Entity for this stone.
	 */
	public void cleanup() {
		LogUtils.finer("destroy NPC " + npc.getFullName());
		npc.despawn();
		npc.destroy();
	}

}

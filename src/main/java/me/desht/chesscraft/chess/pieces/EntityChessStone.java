package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.pieces.EntityChessSet.EntityDetails;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;

import chesspresso.Chess;

public class EntityChessStone extends ChessStone {

	private final NPC npc;

	protected EntityChessStone(int stone, EntityDetails entityDetails, final Location loc, float yaw) {
		super(stone);

		loc.setYaw(yaw);
		LogUtils.finer("create " + stone + "[" + entityDetails.getType() + "] @" + loc);

		String name = ChessUtils.getColour(Chess.stoneToColor(stone)) + " " + ChessUtils.pieceToStr(Chess.stoneToPiece(stone));
		npc = CitizensAPI.getNamedNPCRegistry("chesscraft").createNPC(entityDetails.getType(), name);
		npc.setProtected(true);
		npc.addTrait(ChessPieceTrait.class);
		npc.getNavigator().getLocalParameters().speedModifier(1.25f).distanceMargin(0.0);
		npc.spawn(loc);
		setExtraDetails(npc, entityDetails);

		Bukkit.getPluginManager().registerEvents(npc.getTrait(ChessPieceTrait.class), ChessCraft.getInstance());
	}

	private void setExtraDetails(NPC npc, EntityDetails details) {
		Entity entity = npc.getBukkitEntity();
		switch (details.getType()) {
		case SLIME: case MAGMA_CUBE:
			try {
				int size = Integer.parseInt(details.getExtraData());
				((Slime) entity).setSize(size);
			} catch (IllegalArgumentException e) {
				LogUtils.warning("invalid slime size: " + details.getExtraData());
			}
			// slimes are really slow by default
			npc.getNavigator().getLocalParameters().speedModifier(4.5f);
			break;
		case SKELETON:
			try {
				SkeletonType st = SkeletonType.valueOf(details.getExtraData().toUpperCase());
				((Skeleton)entity).setSkeletonType(st);
			} catch (IllegalArgumentException e) {
				LogUtils.warning("invalid skeleton type: " + details.getExtraData());
			}
			break;
		case HORSE:
			String[] f = details.getExtraData().toUpperCase().split(",");
			if (f.length == 3) {
				try {
					Horse.Variant hv = Horse.Variant.valueOf(f[0]);
					Horse.Color hc = Horse.Color.valueOf(f[1]);
					Horse.Style hs = Horse.Style.valueOf(f[2]);
					((Horse) entity).setVariant(hv);
					((Horse) entity).setColor(hc);
					((Horse) entity).setStyle(hs);
				} catch (IllegalArgumentException e) {
					LogUtils.warning("invalid horse details: " + details.getExtraData() + " - " + e.getMessage());
				}
			} else {
				LogUtils.warning("invalid horse details: " + details.getExtraData() + " (want variant,color,style)");
			}
			break;
		case VILLAGER:
			try {
				Villager.Profession p = Villager.Profession.valueOf(details.getExtraData().toUpperCase());
				((Villager)entity).setProfession(p);
			} catch (IllegalArgumentException e) {
				LogUtils.warning("invalid villager profession: " + details.getExtraData());
			}
			break;
		default:
			break;
		}
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

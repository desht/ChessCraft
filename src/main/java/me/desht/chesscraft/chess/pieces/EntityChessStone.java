package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import chesspresso.Chess;

public class EntityChessStone extends ChessStone {

	private final NPC npc;

	private enum EquipmentLocation { HELD, BOOTS, LEGS, CHEST, HELMET }

	protected EntityChessStone(int stone, ConfigurationSection entityDetails, final Location loc, float yaw) {
		super(stone);

		loc.setYaw(yaw);
		LogUtils.finer("create " + stone + "[" + entityDetails.get("_entity") + "] @" + loc);

		String name = ChessUtils.getColour(Chess.stoneToColor(stone)) + " " + ChessUtils.pieceToStr(Chess.stoneToPiece(stone));
		npc = CitizensAPI.getNamedNPCRegistry("chesscraft").createNPC((EntityType) entityDetails.get("_entity"), name);
		npc.setProtected(true);
		npc.addTrait(ChessPieceTrait.class);
		npc.getNavigator().getLocalParameters().speedModifier(2.0f).distanceMargin(0.0);
		npc.spawn(loc);
		setEntityDetails(entityDetails);

		Bukkit.getPluginManager().registerEvents(npc.getTrait(ChessPieceTrait.class), ChessCraft.getInstance());
	}

	private void setEntityDetails(ConfigurationSection details) {
		Entity entity = npc.getBukkitEntity();
		switch (entity.getType()) {
		case SLIME: case MAGMA_CUBE:
			int size = details.getInt("size", 1);
			((Slime) entity).setSize(size);
			// slimes are really slow by default
			npc.getNavigator().getLocalParameters().speedModifier(4.5f);
			break;
		case SKELETON:
			SkeletonType st = SkeletonType.valueOf(details.getString("variant", "normal").toUpperCase());
			((Skeleton)entity).setSkeletonType(st);
			break;
		case HORSE:
			Horse.Variant hv = Horse.Variant.valueOf(details.getString("variant", "horse").toUpperCase());
			Horse.Color hc = Horse.Color.valueOf(details.getString("color", "brown").toUpperCase());
			Horse.Style hs = Horse.Style.valueOf(details.getString("style", "none").toUpperCase());
			((Horse) entity).setVariant(hv);
			((Horse) entity).setColor(hc);
			((Horse) entity).setStyle(hs);
			break;
		case VILLAGER:
			Villager.Profession p = Villager.Profession.valueOf(details.getString("profession", "farmer").toUpperCase());
			((Villager)entity).setProfession(p);
			// villagers are really fast by default
			npc.getNavigator().getLocalParameters().speedModifier(1.0f);
			break;
		case SHEEP:
			DyeColor c = DyeColor.valueOf(details.getString("color", "white").toUpperCase());
			((Sheep)entity).setColor(c);
			break;
		case BLAZE:
			npc.getNavigator().getLocalParameters().speedModifier(4.0f);
			break;
		case OCELOT:
			Ocelot.Type ot = Ocelot.Type.valueOf(details.getString("variant", "wild_ocelot").toUpperCase());
			((Ocelot)entity).setCatType(ot);
			break;
		case WOLF:
			DyeColor wc = DyeColor.valueOf(details.getString("color", "red").toUpperCase());
			((Wolf)entity).setCollarColor(wc);
		default:
			break;
		}
		if (entity instanceof Ageable) {
			if (details.getBoolean("baby", false)) {
				((Ageable)entity).setBaby();
			} else {
				((Ageable)entity).setAdult();
			}
		}
		if (entity instanceof Tameable) {
			((Tameable)entity).setTamed(details.getBoolean("tame", false));
		}
		if (entity instanceof LivingEntity && ((LivingEntity)entity).getEquipment() != null) {
			EntityEquipment eq = ((LivingEntity)entity).getEquipment();
			setEquipment(eq, EquipmentLocation.HELD, details.getString("held"));
			setEquipment(eq, EquipmentLocation.BOOTS, details.getString("boots"));
			setEquipment(eq, EquipmentLocation.LEGS, details.getString("legs"));
			setEquipment(eq, EquipmentLocation.CHEST, details.getString("chest"));
			setEquipment(eq, EquipmentLocation.HELMET, details.getString("helmet"));
		}
	}

	private void setEquipment(EntityEquipment eq, EquipmentLocation where, String materialName) {
		if (materialName == null || materialName.isEmpty()) {
			return;
		}
		Material mat = Material.matchMaterial(materialName);
		Validate.notNull(mat);
		ItemStack stack = new ItemStack(mat, 1);
		switch (where) {
		case HELD: eq.setItemInHand(stack); break;
		case BOOTS: eq.setBoots(stack); break;
		case LEGS: eq.setLeggings(stack); break;
		case CHEST: eq.setChestplate(stack); break;
		case HELMET: eq.setHelmet(stack); break;
		default: break;
		}
	}

	/**
	 * Get the Citizens2 NPC for this chess stone.
	 *
	 * @return the NPC
	 */
	public NPC getNPC() {
		return npc;
	}

	/**
	 * Get the Bukkit Entity for this chess stone.
	 *
	 * @return the Entity
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
	 * Despawn and unregister the NPC for this stone.
	 */
	public void cleanup() {
		LogUtils.finer("destroy NPC " + npc.getFullName());
		npc.despawn();
		npc.destroy();
	}

}

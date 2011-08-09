package me.desht.chesscraft.regions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.desht.chesscraft.enums.Direction;

import org.bukkit.Location;
import org.bukkit.World;
// imports for clear()
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import me.desht.chesscraft.blocks.BlockType;

//imports for weSelect()
import me.desht.chesscraft.ChessCraft;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;

public class Cuboid implements Iterable<Location>, Cloneable {

	protected Location lowerNE; // min x,y,z
	protected Location upperSW; // max x,y,z

	public Cuboid(Location l1, Location l2) {
		if (l1.getWorld() != l2.getWorld()) {
			throw new IllegalArgumentException("locations must be on the same world");
		}

		lowerNE = new Location(l1.getWorld(), Math.min(l1.getX(), l2.getX()),
				Math.min(l1.getY(), l2.getY()), Math.min(l1.getZ(), l2.getZ()));
		upperSW = new Location(l1.getWorld(), Math.max(l1.getX(), l2.getX()),
				Math.max(l1.getY(), l2.getY()), Math.max(l1.getZ(), l2.getZ()));

	}

	public Cuboid(Location l1) {
		lowerNE = l1.clone();//new Location(l1.getWorld(), l1.getX(), l1.getY(), l1.getZ());
		upperSW = l1.clone();//new Location(l1.getWorld(), l1.getX(), l1.getY(), l1.getZ());
	}

	public Cuboid(Cuboid copy) {
		this.lowerNE = copy.lowerNE.clone();
		this.upperSW = copy.upperSW.clone();
	}

	public Location getLowerNE() {
		return lowerNE;
	}

	public Location getUpperSW() {
		return upperSW;
	}

	public Location getCenter() {
		return new Location(getWorld(), getLowerX() + (getUpperX() - getLowerX()),
				getLowerY() + (getUpperY() - getLowerY()),
				getLowerZ() + (getUpperZ() - getLowerZ()));
	}

	public World getWorld() {
		return lowerNE == null ? (upperSW == null ? null : upperSW.getWorld()) : lowerNE.getWorld();
	}

	public List<Location> corners() {
		List<Location> res = new ArrayList<Location>(8);
		World w = lowerNE.getWorld();
		int minX = lowerNE.getBlockX(), minY = lowerNE.getBlockY(), minZ = lowerNE.getBlockZ();
		int maxX = upperSW.getBlockX(), maxY = upperSW.getBlockY(), maxZ = upperSW.getBlockZ();
		res.add(new Location(w, minX, minY, minZ));
		res.add(new Location(w, minX, minY, maxZ));
		res.add(new Location(w, minX, maxY, minZ));
		res.add(new Location(w, minX, maxY, maxZ));
		res.add(new Location(w, maxX, minY, minZ));
		res.add(new Location(w, maxX, minY, maxZ));
		res.add(new Location(w, maxX, maxY, minZ));
		res.add(new Location(w, maxX, maxY, maxZ));
		return res;

	}

	public List<Location> walls() {
		List<Location> res = new ArrayList<Location>(8);
		World w = lowerNE.getWorld();
		int minX = lowerNE.getBlockX(), minY = lowerNE.getBlockY(), minZ = lowerNE.getBlockZ();
		int maxX = upperSW.getBlockX(), maxY = upperSW.getBlockY(), maxZ = upperSW.getBlockZ();
		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				res.add(new Location(w, x, y, minZ));
				res.add(new Location(w, x, y, maxZ));
			}
		}
		for (int z = minZ; z <= maxZ; ++z) {
			for (int y = minY; y <= maxY; ++y) {
				res.add(new Location(w, minX, y, z));
				res.add(new Location(w, maxX, y, z));
			}
		}
		return res;
	}

	public Cuboid expand(Direction dir, int amount) {
		//TODO: if negative amount, don't collapse beyond self
		switch (dir) {
			case North:
				lowerNE.setX(lowerNE.getBlockX() - amount);
				break;
			case South:
				upperSW.setX(upperSW.getBlockX() + amount);
				break;
			case East:
				lowerNE.setZ(lowerNE.getBlockZ() - amount);
				break;
			case West:
				upperSW.setZ(upperSW.getBlockZ() + amount);
				break;
			case Down:
				lowerNE.setY(lowerNE.getBlockY() - amount);
				break;
			case Up:
				upperSW.setY(upperSW.getBlockY() + amount);
				break;
			default:
				throw new IllegalArgumentException("invalid direction " + dir);
		}
		return this;
	}

	public Cuboid shift(Direction dir, int amount) {
		return expand(dir, amount).expand(opposite(dir), -amount);
	}

	public Cuboid outset(Direction dir, int amount) {
		switch (dir) {
			case Horizontal:
				expand(Direction.North, amount);
				expand(Direction.South, amount);
				expand(Direction.East, amount);
				expand(Direction.West, amount);
				break;
			case Vertical:
				expand(Direction.Down, amount);
				expand(Direction.Up, amount);
				break;
			case Both:
				outset(Direction.Horizontal, amount);
				outset(Direction.Vertical, amount);
				break;
			default:
				throw new IllegalArgumentException("invalid direction " + dir);
		}
		return this;
	}

	public Cuboid inset(Direction dir, int amount) {
		return outset(dir, -amount);
	}

	public Direction opposite(Direction dir) {
		switch (dir) {
			case North:
				return Direction.South;
			case South:
				return Direction.North;
			case West:
				return Direction.East;
			case East:
				return Direction.West;
			case Up:
				return Direction.Down;
			case Down:
				return Direction.Up;
			case Horizontal:
				return Direction.Vertical;
			case Vertical:
				return Direction.Horizontal;
		}
		return Direction.Unknown;
	}

	public boolean contains(int x, int y, int z) {
		if (x >= lowerNE.getBlockX() && x <= upperSW.getBlockX() && y >= lowerNE.getBlockY()
				&& y <= upperSW.getBlockY() && z >= lowerNE.getBlockZ() && z <= upperSW.getBlockZ()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean contains(Location l) {
		if (l.getWorld() != lowerNE.getWorld()) {
			return false;
		}
		return contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}

	public int volume() {
		return getSizeX() * getSizeY() * getSizeZ();
	}

	public byte averageLightLevel() {
		long total = 0;
		int n = 0;
		for (Location l : this) {
			if (l.getBlock().isEmpty()) {
				total += l.getBlock().getLightLevel();
				++n;
			}
		}
		return (byte) (total / n);
	}

	/**
	 * delete blocks in bounds, but don't allow items to drop (paintings are not
	 * blocks, and are not included...) also does not scan the faces of the
	 * region for drops when the region is cleared
	 */
	public void clear() {
		// first remove blocks that might pop off & leave a drop
		for (Location l : this) {
			Block b = l.getBlock();
			if (BlockType.shouldPlaceLast(b.getTypeId())) {
				b.setTypeId(0);
			}// also check if this is a container
			else if (BlockType.isContainerBlock(b.getTypeId())) {
				BlockState state = b.getState();
				if (state instanceof org.bukkit.block.ContainerBlock) {
					org.bukkit.block.ContainerBlock chest = (org.bukkit.block.ContainerBlock) state;
					Inventory inven = chest.getInventory();
					inven.clear();
				}
			}
		}
		// now wipe all (remaining) blocks
		for (Location l : this) {
			l.getBlock().setTypeId(0);
		}
	}

	public void set(int blockID) {
		if (blockID == 0) {
			clear();
		} else {
			for (Location l : this) {
				l.getBlock().setTypeId(blockID);
			}
		}
	}

	public void set(int blockID, byte data) {
		if (blockID == 0) {
			clear();
		} else {
			for (Location l : this) {
				l.getBlock().setTypeIdAndData(blockID, data, true);
			}
		}
	}

	public void setWalls(int blockID) {
		setWalls(blockID, (byte) 0);
	}

	public void setWalls(int blockID, byte data) {
		World w = lowerNE.getWorld();
		int minX = lowerNE.getBlockX(), minY = lowerNE.getBlockY(), minZ = lowerNE.getBlockZ();
		int maxX = upperSW.getBlockX(), maxY = upperSW.getBlockY(), maxZ = upperSW.getBlockZ();
		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				(new Location(w, x, y, minZ)).getBlock().setTypeIdAndData(blockID, data, true);
				(new Location(w, x, y, maxZ)).getBlock().setTypeIdAndData(blockID, data, true);
			}
		}
		for (int z = minZ; z <= maxZ; ++z) {
			for (int y = minY; y <= maxY; ++y) {
				(new Location(w, minX, y, z)).getBlock().setTypeIdAndData(blockID, data, true);
				(new Location(w, maxX, y, z)).getBlock().setTypeIdAndData(blockID, data, true);
			}
		}
	}

	/**
	 * get a block relative to the lower NE point of this cuboid
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Block getRelativeBlock(int x, int y, int z) {
		return lowerNE.clone().add(x, y, z).getBlock();
	}

	@Override
	public Iterator<Location> iterator() {
		return new CuboidIterator(lowerNE, upperSW);
	}

	@Override
	public Cuboid clone() {
		return new Cuboid(this);
	}

	@Override
	public String toString() {
		return lowerNE.toString() + " -> " + upperSW.toString();
	}

	public int getSizeX() {
		return (upperSW.getBlockX() - lowerNE.getBlockX()) + 1;
	}

	public int getSizeY() {
		return (upperSW.getBlockY() - lowerNE.getBlockY()) + 1;
	}

	public int getSizeZ() {
		return (upperSW.getBlockZ() - lowerNE.getBlockZ()) + 1;
	}

	public int getLowerX() {
		return lowerNE.getBlockX();
	}

	public int getLowerY() {
		return lowerNE.getBlockY();
	}

	public int getLowerZ() {
		return lowerNE.getBlockZ();
	}

	public int getUpperX() {
		return upperSW.getBlockX();
	}

	public int getUpperY() {
		return upperSW.getBlockY();
	}

	public int getUpperZ() {
		return upperSW.getBlockZ();
	}

	public void weSelect(org.bukkit.entity.Player p) {
		if (p != null) {
			// didn't call "ChessCraft.getWorldEdit()" to keep packages generic
//			org.bukkit.Server sv = p.getServer();
//			org.bukkit.plugin.PluginManager m = sv.getPluginManager();
//			org.bukkit.plugin.Plugin we = m.getPlugin("WorldEdit");
//			if (we != null && we instanceof com.sk89q.worldedit.bukkit.WorldEditPlugin) {
//				com.sk89q.worldedit.bukkit.selections.CuboidSelection s =
//						new com.sk89q.worldedit.bukkit.selections.CuboidSelection(
//						getWorld(), getUpperSW(), getLowerNE());
//				((com.sk89q.worldedit.bukkit.WorldEditPlugin) we).setSelection(p, s);
//			}
			WorldEditPlugin wep = ChessCraft.getWorldEdit();
			if (wep != null) {
				CuboidSelection s = new CuboidSelection(getWorld(), getUpperSW(), getLowerNE());
				wep.setSelection(p, s);
			}
		}
	}
}

package me.desht.chesscraft.regions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.util.WorldEditUtils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
// imports for clear()
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.inventory.Inventory;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.BlockUtils;
import me.desht.chesscraft.blocks.MaterialWithData;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityPlayer;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.entity.Player;

public class Cuboid implements Iterable<Block>, Cloneable {
	private static WorldEditPlugin wep = null;

	protected Location lowerNE; // min x,y,z
	protected Location upperSW; // max x,y,z

	public static void setWorldEdit(WorldEditPlugin p) {
		wep = p;
	}

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

	public List<Block> corners() {
		List<Block> res = new ArrayList<Block>(8);
		World w = lowerNE.getWorld();
		int minX = lowerNE.getBlockX(), minY = lowerNE.getBlockY(), minZ = lowerNE.getBlockZ();
		int maxX = upperSW.getBlockX(), maxY = upperSW.getBlockY(), maxZ = upperSW.getBlockZ();
		res.add(w.getBlockAt(minX, minY, minZ));
		res.add(w.getBlockAt(minX, minY, maxZ));
		res.add(w.getBlockAt(minX, maxY, minZ));
		res.add(w.getBlockAt(minX, maxY, maxZ));
		res.add(w.getBlockAt(maxX, minY, minZ));
		res.add(w.getBlockAt(maxX, minY, maxZ));
		res.add(w.getBlockAt(maxX, maxY, minZ));
		res.add(w.getBlockAt(maxX, maxY, maxZ));
		return res;

	}

	public List<Block> walls() {
		List<Block> res = new ArrayList<Block>(8);
		World w = lowerNE.getWorld();
		int minX = lowerNE.getBlockX(), minY = lowerNE.getBlockY(), minZ = lowerNE.getBlockZ();
		int maxX = upperSW.getBlockX(), maxY = upperSW.getBlockY(), maxZ = upperSW.getBlockZ();
		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				res.add(w.getBlockAt(x, y, minZ));
				res.add(w.getBlockAt(x, y, maxZ));
			}
		}
		for (int z = minZ; z <= maxZ; ++z) {
			for (int y = minY; y <= maxY; ++y) {
				res.add(w.getBlockAt(minX, y, z));
				res.add(w.getBlockAt(maxX, y, z));
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

	public boolean contains(Block b) {
		return contains(b.getLocation());
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
		for (Block b : this) {
			if (b.isEmpty()) {
				total += b.getLightLevel();
				++n;
			}
		}
		return n > 0 ? (byte) (total / n) : 0;
	}

	/**
	 * delete blocks in bounds, but don't allow items to drop (paintings are not
	 * blocks, and are not included...) also does not scan the faces of the
	 * region for drops when the region is cleared
	 *
	 * @param fast	Use low-level NMS calls to clear the Cuboid to avoid excessive
	 * 			lighting recalculation
	 */
	public void clear(boolean fast) {
		// first remove blocks that might pop off & leave a drop
		for (Block b : this) {
			if (BlockType.shouldPlaceLast(b.getTypeId())) {
				if (fast) {
					BlockUtils.setBlockFast(b, 0);
				} else {
					b.setTypeId(0);
				}
			} else if (BlockType.isContainerBlock(b.getTypeId())) {
				// also check if this is a container, and empty it if necessary
				BlockState state = b.getState();
				if (state instanceof org.bukkit.block.ContainerBlock) {
					org.bukkit.block.ContainerBlock chest = (org.bukkit.block.ContainerBlock) state;
					Inventory inven = chest.getInventory();
					inven.clear();
				}
			}
		}
		// now wipe all (remaining) blocks
		if (fast) {
			for (Block b : this) {
				BlockUtils.setBlockFast(b, 0);
			}
		} else {
			for (Block b : this) {
				b.setTypeId(0);
			}
		}
	}

	public void set(int blockID, boolean fast) {
		//		long start = System.nanoTime();

		if (blockID == 0) {
			clear(fast);
		} else {
			if (fast) {
				for (Block b : this) {
					BlockUtils.setBlockFast(b, blockID);
				}
			} else {
				for (Block b : this) {
					b.setTypeId(blockID);
				}
			}
		}

		//		System.out.println("Cuboid set " + blockID + ": " + (System.nanoTime() - start) + "ns");
	}

	public void set(int blockID, Byte data, boolean fast) {
		//		long start = System.nanoTime();

		if (blockID == 0) {
			clear(fast);
		} else {
			if (data != null) {
				if (fast) {
					for (Block b : this) {
						BlockUtils.setBlockFast(b, blockID, data);
					}
				} else {
					for (Block b : this) {
						b.setTypeIdAndData(blockID, data, false);
					}
				}
			} else {
				if (fast) {
					for (Block b : this) {
						BlockUtils.setBlockFast(b, blockID);
					}
				} else {
					for (Block b : this) {
						b.setTypeId(blockID, false);
					}
				}
			}
		}

		//		System.out.println("Cuboid set " + blockID + "/" + data + ": " + (System.nanoTime() - start) + "ns");
	}

	public void set(MaterialWithData mat, boolean fast) {
		set(mat.getMaterial(), mat.getData(), fast);
	}

	public void setWalls(int blockID) {
		setWalls(blockID, null);
	}

	/**
	 * Get a list of the chunks which are fully or partially contained in this cuboid.
	 * 
	 * @return a list of Chunk objects
	 */
	public List<Chunk> getChunks() {
		List<Chunk> res = new ArrayList<Chunk>();

		World w = getLowerNE().getWorld();
		int x1 = getLowerX(); int x2 = getUpperX();
		int z1 = getLowerZ(); int z2 = getUpperZ();
		for (int x = x1; x <= x2; x += 16) {
			for (int z = z1; z <= z2; z += 16) {
				res.add(w.getChunkAt(x, z));
			}
		}
		return res;
	}

	/**
	 * Force lighting to be recalculated for all chunks occupied by the cuboid.
	 */
	public void initLighting() {
		for (Chunk c : getChunks()) {
			((CraftChunk)c).getHandle().initLighting();
			//			System.out.println("chunk " + c + ": relighted"); 
		}
	}

	/**
	 * Any players within the threshold distance (DIST_SQUARED) of the cuboid may need
	 * to be notified of any fast changes that happened, to avoid "phantom" blocks showing
	 * up on the client.  Add the chunk coordinates of affected chunks to those players'
	 * chunk queue.
	 */
//	@SuppressWarnings("unchecked")
	public void sendClientChanges() {
//		int threshold = (Bukkit.getServer().getViewDistance() << 4) + 32;
//		System.out.println("view dist = " + threshold);
//		threshold = threshold * threshold;
//
//		List<ChunkCoordIntPair> pairs = new ArrayList<ChunkCoordIntPair>();
//		for (Chunk c : getChunks()) {
//			pairs.add(new ChunkCoordIntPair(c.getX() >> 4, c.getZ() >> 4));
//		}
//		int centerX = getLowerX() + getSizeX() / 2;	
//		int centerZ = getLowerZ() + getSizeZ() / 2;
//		for (Player player : lowerNE.getWorld().getPlayers()) {
//			int px = player.getLocation().getBlockX();
//			int pz = player.getLocation().getBlockZ();
//			System.out.println("px = " + px + ", pz = " + pz + "   cx = " + centerX + ", cz = " + centerZ + "   threshold = " + threshold);
//			if ((px - centerX) * (px - centerX) + (pz - centerZ) * (pz - centerZ) < threshold) {
//				EntityPlayer ep = ((CraftPlayer) player).getHandle();
//				ep.chunkCoordIntPairQueue.addAll(pairs);
//				for (ChunkCoordIntPair p : pairs) {
//					System.out.println("send " + player.getName() + ": chunk change: " + p.x + "," + p.z);
//				}
//			}
//		}
		
		for (Chunk c : getChunks()) {
			lowerNE.getWorld().refreshChunk(c.getX() >> 4, c.getZ() >> 4);
		}
	}

	public void setWalls(int blockID, Byte data) {
		World w = lowerNE.getWorld();
		int minX = lowerNE.getBlockX(), minY = lowerNE.getBlockY(), minZ = lowerNE.getBlockZ();
		int maxX = upperSW.getBlockX(), maxY = upperSW.getBlockY(), maxZ = upperSW.getBlockZ();
		if (data != null) {
			for (int x = minX; x <= maxX; ++x) {
				for (int y = minY; y <= maxY; ++y) {
					w.getBlockAt(x, y, minZ).setTypeIdAndData(blockID, data, true);
					w.getBlockAt(x, y, maxZ).setTypeIdAndData(blockID, data, true);
				}
			}
			for (int z = minZ; z <= maxZ; ++z) {
				for (int y = minY; y <= maxY; ++y) {
					w.getBlockAt(minX, y, z).setTypeIdAndData(blockID, data, true);
					w.getBlockAt(maxX, y, z).setTypeIdAndData(blockID, data, true);
				}
			}
		} else {
			for (int x = minX; x <= maxX; ++x) {
				for (int y = minY; y <= maxY; ++y) {
					w.getBlockAt(x, y, minZ).setTypeId(blockID, true);
					w.getBlockAt(x, y, maxZ).setTypeId(blockID, true);
				}
			}
			for (int z = minZ; z <= maxZ; ++z) {
				for (int y = minY; y <= maxY; ++y) {
					w.getBlockAt(minX, y, z).setTypeId(blockID, true);
					w.getBlockAt(maxX, y, z).setTypeId(blockID, true);
				}
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
	public Iterator<Block> iterator() {
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

	public void weSelect(String playerName){
		if (lowerNE != null){
			List<Player> players = lowerNE.getWorld().getPlayers();
			for (Player p : players){
				if (p.getName().equalsIgnoreCase(playerName)){
					weSelect(p);
					return;
				}
			}
		}
	}

	public void weSelect(Player p) {
		if (p != null && wep != null) {
			WorldEditUtils.weSelect(this, p);
		}
	}

	public class CuboidIterator implements Iterator<Block> {

		private Location base;
		private int x, y, z;
		private int sizeX, sizeY, sizeZ;

		public CuboidIterator(Location l1, Location l2) {
			base = l1.clone();
			sizeX = Math.abs(l2.getBlockX() - l1.getBlockX()) + 1;
			sizeY = Math.abs(l2.getBlockY() - l1.getBlockY()) + 1;
			sizeZ = Math.abs(l2.getBlockZ() - l1.getBlockZ()) + 1;
			x = y = z = 0;
		}

		@Override
		public boolean hasNext() {
			return x < sizeX && y < sizeY && z < sizeZ;
		}

		@Override
		public Block next() {
			Block b = base.getWorld().getBlockAt(base.getBlockX() + x, base.getBlockY() + y, base.getBlockZ() + z);
			if (++x >= sizeX) {
				x = 0;
				if (++y >= sizeY) {
					y = 0;
					++z;
				}
			}
			return b;
		}

		@Override
		public void remove() {
			// nop
		}
	}

}

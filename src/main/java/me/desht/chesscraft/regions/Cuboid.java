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

	private final World world;
	private final int x1, y1, z1;
	private final int x2, y2, z2;
	//	protected final Location lowerNE; // min x,y,z
	//	protected final Location upperSW; // max x,y,z

	public static void setWorldEdit(WorldEditPlugin p) {
		wep = p;
	}

	public Cuboid(Location l1, Location l2) {
		if (l1.getWorld() != l2.getWorld()) {
			throw new IllegalArgumentException("locations must be on the same world");
		}
		world = l1.getWorld();
		x1 = Math.min(l1.getBlockX(), l2.getBlockX());
		y1 = Math.min(l1.getBlockY(), l2.getBlockY());
		z1 = Math.min(l1.getBlockZ(), l2.getBlockZ());
		x2 = Math.max(l1.getBlockX(), l2.getBlockX());
		y2 = Math.max(l1.getBlockY(), l2.getBlockY());
		z2 = Math.max(l1.getBlockZ(), l2.getBlockZ());
	}

	public Cuboid(Location l1) {
		this(l1, l1);
	}

	public Cuboid(Cuboid other) {
		this(other.world, other.x1, other.y1, other.z1, other.x2, other.y2, other.z2);
	}

	public Cuboid(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
		this.world = world;
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.z1 = z1;
		this.z2 = z2;
	}

	public Location getLowerNE() {
		return new Location(world, x1, y1, z1);
	}

	public Location getUpperSW() {
		return new Location(world, x2, y2, z2);
	}

	public Location getCenter() {
		return new Location(getWorld(), getLowerX() + (getUpperX() - getLowerX()),
		                    getLowerY() + (getUpperY() - getLowerY()),
		                    getLowerZ() + (getUpperZ() - getLowerZ()));
	}

	public World getWorld() {
		return world;
	}

	public List<Block> corners() {
		List<Block> res = new ArrayList<Block>(8);
		res.add(world.getBlockAt(x1, y1, z1));
		res.add(world.getBlockAt(x1, y1, z2));
		res.add(world.getBlockAt(x1, y2, z1));
		res.add(world.getBlockAt(x1, y2, z2));
		res.add(world.getBlockAt(x2, y1, z1));
		res.add(world.getBlockAt(x2, y1, z2));
		res.add(world.getBlockAt(x2, y2, z1));
		res.add(world.getBlockAt(x2, y2, z2));
		return res;

	}

	public List<Block> walls() {
		List<Block> res = new ArrayList<Block>(8);
		for (int x = x1; x <= x2; ++x) {
			for (int y = y1; y <= y2; ++y) {
				res.add(world.getBlockAt(x, y, z1));
				res.add(world.getBlockAt(x, y, z2));
			}
		}
		for (int z = z1; z <= z2; ++z) {
			for (int y = y1; y <= y2; ++y) {
				res.add(world.getBlockAt(x1, y, z));
				res.add(world.getBlockAt(x2, y, z));
			}
		}
		return res;
	}

	public Cuboid expand(Direction dir, int amount) {		
		switch (dir) {
		case North:
			return new Cuboid(world, x1 - amount, y1, z1, x2, y2, z2);
			//			lowerNE.setX(lowerNE.getBlockX() - amount);
		case South:
			return new Cuboid(world, x1, y1, z1, x2 + amount, y2, z2);
			//			upperSW.setX(upperSW.getBlockX() + amount);
		case East:
			return new Cuboid(world, x1, y1, z1 - amount, x2, y2, z2);
			//			lowerNE.setZ(lowerNE.getBlockZ() - amount);
		case West:
			return new Cuboid(world, x1, y1, z1, x2, y2, z2 + amount);
			//			upperSW.setZ(upperSW.getBlockZ() + amount);
		case Down:
			return new Cuboid(world, x1, y1 - amount, z1, x2, y2, z2);
			//			lowerNE.setY(lowerNE.getBlockY() - amount);
		case Up:
			return new Cuboid(world, x1, y1, z1, x2, y2 + amount, z2);
			//			upperSW.setY(upperSW.getBlockY() + amount);
		default:
			throw new IllegalArgumentException("invalid direction " + dir);
		}
	}

	public Cuboid shift(Direction dir, int amount) {
		return expand(dir, amount).expand(opposite(dir), -amount);
	}

	public Cuboid outset(Direction dir, int amount) {
		Cuboid c;
		switch (dir) {
		case Horizontal:
			c = expand(Direction.North, amount).expand(Direction.South, amount).expand(Direction.East, amount).expand(Direction.West, amount);
			break;
		case Vertical:
			c = expand(Direction.Down, amount).expand(Direction.Up, amount);
			break;
		case Both:
			c = outset(Direction.Horizontal, amount).outset(Direction.Vertical, amount);
			break;
		default:
			throw new IllegalArgumentException("invalid direction " + dir);
		}
		return c;
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
		return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
	}

	public boolean contains(Block b) {
		return contains(b.getLocation());
	}

	public boolean contains(Location l) {
		if (l.getWorld() != world) {
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
		int x1 = getLowerX() & ~0xf; int x2 = getUpperX() & ~0xf;
		int z1 = getLowerZ() & ~0xf; int z2 = getUpperZ() & ~0xf;
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
	@SuppressWarnings("unchecked")
	public void sendClientChanges() {
		int threshold = (Bukkit.getServer().getViewDistance() << 4) + 32;
		//		System.out.println("view dist = " + threshold);
		threshold = threshold * threshold;

		List<ChunkCoordIntPair> pairs = new ArrayList<ChunkCoordIntPair>();
		for (Chunk c : getChunks()) {
			pairs.add(new ChunkCoordIntPair(c.getX() >> 4, c.getZ() >> 4));
		}
		int centerX = getLowerX() + getSizeX() / 2;	
		int centerZ = getLowerZ() + getSizeZ() / 2;
		for (Player player : world.getPlayers()) {
			int px = player.getLocation().getBlockX();
			int pz = player.getLocation().getBlockZ();
			//			System.out.println("px = " + px + ", pz = " + pz + "   cx = " + centerX + ", cz = " + centerZ + "   threshold = " + threshold);
			if ((px - centerX) * (px - centerX) + (pz - centerZ) * (pz - centerZ) < threshold) {
				EntityPlayer ep = ((CraftPlayer) player).getHandle();
				ep.chunkCoordIntPairQueue.addAll(pairs);
				//				for (ChunkCoordIntPair p : pairs) {
				//					System.out.println("send " + player.getName() + ": chunk change: " + p.x + "," + p.z);
				//				}
			}
		}

		//		for (Chunk c : getChunks()) {
		//			lowerNE.getWorld().refreshChunk(c.getX() >> 4, c.getZ() >> 4);
		//		}
	}

	public void setWalls(int blockID, Byte data) {
		if (data != null) {
			for (int x = x1; x <= x2; ++x) {
				for (int y = y1; y <= y2; ++y) {
					world.getBlockAt(x, y, z1).setTypeIdAndData(blockID, data, true);
					world.getBlockAt(x, y, z2).setTypeIdAndData(blockID, data, true);
				}
			}
			for (int z = z1; z <= z2; ++z) {
				for (int y = y1; y <= y2; ++y) {
					world.getBlockAt(x1, y, z).setTypeIdAndData(blockID, data, true);
					world.getBlockAt(x2, y, z).setTypeIdAndData(blockID, data, true);
				}
			}
		} else {
			for (int x = x1; x <= x2; ++x) {
				for (int y = y1; y <= y2; ++y) {
					world.getBlockAt(x, y, z1).setTypeId(blockID, true);
					world.getBlockAt(x, y, z2).setTypeId(blockID, true);
				}
			}
			for (int z = z1; z <= z2; ++z) {
				for (int y = y1; y <= y2; ++y) {
					world.getBlockAt(x1, y, z).setTypeId(blockID, true);
					world.getBlockAt(x2, y, z).setTypeId(blockID, true);
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
		return world.getBlockAt(x1 + x, y1 + y, z1 + z);
	}

	@Override
	public Iterator<Block> iterator() {
		return new CuboidIterator(world, x1, y1, z1, x2, y2, z2);
	}

	@Override
	public Cuboid clone() {
		return new Cuboid(this);
	}

	@Override
	public String toString() {
		return new String("Cuboid: " + world.getName() + "," + x1 + "," + y1 + "," + z1 + "=>" + x2 + "," + y2 + "," + z2);
	}

	public int getSizeX() {
		return (x2 - x1) + 1;
	}

	public int getSizeY() {
		return (y2 - y1) + 1;
	}

	public int getSizeZ() {
		return (z2 - z1) + 1;
	}

	public int getLowerX() {
		return x1;
	}

	public int getLowerY() {
		return y1;
	}

	public int getLowerZ() {
		return z1;
	}

	public int getUpperX() {
		return x2;
	}

	public int getUpperY() {
		return y2;
	}

	public int getUpperZ() {
		return z2;
	}

	public void weSelect(String playerName) {
		Player p = Bukkit.getPlayer(playerName);
		if (p == null || p.getWorld() != world) {
			return;
		}
		weSelect(p);
	}

	public void weSelect(Player p) {
		if (p != null && wep != null) {
			WorldEditUtils.weSelect(this, p);
		}
	}

	public class CuboidIterator implements Iterator<Block> {
		private World w;
		private int baseX, baseY, baseZ;
		private int x, y, z;
		private int sizeX, sizeY, sizeZ;

		public CuboidIterator(World w, int x1, int y1, int z1, int x2, int y2, int z2) {
			this.w = w;
			baseX = x1;
			baseY = y1;
			baseZ = z1;
			sizeX = Math.abs(x2 - x1) + 1;
			sizeY = Math.abs(y2 - y1) + 1;
			sizeZ = Math.abs(z2 - z1) + 1;
			x = y = z = 0;
		}

		@Override
		public boolean hasNext() {
			return x < sizeX && y < sizeY && z < sizeZ;
		}

		@Override
		public Block next() {
			Block b = w.getBlockAt(baseX + x, baseY + y, baseZ + z);
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

package me.desht.chesscraft.regions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.util.WorldEditUtils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.inventory.InventoryHolder;

import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.BlockUtils;
import me.desht.chesscraft.blocks.MaterialWithData;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EnumSkyBlock;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.entity.Player;

public class Cuboid implements Iterable<Block>, Cloneable {
	private static WorldEditPlugin wep = null;

	private final World world;
	private final int x1, y1, z1;
	private final int x2, y2, z2;

	public static void setWorldEdit(WorldEditPlugin p) {
		wep = p;
	}

	/**
	 * Construct a Cuboid given two Location objects which represent any two corners
	 * of the Cuboid.
	 * 
	 * @param l1 one of the corners
	 * @param l2 the other corner
	 */
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

	/**
	 * Construct a one-block Cuboid at the given Location of the Cuboid.
	 * 
	 * @param l1 location of the Cuboid
	 */
	public Cuboid(Location l1) {
		this(l1, l1);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other the Cuboid to copy
	 */
	public Cuboid(Cuboid other) {
		this(other.world, other.x1, other.y1, other.z1, other.x2, other.y2, other.z2);
	}

	/**
	 * Construct a Cuboid in the given World and xyz co-ordinates
	 * 
	 * @param world the Cuboid's world
	 * @param x1 X co-ordinate of corner 1
	 * @param y1 Y co-ordinate of corner 1
	 * @param z1 Z co-ordinate of corner 1
	 * @param x2 X co-ordinate of corner 2
	 * @param y2 Y co-ordinate of corner 2
	 * @param z2 Z co-ordinate of corner 2
	 */
	public Cuboid(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
		this.world = world;
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.z1 = z1;
		this.z2 = z2;
	}

	/**
	 * Get the Location of the lower northeast corner of the Cuboid (minimum XYZ
	 * co-ordinates).
	 * 
	 * @return Location of the lower northeast corner
	 */
	public Location getLowerNE() {
		return new Location(world, x1, y1, z1);
	}

	/**
	 * Get the Location of the upper southwest corner of the Cuboid (maximum XYZ
	 * co-ordinates).
	 * 
	 * @return Location of the upper southwest corner
	 */
	public Location getUpperSW() {
		return new Location(world, x2, y2, z2);
	}

	/**
	 * Get the the centre of the Cuboid
	 * 
	 * @return Location at the centre of the Cuboid
	 */
	public Location getCenter() {
		return new Location(getWorld(), getLowerX() + (getUpperX() - getLowerX()) / 2,
		                    getLowerY() + (getUpperY() - getLowerY()) / 2,
		                    getLowerZ() + (getUpperZ() - getLowerZ()) / 2);
	}

	/**
	 * Get the Cuboid's world.
	 *
	 * @return the World object representing this Cuboid's world
	 */
	public World getWorld() {
		return world;
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

	/**
	 * Get the Blocks at the eight corners of the Cuboid.
	 *
	 * @return list of Block objects representing the Cuboid corners
	 */
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

	/**
	 * Expand the Cuboid in the given direction by the given amount.  Negative amounts will
	 * shrink the Cuboid in the given direction.  Shrinking a cuboid's face past the opposite face
	 * is not an error and will return a valid Cuboid.
	 * 
	 * @param dir
	 * @param amount
	 * @return
	 */
	public Cuboid expand(Direction dir, int amount) {		
		switch (dir) {
		case North:
			return new Cuboid(world, x1 - amount, y1, z1, x2, y2, z2);
		case South:
			return new Cuboid(world, x1, y1, z1, x2 + amount, y2, z2);
		case East:
			return new Cuboid(world, x1, y1, z1 - amount, x2, y2, z2);
		case West:
			return new Cuboid(world, x1, y1, z1, x2, y2, z2 + amount);
		case Down:
			return new Cuboid(world, x1, y1 - amount, z1, x2, y2, z2);
		case Up:
			return new Cuboid(world, x1, y1, z1, x2, y2 + amount, z2);
		default:
			throw new IllegalArgumentException("invalid direction " + dir);
		}
	}

	/**
	 * Shift the Cuboid in the given direction by the given amount.
	 * 
	 * @param dir
	 * @param amount
	 * @return
	 */
	public Cuboid shift(Direction dir, int amount) {
		return expand(dir, amount).expand(dir.opposite(), -amount);
	}

	/**
	 * Outset (grow) the Cuboid in the given direction by the given amount.
	 * 
	 * @param dir
	 * @param amount
	 * @return
	 */
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

	/**
	 * Inset (shrink) the Cuboid in the given direction by the given amount.  Equivalent
	 * to calling outset() with a negative amount.
	 * 
	 * @param dir
	 * @param amount
	 * @return
	 */
	public Cuboid inset(Direction dir, int amount) {
		return outset(dir, -amount);
	}

	/**
	 * Return true if the point at (x,y,z) is contained within the Cuboid.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean contains(int x, int y, int z) {
		return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
	}

	/**
	 * Check if the given Block is contained within the Cuboid.
	 * 
	 * @param b
	 * @return
	 */
	public boolean contains(Block b) {
		return contains(b.getLocation());
	}

	/**
	 * Check if the given Location is contained within the Cuboid.
	 * 
	 * @param l
	 * @return
	 */
	public boolean contains(Location l) {
		if (l.getWorld() != world) {
			return false;
		}
		return contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}

	/**
	 * Get the volume of the Cuboid.
	 * 
	 * @return
	 */
	public int volume() {
		return getSizeX() * getSizeY() * getSizeZ();
	}

	/**
	 * Get the average light level of all empty (air) blocks in the Cuboid.  Returns 0 
	 * if there are no empty blocks.
	 * 
	 * @return
	 */
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
	 * Delete blocks, but don't allow items to drop (paintings are not
	 * blocks, and are not included).  Does not check for blocks attached to the
	 * outside faces of the Cuboid.
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
				if (state instanceof InventoryHolder) {
					InventoryHolder ih = (InventoryHolder) state;
					ih.getInventory().clear();
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

	/**
	 * Set all the blocks within the Cuboid to the given block ID.
	 * 
	 * @param blockID
	 * @param fast
	 */
	public void set(int blockID, boolean fast) {
		long start = System.nanoTime();

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

		ChessCraftLogger.finer("Cuboid: " + this + ": set " + blockID + ": " + (System.nanoTime() - start) + "ns");
	}

	/**
	 * Set all the blocks within the Cuboid to the given block ID and data byte.
	 * 
	 * @param blockID
	 * @param data
	 * @param fast
	 */
	public void set(int blockID, Byte data, boolean fast) {
		long start = System.nanoTime();

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
		
		ChessCraftLogger.finer("Cuboid: " + this + ": set " + blockID + "/" + data + ": " + (System.nanoTime() - start) + "ns");

	}

	/**
	 * Set all the blocks within the Cuboid to the given MaterialWithData
	 * 
	 * @param mat
	 * @param fast
	 */
	public void set(MaterialWithData mat, boolean fast) {
		set(mat.getMaterial(), mat.getData(), fast);
	}

	/**
	 * Contract the Cuboid, returning a Cuboid with any air around the edges removed, just
	 * large enough to include all non-air blocks.
	 */
	public Cuboid contract() {
		return this.
				contract(Direction.Down).
				contract(Direction.South).
				contract(Direction.East).
				contract(Direction.Up).
				contract(Direction.North).
				contract(Direction.West);
	}
	
	/**
	 * Contract the Cuboid in the given direction, returning a new Cuboid which has no exterior empty space.
	 * E.g. a direction of Down will push the top face downwards as much as possible.
	 * 
	 * @param dir
	 * @return
	 */
	public Cuboid contract(Direction dir) {
		Cuboid face = getFace(dir.opposite());
		switch (dir) {
		case Down:
			while (face.containsOnly(0) && face.getLowerY() > this.getLowerY()) {
				face = face.shift(Direction.Down, 1);
			}
			return new Cuboid(world, x1, y1, z1, x2, face.getUpperY(), z2);
		case Up:
			while (face.containsOnly(0) && face.getUpperY() < this.getUpperY()) {
				face = face.shift(Direction.Up, 1);
			}
			return new Cuboid(world, x1, face.getLowerY(), z1, x2, y2, z2);
		case North:
			while (face.containsOnly(0) && face.getLowerX() > this.getLowerX()) {
				face = face.shift(Direction.North, 1);
			}
			return new Cuboid(world, x1, y1, z1, face.getUpperX(), y2, z2);
		case South:
			while (face.containsOnly(0) && face.getUpperX() < this.getUpperX()) {
				face = face.shift(Direction.South, 1);
			}
			return new Cuboid(world, face.getLowerX(), y1, z1, x2, y2, z2);
		case East:
			while (face.containsOnly(0) && face.getLowerZ() > this.getLowerZ()) {
				face = face.shift(Direction.East, 1);
			}
			return new Cuboid(world, x1, y1, z1, x2, y2, face.getUpperZ());
		case West:
			while (face.containsOnly(0) && face.getUpperZ() < this.getUpperZ()) {
				face = face.shift(Direction.West, 1);
			}
			return new Cuboid(world, x1, y1, face.getLowerZ(), x2, y2, z2);
		default:
			throw new IllegalArgumentException("Invalid direction " + dir);
		}
	}
	
	/**
	 * Get the Cuboid representing the face of this Cuboid.  The resulting Cuboid will be
	 * one block thick in the axis perpendicular to the requested face.
	 * 
	 * @param dir	which face of the Cuboid to get 
	 * @return	the Cuboid representing this Cuboid's requested face
	 */
	public Cuboid getFace(Direction dir	) {
		switch (dir) {
		case Down:
			return new Cuboid(world, x1, y1, z1, x2, y1, z2);
		case Up:
			return new Cuboid(world, x1, y2, z1, x2, y2, z2);
		case North:
			return new Cuboid(world, x1, y1, z1, x1, y2, z2);
		case South:
			return new Cuboid(world, x2, y1, z1, x2, y2, z2);
		case East:
			return new Cuboid(world, x1, y1, z1, x2, y2, z1);
		case West:
			return new Cuboid(world, x1, y1, z2, x2, y2, z2);
		default:
			throw new IllegalArgumentException("Invalid direction " + dir);
		}
	}

	/**
	 * Check if the Cuboid contains only blocks of the given type
	 * 
	 * @param blockId	the block ID to check for
	 * @return			true if this Cuboid contains only blocks of the given type
	 */
	public boolean containsOnly(int blockId) {
		for (Block b : this) {
			if (b.getTypeId() != blockId) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the Cuboid big enough to hold this Cuboid and the other one.
	 * 
	 * @param other
	 * @return
	 */
	public Cuboid getBoundingCuboid(Cuboid other) {
		if (other == null) {
			return this;
		}
		
		int xMin = Math.min(getLowerX(), other.getLowerX());
		int yMin = Math.min(getLowerY(), other.getLowerY());
		int zMin = Math.min(getLowerZ(), other.getLowerZ());
		int xMax = Math.max(getUpperX(), other.getUpperX());
		int yMax = Math.max(getUpperY(), other.getUpperY());
		int zMax = Math.max(getUpperZ(), other.getUpperZ());
		
		return new Cuboid(world, xMin, yMin, zMin, xMax, yMax, zMax);
	}
	
	/**
	 * Get a block relative to the lower NE point of the Cuboid.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Block getRelativeBlock(int x, int y, int z) {
		return world.getBlockAt(x1 + x, y1 + y, z1 + z);
	}

	/**
	 * Get a list of the chunks which are fully or partially contained in this cuboid.
	 * 
	 * @return a list of Chunk objects
	 */
	public List<Chunk> getChunks() {
		List<Chunk> res = new ArrayList<Chunk>();
	
		int x1 = getLowerX() & ~0xf; int x2 = getUpperX() & ~0xf;
		int z1 = getLowerZ() & ~0xf; int z2 = getUpperZ() & ~0xf;
		for (int x = x1; x <= x2; x += 16) {
			for (int z = z1; z <= z2; z += 16) {
				res.add(world.getChunkAt(x, z));
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
			ChessCraftLogger.finer("Cuboid: initLighting: chunk " + c + ": relit"); 
		}
	}
	
	/**
	 * Set the light level of all blocks within this Cuboid.
	 * 
	 * @param level			the required light level
	 * @param allBlocks		if true, set the level for all blocks, not just air blocks
	 */
	public void forceLightLevel(int level) {
		long start = System.nanoTime();
		net.minecraft.server.World w = ((CraftWorld) getWorld()).getHandle();
		for (int x = getLowerX(); x < getUpperX(); x++) {
			for (int z = getLowerZ(); z < getUpperZ(); z++) {
				for (int y = getLowerY(); y < getUpperY(); y++) {
					w.a(EnumSkyBlock.BLOCK, x, y, z, level);
				}
			}
		}
		ChessCraftLogger.finer("Cuboid: forceLightLevel: " + this + " (level " + level + ") in " + (System.nanoTime() - start) + " ns");
	}
	
	/**
	 * Any players within the threshold distance of the cuboid may need
	 * to be notified of any fast changes that happened, to avoid "phantom" blocks showing
	 * up on the client.  Add the chunk coordinates of affected chunks to those players'
	 * chunk queue.
	 */
	public void sendClientChanges() {
		int threshold = (Bukkit.getServer().getViewDistance() << 4) + 32;
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
			if ((px - centerX) * (px - centerX) + (pz - centerZ) * (pz - centerZ) < threshold) {
				EntityPlayer ep = ((CraftPlayer) player).getHandle();
				queueChunks(ep, pairs);
//				System.out.print("chunkCoordIntPair: " );
//				for (Object o : ep.chunkCoordIntPairQueue) {
//					System.out.print(((ChunkCoordIntPair)o).x + "," + ((ChunkCoordIntPair)o).z + " ");
//				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")	
	private void queueChunks(EntityPlayer ep, List<ChunkCoordIntPair> pairs) {
		Set<ChunkCoordIntPair> queued = new HashSet<ChunkCoordIntPair>();
		for (Object o : ep.chunkCoordIntPairQueue) {
			queued.add((ChunkCoordIntPair) o);
		}
		for (ChunkCoordIntPair pair : pairs) {
			if (!queued.contains(pair)) {
				ep.chunkCoordIntPairQueue.add(pair);
			}
		}
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

	/**
	 * Set the current WorldEdit selection for the player to this Cuboid
	 * 
	 * @param playerName	name of the player
	 */
	public void worldEditSetSelection(String playerName) {
		Player p = Bukkit.getPlayer(playerName);
		if (p == null || p.getWorld() != world) {
			return;
		}
		worldEditSetSelection(p);
	}

	/**
	 * Set the current WorldEdit selection for the player to this Cuboid
	 * 
	 * @param playerName	the Player object
	 */
	public void worldEditSetSelection(Player p) {
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

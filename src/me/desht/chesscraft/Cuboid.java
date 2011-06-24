package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

public class Cuboid implements Iterable<Location> {
	public enum Direction { 
		North, East, South, West, Up, Down, Horizontal, Vertical, Both, Unknown
	};
	private Location lowerNE;	// min x,y,z
	private Location upperSW;	// max x,y,z
	
	Cuboid(Location l1, Location l2) {
		if (l1.getWorld() != l2.getWorld()) 
			throw new IllegalArgumentException("locations must be on the same world");

		lowerNE = new Location(l1.getWorld(), Math.min(l1.getX(), l2.getX()), Math.min(l1.getY(), l2.getY()), Math.min(l1.getZ(), l2.getZ()));
		upperSW = new Location(l1.getWorld(), Math.max(l1.getX(), l2.getX()), Math.max(l1.getY(), l2.getY()), Math.max(l1.getZ(), l2.getZ()));
	}
	
	public Location getLowerNE() {
		return lowerNE;
	}

	public Location getUpperSW() {
		return upperSW;
	}

	public List<Location>corners() {
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
	
	public Cuboid expand(Direction dir, int amount) {
		switch (dir) {
		case North: lowerNE.setX(lowerNE.getBlockX() - amount); break;
		case South: upperSW.setX(upperSW.getBlockX() + amount); break;
		case East:  lowerNE.setZ(lowerNE.getBlockZ() - amount); break;
		case West:  upperSW.setZ(upperSW.getBlockZ() + amount); break;
		case Down:  lowerNE.setY(lowerNE.getBlockY() - amount); break;  
		case Up:    upperSW.setY(upperSW.getBlockY() + amount); break;
		default: throw new IllegalArgumentException("invalid direction " + dir);
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
		case North: return Direction.South;
		case South: return Direction.North;
		case West:  return Direction.East;
		case East:  return Direction.West;
		case Up:    return Direction.Down;
		case Down:  return Direction.Up;
		case Horizontal: return Direction.Vertical;
		case Vertical:   return Direction.Horizontal;
		}
		return Direction.Unknown;
	}
	
	public boolean contains(int x, int y, int z) {
		if (x >= lowerNE.getBlockX() && x <= upperSW.getBlockX() &&
				y >= lowerNE.getBlockY() && y <= upperSW.getBlockY() &&
				z >= lowerNE.getBlockZ() && z <= upperSW.getBlockZ())
			return true;
		else 
			return false;
	}
	public boolean contains(Location l)	{
		if (l.getWorld() != lowerNE.getWorld())
			return false;
		return contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}
	
	@Override
	public Iterator<Location> iterator() {
		return new CuboidIterator(lowerNE, upperSW);
	}
	
	public String toString() {
		return lowerNE.toString() + " -> " + upperSW.toString();
	}
}

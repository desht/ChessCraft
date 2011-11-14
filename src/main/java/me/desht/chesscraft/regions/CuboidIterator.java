package me.desht.chesscraft.regions;

import java.util.Iterator;

import org.bukkit.Location;

public class CuboidIterator implements Iterator<Location> {

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
	public Location next() {
		Location res = new Location(base.getWorld(), base.getBlockX() + x, base.getBlockY() + y, base.getBlockZ() + z);
		if (++x >= sizeX) {
			x = 0;
			if (++y >= sizeY) {
				y = 0;
				++z;
			}
		}
		return res;
	}

	@Override
	public void remove() {
	}
}

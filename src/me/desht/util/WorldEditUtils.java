package me.desht.util;

import me.desht.chesscraft.regions.Cuboid;

import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import me.desht.chesscraft.ChessCraft;

public class WorldEditUtils {
	
	public static void weSelect(Cuboid c, Player p) {
		WorldEditPlugin wep = ChessCraft.getWorldEdit();
		if (wep == null) {
			return;
		}
		CuboidSelection s = new CuboidSelection(c.getWorld(), c.getUpperSW(), c.getLowerNE());
		wep.setSelection(p, s);
	}
	
}

package me.desht.chesscraft.blocks;

import java.io.File;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.TerrainManager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TerrainBackup {

	public static boolean save(Player player, BoardView view) {
		boolean saved = false;
		try {
			TerrainManager tm = new TerrainManager(ChessCraft.getWorldEdit(), player);
			
			Cuboid c = view.getOuterBounds();
			Location l1 = c.getLowerNE();
			Location l2 = c.getUpperSW();
			tm.saveTerrain(new File(DirectoryStructure.getSchematicsDirectory(), view.getName()), l1, l2);
			saved = true;
		} catch (Exception e) {
			LogUtils.warning(e.getMessage());
		}
		return saved;
	}

	public static boolean reload(Player player, BoardView view) {	
		boolean restored = false;
		try {
			TerrainManager tm = new TerrainManager(ChessCraft.getWorldEdit(), player);
			tm.loadSchematic(new File(DirectoryStructure.getSchematicsDirectory(), view.getName()));
			restored = true;
		} catch (Exception e) {
			LogUtils.warning(e.getMessage());
		}
		return restored;
	}
}

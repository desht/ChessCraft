package me.desht.chesscraft;

import me.desht.chesscraft.regions.Cuboid;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.FilenameException;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.data.DataException;

public class TerrainBackup {

    private Player player;
    private WorldEditPlugin wep;
    private WorldEdit we;
    private LocalSession localSession;
    private EditSession editSession;
    private LocalPlayer localPlayer;
    private CuboidClipboard clipboard;
    private File saveFile;
    private Vector min, max;

    private TerrainBackup(ChessCraft plugin, Player player, BoardView view) throws FilenameException {
        this.player = player;

        wep = plugin.getWorldEdit();
        if (wep == null) {
            return;
        }
        we = wep.getWorldEdit();

        Cuboid bounds = view.getOuterBounds();
        Location l1 = bounds.getUpperSW();
        Location l2 = bounds.getLowerNE();
        max = new Vector(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ());
        min = new Vector(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ());

        localPlayer = wep.wrapPlayer(player);
        localSession = we.getSession(localPlayer);
        editSession = localSession.createEditSession(localPlayer);
        
        try {
            saveFile = we.getSafeSaveFile(localPlayer, ChessConfig.getSchematicsDirectory(), view.getName(), "schematic", new String[]{"schematic"});
        } catch (FilenameException ex) {
            ChessCraft.log(Level.WARNING, ex.getMessage());
        }
    }

    private void saveTerrain() {
        if (wep == null) {
            return;
        }

        try {
            editSession.enableQueue();
            clipboard = new CuboidClipboard(max.subtract(min).add(new Vector(1, 1, 1)), min);
            clipboard.copy(editSession);
            clipboard.saveSchematic(saveFile);
            editSession.flushQueue();
        } catch (DataException e) {
            ChessUtils.errorMessage(player, "Terrain backup could not be written: " + e.getMessage());
        } catch (IOException e) {
            ChessUtils.errorMessage(player, "Terrain backup could not be written: " + e.getMessage());
        }
    }

    private void reloadTerrain() {
        if (wep == null) {
            return;
        }

        try {
            editSession.enableQueue();
            localSession.setClipboard(CuboidClipboard.loadSchematic(saveFile));
            Vector pos = localSession.getClipboard().getOrigin();
            localSession.getClipboard().place(editSession, pos, false);
            editSession.flushQueue();
            we.flushBlockBag(localPlayer, editSession);
            if (!saveFile.delete()) {
                ChessCraft.log(Level.WARNING, "Could not delete " + saveFile);
            }
        } catch (Exception e) {
            // DataException, IOException, EmptyClipboardException, MaxChangedBlocksException
            ChessUtils.errorMessage(player, "Terrain backup could not be restored: " + e.getMessage());
            
        }
    }

    public static void save(ChessCraft plugin, Player player, BoardView view) {
        if (plugin.getWorldEdit() != null) {
            try {
                TerrainBackup tb = new TerrainBackup(plugin, player, view);
                tb.saveTerrain();
            } catch (FilenameException e) {
                ChessCraft.log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static void reload(ChessCraft plugin, Player player, BoardView view) {
        try {
            TerrainBackup tb = new TerrainBackup(plugin, player, view);
            tb.reloadTerrain();
        } catch (FilenameException e) {
            ChessCraft.log(Level.WARNING, e.getMessage());
            // can't restore the terrain, so just replace with air
            view.wipe();
        }
    }
}

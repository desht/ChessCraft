package me.desht.chesscraft;

import me.desht.chesscraft.expector.ExpectResponse;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.iConomy.*;
import com.jascotty2.net.InstallDependency;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.desht.chesscraft.enums.ChessPermission;

public class ChessCraft extends JavaPlugin {

    static final Logger logger = Logger.getLogger("Minecraft");
    private static PluginDescriptionFile description;
    
    private final Map<String, Location> lastPos = new HashMap<String, Location>();
    
    protected ChessPlayerListener playerListener;
    protected ChessBlockListener blockListener;
    protected ChessEntityListener entityListener;
    protected ChessCommandExecutor commandExecutor;
    protected ChessPersistence persistence;
    protected ChessPieceLibrary library;
    protected ExpectResponse expecter;
    public ChessConfig config = null;
    public ChessUtils util = null;
    
    protected WorldEditPlugin worldEditPlugin = null;
    protected iConomy iConomy = null;

    /*-----------------------------------------------------------------*/
    @Override
    public void onLoad() {
        checkForChesspresso();
    }

    @Override
    public void onEnable() {
        description = this.getDescription();
        util = new ChessUtils(this);
        config = new ChessConfig(this);
        config.load();

        playerListener = new ChessPlayerListener(this);
        blockListener = new ChessBlockListener(this);
        entityListener = new ChessEntityListener(this);
        commandExecutor = new ChessCommandExecutor(this);

        library = new ChessPieceLibrary(this);
        persistence = new ChessPersistence(this);
        expecter = new ExpectResponse();

        setupWorldEdit();
        if (ChessPermission.setupPermissions(this.getServer())) {
            log(Level.INFO, "Permissions detected");
        } else {
            log(Level.INFO, "Permissions not detected, using ops");
        }
        
        getCommand("chess").setExecutor(commandExecutor);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_ANIMATION, playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, new ChessServerListener(this), Event.Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLUGIN_DISABLE, new ChessServerListener(this), Event.Priority.Monitor, this);

        persistence.reload();

        util.setupRepeatingTask(2);

        log(" version " + description.getVersion() + " is enabled!");
    }

    @Override
    public void onDisable() {
        for (Game game : Game.listGames()) {
            game.clockTick();
        }
        getServer().getScheduler().cancelTasks(this);
        persistence.save();
        log("disabled!");
    }

    private void checkForChesspresso() {
        File chesspressoFile = new File("lib", ChessConfig.getChesspressoLibName());
        if (!chesspressoFile.exists()) {

            log(Level.INFO, "Downloading Chesspresso lib from " + ChessConfig.getChesspressoDownload() + " ...");

            if (!InstallDependency.install(chesspressoFile.getAbsolutePath(),
                    ChessConfig.getChesspressoZipName(),
                    ChessConfig.getChesspressoDownload())) {
                log(Level.SEVERE, "Could not download Chesspresso library automatically.. ");
                log(Level.SEVERE, "You will need to obtain a copy manually and install it at " + chesspressoFile.getAbsolutePath());
                super.setEnabled(false);
                return;
            }
        }
//            try {
//                Class.forName("chesspresso.Chess").newInstance();
//            } catch (ClassNotFoundException e) {
//                log(Level.SEVERE, "Chesspresso downloaded, but can't find the class?");
//            } catch (Exception ex) {
//                log(Level.SEVERE, "Chesspresso Loading Error", ex);
//            }
//             catch (InstantiationException ex) {
//                Logger.getLogger(ChessCraft.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IllegalAccessException ex) {
//                Logger.getLogger(ChessCraft.class.getName()).log(Level.SEVERE, null, ex);
//            }
    }

    private void setupWorldEdit() {
        Plugin p = this.getServer().getPluginManager().getPlugin("WorldEdit");
        if (p != null && p instanceof WorldEditPlugin) {
            worldEditPlugin = (WorldEditPlugin) p;
            log(Level.INFO, "WorldEdit plugin detected - chess board terrain saving enabled.");
        } else {
            log(Level.INFO, "WorldEdit plugin not detected - chess board terrain saving disabled.");
        }
    }

    public WorldEditPlugin getWorldEdit() {
        return worldEditPlugin;
    }

    /*-----------------------------------------------------------------*/
    
    protected static String getPluginName() {
    	return description != null ? description.getName() : "ChessCraft";
    }
    
    protected static void log(String message) {
        logger.log(Level.INFO, String.format("%s: %s", getPluginName(), message));
    }

    protected static void log(Level level, String message) {
        logger.log(level, String.format("%s: %s", getPluginName(), message));
    }

    protected static void log(Level level, String message, Exception err) {
        logger.log(level, String.format("%s: %s", getPluginName(),
                message == null ? (err == null ? "?" : err.getMessage()) : message), err);
    }

    /*-----------------------------------------------------------------*/
    Location getLastPos(Player player) {
        return lastPos.get(player.getName());
    }

    void setLastPos(Player player, Location loc) {
        lastPos.put(player.getName(), loc);
    }

    /*-----------------------------------------------------------------*/
    public void maybeSave() {
        if (getConfiguration().getBoolean("autosave", true)) {
            persistence.save();
        }
    }

    ChessCommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

}

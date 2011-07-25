/**
 * Programmer: Jacob Scott
 * Program Name: ChessConfig
 * Description: class for organizing configuration settings
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * @author jacob
 */
public class ChessConfig {

    private static final String chesspressoDownload =
            "http://sourceforge.net/projects/chesspresso/files/chesspresso-lib/0.9.2/Chesspresso-lib-0.9.2.zip/download";
    private static final String chesspressoLibName = "Chesspresso-lib.jar";
    private static final String chesspressoZipName = "Chesspresso-lib-0.9.2/Chesspresso-lib.jar";
    private static String directory = "plugins" + File.separator + "ChessCraft";
    ChessCraft plugin = null;
    Configuration config = null;
    private static final Map<String, Object> configItems = new HashMap<String, Object>() {

        {
            put("autosave", true);
            put("tick_interval", 1);
            put("broadcast_results", true);
            put("auto_delete_finished", 30);
            put("no_building", true);
            put("no_creatures", true);
            put("no_explosions", true);
            put("no_burning", true);
            put("no_pvp", true);
            put("wand_item", "air");
            put("auto_teleport_on_join", true);
            put("highlight_last_move", true);
            put("timeout_forfeit", 60);
            put("timeout_auto_delete", 180);
            put("stake.default", 0.0);
            put("stake.smallIncrement", 1.0);
            put("stake.largeIncrement", 10.0);
        }
    };

    public ChessConfig(ChessCraft plugin) {
        this.plugin = plugin;
        if (plugin != null) {
            directory = plugin.getDataFolder().getAbsolutePath();
            config = plugin.getConfiguration();
            System.out.println(directory);
        }
    }

    public void load() {

        setupDefaultStructure();

        configInitialise();

    }

    public static String getChesspressoDownload() {
        return chesspressoDownload;
    }

    public static String getChesspressoLibName() {
        return chesspressoLibName;
    }

    public static String getChesspressoZipName() {
        return chesspressoZipName;
    }

    public static String getDirectory() {
        return directory;
    }

    private void setupDefaultStructure() {
            createDir(null);
            createDir("archive");
            createDir("board_styles");
            createDir("piece_styles");
            createDir("schematics");

            extractResource("/datafiles/board_styles/Standard.yml", "board_styles/Standard.yml");
            extractResource("/datafiles/board_styles/Open.yml", "board_styles/Open.yml");
            extractResource("/datafiles/board_styles/woodsand.yml", "board_styles/woodsand.yml");

            extractResource("/datafiles/piece_styles/Standard.yml", "piece_styles/Standard.yml");
            extractResource("/datafiles/piece_styles/twist.yml", "piece_styles/twist.yml");
            extractResource("/datafiles/piece_styles/sandwood.yml", "piece_styles/sandwood.yml");

    }

    void createDir(String dir) {
        File f = new File(directory);
        if (f.isDirectory()) {
            return;
        }
        if (!f.mkdir()) {
            ChessCraft.log(Level.WARNING, "Can't make directory " + f.getName());
        }
    }

    private void extractResource(String from, String to) {
        File of = new File(directory, to);
        if (of.exists()) {
            return;
        }
        OutputStream out = null;
        try {
            InputStream in = this.getClass().getResourceAsStream(from);
            if (in == null) {
                ChessCraft.log(Level.WARNING, "can't extract resource " + from + " from plugin JAR");
            } else {
                out = new FileOutputStream(of);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        } catch (FileNotFoundException ex) {
            ChessCraft.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            ChessCraft.log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                //ChessCraft.log(Level.SEVERE, null, ex);
            }
        }
    }

    /*-----------------------------------------------------------------*/
    private void configInitialise() {
        Boolean saveNeeded = false;
        for (String k : configItems.keySet()) {
            if (config.getProperty(k) == null) {
                saveNeeded = true;
                config.setProperty(k, configItems.get(k));
            }
        }
        if (saveNeeded) {
            config.save();
        }
    }

    /**
     * @return a sorted list of all config keys
     */
    List<String> getConfigList() {
        ArrayList<String> res = new ArrayList<String>();
        for (String k : configItems.keySet()) {
            res.add(k + " = '" + config.getString(k) + "'");
        }
        Collections.sort(res);
        return res;
    }

    void setConfigItem(Player player, String key, String val) {
        if (configItems.get(key) == null) {
            ChessUtils.errorMessage(player, "No such config key: " + key);
            ChessUtils.errorMessage(player, "Use '/chess getcfg' to list all valid keys");
            return;
        }
        if (configItems.get(key) instanceof Boolean) {
            Boolean bVal = false;
            if (val.equals("false") || val.equals("no")) {
                bVal = false;
            } else if (val.equals("true") || val.equals("yes")) {
                bVal = true;
            } else {
                ChessUtils.errorMessage(player, "Invalid boolean value " + val + " - use true/yes or false/no.");
                return;
            }
            config.setProperty(key, bVal);
        } else if (configItems.get(key) instanceof Integer) {
            try {
                int nVal = Integer.parseInt(val);
                config.setProperty(key, nVal);
            } catch (NumberFormatException e) {
                ChessUtils.errorMessage(player, "Invalid numeric value: " + val);
            }
        } else if (configItems.get(key) instanceof Double) {
            try {
                double nVal = Double.parseDouble(val);
                config.setProperty(key, nVal);
            } catch (NumberFormatException e) {
                ChessUtils.errorMessage(player, "Invalid numeric value: " + val);
            }
        } else {
            config.setProperty(key, val);
        }

        // special hooks
        if (key.equalsIgnoreCase("tick_interval")) {
            plugin.util.setupRepeatingTask(0);
        }

        ChessUtils.statusMessage(player, key + " is now set to: " + val);
        config.save();
    }
} // end class ChessConfig


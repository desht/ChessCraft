package me.desht.chesscraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.config.Configuration;

public class ChessPersistence {

    private ChessCraft plugin;
    private static final String oldPersistFilename = "persist.yml";
    private static final String persistFolderName = "data";
    // games will be named by gamename, but characteristically cannot contain spaces
    // spaces in filenames to avoid overwriting these files
    private static final String persistBoardsFilename = "saved boards.yml";
    private static final String persistPlayerGamesFilename = "player games.yml";
    private static File persistFolder = null;

    public ChessPersistence(ChessCraft plugin) {
        this.plugin = plugin;
        persistFolder = new File(ChessConfig.getDirectory(), persistFolderName);
    }

    public void save() {
        savePersistedData();
    }

    public void autosaveGames() {
        if (plugin.config.config.getBoolean("autosave", true)) {
            saveGames();
        }
    }

    public void autosaveGame(Game g) {
        if (plugin.config.config.getBoolean("autosave", true)) {
            saveGame(g);
        }
    }

    public void autosaveBoards() {
        if (plugin.config.config.getBoolean("autosave", true)) {
            saveBoards();
        }
    }

    public void autosavePlayerGames() {
        if (plugin.config.config.getBoolean("autosave", true)) {
            savePlayerGames();
        }
    }

    public void reload() {
        for (Game game : Game.listGames()) {
            game.delete(false);
        }
        for (BoardView view : BoardView.listBoardViews()) {
            view.delete();
        }

        loadPersistedData();
    }

    public static List<Object> makeBlockList(Location l) {
        List<Object> list = new ArrayList<Object>();
        list.add(l.getWorld().getName());
        list.add(l.getBlockX());
        list.add(l.getBlockY());
        list.add(l.getBlockZ());

        return list;
    }

    public static World findWorld(String worldName) {
        World w = Bukkit.getServer().getWorld(worldName);

        if (w != null) {
            return w;
        } else {
            throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
        }
    }

    private void savePersistedData() {
        saveBoards();
        saveGames();
        savePlayerGames();
    }

    public void saveBoards() {
        Configuration conf = new Configuration(new File(persistFolder, persistBoardsFilename));

        List<Map<String, Object>> boards = new ArrayList<Map<String, Object>>();
        for (BoardView bv : BoardView.listBoardViews()) {
            boards.add(bv.freeze());
        }
        conf.setProperty("boards", boards);
        conf.save();
    }

    public void saveGames() {
        for (Game g : Game.listGames()) {
            saveGame(g);
        }
    }

    public void saveGame(Game g) {
        Configuration conf = new Configuration(new File(persistFolder, safeFileName(g.getName()) + ".yml"));
        Map<String, Object> st = g.freeze();
        for (String key : st.keySet()) {
            conf.setProperty(key, st.get(key));
        }
        conf.save();
    }

    protected String safeFileName(String name) {
        return name == null ? "" : name.replace("/", "-").
                replace("\\", "-").replace("?", "-").replace(":", ";").
                replace("%", "-").replace("|", ";").replace("\"", "'").
                replace("<", ",").replace(">", ".").replace("+", "=").
                replace("[", "(").replace("]", ")");
    }

    public void savePlayerGames() {
        Configuration conf = new Configuration(new File(persistFolder, persistPlayerGamesFilename));
        conf.setProperty("current_games", Game.getCurrentGames());
        conf.save();
    }

    public void removeGame(Game g) {
        File f = new File(persistFolder, safeFileName(g.getName()) + ".yml");
        if (f.exists()) {
            f.delete();
        }
    }

    protected void autoremoveGames() {
        ArrayList<String> gamenames = new ArrayList<String>();
        for (Game g : Game.listGames()) {
            gamenames.add(g.getName());
        }
        for (File f : persistFolder.listFiles()) {
            if (!f.isFile() || f.getName().contains(" ") || !f.getName().endsWith(".yml")) {
                continue;
            }
            boolean ok = false;
            try {
                Configuration conf = new Configuration(f);
                conf.load();
                ok = gamenames.contains(conf.getString("name", ""));
            } catch (Exception e) {
            }
            if (!ok) {
                f.renameTo(new File(f.getParentFile(), f.getName() + ".bak"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPersistedData() {
        if (!loadOldPersistedData()) {
            int nLoadedBoards = loadBoards();
            int nLoadedGames = loadGames();

            ChessCraft.log(Level.INFO, "loaded " + nLoadedBoards + " saved boards and " + nLoadedGames + " saved games.");

            loadPlayerGames();
        } else {
            save();
        }
    }

    @SuppressWarnings("unchecked")
    private int loadBoards() {
        int nWantedBoards = 0, nLoadedBoards = 0;
        File f = new File(persistFolder, persistBoardsFilename);
        if (f.exists()) {
            Configuration conf = new Configuration(f);
            conf.load();


            List<Map<String, Object>> boards = (List<Map<String, Object>>) conf.getProperty("boards");
            if (boards != null) {
                nWantedBoards = boards.size();
                nLoadedBoards = loadBoards(boards);
            }

            if (nWantedBoards != nLoadedBoards) {
                ChessCraft.log(Level.INFO, "An error occurred while loading saved boards.. making a backup, just in case");
                makeBackup(f);
            }

            for (BoardView bv : BoardView.listBoardViews()) {
                bv.paintAll();
            }
        }
        return nLoadedBoards;
    }

    private int loadGames() {
        HashMap<String, Map<String, Object>> toLoad = new HashMap<String, Map<String, Object>>();

        for (File f : persistFolder.listFiles()) {
            if (!f.isFile() || f.getName().contains(" ") || !f.getName().endsWith(".yml")) {
                continue;
            }
            try {
                Configuration conf = new Configuration(f);
                conf.load();
                conf.setProperty("filename", f.getName());
                String board = conf.getString("boardview");
                if (board == null) {
                    ChessCraft.log(Level.SEVERE, "can't load saved game " + f.getName() + ": boardview is null");
                    f.renameTo(new File(f.getParentFile(), f.getName() + ".bak"));
                } else if (toLoad.containsKey(board)) {
                    // only load the newer game
                    int tstart = conf.getInt("started", 0);
                    int ostart = (Integer) toLoad.get(board).get("started");
                    if (ostart >= tstart) {
                        ChessCraft.log(Level.SEVERE, "can't load saved game " + f.getName() + ": another game is using the same board");
                        f.renameTo(new File(f.getParentFile(), f.getName() + ".bak"));
                    } else {
                        String fn = (String) toLoad.get(board).get("filename");
                        ChessCraft.log(Level.SEVERE, "can't load saved game " + fn + ": another game is using the same board");
                        (new File(f.getParentFile(), fn)).renameTo(new File(f.getParentFile(), fn + ".bak"));
                        toLoad.put(board, conf.getAll());
                    }
                } else {
                    toLoad.put(board, conf.getAll());
                }
            } catch (Exception ex) {
                ChessCraft.log(Level.SEVERE, "can't load saved game " + f.getName() + ": " + ex.getMessage());
                f.renameTo(new File(f.getParentFile(), f.getName() + ".bak"));
            }
        }
        int nLoaded = 0;
        for (Map<String, Object> gameMap : toLoad.values()) {
            if (loadGame(gameMap)) {
                ++nLoaded;
            }
        }
        return nLoaded;
    }

    @SuppressWarnings("unchecked")
    private void loadPlayerGames() {
        File f = new File(plugin.getDataFolder(), persistPlayerGamesFilename);
        if (f.exists()) {
            try {
                Configuration conf = new Configuration(f);
                conf.load();
                Map<String, String> cgMap = (Map<String, String>) conf.getProperty("current_games");
                if (cgMap != null) {
                    for (Entry<String, String> entry : cgMap.entrySet()) {
                        try {
                            Game.setCurrentGame(entry.getKey(), entry.getValue());
                        } catch (ChessException e) {
                            ChessCraft.log(Level.WARNING, "can't set current game for player " + entry.getKey() + ": "
                                    + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                ChessCraft.log(Level.SEVERE, "Unexpected error loading saved player games: " + e.getMessage());
                f.renameTo(new File(f.getParentFile(), f.getName() + ".bak"));
            }
        }
    }

    /**
     * old loading routine
     * if the old file is found, will load it, make a backup, then delete
     * @return true if the old file was found
     */
    @SuppressWarnings("unchecked")
    private boolean loadOldPersistedData() {
        File f = new File(plugin.getDataFolder(), oldPersistFilename);
        if (f.exists()) {
            try {
                Configuration conf = new Configuration(f);
                conf.load();

                int nWantedBoards = 0, nLoadedBoards = 0;
                int nWantedGames = 0, nLoadedGames = 0;

                List<Map<String, Object>> boards = (List<Map<String, Object>>) conf.getProperty("boards");
                if (boards != null) {
                    nWantedBoards = boards.size();
                    nLoadedBoards = loadBoards(boards);
                }
                List<Map<String, Object>> games = (List<Map<String, Object>>) conf.getProperty("games");
                if (games != null) {
                    nWantedGames = games.size();
                    nLoadedGames = loadGames(games);
                }

                if (nWantedBoards != nLoadedBoards || nWantedGames != nLoadedGames) {
                    ChessCraft.log(Level.INFO, "An error occurred while loading the saved data");
                }

                ChessCraft.log(Level.INFO, "loaded " + nLoadedBoards + " saved boards and " + nLoadedGames + " saved games from old file.");

                for (BoardView bv : BoardView.listBoardViews()) {
                    bv.paintAll();
                }

                Map<String, String> cgMap = (Map<String, String>) conf.getProperty("current_games");
                if (cgMap != null) {
                    for (Entry<String, String> entry : cgMap.entrySet()) {
                        try {
                            Game.setCurrentGame(entry.getKey(), entry.getValue());
                        } catch (ChessException e) {
                            ChessCraft.log(Level.WARNING, "can't set current game for player " + entry.getKey() + ": "
                                    + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                ChessCraft.log(Level.SEVERE, "Unexpected Error while loading the saved data: " + e.getMessage());
            }
            ChessCraft.log(Level.INFO, "old file will be backed up, just in case");
            File backup = getBackupFileName(f.getParentFile(), oldPersistFilename);
            // rename much easier than copy & delete :)
            f.renameTo(backup);

            return true;
        }
        return false;
    }

    private void makeBackup(File original) {
        try {
            File backup = getBackupFileName(original.getParentFile(), original.getName());
            copy(original, backup);
        } catch (IOException e) {
            ChessCraft.log(Level.SEVERE, "Error while trying to write backup file: " + e);
        }
    }

    public static File getBackupFileName(File parentFile, String template) {
        String ext = ".BACKUP.";
        File backup;
        int idx = 0;

        do {
            backup = new File(parentFile, template + ext + idx);
            ++idx;
        } while (backup.exists());
        return backup;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    @SuppressWarnings("unchecked")
    private int loadBoards(List<Map<String, Object>> boardList) {
        int nLoaded = 0;
        for (Map<String, Object> boardMap : boardList) {
            String bvName = (String) boardMap.get("name");
            List<Object> origin = (List<Object>) boardMap.get("origin");
            World w = findWorld((String) origin.get(0));
            Location originLoc = new Location(w, (Integer) origin.get(1), (Integer) origin.get(2), (Integer) origin.get(3));
            try {
                BoardView.addBoardView(
                        new BoardView(bvName, plugin, originLoc, (String) boardMap.get("boardStyle"),
                        (String) boardMap.get("pieceStyle")));
                ++nLoaded;
            } catch (Exception e) {
                ChessCraft.log(Level.SEVERE, "can't load board " + bvName + ": " + e.getMessage());
            }
        }
        return nLoaded;
    }

    private int loadGames(List<Map<String, Object>> gameList) {
        int nLoaded = 0;
        for (Map<String, Object> gameMap : gameList) {
            if (loadGame(gameMap)) {
                ++nLoaded;
            }
        }
        return nLoaded;
    }

    private boolean loadGame(Map<String, Object> gameMap) {
        String gameName = (String) gameMap.get("name");
        try {
            BoardView bv = BoardView.getBoardView((String) gameMap.get("boardview"));
            Game game = new Game(plugin, gameName, bv, null);
            if (game.thaw(gameMap)) {
                Game.addGame(gameName, game);
                return true;
            }
        } catch (Exception e) {
            ChessCraft.log(Level.SEVERE, "can't load saved game " + gameName + ": ", e);//  + e.getMessage());
        }
        return false;
    }
}

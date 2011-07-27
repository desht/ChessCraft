/**
 * Programmer: Jacob Scott
 * Program Name: ChessAI
 * Description: class for interfacing with an AI engine
 * Date: Jul 25, 2011
 */
package me.desht.chesscraft;

import com.jascotty2.util.Rand;
import fr.free.jchecs.ai.Engine;
import fr.free.jchecs.ai.EngineFactory;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Player;
import fr.free.jchecs.core.Square;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import me.desht.chesscraft.enums.ChessEngine;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

/**
 * @author jacob
 */
public class ChessAI {

    public final static String AI_PREFIX = "__AI__";
    static ChessCraft plugin = null;
    static BukkitScheduler scheduler = null;
    static HashMap<String, ChessAI> runningAI = new HashMap<String, ChessAI>();
    static HashMap<String, AI_Def> avaliableAI = new HashMap<String, AI_Def>();
    fr.free.jchecs.core.Game _game = null;
    String name = null;
    Game callback = null;
    boolean userToMove = true, isWhite = false;
    int aiTask = -1;
    AI_Def aiSettings = null;

    public ChessAI() throws ChessException {
        aiSettings = getAI(null);
        if (aiSettings == null) {
            throw new ChessException("no free AI was found");
        }
        name = AI_PREFIX + aiSettings.name;
    }

    public ChessAI(String aiName) throws ChessException {
        aiSettings = getAI(aiName);
        if (aiSettings == null) {
            throw new ChessException("AI not found");
        } else if (runningAI.containsKey(aiSettings.name.toLowerCase())) {
            throw new ChessException("AI is busy right now");
        }
        name = AI_PREFIX + aiSettings.name;
    }

    public String getName() {
        return name;
    }

    public void init(boolean aiWhite) {
        if (_game != null) {
            return; // only init once
        }
        _game = new fr.free.jchecs.core.Game();
        //_game.getPlayer(aiWhite)
        Player joueur = _game.getPlayer(!aiWhite);
        joueur.setName("Human");
        joueur.setEngine(null);
        joueur = _game.getPlayer(aiWhite);
        joueur.setName("Computer");
        joueur.setEngine(aiSettings.newInstance());
        isWhite = aiWhite;
    }

    public static void initThreading(ChessCraft plugin) {
        if (plugin != null) {
            ChessAI.plugin = plugin;
            scheduler = plugin.getServer().getScheduler();
        }
    }

    public static void initAI_Names() {
        avaliableAI.clear();
        try {
            File aiFile = new File(ChessConfig.getDirectory(), "AI_settings.yml");
            if (!aiFile.exists()) {
                ChessCraft.log(Level.SEVERE, "AI Loading Error: file not found");
                return;
            }
            Configuration config = new Configuration(aiFile);
            config.load();
            ConfigurationNode n = config.getNode("AI");

            if (n == null) {
                ChessCraft.log(Level.SEVERE, "AI Loading Error: AI definitions not found");
                return;
            }

            for (String a : n.getKeys()) {
                ConfigurationNode d = n.getNode(a);
                if (n.getBoolean("enabled", true)) {
                    for (String name : d.getString("funName", a).split(",")) {
                        if ((name = name.trim()).length() > 0) {
                            avaliableAI.put(name.toLowerCase(),
                                    new AI_Def(name, ChessEngine.getEngine(d.getString("engine")),
                                    d.getInt("depth", 0)));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ChessCraft.log(Level.SEVERE, "AI Loading Error", ex);
        }
    }

    public static ChessAI getNewAI(Game callback) throws ChessException {
        return getNewAI(callback, null, false);
    }

    public static ChessAI getNewAI(Game callback, boolean forceNew) throws ChessException {
        return getNewAI(callback, null, forceNew);
    }

    public static ChessAI getNewAI(Game callback, String aiName) throws ChessException {
        return getNewAI(callback, aiName, false);
    }

    public static ChessAI getNewAI(Game callback, String aiName, boolean forceNew) throws ChessException {
        // uses exceptions method to stop too many AI
        if (!forceNew) {
            int max = plugin.getConfiguration().getInt("ai.max_ai_games", 3);
            if (max == 0) {
                throw new ChessException("AI games are disabled");
            } else if (runningAI.size() >= max) {
                throw new ChessException("there are no AI avaliable to play right now \n"
                        + "(all " + max + " are currently playing games of their own)");
            }
        }

        ChessAI ai = new ChessAI(aiName);
        ai.callback = callback;
        runningAI.put(ai.aiSettings.name.toLowerCase(), ai);

        return ai;
    }

    public static void clearAI() {
        String[] ais = runningAI.keySet().toArray(new String[0]);
        for (String aiName : ais) {
            ChessAI ai = runningAI.get(aiName);
            if(ai!=null){
                ai.removeAI();
            }
        }
        runningAI.clear();
    }

    public void removeAI() {
        if (aiTask != -1) {
            scheduler.cancelTask(aiTask);
        }
        _game.getPlayer(isWhite).setEngine(null);
        _game = null;
        callback = null;
        runningAI.remove(aiSettings.name.toLowerCase());
    }

    public void loadBoard(chesspresso.game.Game game, boolean usersTurn) {
        setUserMove(usersTurn);
    }

    public void setUserMove(boolean move) {
        if (move != userToMove) {
            if (!(userToMove = move)) {
                int wait = plugin.getConfiguration().getInt("ai.min_move_wait", 3);
                aiTask = scheduler.scheduleAsyncDelayedTask(plugin, new Runnable() {

                    public void run() {
                        final MoveGenerator plateau = _game.getBoard();
                        final Engine ia = _game.getPlayer(isWhite).getEngine();
                        if (ia != null) {
                            Move m = ia.getMoveFor(plateau);
                            aiMove(m);
                            aiTask = -1;
                        }
                    }
                }, wait * 20);
            }
        }
    }

    public void loadmove(int fromIndex, int toIndex) {
        Square from = Square.valueOf(fromIndex),
                to = Square.valueOf(toIndex);
        _game.moveFromCurrent(new Move(_game.getBoard().getPieceAt(from), from, to));
        userToMove = !userToMove;
    }

    public void loadDone() {
        if (!userToMove) {
            //trick the other method into starting the ai thread
            userToMove = true;
            setUserMove(false);
        }
    }

    public void userMove(int fromIndex, int toIndex) {
        if (!userToMove) {
            return;
        }
        //System.out.println("user move: " + fromIndex + " to " + toIndex);

        Square from = Square.valueOf(fromIndex),
                to = Square.valueOf(toIndex);
        // or?
//        Square from = Square.valueOf(chesspresso.Chess.sqiToRow(fromIndex), chesspresso.Chess.sqiToCol(fromIndex)),
//                to = Square.valueOf(chesspresso.Chess.sqiToRow(toIndex), chesspresso.Chess.sqiToCol(toIndex));

        //assume move is legal
        _game.moveFromCurrent(new Move(_game.getBoard().getPieceAt(from), from, to));

        setUserMove(false);
    }

    public void aiMove(Move m) {
        if (userToMove || _game == null) {
            return;
        }
        //System.out.println("ai move: " + m);

        try {
            callback.doMove(name, m.getTo().getIndex(), m.getFrom().getIndex());
            if (_game != null){ // if game not been deleted
                _game.moveFromCurrent(m);
            }
        } catch (Exception ex) {
            ChessCraft.log(Level.SEVERE, "Unexpected Exception in AI", ex);
            callback.alert("Unexpected Exception in AI: " + ex.getMessage());
        }

        userToMove = true;
    }

    public static boolean isFree(AI_Def ai) {
        return ai != null && !runningAI.containsKey(ai.name.toLowerCase());
    }

    public static AI_Def getFreeAI(String aiName) {
        AI_Def ai = getAI(aiName);
        return ai != null && !runningAI.containsKey(ai.name.toLowerCase()) ? ai : null;
    }

    public static AI_Def getAI(String aiName) {
        if (aiName == null) {
            // return a random free AI
            ArrayList<Integer> free = new ArrayList<Integer>();
            String ai[] = avaliableAI.keySet().toArray(new String[0]);
            for (int i = 0; i < ai.length; ++i) {
                if (!runningAI.containsKey(ai[i])) {
                    free.add(i);
                }
            }
            if (free.size() > 0) {
                return avaliableAI.get(ai[Rand.RandomInt(0, ai.length - 1)]);
            } else {
                return null;
            }
        }
        // else, return one with a matching name
        //          (if multiple, return one if its the only one free)
        aiName = aiName.toLowerCase();
        if (aiName.startsWith(AI_PREFIX.toLowerCase())) {
            aiName = aiName.substring(AI_PREFIX.length());
        }
        if (!avaliableAI.containsKey(aiName)) {
            String keys[] = avaliableAI.keySet().toArray(new String[0]);
            String matches[] = ChessUtils.fuzzyMatch(aiName, keys, 3);
            if (matches.length == 1) {
                aiName = matches[0];
            } else if (matches.length > 0) {
                // first that is avaliable
                int k = -1;
                for (int i = 0; i < matches.length; ++i) {
                    if (!runningAI.containsKey(matches[i])) {
                        if (k != -1) {
                            k = -1;
                            break;
                        } else {
                            k = i;
                        }
                    }
                }
                if (k != -1) {
                    aiName = matches[k];
                }
            }
        }

        return avaliableAI.get(aiName);
    }

    public static class AI_Def {

        public String name;
        ChessEngine engine;
        int searchDepth;

        public AI_Def(String name, ChessEngine engine, int searchDepth) {
            this.name = name;
            this.engine = engine;
            this.searchDepth = searchDepth;
        }

        public ChessEngine getEngine() {
            return engine;
        }

        public int getSearchDepth() {
            return searchDepth;
        }

        public Engine newInstance() {
            Engine moteur;
            if (engine == null) {
                moteur = EngineFactory.newInstance();
            } else {
                moteur = EngineFactory.newInstance(engine.toString());
                if (searchDepth >= engine.getMinDepth() && searchDepth <= engine.getMaxDepth()) {
                    moteur.setSearchDepthLimit(searchDepth);
                }
            }
            moteur.setOpeningsEnabled(false); // don't use pre-defined openings
            return moteur;
        }
    }
} // end class ChessAI


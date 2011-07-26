/**
 * Programmer: Jacob Scott
 * Program Name: ChessAI
 * Description: class for interfacing with an AI engine
 * Date: Jul 25, 2011
 */
package me.desht.chesscraft;

import fr.free.jchecs.ai.Engine;
import fr.free.jchecs.ai.EngineFactory;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Player;
import fr.free.jchecs.core.Square;
import java.util.HashMap;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @author jacob
 */
public class ChessAI {

    public final static String AI_PREFIX = "__AI__";
    static ChessCraft plugin = null;
    static BukkitScheduler scheduler = null;

    public static void initThreading(ChessCraft plugin) {
        if (plugin != null) {
            ChessAI.plugin = plugin;
            scheduler = plugin.getServer().getScheduler();
        }
    }
    static HashMap<String, ChessAI> allAI = new HashMap<String, ChessAI>();
    fr.free.jchecs.core.Game _game = null;
    String name = null;
    Game callback = null;
    boolean userToMove = true, isWhite = false;

    public String getName() {
        return name;
    }

    public ChessAI() {
        name = AI_PREFIX + System.currentTimeMillis();

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
        final Engine moteur = EngineFactory.newInstance();
        moteur.setOpeningsEnabled(false);
        joueur.setEngine(moteur);

        isWhite = aiWhite;
    }

    public static ChessAI getNewAI(Game callback) {
        ChessAI ai = new ChessAI();
        ai.callback = callback;
        allAI.put(ai.getName(), ai);

        return ai;
    }

    public static void clearAI() {
        allAI.clear();
    }

    public void loadBoard(chesspresso.game.Game game, boolean usersTurn) {
        setUserMove(usersTurn);
    }

    public void setUserMove(boolean move) {
        if (move != userToMove) {
            if (!(userToMove = move)) {
                scheduler.scheduleAsyncDelayedTask(plugin, new Runnable() {

                    public void run() {
                        final MoveGenerator plateau = _game.getBoard();
                        final Engine ia = _game.getPlayer(isWhite).getEngine();
                        if (ia != null) {
                            Move m = ia.getMoveFor(plateau);
                            aiMove(m);
                        }
                    }
                });
            }
        }
    }
    
    public void loadmove(int fromIndex, int toIndex){
        Square from = Square.valueOf(fromIndex),
                to = Square.valueOf(toIndex);
        _game.moveFromCurrent(new Move(_game.getBoard().getPieceAt(from), from, to));
        userToMove = !userToMove;
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
        if (userToMove) {
            return;
        }
        //System.out.println("ai move: " + m);
        
        try {
            callback.doMove(name, m.getTo().getIndex(), m.getFrom().getIndex());
            _game.moveFromCurrent(m);
        } catch (Exception ex) {
            ChessCraft.log(Level.SEVERE, "Unexpected Exception in AI", ex);
            callback.alert("Unexpected Exception in AI: " + ex.getMessage());
        }
//        catch (IllegalMoveException ex) {
//            Logger.getLogger(ChessAI.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ChessException ex) {
//            Logger.getLogger(ChessAI.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println("chesspro fen:");
//        System.out.println(callback.cpGame.getPosition().getFEN());
//        System.out.println("carballo fen:");
//        System.out.println(_game.getFENPosition());
        userToMove = true;
    }
} // end class ChessAI


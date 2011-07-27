package me.desht.chesscraft;

import me.desht.chesscraft.ChessAI.AI_Def;
import me.desht.chesscraft.enums.GameState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.pgn.PGN;
import chesspresso.pgn.PGNWriter;
import chesspresso.position.Position;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.enums.GameResult;

public class Game {

    private static final Map<String, Game> chessGames = new HashMap<String, Game>();
    private static final Map<String, Game> currentGame = new HashMap<String, Game>();
    private static final String archiveDir = "pgn";
    private ChessCraft plugin;
    private String name;
    private chesspresso.game.Game cpGame;
    private BoardView view;
    private String playerWhite, playerBlack;
    private int promotionPiece[] = {Chess.QUEEN, Chess.QUEEN};
    private String invited;
    private GameState state;
    private int fromSquare;
    private Date started;
    private Date lastCheck;
    private int timeWhite, timeBlack;
    private List<Short> history;
    private int delTask;
    private int result;
    private double stake;
    private ChessAI aiPlayer = null;

    public Game(ChessCraft plugin, String name, BoardView view, String playerName) throws ChessException {
        this.plugin = plugin;
        this.view = view;
        this.name = name;
        if (view.getGame() != null) {
            throw new ChessException("That board already has a game on it.");
        }
        view.setGame(this);
        playerWhite = playerName == null ? "" : playerName;
        playerBlack = "";
        timeWhite = timeBlack = 0;
        state = GameState.SETTING_UP;
        fromSquare = Chess.NO_SQUARE;
        invited = "";
        history = new ArrayList<Short>();
        started = new Date();
        lastCheck = new Date();
        result = Chess.RES_NOT_FINISHED;
        delTask = -1;
        stake = Math.min(plugin.getConfiguration().getDouble("stake.default", 0.0),
                Economy.getBalance(playerName));

        setupChesspressoGame();

        getPosition().addPositionListener(view);
    }

    private void setupChesspressoGame() {
        cpGame = new chesspresso.game.Game();

        // seven tag roster
        cpGame.setTag(PGN.TAG_EVENT, getName());
        cpGame.setTag(PGN.TAG_SITE, getView().getName() + " in Minecraftia");
        cpGame.setTag(PGN.TAG_DATE, dateToPGNDate(started));
        cpGame.setTag(PGN.TAG_ROUND, "?");
        cpGame.setTag(PGN.TAG_WHITE, getPlayerWhite());
        cpGame.setTag(PGN.TAG_BLACK, getPlayerBlack());
        cpGame.setTag(PGN.TAG_RESULT, getPGNResult());

        // extra tags
        cpGame.setTag(PGN.TAG_FEN, Position.createInitialPosition().getFEN());
    }

    Map<String, Object> freeze() {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("name", name);
        map.put("boardview", view.getName());
        map.put("playerWhite", playerWhite);
        map.put("playerBlack", playerBlack);
        map.put("state", state.toString());
        map.put("invited", invited);
        map.put("moves", history);
        map.put("started", started.getTime());
        map.put("result", result);
        map.put("promotionWhite", promotionPiece[Chess.WHITE]);
        map.put("promotionBlack", promotionPiece[Chess.BLACK]);
        map.put("timeWhite", timeWhite);
        map.put("timeBlack", timeBlack);
        map.put("stake", stake);

        return map;
    }

    @SuppressWarnings("unchecked")
    boolean thaw(Map<String, Object> map) {
        playerWhite = (String) map.get("playerWhite");
        playerBlack = (String) map.get("playerBlack");
        state = GameState.valueOf((String) map.get("state"));
        invited = (String) map.get("invited");
        List<Integer> hTmp = (List<Integer>) map.get("moves");
        history.clear();
        for (int m : hTmp) {
            history.add((short) m);
        }
        started.setTime((Long) map.get("started"));
        result = (Integer) map.get("result");
        promotionPiece[Chess.WHITE] = (Integer) map.get("promotionWhite");
        promotionPiece[Chess.BLACK] = (Integer) map.get("promotionBlack");
        if (map.containsKey("timeWhite")) {
            timeWhite = (Integer) map.get("timeWhite");
            timeBlack = (Integer) map.get("timeBlack");
        }
        if (map.containsKey("stake")) {
            stake = (Double) map.get("stake");
        }
        setupChesspressoGame();

        try {
            if (isAIPlayer(playerWhite)) {
                aiPlayer = ChessAI.getNewAI(this, playerWhite, true);
                playerWhite = aiPlayer.getName();
                aiPlayer.init(true);
            } else if (isAIPlayer(playerBlack)) {
                aiPlayer = ChessAI.getNewAI(this, playerBlack, true);
                playerBlack = aiPlayer.getName();
                aiPlayer.init(false);
            }

            // Replay the move history to restore the saved board position.
            // We do this instead of just saving the position so that the
            // Chesspresso Game model
            // includes a history of the moves, suitable for creating a PGN file.
            for (short move : history) {
                getPosition().doMove(move);
            }
            // repeat for the ai engine (doesn't support loading from fen)
            if (aiPlayer != null) {
                for (short move : history) {
                    aiPlayer.loadmove(Move.getFromSqi(move), Move.getToSqi(move));
                }
                aiPlayer.loadDone(); // tell ai to start on next move
            }
        } catch (IllegalMoveException e) {
            // should only get here if the save file was corrupted - the history
            // is a list of moves which was already validated before the game was
            // saved
            ChessCraft.log(Level.WARNING, "can't restore move history for game " + getName()
                    + " - move history corrupted?" + "  (game will be deleted)");
            delete();
            return false;
        } catch (Exception e) {
            ChessCraft.log(Level.WARNING, "Unexpected exception restoring game "
                    + getName() + "\n" + e.getMessage() + "  (game will be deleted)");
            // delete game
            delete();
            return false;
        }

        getPosition().addPositionListener(view);

        return true;
    }

    public String getName() {
        return name;
    }

    public final Position getPosition() {
        return cpGame.getPosition();
    }

    public BoardView getView() {
        return view;
    }

    public String getPlayerWhite() {
        return playerWhite;
    }

    public String getPlayerBlack() {
        return playerBlack;
    }

    public int getTimeWhite() {
        return timeWhite;
    }

    public int getTimeBlack() {
        return timeBlack;
    }

    public String getInvited() {
        return invited;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
        if (state == GameState.FINISHED
                && aiPlayer != null) {
            aiPlayer.removeAI();
            aiPlayer = null;
        }
        getView().getControlPanel().repaintSignButtons();
    }

    public int getFromSquare() {
        return fromSquare;
    }

    public Date getStarted() {
        return started;
    }

    public void setFromSquare(int fromSquare) {
        this.fromSquare = fromSquare;
    }

    public List<Short> getHistory() {
        return history;
    }

    public String getOtherPlayer(String name) {
        return name.equals(playerWhite) ? playerBlack : playerWhite;
    }

    public double getStake() {
        return stake;
    }

    public void setStake(double newStake) throws ChessException {
        ensureGameState(GameState.SETTING_UP);

        if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
            throw new ChessException("Stake cannot be changed once both players have joined.");
        }

        this.stake = newStake;
    }

    public void clockTick() {
        if (state != GameState.RUNNING) {
            return;
        }

        Date now = new Date();
        long diff = now.getTime() - lastCheck.getTime();
        lastCheck.setTime(now.getTime());
        if (getPosition().getToPlay() == Chess.WHITE) {
            timeWhite += diff;
            getView().getControlPanel().updateClock(Chess.WHITE, timeWhite);
        } else {
            timeBlack += diff;
            getView().getControlPanel().updateClock(Chess.BLACK, timeBlack);
        }
    }

    public void swapColours() {
        clockTick();
        String tmp = playerWhite;
        playerWhite = playerBlack;
        playerBlack = tmp;
        alert(playerWhite, "Side swap!  You are now playing White.");
        alert(playerBlack, "Side swap!  You are now playing Black.");
    }

    public void addPlayer(String playerName) throws ChessException {
        ensureGameState(GameState.SETTING_UP);
        if (!playerBlack.isEmpty() && !playerWhite.isEmpty()) {
            throw new ChessException("This game already has two players.");
        }

        String otherPlayer = playerBlack.isEmpty() ? playerWhite : playerBlack;

        if (isAIPlayer(playerName)) {
            addAI(playerName);
        } else {
            if (!invited.equals("*") && !invited.equalsIgnoreCase(playerName)) {
                throw new ChessException("You don't have an invitation for this game.");
            }
            if (playerBlack.isEmpty()) {
                playerBlack = playerName;
            } else {//if (playerWhite.isEmpty()) {
                playerWhite = playerName;
            }
        }
        getView().getControlPanel().repaintSignButtons();
        alert(otherPlayer, playerName + " has joined your game.");
        clearInvitation();
        if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
            alert("Start the game by typing &f/game start&-.");
        }
    }

    void addAI(String aiName) throws ChessException {
        if (playerWhite.isEmpty()) {
            aiPlayer = ChessAI.getNewAI(this, aiName);
            playerWhite = aiPlayer.getName();
            aiPlayer.init(true);
            aiPlayer.setUserMove(false); // tell ai to start thinking
        } else if (playerBlack.isEmpty()) {
            aiPlayer = ChessAI.getNewAI(this, aiName);
            playerBlack = aiPlayer.getName();
            aiPlayer.init(false);
        }
    }

    public void invitePlayer(String inviterName, String inviteeName) throws ChessException {
        inviteSanityCheck(inviterName);
        // Looks like partial name matching is already handled by getPlayer()...
        Player player = plugin.getServer().getPlayer(inviteeName);
        if (player == null) {

            ChessAI.AI_Def ai = ChessAI.getAI(inviteeName);
            if (ai != null) {
                if (!ChessAI.isFree(ai)) {
                    throw new ChessException("That AI is currently busy playing a game right now");
                }
                addPlayer(ChessAI.getAIPrefix() + ai.name);
                return;
            }
            throw new ChessException("Player " + inviteeName + " is not online.");
        } else {
            inviteeName = player.getName();

            //resend instead
            //if (invited.equals(inviteeName)) return;

            alert(inviteeName, "You have been invited to this game by &6" + inviterName + "&-.");
            alert(inviteeName, "Type &f/chess join&- to join the game.");
            if (!invited.isEmpty()) {
                alert(invited, "Your invitation has been withdrawn.");
            }
            invited = inviteeName;
            alert(inviterName, "An invitation has been sent to &6" + invited + "&-.");
        }
    }

    public void inviteOpen(String inviterName) throws ChessException {
        inviteSanityCheck(inviterName);
        Bukkit.getServer().broadcastMessage(
                ChessUtils.parseColourSpec("&e:: &6" + inviterName
                + "&e has created an open invitation to a chess game."));
        Bukkit.getServer().broadcastMessage(
                ChessUtils.parseColourSpec("&e:: " + "Type &f/chess join " + getName()
                + "&e to join."));
        invited = "*";
    }

    private void inviteSanityCheck(String inviterName) throws ChessException {
        ensurePlayerInGame(inviterName);
        ensureGameState(GameState.SETTING_UP);

        if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
            // if one is an AI, allow the AI to leave
            if (aiPlayer != null) {
                if (isAIPlayer(playerWhite)) {
                    playerWhite = "";
                } else {
                    playerBlack = "";
                }
                aiPlayer.removeAI();
            } else {
                throw new ChessException("This game already has two players!");
            }
        }
    }

    public void clearInvitation() {
        invited = "";
    }

    public void start(String playerName) throws ChessException {
        ensurePlayerInGame(playerName);
        ensureGameState(GameState.SETTING_UP);

        if (playerWhite.isEmpty() || playerBlack.isEmpty()) {
            addAI(null);
            alert(playerName, aiPlayer.getName() + " has joined your game.");
        }
        if (!canAffordToPlay(playerWhite)) {
            throw new ChessException("White can't afford to play! (need " + Economy.format(stake) + ")");
        }
        if (!canAffordToPlay(playerBlack)) {
            throw new ChessException("Black can't afford to play! (need " + Economy.format(stake) + ")");
        }
        alert(playerWhite, "Game started!  You are playing &fWhite&-.");
        alert(playerBlack, "Game started!  You are playing &fBlack&-.");
        if (Economy.active() && stake > 0.0f) {
            if (!isAIPlayer(playerWhite)) {
                Economy.subtractMoney(playerWhite, stake);
            }
            if (!isAIPlayer(playerBlack)) {
                Economy.subtractMoney(playerBlack, stake);
            }
            double s2 = playerWhite.equals(playerBlack) ? stake * 2 : stake;
            alert("You have paid a stake of " + Economy.format(s2) + ".");
        }
        setState(GameState.RUNNING);
    }

    public void resign(String playerName) throws ChessException {
        if (state != GameState.RUNNING) {
            throw new ChessException("The game has not yet started.");
        }

        ensurePlayerInGame(playerName);
        //ensurePlayerToMove(playerName);

        setState(GameState.FINISHED);
        String winner;
        String loser = playerName;
        if (loser.equalsIgnoreCase(playerWhite)) {
            winner = playerBlack;
            cpGame.setTag(PGN.TAG_RESULT, "0-1");
            result = Chess.RES_BLACK_WINS;
        } else {
            winner = playerWhite;
            cpGame.setTag(PGN.TAG_RESULT, "1-0");
            result = Chess.RES_WHITE_WINS;
        }
        announceResult(winner, loser, GameResult.Resigned);
    }

    public void winByDefault(String playerName) throws ChessException {
        ensurePlayerInGame(playerName);

        setState(GameState.FINISHED);
        String winner = playerName;
        String loser;
        if (winner.equalsIgnoreCase(playerWhite)) {
            loser = playerBlack;
            cpGame.setTag(PGN.TAG_RESULT, "1-0");
            result = Chess.RES_WHITE_WINS;
        } else {
            loser = playerWhite;
            cpGame.setTag(PGN.TAG_RESULT, "0-1");
            result = Chess.RES_BLACK_WINS;
        }
        announceResult(winner, loser, GameResult.Forfeited);
    }

    public int getPromotionPiece(int colour) {
        return promotionPiece[colour];
    }

    public void setPromotionPiece(String playerName, int piece) throws ChessException {
        ensurePlayerInGame(playerName);

        if (piece != Chess.QUEEN && piece != Chess.ROOK && piece != Chess.BISHOP && piece != Chess.KNIGHT) {
            throw new ChessException("Invalid promotion piece: " + Chess.pieceToChar(piece));
        }
        if (playerName.equals(playerWhite)) {
            promotionPiece[Chess.WHITE] = piece;
        }
        if (playerName.equals(playerBlack)) {
            promotionPiece[Chess.BLACK] = piece;
        }
    }

    public void drawn() throws ChessException {
        ensureGameState(GameState.RUNNING);

        setState(GameState.FINISHED);
        result = Chess.RES_DRAW;
        cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
        announceResult(playerWhite, playerBlack, GameResult.DrawAgreed);
    }

    /**
     * Do a move for playerName to toSquare <br>
     * fromSquare should already be set, <br>
     * either from command-line, or from clicking a piece
     * @param playerName
     * @param toSquare
     * @throws IllegalMoveException
     * @throws ChessException
     */
    public void doMove(String playerName, int toSquare) throws IllegalMoveException, ChessException {
        doMove(playerName, toSquare, fromSquare);
    }

    public void doMove(String playerName, int toSquare, int fromSquare) throws IllegalMoveException, ChessException {
        ensureGameState(GameState.RUNNING);
        ensurePlayerToMove(playerName);
        if (fromSquare == Chess.NO_SQUARE) {
            return;
        }

        Boolean isCapturing = getPosition().getPiece(toSquare) != Chess.NO_PIECE;
        int prevToMove = getPosition().getToPlay();
        short move = Move.getRegularMove(fromSquare, toSquare, isCapturing);
        try {
            short realMove = checkMove(move);
            if (plugin.config.config.getBoolean("highlight_last_move", true)) {
                view.highlightSquares(fromSquare, toSquare);
            }
            getPosition().doMove(realMove);
            Move lastMove = getPosition().getLastMove();
            history.add(realMove);
            clockTick();
            if (getPosition().isMate()) {
                announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.Checkmate);
                cpGame.setTag(PGN.TAG_RESULT, getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0");
                result = getPosition().getToPlay() == Chess.WHITE ? Chess.RES_BLACK_WINS : Chess.RES_WHITE_WINS;
                setState(GameState.FINISHED);
            } else if (getPosition().isStaleMate()) {
                announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.Stalemate);
                result = Chess.RES_DRAW;
                cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
                setState(GameState.FINISHED);
            } else if (getPosition().getHalfMoveClock() >= 50) {
                announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.FiftyMoveRule);
                result = Chess.RES_DRAW;
                cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
                setState(GameState.FINISHED);
            } else {
                // the game continues...
                String nextPlayer = getPlayerToMove();
                if (isAIPlayer(nextPlayer)) {
                    aiPlayer.userMove(fromSquare, toSquare);
                } else {
                    String checkNotify = getPosition().isCheck() ? " &5+++CHECK+++" : "";
                    alert(nextPlayer, "&f" + getColour(prevToMove) + "&- played [" + lastMove.getLAN() + "]."
                            + checkNotify);
                    alert(nextPlayer, "It is your move &f(" + getColour(getPosition().getToPlay()) + ")&-.");
                }
            }
            this.fromSquare = Chess.NO_SQUARE;
        } catch (IllegalMoveException e) {
            throw e;
        }
    }

    public boolean isAIPlayer(String name) {
        // simple name match.. not checking if in this game
        return name.startsWith(ChessAI.getAIPrefix());
    }

    public String getPGNResult() {
        switch (result) {
            case Chess.RES_NOT_FINISHED:
                return "*";
            case Chess.RES_WHITE_WINS:
                return "1-0";
            case Chess.RES_BLACK_WINS:
                return "0-1";
            case Chess.RES_DRAW:
                return "1/2-1/2";
            default:
                return "*";
        }
    }

    /**
     * Announce the result of the game to the server
     * @param p1 the winner
     * @param p2 the loser (unless it's a draw)
     * @param rt result to announce
     */
    public void announceResult(String p1, String p2, GameResult rt) {
        String msg = "";
        switch (rt) {
            case Checkmate:
                msg = "&6" + p1 + "&e checkmated &6" + p2 + "&e in a game of Chess!";
                break;
            case Stalemate:
                msg = "&6" + p1 + "&e drew with &6" + p2 + "&e (stalemate) in a game of Chess!";
                break;
            case FiftyMoveRule:
                msg = "&6" + p1 + "&e drew with &6" + p2 + "&e (50-move rule) in a game of Chess!";
                break;
            case DrawAgreed:
                msg = "&6" + p1 + "&e drew with &6" + p2 + "&e (draw agreed) in a game of Chess!";
                break;
            case Resigned:
                msg = "&6" + p1 + "&e beat &6" + p2 + "&e (resigned) in a game of Chess!";
                break;
            case Forfeited:
                msg = "&6" + p1 + "&e beat &6" + p2 + "&e (forfeited) in a game of Chess!";
                break;
        }
        if (plugin.getConfiguration().getBoolean("broadcast_results", true)) {
            if (!msg.isEmpty()) {
                Bukkit.getServer().broadcastMessage(ChessUtils.parseColourSpec(ChatColor.YELLOW + ":: " + msg));
            }
        } else {
            if (!msg.isEmpty()) {
                alert(msg);
            }
        }
        handlePayout(rt, p1, p2);
        setupAutoDeletion();
    }

    private void handlePayout(GameResult rt, String p1, String p2) {
        if (stake <= 0.0) {
            return;
        }
        if (getState() == GameState.SETTING_UP) {
            return;
        }

        if (rt == GameResult.Checkmate || rt == GameResult.Resigned) {
            // somebody won
            if (!isAIPlayer(p1)) {
            	double winnings;
            	if (isAIPlayer(p2)) {
            		AI_Def ai = ChessAI.getAI(p2);
            		if (ai != null) {
            			winnings = stake * (1.0 + ai.getPayoutMultiplier());
            		} else {
            			winnings = stake * 2.0;
            			ChessCraft.log(Level.WARNING, "couldn't retrieve AI definition for " + p2);
            		}
            	} else {
            		winnings = stake * 2.0;
            	}
                Economy.addMoney(p1, winnings);
                alert(p1, "You have won " + Economy.format(winnings) + "!");
            }
            alert(p2, "You lost your stake of " + Economy.format(stake) + "!");
        } else {
            // a draw
            if (!isAIPlayer(p1)) {
                Economy.addMoney(p1, stake);
            }
            if (!isAIPlayer(p2)) {
                Economy.addMoney(p2, stake);
            }
            alert("You get your stake of " + Economy.format(stake) + " back.");
        }

        stake = 0.0;
    }

    private void setupAutoDeletion() {
        int autoDel = plugin.getConfiguration().getInt("auto_delete.finished", 0);
        if (autoDel > 0) {
            delTask = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                public void run() {
                    alert("Game auto-deleted!");
                    delete();
                }
            }, autoDel * 20L);

            if (delTask != -1) {
                alert("Game will auto-delete in " + autoDel + " seconds.");
            }
            alert("Type &f/chess archive&- to archive to PGN file.");
        }
    }

    public void cancelAutoDelete() {
        if (delTask == -1) {
            return;
        }
        Bukkit.getServer().getScheduler().cancelTask(delTask);
        delTask = -1;
    }

    public void delete() {
        cancelAutoDelete();
        getView().highlightSquares(-1, -1);
        getView().setGame(null);
        getView().paintAll();
        handlePayout(GameResult.Abandoned, playerWhite, playerBlack);
        try {
            Game.removeGame(getName());
        } catch (ChessException e) {
            ChessCraft.log(Level.WARNING, e.getMessage());
        }
    }

    /**
     * Check if the move is really allowed
     * Also account for special cases: castling, en passant, pawn promotion
     * @param move move to check
     * @return move, if allowed
     * @throws IllegalMoveException if not allowed
     */
    private short checkMove(short move) throws IllegalMoveException {
        int sqiFrom = Move.getFromSqi(move);
        int sqiTo = Move.getToSqi(move);
        int toPlay = getPosition().getToPlay();

        if (getPosition().getPiece(sqiFrom) == Chess.KING) {
            // Castling?
            if (sqiFrom == Chess.E1 && sqiTo == Chess.G1 || sqiFrom == Chess.E8 && sqiTo == Chess.G8) {
                move = Move.getShortCastle(toPlay);
            } else if (sqiFrom == Chess.E1 && sqiTo == Chess.C1 || sqiFrom == Chess.E8 && sqiTo == Chess.C8) {
                move = Move.getLongCastle(toPlay);
            }
        } else if (getPosition().getPiece(sqiFrom) == Chess.PAWN
                && (Chess.sqiToRow(sqiTo) == 7 || Chess.sqiToRow(sqiTo) == 0)) {
            // Promotion?
            boolean capturing = getPosition().getPiece(sqiTo) != Chess.NO_PIECE;
            move = Move.getPawnMove(sqiFrom, sqiTo, capturing, promotionPiece[toPlay]);
        } else if (getPosition().getPiece(sqiFrom) == Chess.PAWN && getPosition().getPiece(sqiTo) == Chess.NO_PIECE) {
            // En passant?
            int toCol = Chess.sqiToCol(sqiTo);
            int fromCol = Chess.sqiToCol(sqiFrom);
            if ((toCol == fromCol - 1 || toCol == fromCol + 1)
                    && (Chess.sqiToRow(sqiFrom) == 4 && Chess.sqiToRow(sqiTo) == 5 || Chess.sqiToRow(sqiFrom) == 3
                    && Chess.sqiToRow(sqiTo) == 2)) {
                move = Move.getEPMove(sqiFrom, sqiTo);
            }
        }

        for (short aMove : getPosition().getAllMoves()) {
            if (move == aMove) {
                return move;
            }
        }
        throw new IllegalMoveException(move);
    }

    public int playingAs(String name) {
        if (name.equalsIgnoreCase(playerWhite)) {
            return Chess.WHITE;
        } else if (name.equalsIgnoreCase(playerBlack)) {
            return Chess.BLACK;
        } else {
            return Chess.NOBODY;
        }
    }

    /**
     * get PGN result
     * @return game result in PGN notation
     */
    public String getResult() {
        if (getState() != GameState.FINISHED) {
            return "*";
        }

        if (getPosition().isMate()) {
            return getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0";
        } else {
            return "1/2-1/2";
        }
    }

    public static String getColour(int c) {
        switch (c) {
            case Chess.WHITE:
                return "White";
            case Chess.BLACK:
                return "Black";
            default:
                return "???";
        }
    }

    public void alert(String playerName, String message) {
        if (playerName.isEmpty() || isAIPlayer(playerName)) {
            return;
        }
        Player p = Bukkit.getServer().getPlayer(playerName);
        if (p != null) {
            ChessUtils.alertMessage(p, "&6:: &-Game &6" + getName() + "&-: " + message);
        }
    }

    public void alert(String message) {
        alert(playerWhite, message);
        if (!playerWhite.equalsIgnoreCase(playerBlack)) {
            alert(playerBlack, message);
        }
    }

    public String getPlayerToMove() {
        return getPosition().getToPlay() == Chess.WHITE ? playerWhite : playerBlack;
    }

    public String getPlayerNotToMove() {
        return getPosition().getToPlay() == Chess.BLACK ? playerWhite : playerBlack;
    }

    public Boolean isPlayerInGame(String playerName) {
        return (playerName.equalsIgnoreCase(playerWhite) || playerName.equalsIgnoreCase(playerBlack));
    }

    public Boolean isPlayerToMove(String playerName) {
        return playerName.equalsIgnoreCase(getPlayerToMove());
    }

    public File writePGN(boolean force) throws ChessException {
        plugin.config.createDir(archiveDir);

        File f = makePGNName();
        if (f.exists() && !force) {
            throw new ChessException("Archive file " + f.getName() + " already exists - won't overwrite.");
        }

        try {
            PrintWriter pw = new PrintWriter(f);
            PGNWriter w = new PGNWriter(pw);
            w.write(cpGame.getModel());
            pw.close();
            return f;
        } catch (FileNotFoundException e) {
            throw new ChessException("can't write PGN archive " + f.getName() + ": " + e.getMessage());
        }
    }

    private File makePGNName() {
        String baseName = getName() + "_" + dateToPGNDate(new Date());

        int n = 1;
        File f;
        do {
            f = new File(plugin.getDataFolder(), archiveDir + File.separator + baseName + "_" + n + ".pgn");
            ++n;
        } while (f.exists());

        return f;
    }

    /**
     * get PGN format of the date
     * (the version in chesspresso.pgn.PGN gets the month wrong :( )
     * @param date date to convert
     * @return PGN format of the date
     */
    private static String dateToPGNDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "." + getRights("00" + (cal.get(Calendar.MONTH) + 1), 2) + "."
                + getRights("00" + cal.get(Calendar.DAY_OF_MONTH), 2);
    }

    private static String getRights(String s, int num) {
        return s.substring(s.length() - num);
    }

    public void setFen(String fen) {
        getPosition().set(new Position(fen));
        // manually overriding the position invalidates the move history
        getHistory().clear();
    }

    public static String secondsToHMS(int n) {
        n /= 1000;

        int secs = n % 60;
        int hrs = n / 3600;
        int mins = (n - (hrs * 3600)) / 60;

        return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs);
    }

    public int getNextPromotionPiece(int colour) {
        switch (promotionPiece[colour]) {
            case Chess.QUEEN:
                return Chess.KNIGHT;
            case Chess.KNIGHT:
                return Chess.BISHOP;
            case Chess.BISHOP:
                return Chess.ROOK;
            case Chess.ROOK:
                return Chess.QUEEN;
            default:
                return Chess.QUEEN;
        }
    }

    public void checkForAutoDelete() {
        if (getState() == GameState.SETTING_UP) {
            long now = System.currentTimeMillis();
            long elapsed = (now - started.getTime()) / 1000;
            int timeout = plugin.getConfiguration().getInt("auto_delete.not_started", 180);
            if (timeout <= 0) {
                return;
            }
            if (elapsed > timeout && (playerWhite.isEmpty() || playerBlack.isEmpty())) {
                alert("Game auto-deleted (not started within " + timeout + " seconds)");
                ChessCraft.log(Level.INFO, "Auto-deleted game " + getName() + " (not started within " + timeout + " seconds)");
                delete();
            }
        }
    }

    public void ensurePlayerInGame(String playerName) throws ChessException {
        if (!playerName.equals(playerWhite) && !playerName.equals(playerBlack)) {
            throw new ChessException("You are not in this game!");
        }
    }

    public void ensurePlayerToMove(String playerName) throws ChessException {
        if (!playerName.equals(getPlayerToMove())) {
            throw new ChessException("It is not your turn.");
        }
    }

    public void ensureGameState(GameState state) throws ChessException {
        if (getState() != state) {
            throw new ChessException("Game should be in state " + state);
        }
    }

    private boolean canAffordToPlay(String playerName) {
        if (isAIPlayer(playerName)) {
            return true;
        }
        return stake <= 0.0 || !Economy.active()
                || Economy.canAfford(playerName, stake);
    }

    /*--------------------------------------------------------------------------------*/
    public static void addGame(String gameName, Game game) {
        if (game != null && !chessGames.containsKey(gameName)) {
            chessGames.put(gameName, game);
        }
    }

    public static void removeGame(String gameName) throws ChessException {
        Game game = getGame(gameName);

        List<String> toRemove = new ArrayList<String>();
        for (String p : currentGame.keySet()) {
            if (currentGame.get(p) == game) {
                toRemove.add(p);
            }
        }
        for (String p : toRemove) {
            currentGame.remove(p);
        }
        chessGames.remove(gameName);
    }

    public static boolean checkGame(String name) {
        return chessGames.containsKey(name);
    }

    public static List<Game> listGames(boolean isSorted) {
        if (isSorted) {
            SortedSet<String> sorted = new TreeSet<String>(chessGames.keySet());
            List<Game> res = new ArrayList<Game>();
            for (String name : sorted) {
                res.add(chessGames.get(name));
            }
            return res;
        } else {
            return new ArrayList<Game>(chessGames.values());
        }
    }

    public static List<Game> listGames() {
        return listGames(false);
    }

    public static Game getGame(String name) throws ChessException {
        if (!chessGames.containsKey(name)) {
            if (chessGames.size() > 0) {
                // try "fuzzy" search
                String keys[] = chessGames.keySet().toArray(new String[0]);
                String matches[] = ChessUtils.fuzzyMatch(name, keys, 3);

                if (matches.length == 1) {
                    return chessGames.get(matches[0]);
                } else {
                    // partial-name search
                    int k = -1, c = 0;
                    name = name.toLowerCase();
                    for (int i = 0; i < keys.length; ++i) {
                        if (keys[i].toLowerCase().startsWith(name)) {
                            k = i;
                            ++c;
                        }
                    }
                    if (k >= 0 && c == 1) {
                        return chessGames.get(keys[k]);
                    }
                }
                // todo: if multiple matches, check if only one is waiting for more players
                //          (and return that one)
            }
            throw new ChessException("No such game '" + name + "'");
        }
        return chessGames.get(name);
    }

    public static void setCurrentGame(String playerName, String gameName) throws ChessException {
        Game game = getGame(gameName);
        setCurrentGame(playerName, game);
    }

    public static void setCurrentGame(String playerName, Game game) {
        currentGame.put(playerName, game);
    }

    public static Game getCurrentGame(Player player) throws ChessException {
        return getCurrentGame(player, false);
    }

    public static Game getCurrentGame(Player player, boolean verify) throws ChessException {
        if (player == null) {
            return null;
        }
        Game game = currentGame.get(player.getName());
        if (verify && game == null) {
            throw new ChessException("No active game - set one with '/chess game <name>'");
        }
        return game;
    }

    public static Map<String, String> getCurrentGames() {
        Map<String, String> res = new HashMap<String, String>();
        for (String s : currentGame.keySet()) {
            Game game = currentGame.get(s);
            if (game != null) {
                res.put(s, game.getName());
            }
        }
        return res;
    }

    public static String makeGameName(Player player) {
        String base = player.getName();
        String res;
        int n = 1;
        do {
            res = base + "-" + n++;
        } while (Game.checkGame(res));

        return res;
    }

    public boolean playerCanDelete(Player pl) {
        if (pl == null) {
            return false;
        }
        String plN = pl.getName();
        if (state == GameState.SETTING_UP) {
            if (!playerWhite.isEmpty() && playerBlack.isEmpty()) {
                return playerWhite.equalsIgnoreCase(plN);
            } else if (playerWhite.isEmpty() && !playerBlack.isEmpty()) {
                return playerBlack.equalsIgnoreCase(plN);
            } else if (playerWhite.equalsIgnoreCase(plN)) {
                Player other = pl.getServer().getPlayer(playerBlack);
                return other == null || !other.isOnline();
            } else if (playerBlack.equalsIgnoreCase(plN)) {
                Player other = pl.getServer().getPlayer(playerWhite);
                return other == null || !other.isOnline();
            }
        }
        return false;
    }

    public boolean isAIGame() {
        return isAIPlayer(playerWhite) || isAIPlayer(playerBlack);
    }
}

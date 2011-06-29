package me.desht.chesscraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.pgn.PGN;
import chesspresso.pgn.PGNWriter;
import chesspresso.position.Position;

public class Game {
	enum ResultType {
		Checkmate, Stalemate, DrawAgreed, Resigned, Abandoned
	}
	private static final String archiveDir = "pgn";
	private ChessCraft plugin;
	private String name;
//	private Position position;
	private chesspresso.game.Game cpGame;
	private BoardView view;
	private String playerWhite, playerBlack;
	private int promo[] = { Chess.QUEEN, Chess.QUEEN };
	private String invited;
	private GameState state;
	private int fromSquare;
	private Date started;
	private List<Short> history;
	private int delTask;
	private int result;
	
	Game(ChessCraft plugin, String name, BoardView view, Player player) throws ChessException {
		this.plugin = plugin;
		this.view = view;
		this.name = name;
		if (view.getGame() != null)
			throw new ChessException("That board already has a game on it.");
		view.setGame(this);
		playerWhite = player == null ? "" : player.getName();
		playerBlack = "";
		state = GameState.SETTING_UP;
		fromSquare = Chess.NO_SQUARE;
		invited = "";
		history = new ArrayList<Short>();
		started = new Date();
		result = Chess.RES_NOT_FINISHED;

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
	
	Map<String,Object> freeze() {
		Map<String,Object> map = new HashMap<String,Object>();
		
		map.put("name", name);
		map.put("boardview", view.getName());
		map.put("playerWhite", playerWhite);
		map.put("playerBlack", playerBlack);
		map.put("state", state.toString());
		map.put("invited", invited);
		map.put("moves", history);
		map.put("started", started.getTime());
		map.put("result", result);
		
		return map;
	}
	
	@SuppressWarnings("unchecked")
	void thaw(Map<String,Object> map) {
		playerWhite = (String) map.get("playerWhite");
		playerBlack = (String) map.get("playerBlack");
		state = GameState.valueOf((String) map.get("state"));
		invited = (String) map.get("invited");
		List<Integer> hTmp = (List<Integer>) map.get("moves");
		history.clear();
		for (int m : hTmp) { history.add((short) m); } 
		started.setTime((Long) map.get("started"));
		result = (Integer) map.get("result");

		setupChesspressoGame();

		// Replay the move history to restore the saved board position.
		// We do this instead of just saving the position so that the chesspresso Game model
		// includes a history of the moves, suitable for creating a PGN file.
		try {
			for (short move : history) {
				getPosition().doMove(move);
			}
		} catch (IllegalMoveException e) {
			// should only get here if the save file was corrupted - the history is a list 
			// of moves which have already been validated before the game was saved
			plugin.log(Level.WARNING, "can't restore move history for game " + getName() + " - move history corrupted?");
		}

		getPosition().addPositionListener(view);
	}
	
	String getName() {
		return name;
	}
	
	Position getPosition() {
		return cpGame.getPosition();
	}

	BoardView getView() {
		return view;
	}

	String getPlayerWhite() {
		return playerWhite;
	}

	String getPlayerBlack() {
		return playerBlack;
	}

	String getInvited() {
		return invited;
	}

	GameState getState() {
		return state;
	}

	int getFromSquare() {
		return fromSquare;
	}

	Date getStarted() {
		return started;
	}

	void setFromSquare(int fromSquare) {
		this.fromSquare = fromSquare;
	}
	
	String getOtherPlayer(String name) {
		return name.equals(playerWhite) ? playerBlack : playerWhite;
	}

	void swapColours() {
		String tmp = playerWhite;
		playerWhite = playerBlack;
		playerBlack = tmp;
		alert(playerWhite, "Side swap!  You are now playing White.");
		alert(playerBlack, "Side swap!  You are now playing Black.");
	}
	
	void addPlayer(Player player) throws ChessException {
		if (state != GameState.SETTING_UP) {
			throw new ChessException("Can only add players during game setup phase.");
		}
		if (!invited.equals("*") && !invited.equalsIgnoreCase(player.getName())) {
			throw new ChessException("Player " + player.getName() + " doesn't have an invitation.");
		}
		String otherPlayer = null;
		if (playerBlack.isEmpty()) {
			playerBlack = player.getName();
			otherPlayer = playerWhite;
		} else if (playerWhite.isEmpty()) {
			playerWhite = player.getName();
			otherPlayer = playerBlack;
		} else {
			throw new ChessException("This game already has two players.");
		}
		
		alert(otherPlayer, player.getName() + " has joined your game.");
		clearInvitation();
	}
	
	void invitePlayer(Player inviter, Player invitee) throws ChessException {
		if (!isPlayerInGame(inviter))
			throw new ChessException("Can't invite a player to a game you're not in!");
		if (invited.equals(invitee.getName()))
			return;
		alert(invitee, "You have been invited to the chess game '" + getName() + "' by " + inviter.getName() + ".");
		alert(invitee, "Type '/chess join' to join the game.");
		if (!invited.isEmpty()) {
			Player oldInvited = Bukkit.getServer().getPlayer(invited);
			if (oldInvited != null)
				alert(oldInvited, "Your invitation to chess game '" + getName() + "' has been withdrawn.");
		}
		invited = invitee.getName();
	}
	
	void inviteOpen(Player inviter) throws ChessException {
		if (!isPlayerInGame(inviter))
			throw new ChessException("Can't invite a player to a game you're not in!");
		Bukkit.getServer().broadcastMessage(inviter.getName() + " has created an open invitation to a chess game.");
		Bukkit.getServer().broadcastMessage("Type '/chess join " + getName() + "' to join.");
		invited = "*";
	}
		
	void clearInvitation() {
		invited = "";
	}
	
	void start(Player p) throws ChessException {
		if (state != GameState.SETTING_UP) 
			throw new ChessException("This game has already been started!");
		if (!isPlayerInGame(p))
			throw new ChessException("Can't start a game you're not in!");
		if (playerWhite.isEmpty())
			throw new ChessException("There is no white player yet.");
		if (playerBlack.isEmpty())
			throw new ChessException("There is no black player yet.");
		alert(playerWhite, "game started!  You are playing White.");
		alert(playerBlack, "game started!  You are playing Black.");
		state = GameState.RUNNING;
	}
	
	void resign(Player p) throws ChessException {
		if (!isPlayerInGame(p))
			throw new ChessException("Can't resign a game you're not in!");
		state = GameState.FINISHED;
		String winner;
		String loser = p.getName();
		if (loser.equalsIgnoreCase(playerWhite)) {
			winner = playerBlack;
			cpGame.setTag(PGN.TAG_RESULT, "0-1");
			result = Chess.RES_WHITE_WINS;
		} else {
			winner = playerWhite;
			cpGame.setTag(PGN.TAG_RESULT, "1-0");
			result = Chess.RES_BLACK_WINS;
		}
		announceResult(winner, loser, ResultType.Resigned);
	}
	
	void drawn() {
		state = GameState.FINISHED;
		result = Chess.RES_DRAW;
		cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
		announceResult(playerWhite, playerBlack, ResultType.DrawAgreed);
	}
	
	// Do a move for Player p to toSquare.  fromSquare is already set, either from 
	// command-line, or from clicking a piece
	void doMove(Player p, int toSquare) throws IllegalMoveException, ChessException {
		if (fromSquare == Chess.NO_SQUARE) {
			return;
		}
		if (state != GameState.RUNNING) {
			throw new ChessException("Chess game '" + getName() + "': Game is not running!");
		}
		if (!p.getName().equals(getPlayerToMove())) {
			throw new ChessException("Chess game '" + getName() + "': It is not your move!");
		}
		
		Boolean capturing = getPosition().getPiece(toSquare) != Chess.NO_PIECE;
		int prevToMove = getPosition().getToPlay();
		short move = Move.getRegularMove(fromSquare, toSquare, capturing);
		try {
			short realMove = checkMove(move);
			getPosition().doMove(realMove);
			Move lastMove = getPosition().getLastMove();
			fromSquare = Chess.NO_SQUARE;
			history.add(realMove);
			if (getPosition().isMate()) {
				announceResult(getPlayerNotToMove(), getPlayerToMove(), ResultType.Checkmate);
				cpGame.setTag(PGN.TAG_RESULT, getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0");
				result = getPosition().getToPlay() == Chess.WHITE ? Chess.RES_BLACK_WINS : Chess.RES_WHITE_WINS;
				state = GameState.FINISHED;
			} else if (getPosition().isStaleMate()) {
				announceResult(getPlayerNotToMove(), getPlayerToMove(), ResultType.Stalemate);
				result = Chess.RES_DRAW;
				cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
				state = GameState.FINISHED;
			} else {
				alert(getPlayerToMove(), getColour(prevToMove) + " played [" + lastMove.getLAN() + "].");
				alert(getPlayerToMove(), "It is your move (" + getColour(getPosition().getToPlay()) + ").");
			}
		} catch (IllegalMoveException e) {
			throw e;
		}
	}
	
	String getPGNResult() {
		switch(result) {
		case Chess.RES_NOT_FINISHED: return "*";
		case Chess.RES_WHITE_WINS: return "1-0";
		case Chess.RES_BLACK_WINS: return "0-1";
		case Chess.RES_DRAW: return "1/2-1/2";
		default: return "*";
		}
	}
	
	// Announce the result of the game to the server
	// p1 is the winner, p2 is the loser (unless it's a draw)
	void announceResult(String p1, String p2, ResultType rt) {
		if (plugin.getConfiguration().getBoolean("broadcast_results", true)) {
			String msg = "";
			switch(rt) {
			case Checkmate:
				msg = p1 + " checkmated " + p2 + " in a game of Chess!"; break;
			case Stalemate:
				msg = p1 + " drew with " + p2 + " (stalemate) in a game of Chess!"; break;
			case DrawAgreed:
				msg = p1 + " drew with " + p2 + " (draw agreed) in a game of Chess!"; break;
			case Resigned:
				msg = p1 + " beat " + p2 + " (resigned) in a game of Chess!"; break;
			}
			if (!msg.isEmpty())
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + ":: " + msg);
		} else {
			switch(rt) {
			case Checkmate:
				alert(p1, "You checkmated " + p2 + "!");
				alert(p2, "You were checkmated by " + p1 + "!");
				break;
			case Stalemate:
				alert("Game is drawn - stalemate!");
				break;
			case Resigned:
				alert(p1, p2 + " has resigned - you win!");
				alert(p2, "You have resigned. " + p1 + " wins!");
				break;
			case DrawAgreed:
				alert("Game is drawn - draw agreed!");
				break;
			}
		}
		int autoDel = plugin.getConfiguration().getInt("auto_delete_finished", 0);
		if (autoDel > 0) {
			delTask = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					alert("Game auto-deleted!");
					getView().setGame(null);
					getView().paintAll();
					try {
						plugin.removeGame(getName());
					} catch (ChessException e) {
						plugin.log(Level.WARNING, e.getMessage());
					}
				}
			}, autoDel * 20);
			
			if (delTask != -1)
				alert("This game will auto-delete in " + autoDel + " seconds.");
			alert("Type '/chess archive' within " + autoDel + " seconds to save this game to PGN and delete it");
		}
	}
	
	// Check if the move is really allowed
	// Also account for special cases: castling, en passant, pawn promotion
	private short checkMove(short move) throws IllegalMoveException {
		int from = Move.getFromSqi(move);
		int to = Move.getToSqi(move);
		
		if (getPosition().getPiece(from) == Chess.KING) {
			// Castling?
			if (from == Chess.E1 && to == Chess.G1 || from == Chess.E8 && to == Chess.G8)
				move = Move.getShortCastle(getPosition().getToPlay());
			else if (from == Chess.E1 && to == Chess.B1 || from == Chess.E8 && to == Chess.B8)
				move = Move.getLongCastle(getPosition().getToPlay());
		} else if (getPosition().getPiece(from) == Chess.PAWN && Chess.sqiToRow(to) == 7) {
			// Promotion?
			boolean capturing = getPosition().getPiece(to) != Chess.NO_PIECE;
			// TODO: allow player to specify the promotion piece
			move = Move.getPawnMove(from, to, capturing, promo[getPosition().getToPlay()]);
		} else if (getPosition().getPiece(from) == Chess.PAWN && getPosition().getPiece(to) == Chess.NO_PIECE) {
			// En passant?
			int toCol = Chess.sqiToCol(to);
			int fromCol = Chess.sqiToCol(from);
			if (Chess.sqiToRow(from) == 4 && Chess.sqiToRow(to) == 5 && (toCol == fromCol - 1 || toCol == fromCol + 1)) {
				move = Move.getEPMove(from, to);
			}
		}
			
		for (short aMove : getPosition().getAllMoves()) {
			if (move == aMove) return move;
		}
		throw new IllegalMoveException(move);
	}

	int playingAs(String name) {
		if (name.equalsIgnoreCase(playerWhite)) {
			return Chess.WHITE;
		} else if (name.equalsIgnoreCase(playerBlack)) {
			return Chess.BLACK;
		} else {
			return Chess.NOBODY;
		}
	}

	// return game result in PGN notation
	String getResult() {
		if (getState() != GameState.FINISHED)
			return "*";
		
		if (getPosition().isMate()) {
			return getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0";
		} else {
			return "1/2-1/2";
		}
	}
	
	static String getColour(int c) {
		switch(c) {
		case Chess.WHITE: return "White";
		case Chess.BLACK: return "Black";
		default: return "???";
		}
	}
	
	void alert(Player player, String message) {
		player.sendMessage(ChatColor.YELLOW + ":: Chess game '" + getName() + "': " + message);
	}
	void alert(String playerName, String message) {
		Player p = Bukkit.getServer().getPlayer(playerName);
		if (p != null)
			alert(p, message);
	}
	void alert(String message) {
		alert(playerWhite, message);
		alert(playerBlack, message);
	}
	
	String getPlayerToMove() {
		return getPosition().getToPlay() == Chess.WHITE ? playerWhite : playerBlack;
	}
	String getPlayerNotToMove() {
		return getPosition().getToPlay() == Chess.BLACK ? playerWhite : playerBlack;
	}

	Boolean isPlayerInGame(Player p) {
		return (p.getName().equalsIgnoreCase(playerWhite) || p.getName().equalsIgnoreCase(playerBlack));
	}
	
	Boolean isPlayerToMove(Player p) {
		return p.getName().equalsIgnoreCase(getPlayerToMove());
	}

	File writePGN(boolean force) throws ChessException {
		new File(plugin.getDataFolder(), archiveDir).mkdir();
		
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
			n++;
		} while (f.exists());

		return f;
	}
	
	// the version in chesspresso.pgn.PGN gets the month wrong :(
	private static String getRights(String s, int num)
    {
        return s.substring(s.length() - num);
    }
	private static String dateToPGNDate(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "."
             + getRights("00" + (cal.get(Calendar.MONTH) + 1), 2) + "."
             + getRights("00" + cal.get(Calendar.DAY_OF_MONTH), 2);
    }
}

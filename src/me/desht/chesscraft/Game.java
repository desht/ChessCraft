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
		Checkmate, Stalemate, DrawAgreed, Resigned, Abandoned, FiftyMoveRule
	}
	private static final String archiveDir = "pgn";
	private ChessCraft plugin;
	private String name;
	private chesspresso.game.Game cpGame;
	private BoardView view;
	private String playerWhite, playerBlack;
	private int promotionPiece[] = { Chess.QUEEN, Chess.QUEEN };
	private String invited;
	private GameState state;
	private int fromSquare;
	private Date started;
	private Date lastCheck;
	private int timeWhite, timeBlack;
	private List<Short> history;
	private int delTask;
	private int result;
	
	Game(ChessCraft plugin, String name, BoardView view, String playerName) throws ChessException {
		this.plugin = plugin;
		this.view = view;
		this.name = name;
		if (view.getGame() != null)
			throw new ChessException("That board already has a game on it.");
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
		map.put("promotionWhite", promotionPiece[Chess.WHITE]);
		map.put("promotionBlack", promotionPiece[Chess.BLACK]);
		map.put("timeWhite", timeWhite);
		map.put("timeBlack", timeBlack);
		
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
		promotionPiece[Chess.WHITE] = (Integer) map.get("promotionWhite");
		promotionPiece[Chess.BLACK] = (Integer) map.get("promotionBlack");
		if (map.containsKey("timeWhite")) {
			timeWhite = (Integer) map.get("timeWhite");
			timeBlack = (Integer) map.get("timeBlack");
		}
		setupChesspressoGame();

		// Replay the move history to restore the saved board position.
		// We do this instead of just saving the position so that the Chesspresso Game model
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

	int getTimeWhite() {
		return timeWhite;
	}

	int getTimeBlack() {
		return timeBlack;
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
	
	List<Short> getHistory() {
		return history;
	}
	
	String getOtherPlayer(String name) {
		return name.equals(playerWhite) ? playerBlack : playerWhite;
	}

	void clockTick() {
		if (state != GameState.RUNNING)
			return;

		Date now = new Date();
		long diff = now.getTime() - lastCheck.getTime();
		lastCheck.setTime(now.getTime());
		if (getPosition().getToPlay() == Chess.WHITE) {
			timeWhite += diff;
			getView().updateClock(Chess.WHITE, timeWhite);
		} else {
			timeBlack += diff;
			getView().updateClock(Chess.BLACK, timeBlack);
		}
		
		checkForAIResponse();
	}
	
	private void checkForAIResponse() {
		// TODO: STUB method for when we add AI
	}

	void swapColours() {
		clockTick();
		String tmp = playerWhite;
		playerWhite = playerBlack;
		playerBlack = tmp;
		alert(playerWhite, "Side swap!  You are now playing White.");
		alert(playerBlack, "Side swap!  You are now playing Black.");
	}
	
	void addPlayer(String playerName) throws ChessException {
		if (state != GameState.SETTING_UP) {
			throw new ChessException("Can only add players during game setup phase.");
		}
		if (!invited.equals("*") && !invited.equalsIgnoreCase(playerName)) {
			throw new ChessException("You don't have an invitation for this game.");
		}
		String otherPlayer = null;
		if (playerBlack.isEmpty()) {
			playerBlack = playerName;
			otherPlayer = playerWhite;
		} else if (playerWhite.isEmpty()) {
			playerWhite = playerName;
			otherPlayer = playerBlack;
		} else {
			throw new ChessException("This game already has two players.");
		}
		
		alert(otherPlayer, playerName + " has joined your game.");
		clearInvitation();
	}
	
	void invitePlayer(String inviterName, String inviteeName) throws ChessException {
		inviteSanityCheck(inviterName);
		if (invited.equals(inviteeName))
			return;
		alert(inviteeName, "You have been invited to this game by &6" + inviterName + "&-.");
		alert(inviteeName, "Type &f/chess join&- to join the game.");
		if (!invited.isEmpty()) {
			alert(invited, "Your invitation has been withdrawn.");
		}
		invited = inviteeName;
	}
	
	void inviteOpen(String inviterName) throws ChessException {
		inviteSanityCheck(inviterName);
		Bukkit.getServer().broadcastMessage(ChessCraft.parseColourSpec("&e:: &6" + inviterName + "&e has created an open invitation to a chess game."));
		Bukkit.getServer().broadcastMessage(ChessCraft.parseColourSpec("&e:: " + "Type &f/chess join " + getName() + "&e to join."));
		invited = "*";
	}

	private void inviteSanityCheck(String inviterName) throws ChessException {
		if (getState() != GameState.SETTING_UP)
			throw new ChessException("This game has already been started!");
		if (!isPlayerInGame(inviterName))
			throw new ChessException("Can't invite a player to a game you're not in!");
		if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) 
			throw new ChessException("This game already has two players!");
	}
		
	void clearInvitation() {
		invited = "";
	}
	
	void start(String playerName) throws ChessException {
		if (state != GameState.SETTING_UP) 
			throw new ChessException("This game has already been started!");
		if (!isPlayerInGame(playerName))
			throw new ChessException("Can't start a game you're not in!");
		if (playerWhite.isEmpty())
			throw new ChessException("There is no white player yet.");
		if (playerBlack.isEmpty())
			throw new ChessException("There is no black player yet.");
		alert(playerWhite, "Game started!  You are playing &fWhite&-.");
		alert(playerBlack, "Game started!  You are playing &fBlack&-.");
		state = GameState.RUNNING;
	}
	
	void resign(String playerName) throws ChessException {
		if (state != GameState.RUNNING)
			throw new ChessException("The game has not yet started.");
		if (!isPlayerInGame(playerName))
			throw new ChessException("Can't resign a game you're not in!");
		state = GameState.FINISHED;
		String winner;
		String loser = playerName;
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
	
	void setPromotionPiece(String playerName, int piece) throws ChessException {
		if (piece != Chess.QUEEN && piece != Chess.ROOK && piece != Chess.BISHOP && piece != Chess.KNIGHT)
			throw new ChessException("Invalid promotion piece: " + Chess.pieceToChar(piece));
		if (!isPlayerInGame(playerName))
			throw new ChessException("Can't set promotion piece for a game you're not in!");
		if (playerName.equals(playerWhite))
			promotionPiece[Chess.WHITE] = piece;
		if (playerName.equals(playerBlack))
			promotionPiece[Chess.BLACK] = piece;	
	}
	
	void drawn() {
		state = GameState.FINISHED;
		result = Chess.RES_DRAW;
		cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
		announceResult(playerWhite, playerBlack, ResultType.DrawAgreed);
	}
	
	// Do a move for playerName to toSquare.  fromSquare is already set, either from 
	// command-line, or from clicking a piece
	void doMove(String playerName, int toSquare) throws IllegalMoveException, ChessException {
		if (fromSquare == Chess.NO_SQUARE) {
			return;
		}
		if (state != GameState.RUNNING) {
			throw new ChessException("Chess game '" + getName() + "': Game is not running!");
		}
		if (!playerName.equals(getPlayerToMove())) {
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
			clockTick();
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
			} else if (getPosition().getHalfMoveClock() >= 50) {
				announceResult(getPlayerNotToMove(), getPlayerToMove(), ResultType.FiftyMoveRule);
				result = Chess.RES_DRAW;
				cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2");
				state = GameState.FINISHED;
			} else {
				String nextPlayer = getPlayerToMove();
				if (isAIPlayer(nextPlayer)) {
					// TODO: set AI to thinking above next move and add poll for move ready
				} else {
					alert(nextPlayer, "&f" + getColour(prevToMove) + "&- played [" + lastMove.getLAN() + "].");
					alert(nextPlayer, "It is your move &f(" + getColour(getPosition().getToPlay()) + ")&-.");
				}
			}
		} catch (IllegalMoveException e) {
			throw e;
		}
	}
	
	boolean isAIPlayer(String name) {
		// no support for AI players yet...
		return false;
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
				msg = "&6" + p1 + "&e checkmated &6" + p2 + "&e in a game of Chess!"; break;
			case Stalemate:
				msg = "&6" + p1 + "&e drew with &6" + p2 + "&e (stalemate) in a game of Chess!"; break;
			case FiftyMoveRule:
				msg = "&6" + p1 + "&e drew with &6" + p2 + "&e (50-move rule) in a game of Chess!"; break;
			case DrawAgreed:
				msg = "&6" + p1 + "&e drew with &6" + p2 + "&e (draw agreed) in a game of Chess!"; break;
			case Resigned:
				msg ="&6" +  p1 + "&e beat &6" + p2 + "&e (resigned) in a game of Chess!"; break;
			}
			if (!msg.isEmpty())
				Bukkit.getServer().broadcastMessage(ChessCraft.parseColourSpec(ChatColor.YELLOW + ":: " + msg));
		} else {
			switch(rt) {
			case Checkmate:
				alert(p1, "You checkmated &6" + p2 + "&-!");
				alert(p2, "You were checkmated by &6" + p1 + "&-!");
				break;
			case Stalemate:
				alert("Game is drawn - stalemate!");
				break;
			case Resigned:
				alert(p1, "&6" + p2+ "&- has resigned - you win!");
				alert(p2, "You have resigned. &6" + p1 + "&- wins!");
				break;
			case DrawAgreed:
				alert("Game is drawn - draw agreed!");
				break;
			}
		}
		setupAutoDeletion();
	}

	private void setupAutoDeletion() {
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
			}, autoDel * 20L);
			
			if (delTask != -1)
				alert("This game will auto-delete in " + autoDel + " seconds.");
			alert("Type &f/chess archive&- within " + autoDel + " seconds to save this game to PGN.");
		}
	}
	
	// Check if the move is really allowed
	// Also account for special cases: castling, en passant, pawn promotion
	private short checkMove(short move) throws IllegalMoveException {
		int from = Move.getFromSqi(move);
		int to = Move.getToSqi(move);
		int toPlay = getPosition().getToPlay();
		
		if (getPosition().getPiece(from) == Chess.KING) {
			// Castling?
			if (from == Chess.E1 && to == Chess.G1 || from == Chess.E8 && to == Chess.G8)
				move = Move.getShortCastle(getPosition().getToPlay());
			else if (from == Chess.E1 && to == Chess.B1 || from == Chess.E8 && to == Chess.B8)
				move = Move.getLongCastle(getPosition().getToPlay());
		} else if (getPosition().getPiece(from) == Chess.PAWN && (Chess.sqiToRow(to) == 7 || Chess.sqiToRow(to) == 0)) {
			// Promotion?
			boolean capturing = getPosition().getPiece(to) != Chess.NO_PIECE;
			move = Move.getPawnMove(from, to, capturing, promotionPiece[toPlay]);
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
	
	void alert(String playerName, String message) {
		if (playerName.isEmpty() || isAIPlayer(playerName))
			return;
		Player p = Bukkit.getServer().getPlayer(playerName);
		if (p != null) {
			plugin.alertMessage(p, "&6:: &-Chess game &6" + getName() + "&-: " + message);
		}
	}
	void alert(String message) {
		alert(playerWhite, message);
		if (!playerWhite.equalsIgnoreCase(playerBlack))
			alert(playerBlack, message);
	}
	
	String getPlayerToMove() {
		return getPosition().getToPlay() == Chess.WHITE ? playerWhite : playerBlack;
	}
	String getPlayerNotToMove() {
		return getPosition().getToPlay() == Chess.BLACK ? playerWhite : playerBlack;
	}

	Boolean isPlayerInGame(String playerName) {
		return (playerName.equalsIgnoreCase(playerWhite) || playerName.equalsIgnoreCase(playerBlack));
	}
	
	Boolean isPlayerToMove(String playerName) {
		return playerName.equalsIgnoreCase(getPlayerToMove());
	}

	File writePGN(boolean force) throws ChessException {
		plugin.createDir(archiveDir);
		
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

	void setFen(String fen) {
		getPosition().set(new Position(fen));
		// manually overriding the position invalidates the move history
		getHistory().clear();
	}
	
	static String secondsToHMS(int n) {
		n /= 1000;
		
		int secs = n % 60;
		int hrs = n / 3600;
		int mins = (n - (hrs * 3600)) / 60;
		
		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs);
	}
}

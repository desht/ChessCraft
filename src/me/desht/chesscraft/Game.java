package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.position.Position;

public class Game {
	enum ResultType {
		Checkmate, Stalemate, DrawAgreed, Resigned, Abandoned
	}
	private ChessCraft plugin;
	private String name;
	private Position position;
	private BoardView view;
	private String playerWhite, playerBlack;
	private int promo[] = { Chess.QUEEN, Chess.QUEEN };
	private String invited;
	private GameState state;
	private int fromSquare;
	private List<String> history;
	
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
		history = new ArrayList<String>();
		
		position = Position.createInitialPosition();
		position.addPositionChangeListener(view);
		position.addPositionListener(view);
	}
	
	Map<String,Object> freeze() {
		Map<String,Object> result = new HashMap<String,Object>();
		
		result.put("name", name);
		result.put("boardview", view.getName());
		result.put("playerWhite", playerWhite);
		result.put("playerBlack", playerBlack);
		result.put("state", state.toString());
		result.put("invited", invited);
		result.put("moves", history);
		result.put("position", position.getFEN());
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	void thaw(Map<String,Object> map) {
		playerWhite = (String) map.get("playerWhite");
		playerBlack = (String) map.get("playerBlack");
		state = GameState.valueOf((String) map.get("state"));
		invited = (String) map.get("invited");
		history = (List<String>) map.get("moves");
		position = new Position((String) map.get("position"));
		position.addPositionChangeListener(view);
		position.addPositionListener(view);
	}
	
	String getName() {
		return name;
	}
	
	Position getPosition() {
		return position;
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

	public int getFromSquare() {
		return fromSquare;
	}

	public void setFromSquare(int fromSquare) {
		this.fromSquare = fromSquare;
	}

	void swapColours(Player player) {
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
		if (!isPlayerInGame(p))
			throw new ChessException("Can't start a game you're not in!");
		if (playerWhite.isEmpty())
			throw new ChessException("There is no white player yet.");
		if (playerBlack.isEmpty())
			throw new ChessException("There is no black player yet.");
		alert(playerWhite, " game started!  Your turn.");
		alert(playerBlack, " game started!  White's turn.");
		state = GameState.RUNNING;
	}
	
	void resign(Player p) throws ChessException {
		if (!isPlayerInGame(p))
			throw new ChessException("Can't start a game you're not in!");
		state = GameState.FINISHED;
		String winner;
		String loser = p.getName();
		if (loser.equalsIgnoreCase(playerWhite)) 
			winner = playerBlack;
		else
			winner = playerWhite;
		announceResult(winner, loser, ResultType.Resigned);
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
		if (!p.getName().equalsIgnoreCase(getPlayerToMove())) {
			throw new ChessException("Chess game '" + getName() + "': It is not your move!");
		}
		
		Boolean capturing = position.getPiece(toSquare) != Chess.NO_PIECE;
		int prevToMove = position.getToPlay();
		short move = Move.getRegularMove(fromSquare, toSquare, capturing);
		try {
			short realMove = checkMove(move);
			position.doMove(realMove);
			Move lastMove = position.getLastMove();
			fromSquare = Chess.NO_SQUARE;
			history.add(lastMove.getLAN());
			if (position.isMate()) {
				announceResult(getPlayerNotToMove(), getPlayerToMove(), ResultType.Checkmate);
				state = GameState.FINISHED;
			} else if (position.isStaleMate()) {
				announceResult(getPlayerNotToMove(), getPlayerToMove(), ResultType.Stalemate);
				state = GameState.FINISHED;
			} else {
				alert(getPlayerToMove(), getColour(prevToMove) + " played [" + lastMove.getLAN() + "].");
				alert(getPlayerToMove(), "It is your move (" + getColour(position.getToPlay()) + ").");
			}
		} catch (IllegalMoveException e) {
			throw e;
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
				alert(p1, "Game is drawn - stalemate!");
				alert(p2, "Game is drawn - stalemate!");
				break;
			case Resigned:
				alert(p1, p2 + " has resigned - you win!");
				alert(p2, "You have resigned. " + p1 + " wins!");
				break;
			case DrawAgreed:
				alert(p1, "Game is drawn - draw agreed!");
				alert(p1, "Game is drawn - draw agreed!");
				break;
			}
		}
	}
	
	// Check if the move is really allowed
	// Also account for special cases: castling, en passant, pawn promotion
	private short checkMove(short move) throws IllegalMoveException {
		int from = Move.getFromSqi(move);
		int to = Move.getToSqi(move);
		
		if (position.getPiece(from) == Chess.KING) {
			// Castling?
			if (from == Chess.E1 && to == Chess.G1 || from == Chess.E8 && to == Chess.G8)
				move = Move.getShortCastle(position.getToPlay());
			else if (from == Chess.E1 && to == Chess.B1 || from == Chess.E8 && to == Chess.B8)
				move = Move.getLongCastle(position.getToPlay());
		} else if (position.getPiece(from) == Chess.PAWN && Chess.sqiToRow(to) == 7) {
			// Promotion?
			boolean capturing = position.getPiece(to) != Chess.NO_PIECE;
			// TODO: allow player to specify the promotion piece
			move = Move.getPawnMove(from, to, capturing, promo[position.getToPlay()]);
		} else if (position.getPiece(from) == Chess.PAWN && position.getPiece(to) == Chess.NO_PIECE) {
			// En passant?
			int toCol = Chess.sqiToCol(to);
			int fromCol = Chess.sqiToCol(from);
			if (Chess.sqiToRow(from) == 4 && Chess.sqiToRow(to) == 5 && (toCol == fromCol - 1 || toCol == fromCol + 1)) {
				move = Move.getEPMove(from, to);
			}
		}
			
		for (short aMove : position.getAllMoves()) {
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
	
	String getPlayerToMove() {
		return position.getToPlay() == Chess.WHITE ? playerWhite : playerBlack;
	}
	String getPlayerNotToMove() {
		return position.getToPlay() == Chess.BLACK ? playerWhite : playerBlack;
	}

	Boolean isPlayerInGame(Player p) {
		return (p.getName().equalsIgnoreCase(playerWhite) || p.getName().equalsIgnoreCase(playerBlack));
	}
	
	Boolean isPlayerToMove(Player p) {
		return p.getName().equalsIgnoreCase(getPlayerToMove());
	}

	public void show_all() {
		short[] moves = position.getAllMoves();
		String s = position.getMovesAsString(moves, false);
		System.out.println(s);
	}
}

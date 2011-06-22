package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.position.Position;

public class Game {
//	private ChessCraft plugin;
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
//		this.plugin = plugin;
		this.view = view;
		this.name = name;
		if (view.getGame() != null)
			throw new ChessException("That board already has a game on it.");
		view.setGame(this);
		playerWhite = player.getName();
		playerBlack = "";
		state = GameState.SETTING_UP;
		fromSquare = Chess.NO_SQUARE;
		invited = "";
		history = new ArrayList<String>();
		
		position = Position.createInitialPosition();
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
		if (!invited.equalsIgnoreCase(player.getName())) {
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
	
	void invitePlayer(Player inviter, String name) throws ChessException {
		if (!isPlayerInGame(inviter))
			throw new ChessException("Can't invite a player to a game you're not in!");
		if (name != null) {
			if (Bukkit.getServer().getPlayer(name) == null)
				throw new ChessException("Player " + name + " isn't online.");
		}
		invited = name;
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
//				alert(getPlayerToMove(), "You have been mated by " + getPlayerNotToMove() + "!");
//				alert(getPlayerNotToMove(), "You have mated " + getPlayerToMove() + "!");
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "::" + getPlayerNotToMove() + " checkmated " + getPlayerToMove() + " in a game of chess!");
				state = GameState.FINISHED;
			} else if (position.isStaleMate()) {
//				alert(playerWhite, "The game is a stalemate!");
//				alert(playerBlack, "The game is a stalemate!");
				Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "::" + getPlayerNotToMove() + " drew with " + getPlayerToMove() + " (stalemate) in a game of chess!");
				state = GameState.FINISHED;
			} else {
				alert(getPlayerToMove(), getColour(prevToMove) + " played [" + lastMove.getLAN() + "].");
				alert(getPlayerToMove(), "It is your move (" + getColour(position.getToPlay()) + ").");
			}
		} catch (IllegalMoveException e) {
			throw e;
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
	
	void alert(String playerName, String message) {
		Player p = Bukkit.getServer().getPlayer(playerName);
		if (p == null) {
			return;
		}
		p.sendMessage(ChatColor.YELLOW + ":: Chess game '" + getName() + "': " + message);
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

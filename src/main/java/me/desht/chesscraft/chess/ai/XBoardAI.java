package me.desht.chesscraft.chess.ai;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.chesscraft.chess.ChessGame;

import org.bukkit.configuration.ConfigurationSection;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

public class XBoardAI extends ChessAI {

	private static final Pattern patternMove =
			Pattern.compile("(my)?\\s*move\\s*(is)?\\s*[:>=\\-]?\\s*([a-h][1-8][a-h][1-8][nbrq]?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern patternIllegal =
			Pattern.compile("(Illegal move.+)|(Error.+)", Pattern.CASE_INSENSITIVE);
	
	private final ExternalIO io;

	public XBoardAI(String name, ChessGame chessCraftGame, Boolean isWhite, ConfigurationSection params) {
		super(name, chessCraftGame, isWhite, params);

		String command = params.getString("command", "gnuchess xboard");
		io = new ExternalIO(command);
		io.start();

		io.writeLine("xboard");
		io.writeLine("protover 2");
		io.writeLine("setboard " + getChessCraftGame().getPosition().getFEN());
		int toMove = getChessCraftGame().getPosition().getToPlay();
		if (isWhite && toMove == Chess.WHITE || !isWhite && toMove == Chess.BLACK) {
			io.writeLine("go");
		}
	}

	@Override
	public void shutdown() {
		io.writeLine("exit");
	}

	@Override
	public void run() {
		boolean done = false;
		while (!done) {
			try {
				done = parseCommand();
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
	
	@Override
	public void replayMoves(List<Short> moves) {
		// Do nothing here, because we set the position when the AI was created
		// using the "setboard" command.
	}

	private boolean parseCommand() throws IOException {
		final String line = io.readLine();
		if (line == null) {
			aiHasFailed(new IllegalMoveException("illegal move: " + line));
			return true;
		}

		Matcher matcher;
		matcher = patternMove.matcher(line);
		if(matcher.matches()) {
			int fromSqi = Chess.strToSqi(matcher.group(3).substring(0, 2));
			int toSqi = Chess.strToSqi(matcher.group(3).substring(2, 4));
			aiHasMoved(fromSqi, toSqi);
			return true;
		} else {
			matcher = patternIllegal.matcher(line);
			if (matcher.matches()) {
				aiHasFailed(new IllegalMoveException("illegal move: " + line));
				return true;
			}
		}
		return false;
	}

	@Override
	public void undoLastMove() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void movePiece(int fromSqi, int toSqi, boolean otherPlayer) {
		// we only send the move if it's the other player doing the move
		if (otherPlayer) {
			String move = Chess.sqiToStr(fromSqi) + Chess.sqiToStr(toSqi); 
			io.writeLine(move);
		}
	}
}

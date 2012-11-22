package me.desht.chesscraft.chess.ai;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.chess.TimeControl.RolloverPhase;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;

public class XBoardAI extends ChessAI {

	private static final Pattern patternMove =
			Pattern.compile("(my)?\\s*move\\s*(is)?\\s*[:>=\\-]?\\s*([a-h][1-8][a-h][1-8][nbrq]?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern patternSanMove = 
			Pattern.compile("(my)?\\s*move\\s*(is)?\\s*[:>=\\-]?\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern patternIllegal =
			Pattern.compile("(Illegal move.+)|(Error.+)", Pattern.CASE_INSENSITIVE);

	private final ExternalIO io;
	private final Map<String,String> features = new ConcurrentHashMap<String, String>();

	private boolean moveFormatSAN = false;

	public XBoardAI(String name, ChessGame chessCraftGame, Boolean isWhite, ConfigurationSection params) {
		super(name, chessCraftGame, isWhite, params);

		String command = params.getString("command", "gnuchess xboard");
		io = new ExternalIO(command);
		io.start();

		io.writeLine("xboard");
		io.writeLine("protover 2");

		// this bit gets done asynchronously
		new FeatureReader();

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

	@Override
	public void offerDraw() {
		io.writeLine("draw");
		setDrawOfferedToAI(true);
	}

	public String getFeature(String k) {
		return features.containsKey(k) ? features.get(k) : "";
	}

	private boolean parseCommand() throws IOException {
		final String line = io.readLine();
		if (line == null) {
			aiHasFailed(new IllegalMoveException("illegal move: " + line));
			return true;
		}

		Matcher matcher;
		matcher = moveFormatSAN ? patternSanMove.matcher(line) : patternMove.matcher(line);
		if (matcher.matches()) {
			int fromSqi, toSqi;
			if (moveFormatSAN) {
				Move m = getChessCraftGame().getMoveFromSAN(matcher.group(3));
				if (m == null) {
					aiHasFailed(new IllegalMoveException("illegal move: " + line));
					return true;
				}
				fromSqi = m.getFromSqi();
				toSqi = m.getToSqi();
			} else {
				fromSqi = Chess.strToSqi(matcher.group(3).substring(0, 2));
				toSqi = Chess.strToSqi(matcher.group(3).substring(2, 4));
			}
			aiHasMoved(fromSqi, toSqi);
			return true;
		} else if (line.equals("offer draw")) {
			if (isDrawOfferedToAI()) {
				// AI has accepted the opponent's draw offer; game over
				acceptDrawOffer();
			} else {
				// AI is making a draw offer - relay this to the other player
				makeDrawOffer();
			}
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
		if (toMove()) {
			// stop the AI thinking, and back up one move
			io.writeLine("force");
			setActive(false);
			io.writeLine("undo");	
		} else {
			// undo the AI's last move and the other player's last move
			io.writeLine("undo");
			io.writeLine("undo");
		}
		// it is now the other player's move again
	}

	@Override
	protected void movePiece(int fromSqi, int toSqi, boolean otherPlayer) {
		// we only send the move if it's the other player doing the move
		if (otherPlayer) {
			String move = Chess.sqiToStr(fromSqi) + Chess.sqiToStr(toSqi); 
			io.writeLine(move);
		}
	}

	@Override
	public void notifyTimeControl(TimeControl timeControl) {
		long totalSecs = timeControl.getTotalTime() / 1000;
		long secs = totalSecs % 60;
		long mins = totalSecs / 60;
		
		switch (timeControl.getControlType()) {
		case MOVE_IN:
			io.writeLine("st " + totalSecs);
			break;
		case GAME_IN:
			io.writeLine("level 0 " + mins + ":" + secs + " 0");
			break;
		case ROLLOVER:
			RolloverPhase phase = timeControl.getCurrentPhase();
			io.writeLine("level " + phase.getMoves() + " " + phase.getMinutes() + " " + phase.getIncrement() / 1000);
			break;
		}
	}

	private class FeatureReader implements Runnable {

		private FeatureReader() {
			Bukkit.getScheduler().scheduleAsyncDelayedTask(ChessCraft.getInstance(), this);
		}

		private void readLines() {
			boolean done = false;
			while (!done) {
				try {
					String s = io.readLine();
					LogUtils.finer("featurereader: [" + s + "]");
					if (s.startsWith("feature ")) {
						List<String> f = MiscUtil.splitQuotedString(s.replace("=", " "));
						for (int i = 1; i < f.size(); i += 2) {
							if ((i + 1) >= f.size()) break;
							String k = f.get(i);
							String v = f.get(i+1);
							features.put(k, v);
							if (k.equals("done") && v.equals("1")) {
								LogUtils.fine("feature reader done: " + features.size() + " features reported");
								done = true;
							}
						}
					}
				} catch (IOException e) {
					LogUtils.severe("FeatureReader: caught io exception: " + e.getMessage());
					done = true;
				}
			}

			setReady();
		}

		@Override
		public void run() {
			ExecutorService executor = Executors.newFixedThreadPool(1);

			Callable<Void> readTask = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					readLines();
					return null;
				}
			};

			Future<Void> future = executor.submit(readTask);
			try {
				// we give the AI engine 2 seconds to reply to the "protover" command
				// with a list of features
				future.get(2000, TimeUnit.MILLISECONDS);

				// now it's safe to finish AI init

				if (getFeature("san").equals("1")) {
					moveFormatSAN = true;
				}

				if (getFeature("setboard").equals("1")) {
					io.writeLine("setboard " + getChessCraftGame().getPosition().getFEN());

					if (toMove()) {
						io.writeLine("go");
					}
				} else {
					aiHasFailed(new ChessException("This xboard engine doesn't support the 'setboard' feature"));
				}
			} catch (Exception e) {
				aiHasFailed(e);
			}
		}
	}
}

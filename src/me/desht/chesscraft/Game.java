package me.desht.chesscraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.pgn.PGN;
import chesspresso.pgn.PGNWriter;
import chesspresso.position.Position;

import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.ChessAI.AI_Def;
import me.desht.chesscraft.enums.GameState;
import me.desht.util.Duration;

/**
 * @author des
 *
 */

public class Game {

	private static final Map<String, Game> chessGames = new HashMap<String, Game>();
	private static final Map<String, Game> currentGame = new HashMap<String, Game>();
	
	private ChessCraft plugin;
	private String name;
	private chesspresso.game.Game cpGame;
	private BoardView view;
	private String playerWhite, playerBlack;
	private int promotionPiece[] = { Chess.QUEEN, Chess.QUEEN };
	private String invited;
	private GameState state;
	private int fromSquare;
	private long started, finished, lastMoved;
	private long lastCheck;
	private int timeWhite, timeBlack;
	private List<Short> history;
	private int result;
	private double stake;
	private ChessAI aiPlayer = null;
	private boolean aiHasMoved;
	private int aiFromSqi, aiToSqi;

	public Game(ChessCraft plugin, String name, BoardView view, String playerName) throws ChessException {
		this.plugin = plugin;
		this.view = view;
		this.name = name;
		if (view.getGame() != null) {
			throw new ChessException(Messages.getString("Game.boardAlreadyHasGame")); //$NON-NLS-1$
		}
		playerWhite = playerName == null ? "" : playerName; //$NON-NLS-1$
		playerBlack = ""; //$NON-NLS-1$
		timeWhite = timeBlack = 0;
		state = GameState.SETTING_UP;
		fromSquare = Chess.NO_SQUARE;
		invited = ""; //$NON-NLS-1$
		history = new ArrayList<Short>();
		lastCheck = lastMoved = started = System.currentTimeMillis();
		finished = 0;
		result = Chess.RES_NOT_FINISHED;
		aiHasMoved = false;
		if (playerName != null) {
			stake = Math.min(plugin.getConfiguration().getDouble("stake.default", 0.0), ChessEconomy.getBalance(playerName)); //$NON-NLS-1$
		} else {
			stake = 0.0;
		}

		cpGame = new chesspresso.game.Game();
		setupChesspressoGame();

		view.setGame(this);
		
		getPosition().addPositionListener(view);
	}

	private void setupChesspressoGame() {
		// seven tag roster
		cpGame.setTag(PGN.TAG_EVENT, getName());
		cpGame.setTag(PGN.TAG_SITE, getView().getName() + Messages.getString("Game.sitePGN")); //$NON-NLS-1$
		cpGame.setTag(PGN.TAG_DATE, dateToPGNDate(started));
		cpGame.setTag(PGN.TAG_ROUND, "?"); //$NON-NLS-1$
		cpGame.setTag(PGN.TAG_WHITE, getPlayerWhite());
		cpGame.setTag(PGN.TAG_BLACK, getPlayerBlack());
		cpGame.setTag(PGN.TAG_RESULT, getPGNResult());

		// extra tags
		cpGame.setTag(PGN.TAG_FEN, Position.createInitialPosition().getFEN());
	}

	Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name); //$NON-NLS-1$
		map.put("boardview", view.getName()); //$NON-NLS-1$
		map.put("playerWhite", playerWhite); //$NON-NLS-1$
		map.put("playerBlack", playerBlack); //$NON-NLS-1$
		map.put("state", state.toString()); //$NON-NLS-1$
		map.put("invited", invited); //$NON-NLS-1$
		map.put("moves", history); //$NON-NLS-1$
		map.put("started", started); //$NON-NLS-1$
		map.put("finished", finished); //$NON-NLS-1$
		map.put("lastMoved", lastMoved); //$NON-NLS-1$
		map.put("result", result); //$NON-NLS-1$
		map.put("promotionWhite", promotionPiece[Chess.WHITE]); //$NON-NLS-1$
		map.put("promotionBlack", promotionPiece[Chess.BLACK]); //$NON-NLS-1$
		map.put("timeWhite", timeWhite); //$NON-NLS-1$
		map.put("timeBlack", timeBlack); //$NON-NLS-1$
		map.put("stake", stake); //$NON-NLS-1$
		map.put("aiHasMoved", aiHasMoved); //$NON-NLS-1$
		map.put("aiFromSqi", aiFromSqi); //$NON-NLS-1$
		map.put("aiToSqi", aiToSqi); //$NON-NLS-1$

		return map;
	}

	public void save() {
		plugin.persistence.saveGame(this);
	}

	public void autoSave() {
		if (plugin.getConfiguration().getBoolean("autosave", true)) { //$NON-NLS-1$
			save();
		}
	}

	@SuppressWarnings("unchecked")
	boolean thaw(Map<String, Object> map) throws ChessException, IllegalMoveException {
		playerWhite = (String) map.get("playerWhite"); //$NON-NLS-1$
		playerBlack = (String) map.get("playerBlack"); //$NON-NLS-1$
		state = GameState.valueOf((String) map.get("state")); //$NON-NLS-1$
		invited = (String) map.get("invited"); //$NON-NLS-1$
		List<Integer> hTmp = (List<Integer>) map.get("moves"); //$NON-NLS-1$
		history.clear();
		for (int m : hTmp) {
			history.add((short) m);
		}
		started = (Long) map.get("started"); //$NON-NLS-1$
		if (map.containsKey("finished")) { //$NON-NLS-1$
			// a simple cast to Long won't work here
			finished = Long.parseLong(map.get("finished").toString()); //$NON-NLS-1$
		} else {
			finished = state == GameState.FINISHED ? System.currentTimeMillis() : 0;
		}
		if (map.containsKey("lastMoved")) { //$NON-NLS-1$
			// a simple cast to Long won't work here
			lastMoved = Long.parseLong(map.get("lastMoved").toString()); //$NON-NLS-1$
		} else {
			lastMoved = System.currentTimeMillis();
		}
		result = (Integer) map.get("result"); //$NON-NLS-1$
		promotionPiece[Chess.WHITE] = (Integer) map.get("promotionWhite"); //$NON-NLS-1$
		promotionPiece[Chess.BLACK] = (Integer) map.get("promotionBlack"); //$NON-NLS-1$
		if (map.containsKey("timeWhite")) { //$NON-NLS-1$
			timeWhite = (Integer) map.get("timeWhite"); //$NON-NLS-1$
			timeBlack = (Integer) map.get("timeBlack"); //$NON-NLS-1$
		}
		if (map.containsKey("stake")) { //$NON-NLS-1$
			stake = (Double) map.get("stake"); //$NON-NLS-1$
		}

		if (isAIPlayer(playerWhite)) {
			aiPlayer = ChessAI.getNewAI(this, playerWhite, true);
			playerWhite = aiPlayer.getName();
			aiPlayer.init(true);
		} else if (isAIPlayer(playerBlack)) {
			aiPlayer = ChessAI.getNewAI(this, playerBlack, true);
			playerBlack = aiPlayer.getName();
			aiPlayer.init(false);
		}
		
		if (map.containsKey("aiHasMoved")) {
			aiHasMoved = (Boolean) map.get("aiHasMoved");
			aiFromSqi = (Integer) map.get("aiFromSqi");
			aiToSqi = (Integer) map.get("aiToSqi");
		}

		setupChesspressoGame();
		
		// Replay the move history to restore the saved board position.  We do this
		// instead of just saving the position so that the Chesspresso Game model 
		// includes a history of the moves, suitable for creating a PGN file.
		for (short move : history) {
			getPosition().doMove(move);
		}
		// repeat for the ai engine (doesn't support loading from FEN)
		if (aiPlayer != null) {
			for (short move : history) {
				aiPlayer.loadmove(Move.getFromSqi(move), Move.getToSqi(move));
			}
			aiPlayer.loadDone(); // tell ai to start on next move
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

		if (state == GameState.FINISHED) {
			finished = System.currentTimeMillis();
			if (aiPlayer != null) {
				aiPlayer.removeAI();
				aiPlayer = null;
			}
		}
		getView().getControlPanel().repaintSignButtons();
	}

	public int getFromSquare() {
		return fromSquare;
	}

	public long getStarted() {
		return started;
	}

	public long getFinished() {
		return finished;
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
			throw new ChessException(Messages.getString("Game.stakeCantBeChanged")); //$NON-NLS-1$
		}

		this.stake = newStake;
	}

	/**
	 * Housekeeping task, called every <tick_interval> seconds as a scheduled sync task.
	 */
	public void clockTick() {
		if (state != GameState.RUNNING) {
			return;
		}
		checkForAIMove();
		updateChessClocks();
	}

	private void updateChessClocks() {
		// update the clocks
		long now = System.currentTimeMillis();
		long diff = now - lastCheck;
		lastCheck = now;
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
		alert(playerWhite, Messages.getString("Game.nowPlayingWhite")); //$NON-NLS-1$
		alert(playerBlack, Messages.getString("Game.nowPlayingBlack")); //$NON-NLS-1$
	}

	public void addPlayer(String playerName) throws ChessException {
		ensureGameState(GameState.SETTING_UP);
		if (!playerBlack.isEmpty() && !playerWhite.isEmpty()) {
			throw new ChessException(Messages.getString("Game.gameIsFull")); //$NON-NLS-1$
		}

		String otherPlayer = playerBlack.isEmpty() ? playerWhite : playerBlack;

		if (isAIPlayer(playerName)) {
			addAI(playerName);
		} else {
			if (!invited.equals("*") && !invited.equalsIgnoreCase(playerName)) { //$NON-NLS-1$
				throw new ChessException(Messages.getString("Game.notInvited")); //$NON-NLS-1$
			}
			if (ChessEconomy.active() && !ChessEconomy.canAfford(playerName, getStake())) {
				throw new ChessException(Messages.getString("Game.cantAffordToJoin", ChessEconomy.format(getStake()))); //$NON-NLS-1$
			}
			if (playerBlack.isEmpty()) {
				playerBlack = playerName;
			} else {// if (playerWhite.isEmpty()) {
				playerWhite = playerName;
			}
		}
		getView().getControlPanel().repaintSignButtons();
		alert(otherPlayer, Messages.getString("Game.playerJoined", playerName)); //$NON-NLS-1$
		clearInvitation();
		if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
			if (ChessConfig.getConfiguration().getBoolean("autostart", true)) {
				start(playerName);
			} else {
				alert(Messages.getString("Game.startPrompt")); //$NON-NLS-1$
			}
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
					throw new ChessException(Messages.getString("Game.AIisBusy")); //$NON-NLS-1$
				}
				addPlayer(ai.getFullAIName());
				return;
			}
			throw new ChessException(Messages.getString("Game.playerNotOnline", inviteeName)); //$NON-NLS-1$ 
		} else {
			inviteeName = player.getName();
			alert(inviteeName, Messages.getString("Game.youAreInvited", inviterName)); //$NON-NLS-1$ 
			if (ChessEconomy.active() && getStake() > 0.0) {
				alert(inviteeName, Messages.getString("Game.gameHasStake", ChessEconomy.format(getStake()))); //$NON-NLS-1$
			}
			alert(inviteeName, Messages.getString("Game.joinPrompt")); //$NON-NLS-1$
			if (!invited.isEmpty()) {
				alert(invited, Messages.getString("Game.inviteWithdrawn")); //$NON-NLS-1$
			}
			invited = inviteeName;
			alert(inviterName, Messages.getString("Game.inviteSent", invited)); //$NON-NLS-1$
		}
	}

	public void inviteOpen(String inviterName) throws ChessException {
		inviteSanityCheck(inviterName);
		ChessUtils.broadcastMessage((Messages.getString("Game.openInviteCreated", inviterName))); //$NON-NLS-1$
		if (ChessEconomy.active() && getStake() > 0.0) {
			ChessUtils.broadcastMessage(Messages.getString("Game.gameHasStake", ChessEconomy.format(getStake()))); //$NON-NLS-1$
		}
		ChessUtils.broadcastMessage(Messages.getString("Game.joinPromptGlobal", getName())); //$NON-NLS-1$ 
		invited = "*"; //$NON-NLS-1$
	}

	private void inviteSanityCheck(String inviterName) throws ChessException {
		ensurePlayerInGame(inviterName);
		ensureGameState(GameState.SETTING_UP);

		if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
			// if one is an AI, allow the AI to leave
			if (aiPlayer != null) {
				if (isAIPlayer(playerWhite)) {
					playerWhite = ""; //$NON-NLS-1$
				} else {
					playerBlack = ""; //$NON-NLS-1$
				}
				aiPlayer.removeAI();
			} else {
				throw new ChessException(Messages.getString("Game.gameIsFull")); //$NON-NLS-1$
			}
		}
	}

	public void clearInvitation() {
		invited = ""; //$NON-NLS-1$
	}

	public void start(String playerName) throws ChessException {
		ensurePlayerInGame(playerName);
		ensureGameState(GameState.SETTING_UP);
		
		String whiteStr = Messages.getString("Game.white");
		String blackStr = Messages.getString("Game.black");
		
		if (!canAffordToPlay(playerWhite)) {
			throw new ChessException(Messages.getString("Game.cantAffordToStart", whiteStr, ChessEconomy.format(stake))); //$NON-NLS-1$
		}
		if (!canAffordToPlay(playerBlack)) {
			throw new ChessException(Messages.getString("Game.cantAffordToStart", blackStr, ChessEconomy.format(stake))); //$NON-NLS-1$
		}
		
		if (playerWhite.isEmpty() || playerBlack.isEmpty()) {
			addAI(null);
			alert(playerName, Messages.getString("Game.playerJoined", aiPlayer.getName())); //$NON-NLS-1$
		}
		
		if (ChessConfig.getConfiguration().getBoolean("auto_teleport_on_join", true)) {
			summonPlayers();
		}
		int wandId = new MaterialWithData(plugin.getConfiguration().getString("wand_item")).getMaterial();
		String wand = Material.getMaterial(wandId).toString();
		alert(playerWhite, Messages.getString("Game.started", whiteStr, wand)); //$NON-NLS-1$
		alert(playerBlack, Messages.getString("Game.started", blackStr, wand)); //$NON-NLS-1$
		
		if (ChessEconomy.active() && stake > 0.0f && !playerWhite.equalsIgnoreCase(playerBlack)) {
			if (!isAIPlayer(playerWhite)) {
				ChessEconomy.subtractMoney(playerWhite, stake);
			}
			if (!isAIPlayer(playerBlack)) {
				ChessEconomy.subtractMoney(playerBlack, stake);
			}
			alert(Messages.getString("Game.paidStake", ChessEconomy.format(stake))); //$NON-NLS-1$ 
		}
		
		clearInvitation();
		lastMoved = System.currentTimeMillis();
		setState(GameState.RUNNING);
	}

	public void summonPlayers() throws ChessException {
		summonPlayer(getPlayerWhite());
		summonPlayer(getPlayerBlack());
	}
	
	public void summonPlayer(String player) throws ChessException {
		if (isAIPlayer(player)) {
			return;
		}
		Player p = plugin.getServer().getPlayer(player);
		if (p != null) {
			plugin.getCommandExecutor().tryTeleportToGame(p, this);
		}
	}
	
	public void resign(String playerName) throws ChessException {
		if (state != GameState.RUNNING) {
			throw new ChessException(Messages.getString("Game.notStarted")); //$NON-NLS-1$
		}

		ensurePlayerInGame(playerName);

		setState(GameState.FINISHED);
		String winner;
		String loser = playerName;
		if (loser.equalsIgnoreCase(playerWhite)) {
			winner = playerBlack;
			cpGame.setTag(PGN.TAG_RESULT, "0-1"); //$NON-NLS-1$
			result = Chess.RES_BLACK_WINS;
		} else {
			winner = playerWhite;
			cpGame.setTag(PGN.TAG_RESULT, "1-0"); //$NON-NLS-1$
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
			cpGame.setTag(PGN.TAG_RESULT, "1-0"); //$NON-NLS-1$
			result = Chess.RES_WHITE_WINS;
		} else {
			loser = playerWhite;
			cpGame.setTag(PGN.TAG_RESULT, "0-1"); //$NON-NLS-1$
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
			throw new ChessException(Messages.getString("Game.invalidPromoPiece", Chess.pieceToChar(piece))); //$NON-NLS-1$
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
		cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2"); //$NON-NLS-1$
		announceResult(playerWhite, playerBlack, GameResult.DrawAgreed);
	}

	/**
	 * Do a move for playerName to toSquare <br>
	 * fromSquare should already be set, <br>
	 * either from command-line, or from clicking a piece
	 * 
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
			if (plugin.getConfiguration().getBoolean("highlight_last_move", true)) { //$NON-NLS-1$
				view.highlightSquares(fromSquare, toSquare);
			}
			getPosition().doMove(realMove);
			Move lastMove = getPosition().getLastMove();
			history.add(realMove);
			lastMoved = System.currentTimeMillis();
			autoSave();
			updateChessClocks();
			if (getPosition().isMate()) {
				cpGame.setTag(PGN.TAG_RESULT, getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0"); //$NON-NLS-1$ //$NON-NLS-2$
				result = getPosition().getToPlay() == Chess.WHITE ? Chess.RES_BLACK_WINS : Chess.RES_WHITE_WINS;
				setState(GameState.FINISHED);
				announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.Checkmate);
			} else if (getPosition().isStaleMate()) {
				result = Chess.RES_DRAW;
				cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2"); //$NON-NLS-1$
				setState(GameState.FINISHED);
				announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.Stalemate);
			} else if (getPosition().getHalfMoveClock() >= 50) {
				result = Chess.RES_DRAW;
				cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2"); //$NON-NLS-1$
				setState(GameState.FINISHED);
				announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.FiftyMoveRule);
			} else {
				// the game continues...
				String nextPlayer = getPlayerToMove();
				if (isAIPlayer(nextPlayer)) {
					aiPlayer.userMove(fromSquare, toSquare);
				} else {
					String checkNotify = getPosition().isCheck() ? Messages.getString("Game.check") : ""; //$NON-NLS-1$ //$NON-NLS-2$
					alert(nextPlayer, Messages.getString("Game.playerPlayedMove", getColour(prevToMove), lastMove.getLAN()) //$NON-NLS-1$
							+ checkNotify);
					alert(nextPlayer, Messages.getString("Game.yourMove", getColour(getPosition().getToPlay()))); //$NON-NLS-1$
				}
			}
			this.fromSquare = Chess.NO_SQUARE;
		} catch (IllegalMoveException e) {
			throw e;
		}
	}

	public boolean isAIPlayer(String name) {
		// simple name match.. not checking if in this game
		return name.startsWith(ChessAI.AI_PREFIX);
	}

	public String getPGNResult() {
		switch (result) {
		case Chess.RES_NOT_FINISHED:
			return "*"; //$NON-NLS-1$
		case Chess.RES_WHITE_WINS:
			return "1-0"; //$NON-NLS-1$
		case Chess.RES_BLACK_WINS:
			return "0-1"; //$NON-NLS-1$
		case Chess.RES_DRAW:
			return "1/2-1/2"; //$NON-NLS-1$
		default:
			return "*"; //$NON-NLS-1$
		}
	}

	/**
	 * Announce the result of the game to the server
	 * 
	 * @param p1
	 *            the winner
	 * @param p2
	 *            the loser (unless it's a draw)
	 * @param rt
	 *            result to announce
	 */
	public void announceResult(String p1, String p2, GameResult rt) {
		String msg = ""; //$NON-NLS-1$
		switch (rt) {
		case Checkmate:
			msg = Messages.getString("Game.checkmated", p1, p2); //$NON-NLS-1$
			break;
		case Stalemate:
			msg = Messages.getString("Game.stalemated", p1, p2); //$NON-NLS-1$
			break;
		case FiftyMoveRule:
			msg = Messages.getString("Game.fiftyMoveRule", p1, p2); //$NON-NLS-1$
			break;
		case DrawAgreed:
			msg = Messages.getString("Game.drawAgreed", p1, p2); //$NON-NLS-1$
			break;
		case Resigned:
			msg = Messages.getString("Game.resigned", p1, p2); //$NON-NLS-1$
			break;
		case Forfeited:
			msg = Messages.getString("Game.forfeited", p1, p2); //$NON-NLS-1$
			break;
		}
		if (plugin.getConfiguration().getBoolean("broadcast_results", true) &&
				!p1.equalsIgnoreCase(p2)) { //$NON-NLS-1$
			if (!msg.isEmpty()) {
				ChessUtils.broadcastMessage(msg);
			}
		} else {
			if (!msg.isEmpty()) {
				alert(msg);
			}
		}
		if (p1.equalsIgnoreCase(p2)) {
			return;
		}
		
		handlePayout(rt, p1, p2);
		Results.getResultsHandler().logResult(this, rt);
	}

	private void handlePayout(GameResult rt, String p1, String p2) {
		if (stake <= 0.0 || p1.equalsIgnoreCase(p2)) {
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
						ChessCraftLogger.log(Level.WARNING, "couldn't retrieve AI definition for " + p2); //$NON-NLS-1$
					}
				} else {
					winnings = stake * 2.0;
				}
				ChessEconomy.addMoney(p1, winnings);
				alert(p1, Messages.getString("Game.youWon", ChessEconomy.format(winnings))); //$NON-NLS-1$
			}
			alert(p2, Messages.getString("Game.lostStake", ChessEconomy.format(stake))); //$NON-NLS-1$
		} else {
			// a draw
			if (!isAIPlayer(p1)) {
				ChessEconomy.addMoney(p1, stake);
			}
			if (!isAIPlayer(p2)) {
				ChessEconomy.addMoney(p2, stake);
			}
			alert(Messages.getString("Game.getStakeBack", ChessEconomy.format(stake))); //$NON-NLS-1$
		}

		stake = 0.0;
	}

	/**
	 * Called when a game is permanently deleted.
	 */
	public void deletePermanently() {
		plugin.persistence.removeGameSavefile(this);

		handlePayout(GameResult.Abandoned, playerWhite, playerBlack);

		getView().setGame(null);

		deleteCommon();
	}

	/**
	 * Called for a transitory deletion, where we expect the object to be
	 * shortly restored, e.g. server reload, plugin disable, /chess reload
	 * persist command
	 */
	public void deleteTransitory() {
		deleteCommon();
	}

	private void deleteCommon() {

		if (aiPlayer != null) {
			// this would normally happen when the game goes to state FINISHED,
			// but we could get here if the game is explicitly deleted
			aiPlayer.removeAI();
		}

		try {
			Game.removeGame(getName());
		} catch (ChessException e) {
			ChessCraftLogger.log(Level.WARNING, e.getMessage());
		}
	}

	/**
	 * Check if the move is really allowed Also account for special cases:
	 * castling, en passant, pawn promotion
	 * 
	 * @param move
	 *            move to check
	 * @return move, if allowed
	 * @throws IllegalMoveException
	 *             if not allowed
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
	 * 
	 * @return game result in PGN notation
	 */
	public String getResult() {
		if (getState() != GameState.FINISHED) {
			return "*"; //$NON-NLS-1$
		}

		if (getPosition().isMate()) {
			return getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return "1/2-1/2"; //$NON-NLS-1$
		}
	}

	public static String getColour(int c) {
		switch (c) {
		case Chess.WHITE:
			return Messages.getString("Game.white"); //$NON-NLS-1$
		case Chess.BLACK:
			return Messages.getString("Game.black"); //$NON-NLS-1$
		default:
			return "???"; //$NON-NLS-1$
		}
	}

	public void alert(String playerName, String message) {
		if (playerName.isEmpty() || isAIPlayer(playerName)) {
			return;
		}
		Player p = Bukkit.getServer().getPlayer(playerName);
		if (p != null) {
			ChessUtils.alertMessage(p, Messages.getString("Game.alertPrefix", getName()) + message); //$NON-NLS-1$
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

		File f = makePGNName();
		if (f.exists() && !force) {
			throw new ChessException(Messages.getString("Game.archiveExists", f.getName())); //$NON-NLS-1$
		}

		try {
			PrintWriter pw = new PrintWriter(f);
			PGNWriter w = new PGNWriter(pw);
			w.write(cpGame.getModel());
			pw.close();
			return f;
		} catch (FileNotFoundException e) {
			throw new ChessException(Messages.getString("Game.cantWriteArchive", f.getName(), e.getMessage())); //$NON-NLS-1$
		}
	}

	private File makePGNName() {
		String baseName = getName() + "_" + dateToPGNDate(System.currentTimeMillis()); //$NON-NLS-1$

		int n = 1;
		File f;
		do {
//			f = new File(plugin.getDataFolder(), archiveDir + File.separator + baseName + "_" + n + ".pgn"); //$NON-NLS-1$ //$NON-NLS-2$
			f = new File(ChessConfig.getPGNDirectory(), baseName + "_" + n + ".pgn"); //$NON-NLS-1$
			++n;
		} while (f.exists());

		return f;
	}

	/**
	 * get PGN format of the date (the version in chesspresso.pgn.PGN gets the
	 * month wrong :( )
	 * 
	 * @param date
	 *            date to convert
	 * @return PGN format of the date
	 */
	private static String dateToPGNDate(long when) {
		return new SimpleDateFormat("yyyy.MM.dd").format(new Date(when)); //$NON-NLS-1$
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

		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs); //$NON-NLS-1$
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

	/**
	 * Check if a game needs to be auto-deleted: - Game that has not been
	 * started after a certain duration - Game that has been finished for a
	 * certain duration
	 */
	public void checkForAutoDelete() {
		boolean mustDelete = false;
		String alertStr = null;

		if (getState() == GameState.SETTING_UP) {
			long elapsed = System.currentTimeMillis() - started;
			Duration timeout = new Duration(ChessConfig.getConfiguration().getString("auto_delete.not_started", "3 mins"));
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration() && (playerWhite.isEmpty() || playerBlack.isEmpty())) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteNotStarted",  timeout); //$NON-NLS-1$
			}
		} else if (getState() == GameState.FINISHED) {
			long elapsed = System.currentTimeMillis() - finished;
			Duration timeout = new Duration(ChessConfig.getConfiguration().getString("auto_delete.finished", "30 mins"));
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration()) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteFinished"); //$NON-NLS-1$
			}
		} else if (getState() == GameState.RUNNING) {
			long elapsed = System.currentTimeMillis() - lastMoved;
			Duration timeout = new Duration(ChessConfig.getConfiguration().getString("auto_delete.running", "7 days"));
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration()) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteRunning", timeout); //$NON-NLS-1$
			}
		}

		if (mustDelete) {
			alert(alertStr);
			ChessCraftLogger.log(Level.INFO, alertStr);
			deletePermanently();
		}
	}

	public void ensurePlayerInGame(String playerName) throws ChessException {
		if (!playerName.equals(playerWhite) && !playerName.equals(playerBlack)) {
			throw new ChessException(Messages.getString("Game.notInGame")); //$NON-NLS-1$
		}
	}

	public void ensurePlayerToMove(String playerName) throws ChessException {
		if (!playerName.equals(getPlayerToMove())) {
			throw new ChessException(Messages.getString("Game.notYourTurn")); //$NON-NLS-1$
		}
	}

	public void ensureGameState(GameState state) throws ChessException {
		if (getState() != state) {
			throw new ChessException(Messages.getString("Game.shouldBeState", state)); //$NON-NLS-1$
		}
	}

	private boolean canAffordToPlay(String playerName) {
		if (isAIPlayer(playerName)) {
			return true;
		}
		return stake <= 0.0 || !ChessEconomy.active() || ChessEconomy.canAfford(playerName, stake);
	}

	/*--------------------------------------------------------------------------------*/
	public static void addGame(String gameName, Game game) {
		if (game != null && !chessGames.containsKey(gameName)) {
			chessGames.put(gameName, game);
		}
		if (ChessCraft.getSMS() != null) {
			SMSIntegration.gameCreated(game);
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
		if (ChessCraft.getSMS() != null) {
			SMSIntegration.gameDeleted(game);
		}
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
				// TODO: if multiple matches, check if only one is waiting for
				// more players
				// (and return that one)
			}
			throw new ChessException(Messages.getString("Game.noSuchGame", name)); //$NON-NLS-1$
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
			throw new ChessException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
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
			res = base + "-" + n++; //$NON-NLS-1$
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

	/**
	 * Make a note that the AI has made its move.  This can be acted upon by the 
	 * periodic ticker task.  We can't just do the move directly, because that would
	 * lead to making non-thread-safe Bukkit/Minecraft calls from the AI thread.
	 * 
	 * @param fromSqi
	 * @param toSqi
	 */
	void aiHasMoved(int fromSqi, int toSqi) {
		aiHasMoved = true;
		aiFromSqi = fromSqi;
		aiToSqi = toSqi;
	}
	
	/**
	 * If it's been noted that the AI has moved in its game model, make the
	 * actual move in our game model too.
	 */
	private void checkForAIMove() {
		if (aiHasMoved) {
			try {
				doMove(aiPlayer.getName(), aiToSqi, aiFromSqi);
				aiHasMoved = false;
			} catch (IllegalMoveException e) {
				alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
			} catch (ChessException e) {
				alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
			}
		}
	}
}

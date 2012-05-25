package me.desht.chesscraft.chess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessPersistable;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.SMSIntegration;
import me.desht.chesscraft.chess.ChessAI.AI_Def;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectDrawResponse;
import me.desht.chesscraft.expector.ExpectSwapResponse;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.Duration;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.pgn.PGN;
import chesspresso.pgn.PGNWriter;
import chesspresso.position.Position;

/**
 * @author des
 *
 */
public class ChessGame implements ConfigurationSerializable, ChessPersistable {
	private static final Map<String, ChessGame> chessGames = new HashMap<String, ChessGame>();
	private static final Map<String, ChessGame> currentGame = new HashMap<String, ChessGame>();

	private final String name;
	private final BoardView view;
	private final chesspresso.game.Game cpGame;

	private String playerWhite, playerBlack;
	private int promotionPiece[] = {Chess.QUEEN, Chess.QUEEN};
	private String invited;
	private GameState state;
	private int fromSquare;
	private long created, started, finished, lastMoved;
	private TimeControl tcWhite, tcBlack;
	private List<Short> history;
	private int result;
	private double stake;
	private ChessAI aiPlayer = null;
	private ChessAI aiPlayer2 = null; // for testing AI vs AI - will always play Black
	private int[] tcWarned = new int[2];

	/**
	 * Constructor: Creating a new Chess game.
	 * 
	 * @param name	Name of the game
	 * @param view	The board on which to create the game
	 * @param playerName	Name of the player who is setting up the game
	 * @throws ChessException	If the game can't be created for any reason
	 */
	public ChessGame(String name, BoardView view, String playerName) throws ChessException {
		this.view = view;
		this.name = name;
		if (view.getGame() != null) {
			throw new ChessException(Messages.getString("Game.boardAlreadyHasGame")); //$NON-NLS-1$
		}
		if (view.isDesigning()) {
			throw new ChessException(Messages.getString("Game.boardInDesignMode")); //$NON-NLS-1$
		}
		playerWhite = playerName == null ? "" : playerName; //$NON-NLS-1$
		playerBlack = ""; //$NON-NLS-1$
		state = GameState.SETTING_UP;
		fromSquare = Chess.NO_SQUARE;
		invited = ""; //$NON-NLS-1$
		history = new ArrayList<Short>();
		setTimeControl(ChessCraft.getInstance().getConfig().getString("time_control.default"));
		created = System.currentTimeMillis();
		started = finished = 0L;
		result = Chess.RES_NOT_FINISHED;
		if (playerName != null) {
			double defBalance = ChessCraft.economy == null ? 0.0 : ChessCraft.economy.getBalance(playerName);
			stake = Math.min(ChessCraft.getInstance().getConfig().getDouble("stake.default"), defBalance); //$NON-NLS-1$
		} else {
			stake = 0.0;
		}

		cpGame = setupChesspressoGame();

		view.setGame(this);
	}

	/**
	 * Constructor: Restoring a saved Chess game.
	 * 
	 * @param map	Saved game data
	 * @throws ChessException	If the game can't be created for any reason
	 * @throws IllegalMoveException	If the game data contains an illegal Chess move
	 */
	public ChessGame(ConfigurationSection map) throws ChessException, IllegalMoveException {
		view = BoardView.getBoardView(map.getString("boardview"));
		if (view.getGame() != null) {
			throw new ChessException(Messages.getString("Game.boardAlreadyHasGame")); //$NON-NLS-1$
		}
		if (view.isDesigning()) {
			throw new ChessException(Messages.getString("Game.boardInDesignMode")); //$NON-NLS-1$
		}

		name = map.getString("name");
		playerWhite = map.getString("playerWhite"); //$NON-NLS-1$
		playerBlack = map.getString("playerBlack"); //$NON-NLS-1$
		state = GameState.valueOf(map.getString("state")); //$NON-NLS-1$
		fromSquare = Chess.NO_SQUARE;
		invited = map.getString("invited"); //$NON-NLS-1$
		List<Integer> hTmp = map.getIntegerList("moves"); //$NON-NLS-1$
		history = new ArrayList<Short>();
		for (int m : hTmp) {
			history.add((short) m);
		}
		if (map.contains("timeWhite")) {
			tcWhite = new TimeControl(map.isLong("timeWhite") ? map.getLong("timeWhite") : map.getInt("timeWhite"));
			tcBlack = new TimeControl(map.isLong("timeBlack") ? map.getLong("timeBlack") : map.getInt("timeBlack"));
		} else {
			tcWhite = (TimeControl) map.get("tcWhite");
			tcBlack = (TimeControl) map.get("tcBlack");
		}
		created = map.getLong("created", System.currentTimeMillis()); //$NON-NLS-1$
		started = map.getLong("started"); //$NON-NLS-1$
		finished = map.getLong("finished", state == GameState.FINISHED ? System.currentTimeMillis() : 0);
		lastMoved = map.getLong("lastMoved", System.currentTimeMillis());
		result = map.getInt("result"); //$NON-NLS-1$
		promotionPiece[Chess.WHITE] = map.getInt("promotionWhite"); //$NON-NLS-1$
		promotionPiece[Chess.BLACK] = map.getInt("promotionBlack"); //$NON-NLS-1$
		stake = map.getDouble("stake", 0.0); //$NON-NLS-1$

		if (ChessAI.isAIPlayer(playerWhite)) {
			aiPlayer = ChessAI.getNewAI(this, playerWhite, true, true);
			playerWhite = aiPlayer.getName();
			if (ChessAI.isAIPlayer(playerBlack)) {
				// AI vs AI
				aiPlayer2 = ChessAI.getNewAI(this, playerBlack, true, false);
				playerBlack = aiPlayer2.getName();
			}
		} else if (ChessAI.isAIPlayer(playerBlack)) {
			aiPlayer = ChessAI.getNewAI(this, playerBlack, true, false);
			playerBlack = aiPlayer.getName();
		}

		cpGame = setupChesspressoGame();

		replayMoves();

		view.setGame(this);
	}

	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name); //$NON-NLS-1$
		map.put("boardview", view.getName()); //$NON-NLS-1$
		map.put("playerWhite", playerWhite); //$NON-NLS-1$
		map.put("playerBlack", playerBlack); //$NON-NLS-1$
		map.put("state", state.toString()); //$NON-NLS-1$
		map.put("invited", invited); //$NON-NLS-1$
		map.put("moves", history); //$NON-NLS-1$
		map.put("created", created); //$NON-NLS-1$
		map.put("started", started); //$NON-NLS-1$
		map.put("finished", finished); //$NON-NLS-1$
		map.put("lastMoved", lastMoved); //$NON-NLS-1$
		map.put("result", result); //$NON-NLS-1$
		map.put("promotionWhite", promotionPiece[Chess.WHITE]); //$NON-NLS-1$
		map.put("promotionBlack", promotionPiece[Chess.BLACK]); //$NON-NLS-1$
		map.put("tcWhite", tcWhite); //$NON-NLS-1$
		map.put("tcBlack", tcBlack); //$NON-NLS-1$
		map.put("stake", stake); //$NON-NLS-1$

		return map;
	}

	public static ChessGame deserialize(Map <String,Object> map) throws ChessException, IllegalMoveException {
		Configuration conf = new MemoryConfiguration();
		for (Entry<String, Object> e : map.entrySet()) {
			conf.set(e.getKey(), e.getValue());
		}
		return new ChessGame(conf);
	}

	public String getName() {
		return name;
	}

	@Override
	public File getSaveDirectory() {
		return DirectoryStructure.getGamesPersistDirectory();
	}

	/**
	 * Replay the move history to restore the saved board position.  We do this
	 * instead of just saving the position so that the Chesspresso ChessGame model
	 * includes a history of the moves, suitable for creating a PGN file.
	 * 
	 * @throws IllegalMoveException
	 */
	private void replayMoves() throws IllegalMoveException {
		// load moves into the Chesspresso model
		for (short move : history) {
			getPosition().doMove(move);
		}
		// repeat for the jChecs AI model(s)
		if (aiPlayer != null) {
			aiPlayer.replayMoves(history);
		}
		if (aiPlayer2 != null) {
			aiPlayer2.replayMoves(history);
		}
		// note; could still be problematic.. ai vs ai doesn't load correctly

		// set chess clock activity appropriately
		tcWhite.setActive(getPosition().getToPlay() == Chess.WHITE);
		tcBlack.setActive(!tcWhite.isActive());

		// now check for if AI needs to start
		if (getPosition().getToPlay() == Chess.WHITE && ChessAI.isAIPlayer(playerWhite)) {
			aiPlayer.setUserToMove(false); // tell ai to start thinking
		} else if (getPosition().getToPlay() == Chess.BLACK && ChessAI.isAIPlayer(playerBlack)) {
			if (ChessAI.isAIPlayer(playerWhite)) {
				aiPlayer2.setUserToMove(false);
			} else {
				aiPlayer.setUserToMove(false);
			}
		}
	}

	private chesspresso.game.Game setupChesspressoGame() {
		chesspresso.game.Game cpg = new chesspresso.game.Game();

		// seven tag roster
		cpg.setTag(PGN.TAG_EVENT, getName());
		cpg.setTag(PGN.TAG_SITE, getView().getName() + Messages.getString("Game.sitePGN")); //$NON-NLS-1$
		cpg.setTag(PGN.TAG_DATE, ChessUtils.dateToPGNDate(started));
		cpg.setTag(PGN.TAG_ROUND, "?"); //$NON-NLS-1$
		cpg.setTag(PGN.TAG_WHITE, getPlayerWhite());
		cpg.setTag(PGN.TAG_BLACK, getPlayerBlack());
		cpg.setTag(PGN.TAG_RESULT, getPGNResult());

		// extra tags
		cpg.setTag(PGN.TAG_FEN, Position.createInitialPosition().getFEN());
		return cpg;
	}

	public void save() {
		ChessCraft.getPersistenceHandler().savePersistable("game", this);
	}

	public void autoSave() {
		if (ChessCraft.getInstance().getConfig().getBoolean("autosave")) { //$NON-NLS-1$
			save();
		}
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
				aiPlayer.delete();
				aiPlayer = null;
			}
			if (aiPlayer2 != null) {
				aiPlayer2.delete();
				aiPlayer2 = null;
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

	public long getCreated() {
		return created;
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
		return name.equalsIgnoreCase(playerWhite) ? playerBlack : playerWhite;
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

	public TimeControl getTcWhite() {
		return tcWhite;
	}

	public TimeControl getTcBlack() {
		return tcBlack;
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

	/**
	 * Housekeeping task, called every <tick_interval> seconds as a scheduled sync task.
	 */
	public void clockTick() {
		if (state != GameState.RUNNING) {
			return;
		}
		checkForAIActivity();
		updateChessClocks(false);
	}

	private void updateChessClocks(boolean updateBoth) {
		updateChessClock(Chess.WHITE, tcWhite, updateBoth);
		updateChessClock(Chess.BLACK, tcBlack, updateBoth);
	}

	/**
	 * Update one chess clock.
	 * 
	 * @param colour	Colour of the player's clock
	 * @param tc		The clock to update
	 */
	private void updateChessClock(int colour, TimeControl tc, boolean updateBoth) {
		tc.tick();

		if (tc.isActive() || updateBoth) {
			getView().getControlPanel().updateClock(colour, tc);
			String playerName = colour == Chess.WHITE ? playerWhite : playerBlack;
			if (tc.getRemainingTime() <= 0) {
				try {
					winByDefault(getOtherPlayer(playerName));
				} catch (ChessException e) {
					LogUtils.severe("unexpected exception: " + e.getMessage(), e);
				}
			} else if (needToWarn(tc, colour)) {
				alert(playerName, Messages.getString("Game.timeControlWarning", tc.getRemainingTime() / 1000 + 1));
				tcWarned[colour]++;
			}
		}
	}

	private boolean needToWarn(TimeControl tc, int colour) {
		long remaining = tc.getRemainingTime();
		long t = ChessCraft.getInstance().getConfig().getInt("time_control.warn_seconds") * 1000;
		long tot = tc.getTotalTime();
		long warning = Math.min(t, tot) >>> tcWarned[colour];

		int tickInt = (ChessCraft.getInstance().getConfig().getInt("tick_interval") * 1000) + 50;	// fudge for inaccuracy of tick timer
		return remaining <= warning && remaining > warning - tickInt;
	}

	public void setTimeControl(String spec) throws ChessException {
		ensureGameState(GameState.SETTING_UP);
		try {
			tcWhite = new TimeControl(spec);
			tcBlack = new TimeControl(spec);
		} catch (IllegalArgumentException e) {
			throw new ChessException(e.getMessage());
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

		if (ChessAI.isAIPlayer(playerName)) {
			addAIPlayer(playerName);
		} else {
			if (!invited.equals("*") && !invited.equalsIgnoreCase(playerName)) { //$NON-NLS-1$
				throw new ChessException(Messages.getString("Game.notInvited")); //$NON-NLS-1$
			}
			if (ChessCraft.economy != null && !ChessCraft.economy.has(playerName, getStake())) {
				throw new ChessException(Messages.getString("Game.cantAffordToJoin", ChessUtils.formatStakeStr(getStake()))); //$NON-NLS-1$
			}
			if (playerBlack.isEmpty()) {
				playerBlack = playerName;
			} else { // if (playerWhite.isEmpty()) {
				playerWhite = playerName;
			}
		}
		getView().getControlPanel().repaintSignButtons();
		alert(otherPlayer, Messages.getString("Game.playerJoined", playerName)); //$NON-NLS-1$
		clearInvitation();

		if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
			if (ChessCraft.getInstance().getConfig().getBoolean("autostart", true)) {
				start(playerName);
			} else {
				alert(Messages.getString("Game.startPrompt")); //$NON-NLS-1$
			}
		}
	}

	private void addAIPlayer(String aiName) throws ChessException {
		if (playerWhite.isEmpty()) {
			aiPlayer = ChessAI.getNewAI(this, aiName, false, true);
			playerWhite = aiPlayer.getName();
		} else if (playerBlack.isEmpty()) {
			aiPlayer = ChessAI.getNewAI(this, aiName, false, false);
			playerBlack = aiPlayer.getName();
		}
	}

	public void invitePlayer(String inviterName, String inviteeName) throws ChessException {
		inviteSanityCheck(inviterName);

		if (inviteeName == null) {
			inviteOpen(inviterName);
			return;
		}

		// Partial name matching is already handled by getPlayer()...
		Player player = Bukkit.getServer().getPlayer(inviteeName);
		if (player == null) {
			ChessAI.AI_Def ai = ChessAI.getAIDefinition(inviteeName);
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
			alert(player, Messages.getString("Game.youAreInvited", inviterName)); //$NON-NLS-1$ 
			if (ChessCraft.economy != null && getStake() > 0.0) {
				alert(player, Messages.getString("Game.gameHasStake", ChessUtils.formatStakeStr(getStake()))); //$NON-NLS-1$
			}
			alert(player, Messages.getString("Game.joinPrompt")); //$NON-NLS-1$
			if (!invited.isEmpty()) {
				alert(invited, Messages.getString("Game.inviteWithdrawn")); //$NON-NLS-1$
			}
			invited = inviteeName;
			alert(inviterName, Messages.getString("Game.inviteSent", invited)); //$NON-NLS-1$
		}
	}

	public void inviteOpen(String inviterName) throws ChessException {
		inviteSanityCheck(inviterName);
		MiscUtil.broadcastMessage((Messages.getString("Game.openInviteCreated", inviterName))); //$NON-NLS-1$
		if (ChessCraft.economy != null && getStake() > 0.0) {
			MiscUtil.broadcastMessage(Messages.getString("Game.gameHasStake", ChessUtils.formatStakeStr(getStake()))); //$NON-NLS-1$
		}
		MiscUtil.broadcastMessage(Messages.getString("Game.joinPromptGlobal", getName())); //$NON-NLS-1$ 
		invited = "*"; //$NON-NLS-1$
	}

	private void inviteSanityCheck(String inviterName) throws ChessException {
		ensurePlayerInGame(inviterName);
		ensureGameState(GameState.SETTING_UP);

		if (!playerWhite.isEmpty() && !playerBlack.isEmpty()) {
			// if one is an AI, allow the AI to leave
			if (aiPlayer != null) {
				if (ChessAI.isAIPlayer(playerWhite)) {
					playerWhite = ""; //$NON-NLS-1$
				} else {
					playerBlack = ""; //$NON-NLS-1$
				}
				aiPlayer.delete();
			} else {
				throw new ChessException(Messages.getString("Game.gameIsFull")); //$NON-NLS-1$
			}
		}
	}

	public void clearInvitation() {
		invited = ""; //$NON-NLS-1$
	}

	/**
	 * Start the game.
	 * 
	 * @param playerName	Player who is starting the game
	 * @throws ChessException	if anything goes wrong
	 */
	public void start(String playerName) throws ChessException {
		ensurePlayerInGame(playerName);
		ensureGameState(GameState.SETTING_UP);

		String whiteStr = Messages.getString("Game.white");
		String blackStr = Messages.getString("Game.black");

		if (playerWhite.isEmpty() || playerBlack.isEmpty()) {
			addAIPlayer(null);
			alert(playerName, Messages.getString("Game.playerJoined", aiPlayer.getName())); //$NON-NLS-1$
		}

		if (!canAffordToPlay(playerWhite)) {
			throw new ChessException(Messages.getString("Game.cantAffordToStart", whiteStr, ChessUtils.formatStakeStr(stake))); //$NON-NLS-1$
		}
		if (!canAffordToPlay(playerBlack)) {
			throw new ChessException(Messages.getString("Game.cantAffordToStart", blackStr, ChessUtils.formatStakeStr(stake))); //$NON-NLS-1$
		}

		if (ChessCraft.getInstance().getConfig().getBoolean("auto_teleport_on_join")) {
			summonPlayers();
		}

		String wand = ChessUtils.getWandDescription();
		alert(playerWhite, Messages.getString("Game.started", whiteStr, wand)); //$NON-NLS-1$
		alert(playerBlack, Messages.getString("Game.started", blackStr, wand)); //$NON-NLS-1$

		if (ChessCraft.economy != null && stake > 0.0f && !playerWhite.equalsIgnoreCase(playerBlack)) {
			if (!ChessAI.isAIPlayer(playerWhite)) {
				ChessCraft.economy.withdrawPlayer(playerWhite, stake);
			}
			if (!ChessAI.isAIPlayer(playerBlack)) {
				ChessCraft.economy.withdrawPlayer(playerBlack, stake);
			}
			alert(Messages.getString("Game.paidStake", ChessUtils.formatStakeStr(stake))); //$NON-NLS-1$ 
		}

		if (ChessAI.isAIPlayer(playerWhite)) {
			aiPlayer.setUserToMove(false); // tell AI to start thinking
		}
		
		clearInvitation();
		started = lastMoved = System.currentTimeMillis();
		tcWhite.setActive(true);
		setState(GameState.RUNNING);

		autoSave();
	}

	public void summonPlayers() throws ChessException {
		summonPlayer(Bukkit.getServer().getPlayer(getPlayerWhite()));
		summonPlayer(Bukkit.getServer().getPlayer(getPlayerBlack()));
	}

	public void summonPlayer(Player player) throws ChessException {
		if (player == null || ChessAI.isAIPlayer(player.getName())) {
			return;
		}

		getView().summonPlayer(player);
		
		ChessGame.setCurrentGame(player.getName(), this);
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

	public void drawn(GameResult res) throws ChessException {
		ensureGameState(GameState.RUNNING);

		setState(GameState.FINISHED);
		result = Chess.RES_DRAW;
		cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2"); //$NON-NLS-1$
		announceResult(playerWhite, playerBlack, res);
	}

	public void drawn() throws ChessException {
		drawn(GameResult.DrawAgreed);
	}

	/**
	 * Do a move for playerName to toSquare; fromSquare should already be set
	 * either from the command-line, or by clicking a piece.
	 * 
	 * @param playerName
	 * @param toSquare
	 * @throws IllegalMoveException
	 * @throws ChessException
	 */
	public void doMove(String playerName, int toSquare) throws IllegalMoveException, ChessException {
		doMove(playerName, toSquare, fromSquare);
	}

	/**
	 * Do a move for player from fromSquare to toSquare.
	 * 
	 * @param playerName
	 * @param toSquare
	 * @param fromSquare
	 * @throws IllegalMoveException
	 * @throws ChessException
	 */
	public void doMove(String playerName, int toSquare, int fromSquare) throws IllegalMoveException, ChessException {
		ensureGameState(GameState.RUNNING);
		ensurePlayerToMove(playerName);
		if (fromSquare == Chess.NO_SQUARE) {
			return;
		}

		Boolean isCapturing = getPosition().getPiece(toSquare) != Chess.NO_PIECE;

		int prevToMove = getPosition().getToPlay();
		short move = Move.getRegularMove(fromSquare, toSquare, isCapturing);
		short realMove = validateMove(move);

		// At this point we know the move is a valid move, so go ahead and make the necessary changes...

		getPosition().doMove(realMove);	// the board view will repaint itself at this point
		lastMoved = System.currentTimeMillis();
		history.add(realMove);
		toggleChessClocks();
		autoSave();
		this.fromSquare = Chess.NO_SQUARE;

		Player player = Bukkit.getPlayer(playerName);
		if (player != null) {
			// making a move after a draw or swap offer has been made is equivalent to declining the offer
			if (ChessCraft.getResponseHandler().isExpecting(player, ExpectDrawResponse.class)) {
				MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedDrawOffer")); //$NON-NLS-1$
				ChessCraft.getResponseHandler().cancelAction(player, ExpectDrawResponse.class);
			}
			if (ChessCraft.getResponseHandler().isExpecting(player, ExpectSwapResponse.class)) {
				MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedSwapOffer")); //$NON-NLS-1$
				ChessCraft.getResponseHandler().cancelAction(player, ExpectSwapResponse.class);
			}
		}

		if (checkForFinishingPosition())
			return;

		// the game continues...
		getView().playMovedAlert(playerName);
		String nextPlayer = getPlayerToMove();		
		if (ChessAI.isAIPlayer(nextPlayer)) {
			if (nextPlayer.equals(playerBlack) && ChessAI.isAIPlayer(playerWhite)) {
				// AI vs. AI
				aiPlayer2.userHasMoved(fromSquare, toSquare);
			} else {
				aiPlayer.userHasMoved(fromSquare, toSquare);
			}
		} else {
			alert(nextPlayer, Messages.getString("Game.playerPlayedMove", //$NON-NLS-1$
			                                     ChessUtils.getColour(prevToMove),
			                                     getPosition().getLastMove().getLAN()));
			if (getPosition().isCheck()) {
				getView().playCheckAlert(nextPlayer);
				alert(nextPlayer, Messages.getString("Game.check"));
			} else {
				getView().playMovedAlert(nextPlayer);
			}
		}
	}

	/**
	 * Check the current game position to see if the game is over.
	 * 
	 * @return	true if game is over, false if not
	 */
	private boolean checkForFinishingPosition() {
		if (getPosition().isMate()) {
			cpGame.setTag(PGN.TAG_RESULT, getPosition().getToPlay() == Chess.WHITE ? "0-1" : "1-0"); //$NON-NLS-1$ //$NON-NLS-2$
			result = getPosition().getToPlay() == Chess.WHITE ? Chess.RES_BLACK_WINS : Chess.RES_WHITE_WINS;
			setState(GameState.FINISHED);
			announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.Checkmate);
			return true;
		} else if (getPosition().isStaleMate()) {
			result = Chess.RES_DRAW;
			cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2"); //$NON-NLS-1$
			setState(GameState.FINISHED);
			announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.Stalemate);
			return true;
		} else if (getPosition().getHalfMoveClock() >= 50) {
			result = Chess.RES_DRAW;
			cpGame.setTag(PGN.TAG_RESULT, "1/2-1/2"); //$NON-NLS-1$
			setState(GameState.FINISHED);
			announceResult(getPlayerNotToMove(), getPlayerToMove(), GameResult.FiftyMoveRule);
			return true;
		}
		return false;
	}

	/**
	 * Check if the move is really allowed.  Also account for special cases:
	 * castling, en passant, pawn promotion
	 * 
	 * @param move	move to check
	 * @return 	move, if allowed
	 * @throws IllegalMoveException if not allowed
	 */
	private short validateMove(short move) throws IllegalMoveException {
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

	/**
	 * Handle chess clock switching when a move has been made.
	 */
	private void toggleChessClocks() {
		if (getPosition().getToPlay() == Chess.WHITE) {
			tcBlack.moveMade();
			if (tcBlack.isNewPhase()) {
				alert(playerBlack, Messages.getString("Game.newTimeControlPhase", tcBlack.phaseString()));
			}
			tcWhite.setActive(true);
		} else {
			tcWhite.moveMade();
			if (tcWhite.isNewPhase()) {
				alert(playerWhite, Messages.getString("Game.newTimeControlPhase", tcWhite.phaseString()));
			}
			tcBlack.setActive(true);
		}
		updateChessClocks(true);
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
		case Abandoned:
			msg = Messages.getString("Game.abandoned", p1, p2); //$NON-NLS-1$
			break;
		}
		if (ChessCraft.getInstance().getConfig().getBoolean("broadcast_results")
				&& !p1.equalsIgnoreCase(p2)) { //$NON-NLS-1$
			if (!msg.isEmpty()) {
				MiscUtil.broadcastMessage(msg);
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
		LogUtils.info(msg);
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
			if (!ChessAI.isAIPlayer(p1)) {
				double winnings;
				if (ChessAI.isAIPlayer(p2)) {
					AI_Def ai = ChessAI.getAIDefinition(p2);
					if (ai != null) {
						winnings = stake * (1.0 + ai.getPayoutMultiplier());
					} else {
						winnings = stake * 2.0;
						LogUtils.warning("couldn't retrieve AI definition for " + p2); //$NON-NLS-1$
					}
				} else {
					winnings = stake * 2.0;
				}
				ChessCraft.economy.depositPlayer(p1, winnings);
				alert(p1, Messages.getString("Game.youWon", ChessUtils.formatStakeStr(winnings))); //$NON-NLS-1$
			}
			alert(p2, Messages.getString("Game.lostStake", ChessUtils.formatStakeStr(stake))); //$NON-NLS-1$
		} else {
			// a draw
			if (!ChessAI.isAIPlayer(p1)) {
				ChessCraft.economy.depositPlayer(p1, stake);
			}
			if (!ChessAI.isAIPlayer(p2)) {
				ChessCraft.economy.depositPlayer(p2, stake);
			}
			alert(Messages.getString("Game.getStakeBack", ChessUtils.formatStakeStr(stake))); //$NON-NLS-1$
		}

		stake = 0.0;
	}

	/**
	 * Called when a game is permanently deleted.
	 */
	public void deletePermanently() {
		ChessCraft.getPersistenceHandler().unpersist(this);

		handlePayout(GameResult.Abandoned, playerWhite, playerBlack);

		getView().setGame(null);

		deleteCommon();
	}

	/**
	 * Called for a transitory deletion, where we expect the object to be
	 * shortly restored, e.g. server reload, plugin disable, /chess reload
	 * persist command
	 */
	public void deleteTemporary() {
		deleteCommon();
	}

	private void deleteCommon() {

		if (aiPlayer != null) {
			// this would normally happen when the game goes to state FINISHED,
			// but we could get here if the game is explicitly deleted
			aiPlayer.delete();
		}

		try {
			ChessGame.removeGame(getName());
		} catch (ChessException e) {
			LogUtils.warning(e.getMessage());
		}
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

	public void alert(Player player, String message) {
		MiscUtil.alertMessage(player, Messages.getString("Game.alertPrefix", getName()) + message); //$NON-NLS-1$
	}

	public void alert(String playerName, String message) {
		if (playerName.isEmpty() || ChessAI.isAIPlayer(playerName)) {
			return;
		}
		Player p = Bukkit.getServer().getPlayer(playerName);
		if (p != null) {
			alert(p, message);
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

	public boolean isPlayerInGame(String playerName) {
		return (playerName.equalsIgnoreCase(playerWhite) || playerName.equalsIgnoreCase(playerBlack));
	}

	public boolean isPlayerToMove(String playerName) {
		return playerName.equalsIgnoreCase(getPlayerToMove());
	}

	public File writePGN(boolean force) throws ChessException {
		File f = makePGNFile();
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

	private File makePGNFile() {
		String baseName = getName() + "_" + ChessUtils.dateToPGNDate(System.currentTimeMillis()); //$NON-NLS-1$

		int n = 1;
		File f;
		do {
			f = new File(DirectoryStructure.getPGNDirectory(), baseName + "_" + n + ".pgn"); //$NON-NLS-1$
			++n;
		} while (f.exists());

		return f;
	}

	public void setFen(String fen) {
		getPosition().set(new Position(fen));
		// manually overriding the position invalidates the move history
		getHistory().clear();
	}

	private int getNextPromotionPiece(int colour) {
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

	public void cyclePromotionPiece(String playerName) throws ChessException {
		int colour = playingAs(playerName);
		setPromotionPiece(playerName, getNextPromotionPiece(colour));
	}

	/**
	 * Check if a game needs to be auto-deleted: - ChessGame that has not been
	 * started after a certain duration - ChessGame that has been finished for a
	 * certain duration
	 */
	public void checkForAutoDelete() {
		boolean mustDelete = false;
		String alertStr = null;

		if (getState() == GameState.SETTING_UP) {
			long elapsed = System.currentTimeMillis() - created;
			Duration timeout = new Duration(ChessCraft.getInstance().getConfig().getString("auto_delete.not_started", "3 mins"));
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration() && (playerWhite.isEmpty() || playerBlack.isEmpty())) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteNotStarted", timeout); //$NON-NLS-1$
			}
		} else if (getState() == GameState.FINISHED) {
			long elapsed = System.currentTimeMillis() - finished;
			Duration timeout = new Duration(ChessCraft.getInstance().getConfig().getString("auto_delete.finished", "30 mins"));
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration()) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteFinished"); //$NON-NLS-1$
			}
		} else if (getState() == GameState.RUNNING) {
			long elapsed = System.currentTimeMillis() - lastMoved;
			Duration timeout = new Duration(ChessCraft.getInstance().getConfig().getString("auto_delete.running", "28 days"));
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration()) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteRunning", timeout); //$NON-NLS-1$
			}
		}

		if (mustDelete) {
			alert(alertStr);
			LogUtils.info(alertStr);
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
		if (ChessAI.isAIPlayer(playerName)) {
			return true;
		}
		return stake <= 0.0 || ChessCraft.economy == null || ChessCraft.economy.has(playerName, stake);
	}

	/**
	 * Check if the given player is allowed to delete this game
	 * 
	 * @param pl
	 * @return
	 */
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

	/**
	 * Return true if either player in this game is an AI player
	 * 
	 * @return
	 */
	public boolean isAIGame() {
		return ChessAI.isAIPlayer(playerWhite) || ChessAI.isAIPlayer(playerBlack);
	}

	/**
	 * Adjust the game's stake by the given amount.
	 * 
	 * @param playerName
	 * @param adjustment
	 * @throws ChessException
	 */
	public void adjustStake(double adjustment) throws ChessException {
		double newStake = getStake() + adjustment;
		if (newStake < 0.0)
			return;
		if (ChessCraft.economy != null) {
			String playerName = playerWhite.isEmpty() ? playerBlack : playerWhite;
			if (newStake > ChessCraft.economy.getBalance(playerName)) {
				newStake = ChessCraft.economy.getBalance(playerName);
			}
		}
		setStake(newStake);
	}

	/**
	 * Have the given player offer a draw.
	 * 
	 * @param playerName 	Name of the player making the offer
	 * 						(could be human or AI, not necessarily a valid Bukkit player)
	 * @throws ChessException
	 */
	public void offerDraw(String playerName) throws ChessException {
		Player player = Bukkit.getPlayer(playerName);
		// a null player is OK as long as the player name is in the game,
		// in which case it's probably an AI player
		ensurePlayerInGame(playerName);
		ensurePlayerToMove(playerName);
		ensureGameState(GameState.RUNNING);

		String otherPlayerName = getOtherPlayer(playerName);
		ChessCraft.getResponseHandler().expect(player, new ExpectDrawResponse(this, playerName, otherPlayerName));

		if (player != null) {
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.drawOfferedYou", otherPlayerName)); //$NON-NLS-1$
		}
		alert(otherPlayerName, Messages.getString("ChessCommandExecutor.drawOfferedOther", playerName)); //$NON-NLS-1$
		alert(otherPlayerName, Messages.getString("ChessCommandExecutor.typeYesOrNo")); //$NON-NLS-1$
		getView().getControlPanel().repaintSignButtons();
	}

	/**
	 * Have the given player offer to swap sides.
	 * 
	 * @param playerName	Name of the player making the offer
	 * 						(could be human or AI, not necessarily a valid Bukkit player)
	 * @throws ChessException
	 */
	public void offerSwap(String playerName) throws ChessException {
		Player player = Bukkit.getPlayer(playerName);
		ensurePlayerInGame(playerName);

		String otherPlayerName = getOtherPlayer(playerName);
		if (otherPlayerName.isEmpty()) {
			// no other player yet - just swap
			swapColours();
		} else {
			ChessCraft.getResponseHandler().expect(player, new ExpectSwapResponse(this, playerName, otherPlayerName));
			if (player != null) {
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.sideSwapOfferedYou", otherPlayerName)); //$NON-NLS-1$
			} 
			alert(otherPlayerName, Messages.getString("ChessCommandExecutor.sideSwapOfferedOther", playerName)); //$NON-NLS-1$ 
			alert(otherPlayerName, Messages.getString("ChessCommandExecutor.typeYesOrNo")); //$NON-NLS-1$
		}
		getView().getControlPanel().repaintSignButtons();
	}

	/**
	 * Display details for the game to the given player.
	 * 
	 * @param player
	 */
	public void showGameDetail(Player player) {
		String white = getPlayerWhite().isEmpty() ? "?" : getPlayerWhite(); //$NON-NLS-1$
		String black = getPlayerBlack().isEmpty() ? "?" : getPlayerBlack(); //$NON-NLS-1$

		String bullet = ChatColor.DARK_PURPLE + "* " + ChatColor.AQUA; //$NON-NLS-1$
		MessagePager pager = MessagePager.getPager(player).clear();
		pager.add(Messages.getString("ChessCommandExecutor.gameDetail.name", getName(), getState())); //$NON-NLS-1$ 
		pager.add(bullet + Messages.getString("ChessCommandExecutor.gameDetail.players", white, black, getView().getName())); //$NON-NLS-1$ 
		pager.add(bullet +  Messages.getString("ChessCommandExecutor.gameDetail.halfMoves", getHistory().size())); //$NON-NLS-1$
		if (ChessCraft.economy != null) {
			pager.add(bullet + Messages.getString("ChessCommandExecutor.gameDetail.stake", ChessUtils.formatStakeStr(getStake()))); //$NON-NLS-1$
		}
		pager.add(bullet + (getPosition().getToPlay() == Chess.WHITE ? 
				Messages.getString("ChessCommandExecutor.gameDetail.whiteToPlay") :  //$NON-NLS-1$
					Messages.getString("ChessCommandExecutor.gameDetail.blackToPlay"))); //$NON-NLS-1$

		pager.add(bullet + Messages.getString("ChessCommandExecutor.gameDetail.timeControlType", tcWhite.toString()));	//$NON-NLS-1$
		if (getState() == GameState.RUNNING) {
			pager.add(bullet + Messages.getString("ChessCommandExecutor.gameDetail.clock", tcWhite.getClockString(), tcBlack.getClockString()));	//$NON-NLS-1$
		}
		if (getInvited().equals("*")) { //$NON-NLS-1$
			pager.add(bullet + Messages.getString("ChessCommandExecutor.gameDetail.openInvitation")); //$NON-NLS-1$
		} else if (!getInvited().isEmpty()) {
			pager.add(bullet + Messages.getString("ChessCommandExecutor.gameDetail.invitation", getInvited())); //$NON-NLS-1$
		}
		pager.add(Messages.getString("ChessCommandExecutor.gameDetail.moveHistory")); //$NON-NLS-1$
		List<Short> h = getHistory();
		for (int i = 0; i < h.size(); i += 2) {
			StringBuilder sb = new StringBuilder(String.format("&f%1$d. &-", (i / 2) + 1)); //$NON-NLS-1$
			sb.append(Move.getString(h.get(i)));
			if (i < h.size() - 1) {
				sb.append("  ").append(Move.getString(h.get(i + 1))); //$NON-NLS-1$
			}
			pager.add(sb.toString());
		}

		pager.showPage();
	}

	/**
	 * If it's been noted that the AI has moved in its game model, make the actual
	 * move in our game model too.  Also check if the AI has failed and we need to abandon.
	 */
	private synchronized void checkForAIActivity() {
		checkAIPlayer(aiPlayer);
		checkAIPlayer(aiPlayer2);
	}
	
	private void checkAIPlayer(ChessAI ai) {
		if (ai == null) {
			return;
		}
		
		if (ai.hasFailed()) {
			// this will happen if the AI caught an exception and its state can't be guaranteed anymore
			try {
				drawn(GameResult.Abandoned);
			} catch (ChessException e) {
				// should never get here
				LogUtils.severe("Unexpected exception caught while trying to draw game - deleted", e);
				deletePermanently();
			}
		} else if (ai.getPendingMove() != null) {
			int from = ai.getPendingMove().getFrom().getIndex();
			int to = ai.getPendingMove().getTo().getIndex();
			ai.clearPendingMove();
			try {
				doMove(ai.getName(), to, from);
			} catch (IllegalMoveException e) {
				alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
				ai.setFailed(true);
			} catch (ChessException e) {
				alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
				ai.setFailed(true);
			}
		}
	}

	/*--------------------------------------------------------------------------------*/

	public static void addGame(String gameName, ChessGame game) throws ChessException {
		if (game != null) {
			if (!chessGames.containsKey(gameName)) {
				chessGames.put(gameName, game);
			} else {
				throw new ChessException("trying to register duplicate game " + gameName);
			}
		}
		if (ChessCraft.getSMS() != null) {
			SMSIntegration.gameCreated(game);
		}
	}

	public static void removeGame(String gameName) throws ChessException {
		ChessGame game = getGame(gameName);

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

	public static List<ChessGame> listGames(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(chessGames.keySet());
			List<ChessGame> res = new ArrayList<ChessGame>();
			for (String name : sorted) {
				res.add(chessGames.get(name));
			}
			return res;
		} else {
			return new ArrayList<ChessGame>(chessGames.values());
		}
	}

	public static List<ChessGame> listGames() {
		return listGames(false);
	}

	public static ChessGame getGame(String name) throws ChessException {
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
				// more players (and return that one)
			}
			throw new ChessException(Messages.getString("Game.noSuchGame", name)); //$NON-NLS-1$
		}
		return chessGames.get(name);
	}

	public static void setCurrentGame(String playerName, String gameName) throws ChessException {
		ChessGame game = getGame(gameName);
		setCurrentGame(playerName, game);
	}

	public static void setCurrentGame(String playerName, ChessGame game) {
		currentGame.put(playerName, game);
	}

	public static ChessGame getCurrentGame(Player player) throws ChessException {
		return getCurrentGame(player, false);
	}

	public static ChessGame getCurrentGame(Player player, boolean verify) throws ChessException {
		if (player == null) {
			return null;
		}
		ChessGame game = currentGame.get(player.getName());
		if (verify && game == null) {
			throw new ChessException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
		}
		return game;
	}

	public static Map<String, String> getCurrentGames() {
		Map<String, String> res = new HashMap<String, String>();
		for (String s : currentGame.keySet()) {
			ChessGame game = currentGame.get(s);
			if (game != null) {
				res.put(s, game.getName());
			}
		}
		return res;
	}

	public static String makeGameName(String playerName) {
		String res;
		int n = 1;
		do {
			res = playerName + "-" + n++; //$NON-NLS-1$
		} while (ChessGame.checkGame(res));

		return res;
	}

	/**
	 * Convenience method to create a new chess game.
	 * 
	 * @param player		The player who is creating the game
	 * @param gameName		Name of the game - may be null, in which case a name will be generated
	 * @param boardName		Name of the board for the game - may be null, in which case a free board will be picked
	 * @return	The game object
	 * @throws ChessException	if there is any problem creating the game
	 */
	public static ChessGame createGame(Player player, String gameName, String boardName) throws ChessException {
		BoardView bv;
		if (boardName == null) {
			bv = BoardView.getFreeBoard();
		} else {
			bv = BoardView.getBoardView(boardName);
		}

		String playerName = player.getName();

		if (gameName == null || gameName.equals("-")) {
			gameName = ChessGame.makeGameName(playerName);
		}

		ChessGame game = new ChessGame(gameName, bv, playerName);
		ChessGame.addGame(gameName, game);
		ChessGame.setCurrentGame(playerName, game);
		bv.getControlPanel().repaintSignButtons();

		game.autoSave();

		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.gameCreated", game.getName(), game.getView().getName())); //$NON-NLS-1$ 

		return game;
	}
}

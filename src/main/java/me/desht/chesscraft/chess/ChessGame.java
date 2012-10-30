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
import me.desht.chesscraft.chess.TimeControl.ControlType;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.player.AIChessPlayer;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.chess.player.HumanChessPlayer;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.event.ChessGameCreatedEvent;
import me.desht.chesscraft.event.ChessGameDeletedEvent;
import me.desht.chesscraft.event.ChessGameStateChangedEvent;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectUndoResponse;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.game.Game;
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
	private static final String OPEN_INVITATION = "*";
	private static final Map<String, ChessGame> chessGames = new HashMap<String, ChessGame>();
	private static final Map<String, ChessGame> currentGame = new HashMap<String, ChessGame>();

	private final String name;
	private final BoardView view;
	private final Game cpGame;
	private final int promotionPiece[] = {Chess.QUEEN, Chess.QUEEN};

	private ChessPlayer[] players = new ChessPlayer[2];
	private String invited;
	private GameState state;
	private int fromSquare;
	private long created, started, finished, lastMoved;
	private TimeControl tcWhite, tcBlack;
	private List<Short> history;
	private int result;
	private double stake;
	private final int[] tcWarned = new int[2];

	/**
	 * Constructor: Creating a new Chess game.
	 * 
	 * @param name	Name of the game
	 * @param view	The board on which to create the game
	 * @param playerName	Name of the player who is setting up the game
	 * @throws ChessException	If the game can't be created for any reason
	 */
	public ChessGame(String name, BoardView view, String playerName, int colour) throws ChessException {
		this.view = view;
		this.name = name;
		if (view.getGame() != null) {
			throw new ChessException(Messages.getString("Game.boardAlreadyHasGame")); //$NON-NLS-1$
		}
		if (view.isDesigning()) {
			throw new ChessException(Messages.getString("Game.boardInDesignMode")); //$NON-NLS-1$
		}
		players[Chess.WHITE] = players[Chess.BLACK] = null;
		players[colour] = new HumanChessPlayer(playerName, this, colour);
		state = GameState.SETTING_UP;
		fromSquare = Chess.NO_SQUARE;
		invited = ""; //$NON-NLS-1$
		history = new ArrayList<Short>();
		setTimeControl(view.getDefaultTcSpec());
		created = System.currentTimeMillis();
		started = finished = 0L;
		result = Chess.RES_NOT_FINISHED;
		if (playerName != null && ChessCraft.economy != null) {
			double playerBalance = ChessCraft.economy.getBalance(playerName);
			double defStake = view.getDefaultStake();
			if (view.getLockStake() && defStake > playerBalance) {
				throw new ChessException(Messages.getString("Game.cantAffordToJoin", defStake));
			}
			stake = Math.min(defStake, playerBalance);
		} else {
			stake = 0.0;
		}

		cpGame = setupChesspressoGame();

		view.setGame(this);
	}

	private ChessPlayer createPlayer(String name, int colour) {
		if (name == null) {
			String aiName = AIFactory.instance.getFreeAIName();
			return new AIChessPlayer(aiName, this, colour);
		} else if (AIFactory.isAIPlayer(name)) {
			return new AIChessPlayer(name, this, colour);
		} else if (name.isEmpty()) {
			// this could happen if a game is saved in the setting up phase
			return null;
		} else {
			return new HumanChessPlayer(name, this, colour);
		}
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
		players[Chess.WHITE] = createPlayer(map.getString("playerWhite"), Chess.WHITE);
		players[Chess.BLACK] = createPlayer(map.getString("playerBlack"), Chess.BLACK);
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

		cpGame = setupChesspressoGame();

		replayMoves();

		if (tcWhite.getControlType() != ControlType.NONE) {
			view.getControlPanel().getTcDefs().addCustomSpec(tcWhite.getSpec());
		}
		view.setGame(this);
	}

	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name); //$NON-NLS-1$
		map.put("boardview", view.getName()); //$NON-NLS-1$
		map.put("playerWhite", getWhitePlayerName());
		map.put("playerBlack", getBlackPlayerName());
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
		
		// load moves into the player's (possibly AI) game model
		if (players[Chess.WHITE] != null) players[Chess.WHITE].replayMoves();
		if (players[Chess.BLACK] != null) players[Chess.BLACK].replayMoves();
		
		// set chess clock activity appropriately
		tcWhite.setActive(getPosition().getToPlay() == Chess.WHITE);
		tcBlack.setActive(!tcWhite.isActive());
	}

	private Game setupChesspressoGame() {
		Game cpg = new Game();

		String site = getView().getName() + ", " + Bukkit.getServerName() + Messages.getString("Game.sitePGN");

		// seven tag roster
		cpg.setTag(PGN.TAG_EVENT, getName());
		cpg.setTag(PGN.TAG_SITE, site); //$NON-NLS-1$
		cpg.setTag(PGN.TAG_DATE, ChessUtils.dateToPGNDate(created));
		cpg.setTag(PGN.TAG_ROUND, "?"); //$NON-NLS-1$
		cpg.setTag(PGN.TAG_WHITE, getWhitePlayerName());
		cpg.setTag(PGN.TAG_BLACK, getBlackPlayerName());
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

	public Game getChesspressoGame() {
		return cpGame;
	}

	public Position getPosition() {
		return cpGame.getPosition();
	}

	public BoardView getView() {
		return view;
	}

	/**
	 * Return the player object for the given colour (Chess.WHITE or Chess.BLACK).
	 * 
	 * @param colour
	 * @return
	 */
	public ChessPlayer getPlayer(int colour) {
		return players[colour];
	}
	
	/**
	 * Get the name of the player for the given colour (Chess.WHITE or Chess.BLACK),
	 * or the empty string if there's no player of that colour (yet).
	 * 
	 * @param colour
	 * @return
	 */
	public String getPlayerName(int colour) {
		return players[colour] != null ? players[colour].getName() : "";
	}

	public String getWhitePlayerName() {
		return getPlayerName(Chess.WHITE);
	}

	public String getBlackPlayerName() {
		return getPlayerName(Chess.BLACK);
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
		}
		Bukkit.getPluginManager().callEvent(new ChessGameStateChangedEvent(this));
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

	public String getOtherPlayerName(String name) {
		return name.equalsIgnoreCase(getWhitePlayerName()) ? getBlackPlayerName() : getWhitePlayerName();
	}

	public double getStake() {
		return stake;
	}

	/**
	 * A player is trying to adjust the stake for this game.
	 * 
	 * @param playerName	Name of the player setting the stake
	 * @param newStake		The stake being set
	 * @throws ChessException	if the stake is out of range or not affordable or the game isn't in setup phase
	 */
	public void setStake(String playerName, double newStake) throws ChessException {
		if (ChessCraft.economy == null) {
			return;
		}

		ensureGameState(GameState.SETTING_UP);
		ensurePlayerInGame(playerName);

		if (getView().getLockStake()) {
			throw new ChessException(Messages.getString("Game.stakeLocked")); //$NON-NLS-1$
		}

		if (newStake < 0.0) {
			throw new ChessException(Messages.getString("Game.noNegativeStakes")); //$NON-NLS-1$
		}

		if (!ChessCraft.economy.has(playerName, newStake)) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.cantAffordStake"));	//$NON-NLS-1$
		}

		double max = ChessCraft.getInstance().getConfig().getDouble("stake.max");
		if (max >= 0.0 && newStake > max) {
			throw new ChessException(Messages.getString("Game.stakeTooHigh", max)); //$NON-NLS-1$
		}

		if (isFull()) {
			throw new ChessException(Messages.getString("Game.stakeCantBeChanged")); //$NON-NLS-1$
		}

		this.stake = newStake;
	}

	/**
	 * Adjust the game's stake by the given amount.
	 * 
	 * @param playerName	Name of the player adjusting the stake
	 * @param adjustment	amount to adjust by (may be negative)
	 * @throws ChessException	if the new stake is out of range or not affordable or the game isn't in setup phase
	 */
	public void adjustStake(String playerName, double adjustment) throws ChessException {
		if (ChessCraft.economy == null) {
			return;
		}

		double newStake = getStake() + adjustment;
		double max = ChessCraft.getInstance().getConfig().getDouble("stake.max");

		if (max >= 0.0 && newStake > max && adjustment < 0.0) {
			// allow stake to be adjusted down without throwing an exception
			// could happen if global max stake was changed to something lower than
			// a game's current stake setting
			newStake = Math.min(max, ChessCraft.economy.getBalance(playerName));
		}
		if (!ChessCraft.economy.has(playerName, newStake) && adjustment < 0.0) {
			// similarly for the player's own balance
			newStake = Math.min(max, ChessCraft.economy.getBalance(playerName));
		}

		setStake(playerName, newStake);
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
		if (playerName.equals(getWhitePlayerName())) {
			promotionPiece[Chess.WHITE] = piece;
		} else if (playerName.equals(getBlackPlayerName())) {
			promotionPiece[Chess.BLACK] = piece;
		}
	}

	/**
	 * Housekeeping task, called periodically by the scheduler.  Update the clocks for the game, and
	 * check for any pending AI moves.
	 */
	public void clockTick() {
		if (state != GameState.RUNNING) {
			return;
		}
		checkForAIActivity();
		updateChessClocks(false);
	}

	private void updateChessClocks(boolean force) {
		updateChessClock(Chess.WHITE, tcWhite, force);
		updateChessClock(Chess.BLACK, tcBlack, force);
	}

	/**
	 * Update one chess clock.
	 * 
	 * @param colour	Colour of the player's clock
	 * @param tc		The clock to update
	 * @param force		Force an update even if the clock is not active
	 */
	private void updateChessClock(int colour, TimeControl tc, boolean force) {
		tc.tick();

		if (tc.isActive() || force) {
			getView().getControlPanel().updateClock(colour, tc);
			String playerName = players[colour].getName();
			if (tc.getRemainingTime() <= 0) {
				try {
					winByDefault(getOtherPlayerName(playerName));
				} catch (ChessException e) {
					LogUtils.severe("unexpected exception: " + e.getMessage(), e);
				}
			} else if (needToWarn(tc, colour)) {
				players[colour].alert(Messages.getString("Game.timeControlWarning", tc.getRemainingTime() / 1000 + 1));
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

	public void setTimeControl(String spec) {
		ensureGameState(GameState.SETTING_UP);
		if (view.getLockTcSpec() && tcWhite != null) {
			throw new ChessException(Messages.getString("Game.timeControlLocked"));
		}
		try {
			tcWhite = new TimeControl(spec);
			tcBlack = new TimeControl(spec);
		} catch (IllegalArgumentException e) {
			throw new ChessException(e.getMessage());
		}
	}

	public void swapColours() {
		clockTick();
		
		ChessPlayer tmp = players[Chess.WHITE];
		players[Chess.WHITE] = players[Chess.BLACK];
		players[Chess.BLACK] = tmp;
		
		players[Chess.WHITE].setColour(Chess.WHITE);
		players[Chess.BLACK].setColour(Chess.BLACK);
		
		players[Chess.WHITE].alert(Messages.getString("Game.nowPlayingWhite")); //$NON-NLS-1$
		players[Chess.BLACK].alert(Messages.getString("Game.nowPlayingBlack")); //$NON-NLS-1$
	}

	public boolean isFull() {
		return players[Chess.WHITE] != null && players[Chess.BLACK] != null;
	}
	
	/**
	 * Add the named player (might be an AI) to the game.
	 * 
	 * @param playerName
	 * @throws ChessException
	 */
	public void addPlayer(String playerName) throws ChessException {
		ensureGameState(GameState.SETTING_UP);
		
		if (isFull()) {
			// this could happen if autostart is disabled and two players have already joined
			throw new ChessException(Messages.getString("Game.gameIsFull")); //$NON-NLS-1$
		}
		
		fillEmptyPlayerSlot(playerName);
		
		getView().getControlPanel().repaintSignButtons();
		clearInvitation();

		if (isFull()) {
			if (ChessCraft.getInstance().getConfig().getBoolean("autostart", true)) {
				start(playerName);
			} else {
				alert(Messages.getString("Game.startPrompt")); //$NON-NLS-1$
			}
		}
	}
	
	private void fillEmptyPlayerSlot(String playerName) {
		int colour;
		if (players[Chess.WHITE] != null) {
			colour = Chess.BLACK;
		} else {
			colour = Chess.WHITE;
		}
		int otherColour = ChessUtils.otherColour(colour);
		players[colour] = createPlayer(playerName, colour);
		players[colour].validateInvited("Game.notInvited");
		players[colour].validateAffordability("Game.cantAffordToJoin");
		
		if (players[otherColour] != null)
			players[otherColour].alert(Messages.getString("Game.playerJoined", players[colour].getDisplayName()));
		
	}
	
	/**
	 * One player has just invited another player to this game.
	 * 
	 * @param inviterName
	 * @param inviteeName
	 * @throws ChessException
	 */
	public void invitePlayer(String inviterName, String inviteeName) throws ChessException {
		inviteSanityCheck(inviterName);

		if (inviteeName == null) {
			inviteOpen(inviterName);
			return;
		}

		// Partial name matching is already handled by getPlayer()...
		Player player = Bukkit.getServer().getPlayer(inviteeName);
		if (player != null) {
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
		} else {
			// no human by this name, try to add an AI of the given name
			addPlayer(AIFactory.AI_PREFIX + inviteeName);
		}
	}

	public void inviteOpen(String inviterName) throws ChessException {
		inviteSanityCheck(inviterName);
		MiscUtil.broadcastMessage((Messages.getString("Game.openInviteCreated", inviterName))); //$NON-NLS-1$
		if (ChessCraft.economy != null && getStake() > 0.0) {
			MiscUtil.broadcastMessage(Messages.getString("Game.gameHasStake", ChessUtils.formatStakeStr(getStake()))); //$NON-NLS-1$
		}
		MiscUtil.broadcastMessage(Messages.getString("Game.joinPromptGlobal", getName())); //$NON-NLS-1$ 
		invited = OPEN_INVITATION; //$NON-NLS-1$
	}

	private void inviteSanityCheck(String inviterName) throws ChessException {
		ensurePlayerInGame(inviterName);
		ensureGameState(GameState.SETTING_UP);

		if (isFull()) {
			// if one player is an AI, allow the AI to leave
			if (!players[Chess.WHITE].isHuman()) {
				players[Chess.WHITE].cleanup();
				players[Chess.WHITE] = null; 
			} else if (!players[Chess.BLACK].isHuman()) {
				players[Chess.BLACK].cleanup();
				players[Chess.BLACK] = null; 	
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

		if (!isFull()) {
			// game started with only one player - add an AI
			fillEmptyPlayerSlot(null);
		}

		cpGame.setTag(PGN.TAG_WHITE, getWhitePlayerName());
		cpGame.setTag(PGN.TAG_BLACK, getBlackPlayerName());

		// just in case stake.max got adjusted after game creation...
		double max = ChessCraft.getInstance().getConfig().getDouble("stake.max");
		if (max >= 0 && stake > max) {
			stake = max;
			view.getControlPanel().repaintSignButtons();
		}
		
		players[Chess.WHITE].validateAffordability("Game.cantAffordToStart");
		players[Chess.BLACK].validateAffordability("Game.cantAffordToStart");

		if (ChessCraft.getInstance().getConfig().getBoolean("auto_teleport_on_join")) {
			summonPlayers();
		}

		String wand = ChessUtils.getWandDescription();
		players[Chess.WHITE].alert(Messages.getString("Game.started", whiteStr, wand)); //$NON-NLS-1$
		players[Chess.BLACK].alert(Messages.getString("Game.started", blackStr, wand)); //$NON-NLS-1$

		if (stake > 0.0 && !getWhitePlayerName().equalsIgnoreCase(getBlackPlayerName())) {
			players[Chess.WHITE].withdrawFunds(stake);
			players[Chess.BLACK].withdrawFunds(stake);
		}
		
		clearInvitation();
		started = lastMoved = System.currentTimeMillis();
		tcWhite.setActive(true);
		setState(GameState.RUNNING);

		players[Chess.WHITE].promptForFirstMove();

		autoSave();
	}

	public void summonPlayers() throws ChessException {
		players[Chess.WHITE].summonToGame();
		players[Chess.BLACK].summonToGame();
	}

	public void resign(String playerName) throws ChessException {
		if (state != GameState.RUNNING) {
			throw new ChessException(Messages.getString("Game.notStarted")); //$NON-NLS-1$
		}

		ensurePlayerInGame(playerName);

		setState(GameState.FINISHED);
		String winner;
		String loser = playerName;
		if (loser.equalsIgnoreCase(getWhitePlayerName())) {
			winner = getBlackPlayerName();
			cpGame.setTag(PGN.TAG_RESULT, "0-1"); //$NON-NLS-1$
			result = Chess.RES_BLACK_WINS;
		} else {
			winner = getWhitePlayerName();
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
		if (winner.equalsIgnoreCase(getWhitePlayerName())) {
			loser = getBlackPlayerName();
			cpGame.setTag(PGN.TAG_RESULT, "1-0"); //$NON-NLS-1$
			result = Chess.RES_WHITE_WINS;
		} else {
			loser = getWhitePlayerName();
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
		announceResult(getWhitePlayerName(), getBlackPlayerName(), res);
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

		// At this point we know the move is valid, so go ahead and make the necessary changes...
		getPosition().doMove(realMove);	// the board view will repaint itself at this point
		lastMoved = System.currentTimeMillis();
		history.add(realMove);
		toggleChessClocks();
		autoSave();
		this.fromSquare = Chess.NO_SQUARE;

		players[prevToMove].cancelOffers();

		if (checkForFinishingPosition())
			return;

		// the game continues...
		players[getPosition().getToPlay()].promptForNextMove();
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
				players[Chess.BLACK].alert(Messages.getString("Game.newTimeControlPhase", tcBlack.phaseString()));
			}
			tcWhite.setActive(true);
		} else {
			tcWhite.moveMade();
			if (tcWhite.isNewPhase()) {
				players[Chess.WHITE].alert(Messages.getString("Game.newTimeControlPhase", tcWhite.phaseString()));
			}
			tcBlack.setActive(true);
		}
		updateChessClocks(true);
	}

	public String getPGNResult() {
		switch (result) {
		case Chess.RES_NOT_FINISHED:
			return OPEN_INVITATION; //$NON-NLS-1$
		case Chess.RES_WHITE_WINS:
			return "1-0"; //$NON-NLS-1$
		case Chess.RES_BLACK_WINS:
			return "0-1"; //$NON-NLS-1$
		case Chess.RES_DRAW:
			return "1/2-1/2"; //$NON-NLS-1$
		default:
			return OPEN_INVITATION; //$NON-NLS-1$
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

		handlePayout();
		if (Results.resultsHandlerOK()) {
			Results.getResultsHandler().logResult(this, rt);
		}
		//		LogUtils.info(msg);
	}

	private void handlePayout() {
		if (stake <= 0.0 || getWhitePlayerName().equalsIgnoreCase(getBlackPlayerName())) {
			return;
		}
		if (getState() == GameState.SETTING_UP) {
			return;
		}

		switch (result) {
		case Chess.RES_WHITE_WINS:
			winner(Chess.WHITE);
			break;
		case Chess.RES_BLACK_WINS:
			winner(Chess.BLACK);
			break;
		case Chess.RES_DRAW:
		case Chess.RES_NOT_FINISHED:
			players[Chess.WHITE].depositFunds(stake);
			players[Chess.BLACK].depositFunds(stake);
			alert(Messages.getString("Game.getStakeBack", ChessUtils.formatStakeStr(stake)));
		}

		stake = 0.0;
	}

	private void winner(int winner) {
		int loser = ChessUtils.otherColour(winner);
		double winnings = stake * players[loser].getPayoutMultiplier();
		players[winner].depositFunds(winnings);
		players[winner].alert(Messages.getString("Game.youWon", ChessUtils.formatStakeStr(winnings)));
		players[loser].alert(Messages.getString("Game.lostStake", ChessUtils.formatStakeStr(stake)));
	}

	/**
	 * Called when a game is permanently deleted.
	 */
	public void deletePermanently() {
		ChessCraft.getPersistenceHandler().unpersist(this);

		handlePayout();

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
		if (players[Chess.WHITE] != null) players[Chess.WHITE].cleanup();
		if (players[Chess.BLACK] != null) players[Chess.BLACK].cleanup();

		try {
			ChessGame.removeGame(getName());
		} catch (ChessException e) {
			LogUtils.warning(e.getMessage());
		}
	}

	public int getPlayerColour(String name) {
		if (name.equalsIgnoreCase(getWhitePlayerName())) {
			return Chess.WHITE;
		} else if (name.equalsIgnoreCase(getBlackPlayerName())) {
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
		if (playerName.isEmpty() || AIFactory.isAIPlayer(playerName)) {
			return;
		}
		Player p = Bukkit.getServer().getPlayer(playerName);
		if (p != null) {
			alert(p, message);
		}
	}

	public void alert(String message) {
		if (players[Chess.WHITE] != null) {
			players[Chess.WHITE].alert(message);
		}
		if (players[Chess.BLACK] != null && !getWhitePlayerName().equalsIgnoreCase(getBlackPlayerName())) {
			players[Chess.BLACK].alert(message);
		}
	}

	public String getPlayerToMove() {
		return players[getPosition().getToPlay()].getName();
	}

	public String getPlayerNotToMove() {
		return players[ChessUtils.otherColour(getPosition().getToPlay())].getName();
	}

	public boolean isPlayerInGame(String playerName) {
		return (playerName.equalsIgnoreCase(getWhitePlayerName()) || playerName.equalsIgnoreCase(getBlackPlayerName()));
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
		ensurePlayerInGame(playerName);
		int colour = getPlayerColour(playerName);
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
			if (timeout.getTotalDuration() > 0 && elapsed > timeout.getTotalDuration() && !isFull()) {
				mustDelete = true;
				alertStr = Messages.getString("Game.autoDeleteNotStarted", timeout); //$NON-NLS-1$
			}
		} else if (getState() == GameState.FINISHED) {
			long elapsed = System.currentTimeMillis() - finished;
			Duration timeout = new Duration(ChessCraft.getInstance().getConfig().getString("auto_delete.finished", "30 sec"));
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
		if (!playerName.equals(getWhitePlayerName()) && !playerName.equals(getBlackPlayerName())) {
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
	
	/**
	 * Check if the given player is allowed to delete this game
	 * 
	 * @param pl
	 * @return
	 */
	public boolean playerCanDelete(CommandSender pl) {
		if (pl instanceof ConsoleCommandSender) {
			return true;
		}
		String playerName = pl.getName();
		if (state == GameState.SETTING_UP) {
			String playerWhite = getWhitePlayerName();
			String playerBlack = getBlackPlayerName();
			if (!playerWhite.isEmpty() && playerBlack.isEmpty()) {
				return playerWhite.equalsIgnoreCase(playerName);
			} else if (playerWhite.isEmpty() && !playerBlack.isEmpty()) {
				return playerBlack.equalsIgnoreCase(playerName);
			} else if (playerWhite.equalsIgnoreCase(playerName)) {
				Player other = pl.getServer().getPlayer(playerBlack);
				return other == null || !other.isOnline();
			} else if (playerBlack.equalsIgnoreCase(playerName)) {
				Player other = pl.getServer().getPlayer(playerWhite);
				return other == null || !other.isOnline();
			}
		}
		return false;
	}

	/**
	 * Check if either player in this game is an AI player
	 * 
	 * @return true if either player is an AI, false otherwise
	 */
	public boolean isAIGame() {
		if (players[Chess.WHITE] != null && !players[Chess.WHITE].isHuman()) return true;
		if (players[Chess.BLACK] != null && !players[Chess.BLACK].isHuman()) return true;
		
		return false;
	}

	/**
	 * Have the given player offer a draw.
	 * 
	 * @param playerName 	Name of the player making the offer
	 * 						(could be human or AI, not necessarily a valid Bukkit player)
	 * @throws ChessException
	 */
	public void offerDraw(String playerName) throws ChessException {
		ensurePlayerInGame(playerName);
		ensurePlayerToMove(playerName);
		ensureGameState(GameState.RUNNING);

		int colour = getPlayerColour(playerName);
		int otherColour = ChessUtils.otherColour(colour);
		players[colour].statusMessage(Messages.getString("ChessCommandExecutor.drawOfferedYou", players[otherColour].getName()));
		players[otherColour].drawOffered();
		
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
		ensurePlayerInGame(playerName);

		String otherPlayerName = getOtherPlayerName(playerName);
		if (otherPlayerName.isEmpty()) {
			// no other player yet - just swap
			swapColours();
		} else {
			int colour = getPlayerColour(playerName);
			int otherColour = ChessUtils.otherColour(colour);
			players[colour].statusMessage(Messages.getString("ChessCommandExecutor.swapOfferedYou", players[otherColour].getName()));
			players[otherColour].swapOffered();
		}
		getView().getControlPanel().repaintSignButtons();
	}

	/**
	 * Have the given player offer to undo the last move they made.
	 * 
	 * @param playerName	Name of the player making the offer
	 * @throws ChessException
	 */
	public void offerUndoMove(String playerName) {
		ensurePlayerInGame(playerName);
		ensureGameState(GameState.RUNNING);

		int colour = getPlayerColour(playerName);
		int otherColour = ChessUtils.otherColour(colour);
		
		if (players[otherColour].isHuman()) {
			// playing another human - we need to ask them if it's OK to undo
			String otherPlayerName = players[otherColour].getName();
			ChessCraft.getInstance().responseHandler.expect(otherPlayerName, new ExpectUndoResponse(this, playerName, otherPlayerName));
			players[otherColour].alert(Messages.getString("ChessCommandExecutor.undoOfferedOther", playerName));
			players[otherColour].alert(Messages.getString("ChessCommandExecutor.typeYesOrNo"));
			players[colour].statusMessage(Messages.getString("ChessCommandExecutor.undoOfferedYou", otherPlayerName));
			
			getView().getControlPanel().repaintSignButtons();
		} else {
			if (getStake() > 0.0) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.undoAIWithStake"));
			}
			// playing AI for no stake - just undo the last move
			undoMove(playerName);
		}
	}

	/**
	 * Undo the most recent moves until it's the turn of the given player again.  Not
	 * supported for AI vs AI games, only human vs human or human vs AI.  The undoer 
	 * must be human.
	 * 
	 * @param playerName
	 */
	public void undoMove(String playerName) {
		ensurePlayerInGame(playerName);
		ensureGameState(GameState.RUNNING);

		if (history.size() == 0) return;
		if (history.size() == 1 && playerName.equals(getBlackPlayerName())) return;

		int colour = getPlayerColour(playerName);
		int otherColour = ChessUtils.otherColour(colour);
		
		if (getPosition().getToPlay() == colour) {
			// need to undo two moves - first the other player's last move
			players[otherColour].undoLastMove();
			cpGame.getPosition().undoMove();
			history.remove(history.size() - 1);
		}
		// now undo the undoer's last move
		players[colour].undoLastMove();
		cpGame.getPosition().undoMove();
		history.remove(history.size() - 1);

		int toPlay = getPosition().getToPlay();
		tcWhite.setActive(toPlay == Chess.WHITE);
		tcBlack.setActive(toPlay == Chess.BLACK);
		updateChessClocks(true);
		getView().getControlPanel().repaint();

		autoSave();

		alert(Messages.getString("Game.moveUndone", ChessUtils.getColour(toPlay)));
	}

	/**
	 * Display details for the game to the given player.
	 * 
	 * @param player
	 */
	public void showGameDetail(CommandSender sender) {
		String white = players[Chess.WHITE] == null ? "?" : players[Chess.WHITE].getDisplayName();
		String black = players[Chess.BLACK] == null ? "?" : players[Chess.BLACK].getDisplayName();

		String bullet = ChatColor.DARK_PURPLE + "* " + ChatColor.AQUA; //$NON-NLS-1$
		MessagePager pager = MessagePager.getPager(sender).clear();
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
		if (getInvited().equals(OPEN_INVITATION)) { //$NON-NLS-1$
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
		players[Chess.WHITE].checkPendingMove();
		players[Chess.BLACK].checkPendingMove();
	}
	
	/**
	 * Called when a (human) player has logged out.
	 * 
	 * @param playerName
	 */
	public void playerLeft(String playerName) {
		int colour = getPlayerColour(playerName);
		if (players[colour] != null) {
			players[colour].cleanup();
		}
	}
	
	/*--------------------------------------------------------------------------------*/

	public static void addGame(String gameName, ChessGame game) throws ChessException {
		if (game != null) {
			if (!chessGames.containsKey(gameName)) {
				chessGames.put(gameName, game);
				Bukkit.getPluginManager().callEvent(new ChessGameCreatedEvent(game));
			} else {
				throw new ChessException("trying to register duplicate game " + gameName);
			}
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
		Bukkit.getPluginManager().callEvent(new ChessGameDeletedEvent(game));
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

	public static ChessGame getCurrentGame(String playerName) throws ChessException {
		return getCurrentGame(playerName, false);
	}

	public static ChessGame getCurrentGame(String playerName, boolean verify) throws ChessException {
		ChessGame game = currentGame.get(playerName);
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
	public static ChessGame createGame(Player player, String gameName, String boardName, int colour) throws ChessException {
		BoardView bv;
		if (boardName == null) {
			bv = BoardView.getFreeBoard();
		} else {
			bv = BoardView.getBoardView(boardName);
		}

		return createGame(player, gameName, bv, colour);
	}

	public static ChessGame createGame(Player player, String gameName, BoardView bv, int colour) {
		String playerName = player.getName();

		if (gameName == null || gameName.equals("-")) {
			gameName = ChessGame.makeGameName(playerName);
		}

		ChessGame game = new ChessGame(gameName, bv, playerName, colour);
		ChessGame.addGame(gameName, game);
		ChessGame.setCurrentGame(playerName, game);
		bv.getControlPanel().repaintSignButtons();

		game.autoSave();

		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.gameCreated", game.getName(), game.getView().getName())); //$NON-NLS-1$ 

		return game;
	}
}

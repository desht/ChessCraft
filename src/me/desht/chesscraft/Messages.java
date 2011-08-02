package me.desht.chesscraft;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.util.config.Configuration;

public class Messages {

	static HashMap<String, String> strings = new HashMap<String, String>();
	static File loadedFile = null;

	public static void load() {
		File wanted = new File(ChessConfig.getLanguagesDirectory(),
		                       ChessConfig.getConfiguration().getString("locale", "en_US").toLowerCase() + ".yml");
		
		setDefaults();
		loadedFile = null;
		
		File actual = locateMessageFile(wanted);
		verifyCheck(actual); // debugging - comment out for a release build
		
		if (actual != null && actual.isFile() && actual.canRead()) {
			try {
				Configuration c = new Configuration(actual);
				c.load();
				boolean update = false;
				for (String k : strings.keySet()) {
					Object v = c.getProperty(k);
					if (v != null) {
						setString(k, v);
					} else {
						update = true;
						String[] l = strings.get(k).split("\n");
						c.setProperty(k, l.length > 1 ? l : l[0]);
					}
				}
				if (update) {
					c.save();
				}
				loadedFile = actual;
				return;
			} catch (Exception ex) {
				ChessCraftLogger.warning("Error loading language file", ex);
			}
		}
	}
	
	private static File locateMessageFile(File wanted) {
		if (wanted == null) {
			return null;
		}
//		System.out.println("first check for " + wanted);
		if (!wanted.isFile() || !wanted.canRead()) {
			String basename = wanted.getName().replaceAll("\\.yml$", "");
			if (basename.contains("_")) {
				basename = basename.replaceAll("_.+$", "");
			}
			File actual = new File(wanted.getParent(), basename + ".yml");
//			System.out.println("check for " + actual);
			if (actual.isFile() && actual.canRead()) {
				return actual;
			} else {
//				System.out.println("fall back to default.yml");
				return new File(wanted.getParent(), "default.yml");
			}
		} else {
//			System.out.println(wanted + " exists");
			return wanted;
		}
	}

	public static void setDefaults() {
		strings.clear();
		strings.put("BoardView.boardExists", "A board with this name already exists");
		strings.put("BoardView.boardTooHigh", "Board altitude is too high - roof would be above top of world.");
		strings.put("BoardView.boardWouldIntersect", "Board would intersect existing board {0}.");
		strings.put("BoardView.noFreeBoards", "There are no free boards to create a game on.");
		strings.put("BoardView.noSuchBoard", "No such board {0}.");

		strings.put("ChessAI.AIbusy", "AI is busy right now");
		strings.put("ChessAI.AIdisabled", "AI games are disabled");
		strings.put("ChessAI.AIloadError", "AI Loading Error");
		strings.put("ChessAI.AInotFound", "AI not found");
		strings.put("ChessAI.AIunexpectedException", "Unexpected Exception in AI: {0}");
		strings.put("ChessAI.computer", "Computer");
		strings.put("ChessAI.human", "Human");
		strings.put("ChessAI.noAvailableAIs", "There are no AIs available to play right now\n(all {0,number,integer} are currently in a game).");
		strings.put("ChessAI.noFreeAI", "no free AI was found");

		strings.put("ChessCommandExecutor.activeGameChanged", "Your active game is now &6{0}&-.");
		strings.put("ChessCommandExecutor.activeGameIs", "Your active game is &6{0}&-.");
		strings.put("ChessCommandExecutor.AIdefsReloaded", "AI definitions have been reloaded.");
		strings.put("ChessCommandExecutor.AIList", "&6{1}&-: &f{1}:{2,number,integer}");
		strings.put("ChessCommandExecutor.AIpayout", "payout={0,number,integer}%");
		strings.put("ChessCommandExecutor.algebraicNotation", "&5 (standard algebraic notation)");
		strings.put("ChessCommandExecutor.allBoardsRedrawn", "All boards have been redrawn.");
		strings.put("ChessCommandExecutor.boardCantBeDeleted", "Can't delete board &6{0}&- - it is being used by game &6{0}&-.");
		strings.put("ChessCommandExecutor.boardCreationPrompt", "Left-click a block: create board &6{0}&-. Right-click: cancel.\n"
				+ "This block will become the centre of the board's A1 square.");
		strings.put("ChessCommandExecutor.boardDeleted", "Deleted board &6{0}&-.");
		strings.put("ChessCommandExecutor.boardDetail.board", "&eBoard {0}:");
		strings.put("ChessCommandExecutor.boardDetail.boardStyle", "Board Style: &f{0}");
		strings.put("ChessCommandExecutor.boardDetail.enclosure", "Enclosure: &f{0}");
		strings.put("ChessCommandExecutor.boardDetail.frameWidth", "Frame width: {0,number,integer} ({1})");
		strings.put("ChessCommandExecutor.boardDetail.game", "Game: &f{0}");
		strings.put("ChessCommandExecutor.boardDetail.height", "Height: {0,number,integer}");
		strings.put("ChessCommandExecutor.boardDetail.isLit", "Lit: {0}");
		strings.put("ChessCommandExecutor.boardDetail.lowerNE", "Lower NE corner: &f{0}");
		strings.put("ChessCommandExecutor.boardDetail.pieceStyle", "Piece Style: &f{0}");
		strings.put("ChessCommandExecutor.boardDetail.squareSize", "Square size: &f{0,number,integer} ({1}/{2})");
		strings.put("ChessCommandExecutor.boardDetail.upperSW", "Upper SW corner: &f{0} ");
		strings.put("ChessCommandExecutor.boardList", "&6{0}&-: loc=&f{1}&- style=&6{2}&- game=&6{3}&-");
		strings.put("ChessCommandExecutor.boardRedrawn", "Board &6{0}&- has been redrawn.");
		strings.put("ChessCommandExecutor.cantAffordStake", "You can't afford that stake!");
		strings.put("ChessCommandExecutor.canTeleport", "You can teleport to your game with &f/chess tp {0}");
		strings.put("ChessCommandExecutor.chessSaved", "Chess boards and games have been saved.");
		strings.put("ChessCommandExecutor.configKeySet", "{0} is now set to: {1}");
		strings.put("ChessCommandExecutor.configReloaded", "Configuration (config.yml) has been reloaded");
		strings.put("ChessCommandExecutor.drawOfferedOther", "&6{0}&- has offered a draw.");
		strings.put("ChessCommandExecutor.drawOfferedYou", "You have offered a draw to &6{0}&-.");
		strings.put("ChessCommandExecutor.gameCreated", "Game &6{0}&- has been created on &6{1}&-.\n"
				+ "Now type &f/chess invite <playername>&- to invite someone\n"
				+ "or &f/chess invite&- to create an open invitation.");
		strings.put("ChessCommandExecutor.gameDeleted", "Game &6{0}&- has been deleted.");
		strings.put("ChessCommandExecutor.gameDeletedAlert", "Game deleted by &6{0}&-!");
		strings.put("ChessCommandExecutor.gameDetail.blackToPlay", "Black to play");
		strings.put("ChessCommandExecutor.gameDetail.clock", "Clock: White: {0}, Black: {1}");
		strings.put("ChessCommandExecutor.gameDetail.invitation", "&- has been invited.  Awaiting response.");
		strings.put("ChessCommandExecutor.gameDetail.moveHistory", "&eMove history:");
		strings.put("ChessCommandExecutor.gameDetail.name", "&eGame {0} [{1}]:");
		strings.put("ChessCommandExecutor.gameDetail.openInvitation", "Game has an open invitation");
		strings.put("ChessCommandExecutor.gameDetail.players", "&6{0}&- (White) vs. &6{1}&- (Black) on board &6{2}&-.");
		strings.put("ChessCommandExecutor.gameDetail.stake", "Stake: {0}");
		strings.put("ChessCommandExecutor.gameDetail.whiteToPlay", "White to play");
		strings.put("ChessCommandExecutor.gameDetail.halfMoves", "{0,number,integer} half-moves made");
		strings.put("ChessCommandExecutor.goingToSpawn", "Can't find a safe place to send you - going to spawn point.");
		strings.put("ChessCommandExecutor.invalidFromSquare", "Invalid FROM square in {0}");
		strings.put("ChessCommandExecutor.invalidMoveString", "Invalid move string {0}");
		strings.put("ChessCommandExecutor.invalidNumeric", "Invalid numeric value: {0}");
		strings.put("ChessCommandExecutor.invalidToSquare", "Invalid TO square in {0}");
		strings.put("ChessCommandExecutor.invited", " invited: &6{0}&-");
		strings.put("ChessCommandExecutor.joinedGame", "You have joined the chess game &6{0}&-.\n"
				+ "You will be playing as &f{1}&-.");
		strings.put("ChessCommandExecutor.needToWait", "You need to wait {0,number,integer} seconds more.");
		strings.put("ChessCommandExecutor.noActiveGame", "You have no active game. Use &f/chess game <name>&- to set one.");
		strings.put("ChessCommandExecutor.noBoards", "There are currently no boards.");
		strings.put("ChessCommandExecutor.noCurrentGames", "There are currently no games.");
		strings.put("ChessCommandExecutor.noGame", "(none)");
		strings.put("ChessCommandExecutor.noNegativeStakes", "Negative stakes are not permitted!");
		strings.put("ChessCommandExecutor.noPendingInvitation", "You don't have any pending invitations right now.");
		strings.put("ChessCommandExecutor.notFromConsole", "This command cannot be run from the console.");
		strings.put("ChessCommandExecutor.notOnChessboard", "Not on a chessboard!");
		strings.put("ChessCommandExecutor.otherPlayerMustBeOffline", "You can only do this if the other player has gone offline.");
		strings.put("ChessCommandExecutor.persistedReloaded", "Persisted board and game data has been reloaded.");
		strings.put("ChessCommandExecutor.PGNarchiveWritten", "Wrote PGN archive to {0}");
		strings.put("ChessCommandExecutor.positionUpdatedFEN", "Game position for &6{0}&- has been updated. {1} to play.\n"
				+ "&4NOTE: &-move history invalidated, this game can no longer be saved.");
		strings.put("ChessCommandExecutor.promotionPieceSet", "Promotion piece for game &6{0}&- has been set to &6{1}&-.");
		strings.put("ChessCommandExecutor.sideSwapOfferedOther", "&6{0}&- has offered to swap sides.");
		strings.put("ChessCommandExecutor.sideSwapOfferedYou", "You have offered to swap sides with &6{0}&-.");
		strings.put("ChessCommandExecutor.stakeChanged", "Stake for this game is now {0}.");
		strings.put("ChessCommandExecutor.teleportDestObstructed", "Teleport destination obstructed!");
		strings.put("ChessCommandExecutor.typeYesOrNo", "Type &f/chess yes&- to accept, or &f/chess no&- to decline.");

		strings.put("ChessConfig.invalidBoolean", "Invalid boolean value: {0} (use true/yes or false/no)");
		strings.put("ChessConfig.invalidInteger", "Invalid integer value: {0,number,integer}");
		strings.put("ChessConfig.invalidFloat", "Invalid floating-point value: {0,number,float}");
		strings.put("ChessConfig.noSuchKey", "No such config key: {0}");

		strings.put("ChessEntityListener.goingToSpawn", "Can't find a safe place to displace you - going to spawn.");

		strings.put("ChessPlayerListener.boardCreationCancelled", "Board creation cancelled.");
		strings.put("ChessPlayerListener.currentGames", "Your current chess games: {0}");
		strings.put("ChessPlayerListener.moveCancelled", "Move cancelled.");
		strings.put("ChessPlayerListener.notYourTurn", "It is not your turn!");
		strings.put("ChessPlayerListener.pieceSelected", "Selected your &f{0}&- at &f{1}&-.\n"
				+ "&5-&- Left-click a square or another piece to move your &f{0}.\n"
				+ "&5-&- Left-click the &f{0}&- again to cancel.");
		strings.put("ChessPlayerListener.playerBack", "{0} is back in the game!");
		strings.put("ChessPlayerListener.playerQuit", "{0} quit.  If they don't rejoin within {1,number,integer} seconds, you can type &f/chess win&- to win by default.");
		strings.put("ChessPlayerListener.squareMessage", "Square &6[{0}]&-, board &6{1}");
		strings.put("ChessPlayerListener.youPlayed", "You played &f{0}&-.");

		strings.put("ChessUtils.bishop", "Bishop");
		strings.put("ChessUtils.king", "King");
		strings.put("ChessUtils.knight", "Knight");
		strings.put("ChessUtils.pawn", "Pawn");
		strings.put("ChessUtils.queen", "Queen");
		strings.put("ChessUtils.rook", "Rook");

		strings.put("ControlPanel.acceptDrawBtn", "Accept Draw?");
		strings.put("ControlPanel.acceptSwapBtn", "Accept Swap?");
		strings.put("ControlPanel.blackPawnPromotionBtn", "Black Pawn;Promotion;;&4");
		strings.put("ControlPanel.boardInfoBtn", ";Board;Info");
		strings.put("ControlPanel.chessInviteReminder", "Type &f/chess invite <playername>&- to invite someone");
		strings.put("ControlPanel.createGameBtn", ";Create;Game");
		strings.put("ControlPanel.gameInfoBtn", ";Game;Info");
		strings.put("ControlPanel.halfmoveClock", "Halfmove Clock");
		strings.put("ControlPanel.inviteAnyoneBtn", ";Invite;ANYONE");
		strings.put("ControlPanel.invitePlayerBtn", ";Invite;Player");
		strings.put("ControlPanel.noBtn", ";;No");
		strings.put("ControlPanel.offerDrawBtn", ";Offer;Draw");
		strings.put("ControlPanel.playNumber", "Play Number");
		strings.put("ControlPanel.resignBtn", ";Resign");
		strings.put("ControlPanel.stakeBtn", "Stake;;");
		strings.put("ControlPanel.startGameBtn", ";Start;Game");
		strings.put("ControlPanel.teleportOutBtn", ";Teleport;Out");
		strings.put("ControlPanel.whitePawnPromotionBtn", "White Pawn;Promotion;;&4");
		strings.put("ControlPanel.yesBtn", ";;Yes");

		strings.put("ExpectBoardCreation.boardCreated", "Board &6{0}&- has been created at {1}.");

		strings.put("ExpectYesNoOffer.drawOfferAccepted", "Your draw offer has been accepted by &6{0}&-.");
		strings.put("ExpectYesNoOffer.drawOfferDeclined", "Your draw offer has been declined by &6{0}&-.");
		strings.put("ExpectYesNoOffer.swapOfferAccepted", "Your swap offer has been accepted by &6{0}&-.");
		strings.put("ExpectYesNoOffer.swapOfferDeclined", "Your swap offer has been declined by &6{0}&-.");
		strings.put("ExpectYesNoOffer.youDeclinedDrawOffer", "You have declined the draw offer.");
		strings.put("ExpectYesNoOffer.youDeclinedSwapOffer", "You have declined the swap offer.");

		strings.put("Game.boardAlreadyHasGame", "That board already has a game on it.");
		strings.put("Game.sitePGN", " in Minecraftia");
		strings.put("Game.AIisBusy", "That AI is currently busy playing a game.");
		strings.put("Game.alertPrefix", "&6:: &-Game &6{0}&-:");
		strings.put("Game.archiveExists", "Archive file {0} exists - won't overwrite.");
		strings.put("Game.autoDeleteFinished", "Finished game auto-deleted.");
		strings.put("Game.autoDeleteNotStarted", "Game auto-deleted (not started within {0,number,integer} seconds).");
		strings.put("Game.black", "Black");
		strings.put("Game.blackCantAfford", "Black can't afford to play! (need {0})");
		strings.put("Game.cantAffordStake", "You can't afford the stake for this game (need {0})!");
		strings.put("Game.cantWriteArchive", "Can't write PGN archive {0}: {1}");
		strings.put("Game.check", " &5+++CHECK+++");
		strings.put("Game.checkmated", "&6{0}&- checkmated &6{1}&- in a game of Chess!");
		strings.put("Game.drawAgreed", "&6{0}&- drew with &6{1}&- (draw agreed) in a game of Chess!");
		strings.put("Game.fiftyMoveRule", "&6{0}&- drew with &6{1}&- (50-move rule) in a game of Chess!");
		strings.put("Game.forfeited", "&6{0}&- beat &6{1}&- (forfeited) in a game of Chess!");
		strings.put("Game.gameHasStake", "This game has a stake of &f{0}&-.");
		strings.put("Game.gameIsFull", "This game already has two players.");
		strings.put("Game.getStakeBack", "You get your stake of {0} back.");
		strings.put("Game.invalidPromoPiece", "Invalid promotion piece: {0}.");
		strings.put("Game.inviteSent", "An invitation has been sent to &6{0}&-.");
		strings.put("Game.inviteWithdrawn", "Your invitation has been withdrawn.");
		strings.put("Game.joinPrompt", "Type &f/chess join&- to join the game.");
		strings.put("Game.joinPromptGlobal", "Type &f/chess join {0}&- to join the game.");
		strings.put("Game.lostStake", "You lost your stake of");
		strings.put("Game.noActiveGame", "No active game - set one with &f/chess game <name>&-.");
		strings.put("Game.noSuchGame", "No such game &6{0}&-.");
		strings.put("Game.notInGame", "You are not in this game!");
		strings.put("Game.notInvited", "You don't have an invitation for this game.");
		strings.put("Game.notStarted", "The game has not yet started.");
		strings.put("Game.notYourTurn", "It is not your turn.");
		strings.put("Game.nowPlayingBlack", "Side swap!  You are now playing Black.");
		strings.put("Game.nowPlayingWhite", "Side swap!  You are now playing White.");
		strings.put("Game.openInviteCreated", "&6{0}&e has created an open invitation to a chess game.");
		strings.put("Game.paidStake", "You have paid a stake of {0}.");
		strings.put("Game.playerJoined", "{0} has joined your game.");
		strings.put("Game.playerNotOnline", "Player {0} is not online.");
		strings.put("Game.playerPlayedMove", "&6{0}&- played [{1}]");
		strings.put("Game.resigned", "&6{0}&- beat &6{1}&- (resigned) in a game of Chess!");
		strings.put("Game.shouldBeState", "Game should be in state {0}.");
		strings.put("Game.stakeCantBeChanged", "Stake cannot be changed once both players have joined.");
		strings.put("Game.stalemated", "&6{0}&- drew with &6{1}&- (stalemate) in a game of Chess!");
		strings.put("Game.startedAsBlack", "Game started!  You are playing &fBlack&-.");
		strings.put("Game.startedAsWhite", "Game started!  You are playing &fWhite&-.");
		strings.put("Game.startPrompt", "Start the game by typing &f/game start&-.");
		strings.put("Game.white", "White");
		strings.put("Game.whiteCantAfford", "White can't afford to play! (need {0})");
		strings.put("Game.youAreInvited", "You have been invited to this game by &6{0}&-.");
		strings.put("Game.yourMove", "It is your move &f({0})&-.");
		strings.put("Game.youWon", "You have won {0}!");

		strings.put("MessageBuffer.header", "{0,number,integer} of {1,number,integer} lines (page {2,number,integer}/{3,number,integer})");
		strings.put("MessageBuffer.footer", "Use /chess page [#|n|p] to see other pages");

		strings.put("TerrainBackup.cantDeleteTerrain", "Could not delete {0}");
		strings.put("TerrainBackup.cantRestoreTerrain", "Terrain backup could not be restored: {0}");
		strings.put("TerrainBackup.cantWriteTerrain", "Terrain backup could not be written: {0}");
	}

	/**
	 * debugging: check that strings & File have the same keys <br />
	 * outputs to log any discrepancies
	 */
	private static void verifyCheck(File f) {
		try {
			Configuration c = new Configuration(f);
			c.load();
			for (String k : strings.keySet()) {
				if (c.getProperty(k) == null) {
					ChessCraftLogger.warning("missing from " + f.getName() + ": " + k);
				}
			}
			Map<String, Object> test = c.getAll();
			for (String k : test.keySet()) {
				if(strings.get(k) == null){
					ChessCraftLogger.warning("missing from defaults: " + k);
				}
			}
			loadedFile = f;
			return;
		} catch (Exception ex) {
			ChessCraftLogger.warning("Error loading language file", ex);
		}
	}
	
	protected static void setString(String key, Object value) {
		if (value instanceof String) {
			strings.put(key, (String) value);
		} else if (value instanceof ArrayList<?>){
			@SuppressWarnings("unchecked")
			ArrayList<String> l = (ArrayList<String>) value;
			StringBuilder add = new StringBuilder();
			for (int i = 0; i < l.size(); ++i){
				add.append(l.get(i));
				if (i + 1 < l.size()){
					add.append("\n");
				}
			}
			strings.put(key, add.toString());
		} else {
			ChessCraftLogger.warning("Unexpected keyvalue type: " + key + " = " + value.getClass().toString());
		}
	}

	public static String getString(String key) {
		if (strings.containsKey(key)) {
			return strings.get(key);
		}
		ChessCraftLogger.warning(null, new Exception("Unexpected missing key '" + key + "'"));
		return "!" + key + "!";
	}

	public static String getString(String key, Object... args) {
		return MessageFormat.format(getString(key), args);
	}
}

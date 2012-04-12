package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.TimeControl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import chesspresso.Chess;

import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectDrawResponse;
import me.desht.chesscraft.expector.ExpectInvitePlayer;
import me.desht.chesscraft.expector.ExpectSwapResponse;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.PermissionUtils;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ControlPanel {

	// Button names.  These should correspond directly to the equivalent /chess subcommand name
	// with spaces replaced by dots.  If there's no corresponding command, prefix the name with a
	// "*" (this ensures a permission check isn't done).
	private static final String STAKE = "stake";
	private static final String BLACK_NO = "*black-no";
	private static final String WHITE_NO = "*white-no";
	private static final String BLACK_YES = "*black-yes";
	private static final String WHITE_YES = "*white-yes";
	private static final String BLACK_PROMOTE = "*black-promote";
	private static final String WHITE_PROMOTE = "*white-promote";
	private static final String TELEPORT = "teleport";
	private static final String INVITE_ANYONE = "invite.anyone";
	private static final String INVITE_PLAYER = "invite";
	private static final String BOARD_INFO = "list.board";
	private static final String GAME_INFO = "list.game";
	private static final String OFFER_DRAW = "offer.draw";
	private static final String RESIGN = "resign";
	private static final String START = "start";
	private static final String CREATE_GAME = "create.game";

	public static final int PANEL_WIDTH = 8;

	private final BoardView view;
	private final BoardRotation boardDir, signDir;
	private final MaterialWithData signMat;
	private final Cuboid panelBlocks;
	private final Cuboid toMoveIndicator;
	private final Location halfMoveClockSign;
	private final Location whiteClockSign;
	private final Location blackClockSign;
	private final Location plyCountSign;
	private final Map<String, SignButton> buttons;
	private final Map<Location, SignButton> buttonLocs;

	public ControlPanel(BoardView view) {
		this.view = view;
		boardDir = view.getRotation();
		signDir = boardDir.getRight();

		buttons = new HashMap<String, SignButton>();
		buttonLocs = new HashMap<Location, SignButton>();

		panelBlocks = getBoardControlPanel();

		toMoveIndicator = panelBlocks.inset(Direction.Vertical, 1).
				expand(boardDir.getDirection(), -((PANEL_WIDTH - 2) / 2)).
				expand(boardDir.getDirection().opposite(), -((PANEL_WIDTH - 2) / 2));

		signMat = MaterialWithData.get("wall_sign:" + getSignDirection());
		halfMoveClockSign = getSignLocation(2, 0);
		plyCountSign = getSignLocation(5, 0);
		whiteClockSign = getSignLocation(2, 1);
		blackClockSign = getSignLocation(5, 1);
	}

	public void repaint() {
		panelBlocks.set(view.getControlPanelMaterial(), true);
		panelBlocks.forceLightLevel(view.getChessBoard().getBoardStyle().getLightLevel());

		ChessGame game = view.getGame();
		view.toPlayChanged(game != null ? game.getPosition().getToPlay() : Chess.NOBODY);

		signMat.applyToBlock(halfMoveClockSign.getBlock());
		updateHalfMoveClock(game == null ? 0 : game.getPosition().getHalfMoveClock());

		signMat.applyToBlock(plyCountSign.getBlock());
		updatePlyCount(game == null ? 0 : game.getPosition().getPlyNumber());

		signMat.applyToBlock(whiteClockSign.getBlock());
		signMat.applyToBlock(blackClockSign.getBlock());

		updateClock(Chess.WHITE, game == null ? null : game.getTcWhite());
		updateClock(Chess.BLACK, game == null ? null : game.getTcBlack());

		repaintSignButtons();
	}

	public void repaintSignButtons() {
		ChessGame game = view.getGame();

		boolean settingUp = game != null && game.getState() == GameState.SETTING_UP;
		boolean running = game != null && game.getState() == GameState.RUNNING;
		boolean hasWhite = game != null && !game.getPlayerWhite().isEmpty();
		boolean hasBlack = game != null && !game.getPlayerBlack().isEmpty();
		boolean teleportAllowed = ChessConfig.getConfig().getBoolean("teleporting");

		createSignButton(0, 2, BOARD_INFO, Messages.getString("ControlPanel.boardInfoBtn"), signMat, true); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(0, 1, TELEPORT, Messages.getString("ControlPanel.teleportOutBtn"), signMat, teleportAllowed); //$NON-NLS-1$ //$NON-NLS-2$
		if (ChessCraft.economy != null) {
			createSignButton(7, 1, STAKE, getStakeStr(game), signMat, game != null); //$NON-NLS-1$
		}

		createSignButton(1, 2, CREATE_GAME, Messages.getString("ControlPanel.createGameBtn"), signMat, game == null && !view.isDesigning()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(2, 2, INVITE_PLAYER, Messages.getString("ControlPanel.invitePlayerBtn"), signMat, settingUp //$NON-NLS-1$ //$NON-NLS-2$
		                 && (!hasWhite || !hasBlack));
		createSignButton(3, 2, INVITE_ANYONE, Messages.getString("ControlPanel.inviteAnyoneBtn"), signMat, settingUp //$NON-NLS-1$ //$NON-NLS-2$
		                 && (!hasWhite || !hasBlack));
		createSignButton(4, 2, START, Messages.getString("ControlPanel.startGameBtn"), signMat, settingUp); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(5, 2, OFFER_DRAW, Messages.getString("ControlPanel.offerDrawBtn"), signMat, running); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(6, 2, RESIGN, Messages.getString("ControlPanel.resignBtn"), signMat, running); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(7, 2, GAME_INFO, Messages.getString("ControlPanel.gameInfoBtn"), signMat, game != null); //$NON-NLS-1$ //$NON-NLS-2$

		createSignButton(1, 1, WHITE_PROMOTE, Messages.getString("ControlPanel.whitePawnPromotionBtn") + getPromoStr(game, Chess.WHITE), //$NON-NLS-1$ //$NON-NLS-2$
		                 signMat, hasWhite);
		createSignButton(6, 1, BLACK_PROMOTE, Messages.getString("ControlPanel.blackPawnPromotionBtn") + getPromoStr(game, Chess.BLACK), //$NON-NLS-1$ //$NON-NLS-2$
		                 signMat, hasBlack);

		Player pw = game == null ? null : Bukkit.getServer().getPlayer(game.getPlayerWhite());
		String offerw = getOfferText(pw);
		createSignButton(0, 0, WHITE_YES, offerw + Messages.getString("ControlPanel.yesBtn"), signMat, !offerw.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(1, 0, WHITE_NO, offerw + Messages.getString("ControlPanel.noBtn"), signMat, !offerw.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		Player pb = game == null ? null : Bukkit.getServer().getPlayer(game.getPlayerBlack());
		String offerb = getOfferText(pb);
		createSignButton(6, 0, BLACK_YES, offerb + Messages.getString("ControlPanel.yesBtn"), signMat, !offerb.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(7, 0, BLACK_NO, offerb + Messages.getString("ControlPanel.noBtn"), signMat, !offerb.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Get a teleport-in location for this control panel.  Player will be standing in front of the
	 * control panel, facing it.
	 * 
	 * @return
	 */
	public Location getLocationTP(){
		Location l = (new Cuboid(toMoveIndicator.getCenter())).
				shift(signDir.getDirection(), 3).
				shift(Direction.Down, 1).getLowerNE();
		l.setYaw((signDir.getYaw() + 180.0f) % 360);
		return l;
	}

	private String getOfferText(Player p) {
		if (p == null) {
			return ""; //$NON-NLS-1$
		} else if (ChessCraft.getResponseHandler().isExpecting(p, ExpectDrawResponse.class)) {
			return Messages.getString("ControlPanel.acceptDrawBtn"); //$NON-NLS-1$
		} else if (ChessCraft.getResponseHandler().isExpecting(p, ExpectSwapResponse.class)) {
			return Messages.getString("ControlPanel.acceptSwapBtn"); //$NON-NLS-1$
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private Location getSignLocation(int x, int y) {
		int realX = signDir.getX();
		int realY = panelBlocks.getLowerNE().getBlockY() + y;
		int realZ = signDir.getZ();

		switch(signDir){
		case NORTH:
			realX += panelBlocks.getLowerX();
			realZ += panelBlocks.getLowerZ() + x;
			break;
		case EAST:
			realX += panelBlocks.getUpperX() - x;
			realZ += panelBlocks.getLowerZ();
			break;
		case SOUTH:
			realX += panelBlocks.getLowerX();
			realZ += panelBlocks.getUpperZ() - x;
			break;
		case WEST:
			realX += panelBlocks.getLowerX() + x;
			realZ += panelBlocks.getLowerZ();
			break;
		}
		return new Location(panelBlocks.getWorld(), realX, realY, realZ);
	}

	private void createSignButton(int x, int y, String name, String text, MaterialWithData m, boolean enabled) {
		SignButton button = getSignButton(name);

		if (button != null) {
			button.setText(text.replace("\n", ";"));
			button.setEnabled(enabled);
			button.repaint();
		} else {
			Location loc = getSignLocation(x, y);
			button = new SignButton(name, loc, text, m, enabled);
			button.repaint();
			buttons.put(name, button);
			buttonLocs.put(loc, button);
		}
	}

	public void updateSignButtonText(String name, String text) {
		SignButton button = getSignButton(name);
		if (button != null) {
			button.setText(text);
			button.repaint();
		}
	}

	public SignButton getSignButton(String name) {
		return buttons.get(name);
	}

	public Cuboid getPanelBlocks() {
		return panelBlocks;
	}

	public void signClicked(Player player, Block block, BoardView view, Action action) throws ChessException {
		ChessGame game = view.getGame();
		Location loc = block.getLocation();
		SignButton button = buttonLocs.get(loc);

		if (game != null && (loc.equals(whiteClockSign) || loc.equals(blackClockSign))) {
			// doesn't matter which time control we use here, they should both have the same parameters
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.gameDetail.timeControlType", game.getTcWhite().toString()));
			return;
		}

		if (button == null || !button.isEnabled()) {
			return;
		}

		String name = button.getName();
		if (!name.startsWith("*")) {
			PermissionUtils.requirePerms(player, "chesscraft.commands." + name);
		}

		if (name.equals(CREATE_GAME)) { //$NON-NLS-1$
			ChessGame.createGame(player, null, view.getName());
		} else if (name.equals(START)) { //$NON-NLS-1$
			if (game != null) {
				game.start(player.getName());
			}
		} else if (name.equals(RESIGN)) { //$NON-NLS-1$
			if (game != null) {
				game.resign(player.getName());
			}
		} else if (name.equals(OFFER_DRAW)) { //$NON-NLS-1$
			if (game != null) {
				game.offerDraw(player.getName());
			}
		} else if (name.equals(GAME_INFO)) { //$NON-NLS-1$
			if (game != null) {
				game.showGameDetail(player);
			}
		} else if (name.equals(BOARD_INFO)) { //$NON-NLS-1$
			view.showBoardDetail(player);
		} else if (name.equals(INVITE_PLAYER)) { //$NON-NLS-1$
			if (game != null && (game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty())) {
				ChessCraft.getResponseHandler().expect(player, new ExpectInvitePlayer());
				ChessUtils.statusMessage(player, Messages.getString("ControlPanel.chessInvitePrompt")); //$NON-NLS-1$
			}
		} else if (name.equals(INVITE_ANYONE)) { //$NON-NLS-1$
			if (game != null) {
				game.inviteOpen(player.getName());
			}
		} else if (name.equals(TELEPORT)) { //$NON-NLS-1$
			if (ChessConfig.getConfig().getBoolean("teleporting")) {
				BoardView.teleportOut(player);
			}
		} else if (name.equals(WHITE_PROMOTE)) { //$NON-NLS-1$
			game.cyclePromotionPiece(player.getName());
			view.getControlPanel().updateSignButtonText(WHITE_PROMOTE, "=;=;;&4" + getPromoStr(game, Chess.WHITE)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (name.equals(BLACK_PROMOTE)) { //$NON-NLS-1$
			game.cyclePromotionPiece(player.getName());
			view.getControlPanel().updateSignButtonText(BLACK_PROMOTE, "=;=;;&4" + getPromoStr(game, Chess.BLACK)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (name.equals(WHITE_YES) || name.equals(BLACK_YES)) { //$NON-NLS-1$ //$NON-NLS-2$
			ChessCraft.handleYesNoResponse(player, true);
		} else if (name.equals(WHITE_NO) || name.equals(BLACK_NO)) { //$NON-NLS-1$ //$NON-NLS-2$
			ChessCraft.handleYesNoResponse(player, false);
		} else if (name.equals(STAKE) && ChessCraft.economy != null) { //$NON-NLS-1$
			double stakeIncr;
			if (player.isSneaking()) {
				stakeIncr = ChessConfig.getConfig().getDouble("stake.smallIncrement"); //$NON-NLS-1$
			} else {
				stakeIncr = ChessConfig.getConfig().getDouble("stake.largeIncrement"); //$NON-NLS-1$
			}
			if (action == Action.RIGHT_CLICK_BLOCK) {
				stakeIncr = -stakeIncr;
			}
			if (game == null || (!game.getPlayerWhite().isEmpty() && !game.getPlayerBlack().isEmpty())) {
				return;
			}
			game.adjustStake(stakeIncr);
			view.getControlPanel().updateSignButtonText(STAKE, getStakeStr(game)); //$NON-NLS-1$
		}
	}

	private String getStakeStr(ChessGame game) {
		String buttonText = Messages.getString("ControlPanel.stakeBtn"); //$NON-NLS-1$
		if (game == null) {
			double stake = ChessConfig.getConfig().getDouble("stake.default"); //$NON-NLS-1$
			String stakeStr = getStakeStr(stake).replaceFirst(" ", ";"); //$NON-NLS-1$ //$NON-NLS-2$
			return buttonText + stakeStr;
		} else {
			double stake = game.getStake();
			String stakeStr = getStakeStr(stake).replaceFirst(" ", ";&4"); //$NON-NLS-1$ //$NON-NLS-2$
			String col = game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty() ? "&1" : "&0"; //$NON-NLS-1$ //$NON-NLS-2$
			return col + buttonText + "&4" + stakeStr; //$NON-NLS-1$
		}
	}

	private String getStakeStr(double stake) {
		try {
			return ChessCraft.economy.format(stake);
		} catch (Exception e) {
			ChessCraftLogger.warning("Caught exception from " + ChessCraft.economy.getName() + " while trying to format quantity " + stake + ":");
			e.printStackTrace();
			ChessCraftLogger.warning("ChessCraft will continue but you should verify your economy plugin configuration.");
			return new DecimalFormat("#0.00").format(stake);
		}
	}

	private String getPromoStr(ChessGame game, int colour) {
		if (game == null) {
			return "?"; //$NON-NLS-1$
		}
		return ChessUtils.pieceToStr(game.getPromotionPiece(colour));
	}

	public void updateToMoveIndicator(MaterialWithData mat) {
		toMoveIndicator.set(mat, false);
	}

	public void updatePlyCount(int playNumber) {
		if (plyCountSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) plyCountSign.getBlock().getState();
			setSignLabel(s, Messages.getString("ControlPanel.playNumber")); //$NON-NLS-1$
			s.setLine(2, ChessUtils.parseColourSpec("&4" + playNumber)); //$NON-NLS-1$
			s.update();
		}
	}

	public void updateHalfMoveClock(int halfMoveClock) {
		if (halfMoveClockSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) halfMoveClockSign.getBlock().getState();
			setSignLabel(s, Messages.getString("ControlPanel.halfmoveClock")); //$NON-NLS-1$
			s.setLine(2, ChessUtils.parseColourSpec("&4" + halfMoveClock)); //$NON-NLS-1$
			s.update();
		}
	}

	public void updateClock(int colour, TimeControl tc) {
		Location l;
		if (colour == Chess.WHITE) {
			l = whiteClockSign;
		} else {
			l = blackClockSign;
		}
		if (l.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) l.getBlock().getState();
			setSignLabel(s, ChessUtils.getColour(colour));
			if (tc == null) {
				s.setLine(2, ChessUtils.parseColourSpec("&4" + ChessUtils.milliSecondsToHMS(0)));	//$NON-NLS-1$
				s.setLine(3, "");
			} else {
				s.setLine(2, ChessUtils.parseColourSpec("&4" + tc.getClockString())); //$NON-NLS-1$
				switch (tc.getControlType()) {
				case NONE:
					s.setLine(3, Messages.getString("ControlPanel.timeElapsed"));
					break;
				default:
					s.setLine(3, Messages.getString("ControlPanel.timeRemaining"));
					break;
				}
			}
			s.update();
		} else {
			ChessCraftLogger.warning("Block at " + l + " should be a sign but is not!");
		}

	}

	private void setSignLabel(Sign s, String text) {
		String[] lines = text.split(";");
		if (lines.length == 1) {
			s.setLine(0, "");
			s.setLine(1, lines[0]);
		} else if (lines.length == 2) {
			s.setLine(0, lines[0]);
			s.setLine(1, lines[1]);
		}
	}

	private Cuboid getBoardControlPanel() {
		BoardRotation dir = view.getRotation();
		Location a1 = view.getA1Square();

		int x = a1.getBlockX(), y = a1.getBlockY() + 1, z = a1.getBlockZ();

		// apply applicable rotation (panel on the left-side of board)
		switch (dir) {
		case NORTH:
			x -= (4 * view.getSquareSize() - PANEL_WIDTH / 2);
			z += (int) Math.ceil((view.getFrameWidth() + .5) / 2);
			break;
		case EAST:
			z -= (4 * view.getSquareSize() - PANEL_WIDTH / 2);
			x -= (int) Math.ceil((view.getFrameWidth() + .5) / 2);
			break;
		case SOUTH:
			x += (4 * view.getSquareSize() - PANEL_WIDTH / 2);
			z -= (int) Math.ceil((view.getFrameWidth() + .5) / 2);
			break;
		case WEST:
			z += (4 * view.getSquareSize() - PANEL_WIDTH / 2);
			x += (int) Math.ceil((view.getFrameWidth() + .5) / 2);
			break;
		default:
			ChessCraftLogger.severe("Unexpected BoardOrientation value ", new Exception());
			return null;
		}

		Cuboid panel = new Cuboid(new Location(a1.getWorld(), x, y, z));
		return panel.expand(dir.getDirection(), PANEL_WIDTH - 1).expand(Direction.Up, 2);
	}

	private byte getSignDirection() {
		switch (signDir){
		case NORTH:
			return 4;
		case EAST:
			return 2;
		case SOUTH:
			return 5;
		case WEST:
			return 3;
		default:
			return 0;
		}
	}
}

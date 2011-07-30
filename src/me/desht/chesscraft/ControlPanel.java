package me.desht.chesscraft;

import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.blocks.SignButton;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import chesspresso.Chess;

import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.ExpectAction;

public class ControlPanel {

	private ChessCraft plugin;
	private BoardView view;
	private Cuboid panelBlocks;
	private Cuboid toMoveIndicator;
	private Location halfMoveClockSign;
	private Location whiteClockSign;
	private Location blackClockSign;
	private Location plyCountSign;
	private Map<String, SignButton> buttons;
	private Map<Location, SignButton> buttonLocs;

	public ControlPanel(ChessCraft plugin, BoardView view) {
		this.plugin = plugin;
		this.view = view;

		buttons = new HashMap<String, SignButton>();
		buttonLocs = new HashMap<Location, SignButton>();

		Cuboid bounds = view.getBounds();
		int x = bounds.getUpperSW().getBlockX() - (4 * view.getSquareSize() - 3);
		int y = view.getA1Square().getBlockY() + 1;
		int z = bounds.getUpperSW().getBlockZ() + 1;

		panelBlocks = new Cuboid(new Location(view.getA1Square().getWorld(), x, y, z));
		panelBlocks.expand(Direction.North, 7).expand(Direction.Up, 2);

		toMoveIndicator = new Cuboid(panelBlocks.getUpperSW());
		toMoveIndicator.shift(Direction.Down, 1).shift(Direction.North, 3).expand(Direction.North, 1);

		halfMoveClockSign = getSignLocation(2, 0);
		plyCountSign = getSignLocation(5, 0);
		whiteClockSign = getSignLocation(2, 1);
		blackClockSign = getSignLocation(5, 1);
	}

	public void repaint() {
		World w = view.getA1Square().getWorld();
		for (Location l : panelBlocks) {
			view.getControlPanelMat().applyToBlock(w.getBlockAt(l));
		}

		Game game = view.getGame();
		view.toPlayChanged(game != null ? game.getPosition().getToPlay() : Chess.NOBODY);

		MaterialWithData eastFacingWallSign = new MaterialWithData(68, (byte) 0x2);

		eastFacingWallSign.applyToBlock(halfMoveClockSign.getBlock());
		updateHalfMoveClock(game == null ? 0 : game.getPosition().getHalfMoveClock());

		eastFacingWallSign.applyToBlock(plyCountSign.getBlock());
		updatePlyCount(game == null ? 0 : game.getPosition().getPlyNumber());

		eastFacingWallSign.applyToBlock(whiteClockSign.getBlock());
		eastFacingWallSign.applyToBlock(blackClockSign.getBlock());

		updateClock(Chess.WHITE, game == null ? 0 : game.getTimeWhite());
		updateClock(Chess.BLACK, game == null ? 0 : game.getTimeBlack());

		repaintSignButtons();
	}

	public void repaintSignButtons() {
		MaterialWithData eastFacingWallSign = new MaterialWithData(68, (byte) 0x2);
		Game game = view.getGame();

		boolean settingUp = game != null && game.getState() == GameState.SETTING_UP;
		boolean running = game != null && game.getState() == GameState.RUNNING;
		boolean hasWhite = game != null && !game.getPlayerWhite().isEmpty();
		boolean hasBlack = game != null && !game.getPlayerBlack().isEmpty();

		createSignButton(0, 2, "board-info", Messages.getString("ControlPanel.boardInfoBtn"), eastFacingWallSign, true); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(0, 1, "teleport", Messages.getString("ControlPanel.teleportOutBtn"), eastFacingWallSign, true); //$NON-NLS-1$ //$NON-NLS-2$
		if (Economy.active()) {
			createSignButton(7, 1, "stake", getStakeStr(game), eastFacingWallSign, game != null); //$NON-NLS-1$
		}

		createSignButton(1, 2, "create-game", Messages.getString("ControlPanel.createGameBtn"), eastFacingWallSign, game == null); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(2, 2, "invite-player", Messages.getString("ControlPanel.invitePlayerBtn"), eastFacingWallSign, settingUp //$NON-NLS-1$ //$NON-NLS-2$
				&& (!hasWhite || !hasBlack));
		createSignButton(3, 2, "invite-anyone", Messages.getString("ControlPanel.inviteAnyoneBtn"), eastFacingWallSign, settingUp //$NON-NLS-1$ //$NON-NLS-2$
				&& (!hasWhite || !hasBlack));
		createSignButton(4, 2, "start", Messages.getString("ControlPanel.startGameBtn"), eastFacingWallSign, settingUp);// && //$NON-NLS-1$ //$NON-NLS-2$
																						// hasWhite
																						// &&
																						// hasBlack);
		createSignButton(5, 2, "offer-draw", Messages.getString("ControlPanel.offerDrawBtn"), eastFacingWallSign, running); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(6, 2, "resign", Messages.getString("ControlPanel.resignBtn"), eastFacingWallSign, running); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(7, 2, "game-info", Messages.getString("ControlPanel.gameInfoBtn"), eastFacingWallSign, game != null); //$NON-NLS-1$ //$NON-NLS-2$

		createSignButton(1, 1, "white-promote", Messages.getString("ControlPanel.whitePawnPromotionBtn") + getPromoStr(game, Chess.WHITE), //$NON-NLS-1$ //$NON-NLS-2$
				eastFacingWallSign, hasWhite);
		createSignButton(6, 1, "black-promote", Messages.getString("ControlPanel.blackPawnPromotionBtn") + getPromoStr(game, Chess.BLACK), //$NON-NLS-1$ //$NON-NLS-2$
				eastFacingWallSign, hasBlack);

		Player pw = game == null ? null : plugin.getServer().getPlayer(game.getPlayerWhite());
		String offerw = getOfferText(pw);
		createSignButton(0, 0, "white-yes", offerw + Messages.getString("ControlPanel.yesBtn"), eastFacingWallSign, !offerw.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(1, 0, "white-no", offerw + Messages.getString("ControlPanel.noBtn"), eastFacingWallSign, !offerw.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		Player pb = game == null ? null : plugin.getServer().getPlayer(game.getPlayerBlack());
		String offerb = getOfferText(pb);
		createSignButton(6, 0, "black-yes", offerb + ";;Yes", eastFacingWallSign, !offerb.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(7, 0, "black-no", offerb + ";;No", eastFacingWallSign, !offerb.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String getOfferText(Player p) {
		if (p == null) {
			return ""; //$NON-NLS-1$
		} else if (plugin.expecter.isExpecting(p, ExpectAction.DrawResponse)) {
			return Messages.getString("ControlPanel.acceptDrawBtn"); //$NON-NLS-1$
		} else if (plugin.expecter.isExpecting(p, ExpectAction.SwapResponse)) {
			return Messages.getString("ControlPanel.acceptSwapBtn"); //$NON-NLS-1$
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private Location getSignLocation(int x, int y) {
		int realX = panelBlocks.getUpperSW().getBlockX() - x;
		int realY = panelBlocks.getLowerNE().getBlockY() + y;
		int realZ = panelBlocks.getLowerNE().getBlockZ() - 1;

		return new Location(view.getA1Square().getWorld(), realX, realY, realZ);
	}

	private void createSignButton(int x, int y, String name, String text, MaterialWithData m, boolean enabled) {
		SignButton button = getSignButton(name);

		if (button != null) {
			button.setText(text);
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
		Game game = view.getGame();
		SignButton button = buttonLocs.get(block.getLocation());
		if (button == null) {
			return;
		}

		if (!button.isEnabled()) {
			return;
		}

		String name = button.getName();
		if (name.equals("create-game")) { //$NON-NLS-1$
			plugin.getCommandExecutor().tryCreateGame(player, null, view.getName());
		} else if (name.equals("start")) { //$NON-NLS-1$
			plugin.getCommandExecutor().tryStartGame(player, game);
		} else if (name.equals("resign")) { //$NON-NLS-1$
			plugin.getCommandExecutor().tryResignGame(player, game);
		} else if (name.equals("offer-draw")) { //$NON-NLS-1$
			if (game != null) {
				plugin.getCommandExecutor().tryOfferDraw(player, game);
			}
		} else if (name.equals("game-info")) { //$NON-NLS-1$
			if (game != null) {
				plugin.getCommandExecutor().showGameDetail(player, game.getName());
			}
		} else if (name.equals("board-info")) { //$NON-NLS-1$
			plugin.getCommandExecutor().showBoardDetail(player, view.getName());
		} else if (name.equals("invite-player")) { //$NON-NLS-1$
			if (game != null && (game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty())) {
				ChessUtils.statusMessage(player, Messages.getString("ControlPanel.chessInviteReminder")); //$NON-NLS-1$
			}
		} else if (name.equals("invite-anyone")) { //$NON-NLS-1$
			if (game != null) {
				plugin.getCommandExecutor().tryInvitePlayer(player, game, null);
			}
		} else if (name.equals("teleport")) { //$NON-NLS-1$
			plugin.getCommandExecutor().tryTeleportOut(player);
		} else if (name.equals("white-promote")) { //$NON-NLS-1$
			plugin.getCommandExecutor().nextPromotionPiece(player, Chess.WHITE, game);
			view.getControlPanel().updateSignButtonText("white-promote", "=;=;;&4" + getPromoStr(game, Chess.WHITE)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (name.equals("black-promote")) { //$NON-NLS-1$
			plugin.getCommandExecutor().nextPromotionPiece(player, Chess.BLACK, game);
			view.getControlPanel().updateSignButtonText("black-promote", "=;=;;&4" + getPromoStr(game, Chess.BLACK)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (name.equals("white-yes") || name.equals("black-yes")) { //$NON-NLS-1$ //$NON-NLS-2$
			plugin.getCommandExecutor().doResponse(player, true);
		} else if (name.equals("white-no") || name.equals("black-no")) { //$NON-NLS-1$ //$NON-NLS-2$
			plugin.getCommandExecutor().doResponse(player, false);
		} else if (name.equals("stake") && Economy.active()) { //$NON-NLS-1$
			double stakeIncr;
			if (player.isSneaking()) {
				stakeIncr = plugin.getConfiguration().getDouble("stake.smallIncrement", 1.0); //$NON-NLS-1$
			} else {
				stakeIncr = plugin.getConfiguration().getDouble("stake.largeIncrement", 1.0); //$NON-NLS-1$
			}
			if (action == Action.RIGHT_CLICK_BLOCK) {
				stakeIncr = -stakeIncr;
			}
			if (game == null || (!game.getPlayerWhite().isEmpty() && !game.getPlayerBlack().isEmpty())) {
				return;
			}
			plugin.getCommandExecutor().tryChangeStake(player, game, stakeIncr);
			view.getControlPanel().updateSignButtonText("stake", getStakeStr(game)); //$NON-NLS-1$
		}
	}

	private String getStakeStr(Game game) {
		if (game == null) {
			double stake = plugin.getConfiguration().getDouble("stake.default", 0.0); //$NON-NLS-1$
			String stakeStr = Economy.format(stake).replaceFirst(" ", ";"); //$NON-NLS-1$ //$NON-NLS-2$
			return Messages.getString("ControlPanel.stakeBtn") + stakeStr; //$NON-NLS-1$
		} else {
			double stake = game.getStake();
			String stakeStr = Economy.format(stake).replaceFirst(" ", ";&4"); //$NON-NLS-1$ //$NON-NLS-2$
			String col = game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty() ? "&1" : "&0"; //$NON-NLS-1$ //$NON-NLS-2$
			return col + "Stake;;&4" + stakeStr; //$NON-NLS-1$
		}
	}

	private String getPromoStr(Game game, int colour) {
		if (game == null) {
			return "?"; //$NON-NLS-1$
		}
		return ChessUtils.pieceToStr(game.getPromotionPiece(colour));
	}

	public void updateToMoveIndicator(MaterialWithData mat) {
		for (Location l : toMoveIndicator) {
			mat.applyToBlock(l.getBlock());
		}
	}

	public void updatePlyCount(int playNumber) {
		if (plyCountSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) plyCountSign.getBlock().getState();
			s.setLine(1, Messages.getString("ControlPanel.playNumber")); //$NON-NLS-1$
			s.setLine(2, ChessUtils.parseColourSpec("&4" + playNumber)); //$NON-NLS-1$
			s.update();
		}

	}

	public void updateHalfMoveClock(int halfMoveClock) {
		if (halfMoveClockSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) halfMoveClockSign.getBlock().getState();
			s.setLine(1, Messages.getString("ControlPanel.halfmoveClock")); //$NON-NLS-1$
			s.setLine(2, ChessUtils.parseColourSpec("&4" + halfMoveClock)); //$NON-NLS-1$
			s.update();
		}
	}

	public void updateClock(int colour, int t) {
		Location l;
		if (colour == Chess.WHITE) {
			l = whiteClockSign;
		} else {
			l = blackClockSign;
		}
		if (l.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) l.getBlock().getState();
			s.setLine(1, Game.getColour(colour));
			s.setLine(2, ChessUtils.parseColourSpec("&4" + Game.secondsToHMS(t))); //$NON-NLS-1$
			s.update();
		}
	}
}

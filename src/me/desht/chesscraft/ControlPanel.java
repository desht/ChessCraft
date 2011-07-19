package me.desht.chesscraft;

import java.util.HashMap;
import java.util.Map;

import me.desht.chesscraft.Cuboid.Direction;
import me.desht.chesscraft.ExpectResponse.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import chesspresso.Chess;

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

	ControlPanel(ChessCraft plugin, BoardView view) {
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

	void repaint() {
		World w = view.getA1Square().getWorld();
		for (Location l : panelBlocks) {
			ChessCraft.setBlock(w.getBlockAt(l), view.getFrameMat());
		}
		view.toPlayChanged(Chess.WHITE);

		Game game = view.getGame();

		MaterialWithData eastFacingWallSign = new MaterialWithData(68, (byte) 0x2);

		ChessCraft.setBlock(halfMoveClockSign.getBlock(), eastFacingWallSign);
		updateHalfMoveClock(game == null ? 0 : game.getPosition().getHalfMoveClock());

		ChessCraft.setBlock(plyCountSign.getBlock(), eastFacingWallSign);
		updatePlyCount(game == null ? 0 : game.getPosition().getPlyNumber());

		ChessCraft.setBlock(whiteClockSign.getBlock(), eastFacingWallSign);
		ChessCraft.setBlock(blackClockSign.getBlock(), eastFacingWallSign);
		updateClock(Chess.WHITE, game == null ? 0 : game.getTimeWhite());
		updateClock(Chess.BLACK, game == null ? 0 : game.getTimeBlack());

		repaintSignButtons();
	}

	void repaintSignButtons() {
		MaterialWithData eastFacingWallSign = new MaterialWithData(68, (byte) 0x2);
		Game game = view.getGame();

		boolean settingUp = game != null && game.getState() == GameState.SETTING_UP;
		boolean running = game != null && game.getState() == GameState.RUNNING;
		boolean hasWhite = game != null && !game.getPlayerWhite().isEmpty();
		boolean hasBlack = game != null && !game.getPlayerBlack().isEmpty();

		createSignButton(0, 2, "board-info", ";Board;Info", eastFacingWallSign, true);
		createSignButton(0, 1, "teleport", ";Teleport;Out", eastFacingWallSign, true);

		createSignButton(1, 2, "create-game", ";Create;Game", eastFacingWallSign, game == null);
		createSignButton(2, 2, "invite-player", ";Invite;Player", eastFacingWallSign, settingUp
				&& (!hasWhite || !hasBlack));
		createSignButton(3, 2, "invite-anyone", ";Invite;ANYONE", eastFacingWallSign, settingUp
				&& (!hasWhite || !hasBlack));
		createSignButton(4, 2, "start", ";Start;Game", eastFacingWallSign, settingUp && hasWhite && hasBlack);
		createSignButton(5, 2, "offer-draw", ";Offer;Draw", eastFacingWallSign, running);
		createSignButton(6, 2, "resign", ";Resign", eastFacingWallSign, running);
		createSignButton(7, 2, "game-info", ";Game;Info", eastFacingWallSign, game != null);

		createSignButton(1, 1, "white-promote", "White Pawn;Promotion;;" + getPromoStr(game, Chess.WHITE),
				eastFacingWallSign, hasWhite);
		createSignButton(6, 1, "black-promote", "Black Pawn;Promotion;;" + getPromoStr(game, Chess.BLACK),
				eastFacingWallSign, hasBlack);

		Player pw = game == null ? null : plugin.getServer().getPlayer(game.getPlayerWhite());
		String offerw = getOfferText(pw);
		createSignButton(0, 0, "white-yes", offerw + ";;Yes", eastFacingWallSign, !offerw.isEmpty());
		createSignButton(1, 0, "white-no", offerw + ";;No", eastFacingWallSign, !offerw.isEmpty());
		Player pb = game == null ? null : plugin.getServer().getPlayer(game.getPlayerBlack());
		String offerb = getOfferText(pb);
		createSignButton(6, 0, "black-yes", offerb + ";;Yes", eastFacingWallSign, !offerb.isEmpty());
		createSignButton(7, 0, "black-no", offerb + ";;No", eastFacingWallSign, !offerb.isEmpty());
	}

	private String getOfferText(Player p) {
		if (p == null)
			return "";
		else if (plugin.expecter.isExpecting(p, ExpectAction.DrawResponse))
			return "Accept Draw?";
		else if (plugin.expecter.isExpecting(p, ExpectAction.SwapResponse))
			return "Accept Swap?";
		else
			return "";
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
			buttons.put(name, button);
			buttonLocs.put(loc, button);
		}
	}

	void updateSignButtonText(String name, String text) {
		SignButton button = getSignButton(name);
		if (button != null) {
			button.setText(text);
			button.repaint();
		}
	}

	SignButton getSignButton(String name) {
		return buttons.get(name);
	}

	Cuboid getPanelBlocks() {
		return panelBlocks;
	}

	void signClicked(Player player, Block block, BoardView view) throws ChessException {
		Game game = view.getGame();
		SignButton button = buttonLocs.get(block.getLocation());
		if (button == null) {
			// plugin.log(Level.WARNING, "Can't find button at location " +
			// block.getLocation());
			return;
		}

		if (!button.isEnabled())
			return;

		String name = button.getName();
		if (name.equals("create-game")) {
			plugin.getCommandExecutor().tryCreateGame(player, null, view.getName());
		} else if (name.equals("start")) {
			if (game != null)
				game.start(player.getName());
		} else if (name.equals("resign")) {
			if (game != null)
				game.resign(player.getName());
		} else if (name.equals("offer-draw")) {
			if (game != null)
				plugin.getCommandExecutor().tryOfferDraw(player, game);
		} else if (name.equals("game-info")) {
			if (game != null)
				plugin.getCommandExecutor().showGameDetail(player, game.getName());
		} else if (name.equals("board-info")) {
			plugin.getCommandExecutor().showBoardDetail(player, view.getName());
		} else if (name.equals("invite-player")) {
			if (game != null && (game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty()))
				plugin.statusMessage(player, "Type &f/chess invite <playername>&- to invite someone");
		} else if (name.equals("invite-anyone")) {
			if (game != null && (game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty()))
				game.inviteOpen(player.getName());
		} else if (name.equals("teleport")) {
			plugin.getCommandExecutor().tryTeleportOut(player);
		} else if (name.equals("white-promote")) {
			plugin.getCommandExecutor().nextPromotionPiece(player, Chess.WHITE, game);
			view.getControlPanel().updateSignButtonText("white-promote", "=;=;;" + getPromoStr(game, Chess.WHITE));
		} else if (name.equals("black-promote")) {
			plugin.getCommandExecutor().nextPromotionPiece(player, Chess.BLACK, game);
			view.getControlPanel().updateSignButtonText("black-promote", "=;=;;" + getPromoStr(game, Chess.BLACK));
		} else if (name.equals("white-yes") || name.equals("black-yes")) {
			plugin.getCommandExecutor().doResponse(player, true);
		} else if (name.equals("white-no") || name.equals("black-no")) {
			plugin.getCommandExecutor().doResponse(player, false);
		}
	}

	private String getPromoStr(Game game, int colour) {
		if (game == null)
			return "?";
		return ChessCraft.pieceToStr(game.getPromotionPiece(colour));
	}

	void updateToMoveIndicator(MaterialWithData mat) {
		for (Location l : toMoveIndicator) {
			ChessCraft.setBlock(l.getBlock(), mat);
		}
	}

	void updatePlyCount(int plyNumber) {
		if (plyCountSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) plyCountSign.getBlock().getState();
			s.setLine(1, "Ply Number");
			s.setLine(2, ChessCraft.parseColourSpec("&4" + plyNumber));
			s.update();
		}

	}

	void updateHalfMoveClock(int halfMoveClock) {
		if (halfMoveClockSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) halfMoveClockSign.getBlock().getState();
			s.setLine(1, "Halfmove Clock");
			s.setLine(2, ChessCraft.parseColourSpec("&4" + halfMoveClock));
			s.update();
		}
	}

	void updateClock(int colour, int t) {
		Location l;
		if (colour == Chess.WHITE) {
			l = whiteClockSign;
		} else {
			l = blackClockSign;
		}
		if (l.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) l.getBlock().getState();
			s.setLine(1, Game.getColour(colour));
			s.setLine(2, ChessCraft.parseColourSpec("&4" + Game.secondsToHMS(t)));
			s.update();
		}
	}
}

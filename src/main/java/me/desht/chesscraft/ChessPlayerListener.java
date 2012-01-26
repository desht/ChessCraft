package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessagePager;
import me.desht.chesscraft.blocks.MaterialWithData;

public class ChessPlayerListener implements Listener {
	
	private static final Map<String, List<String>> expecting = new HashMap<String, List<String>>();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();

		try {
			Block b = event.getClickedBlock();
			if (b == null) {
				return;
			}
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (ChessCraft.expecter.isExpecting(player, ExpectBoardCreation.class)) {
					ChessCraft.expecter.cancelAction(player, ExpectBoardCreation.class);
					ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.boardCreationCancelled")); //$NON-NLS-1$
					event.setCancelled(true);
				} else {
					BoardView bv = BoardView.partOfChessBoard(b.getLocation());
					if (bv != null && b.getState() instanceof Sign) {
						bv.getControlPanel().signClicked(player, b, bv, event.getAction());
						event.setCancelled(true);
					}
				}
			} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (ChessCraft.expecter.isExpecting(player, ExpectBoardCreation.class)) {
					ExpectBoardCreation a = (ExpectBoardCreation) ChessCraft.expecter.getAction(player,
							ExpectBoardCreation.class);
					a.setLocation(b.getLocation());
					ChessCraft.expecter.handleAction(player, ExpectBoardCreation.class);
					event.setCancelled(true);
				} else {
					BoardView bv = BoardView.partOfChessBoard(b.getLocation());
					if (bv != null && b.getState() instanceof Sign) {
						bv.getControlPanel().signClicked(player, b, bv, event.getAction());
						event.setCancelled(true);
					}
				}
			}
		} catch (ChessException e) {
			ChessUtils.errorMessage(player, e.getMessage());
			if (ChessCraft.expecter.isExpecting(player, ExpectBoardCreation.class)) {
				ChessCraft.expecter.cancelAction(player, ExpectBoardCreation.class);
				ChessUtils.errorMessage(player, Messages.getString("ChessPlayerListener.boardCreationCancelled")); //$NON-NLS-1$
			}
		}
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		Player player = event.getPlayer();

		Block targetBlock = null;

		try {
			if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
				String wand = ChessConfig.getConfig().getString("wand_item"); //$NON-NLS-1$
				int wandId = (new MaterialWithData(wand)).getMaterial();
				if (player.getItemInHand().getTypeId() == wandId) {
					HashSet<Byte> transparent = new HashSet<Byte>();
					transparent.add((byte) 0); // air
					transparent.add((byte) 20); // glass
					targetBlock = player.getTargetBlock(transparent, 100);
					Location loc = targetBlock.getLocation();
					BoardView bv;
					if ((bv = BoardView.onChessBoard(loc)) != null) {
						boardClicked(player, loc, bv);
					} else if ((bv = BoardView.aboveChessBoard(loc)) != null) {
						pieceClicked(player, loc, bv);
					} else if ((bv = BoardView.partOfChessBoard(loc)) != null) {
						if (bv.isControlPanel(loc)) {
							Location l = bv.getControlPanel().getLocationTP();
							player.teleport(l);
						}
					}
				}
			}
		} catch (ChessException e) {
			ChessUtils.errorMessage(player, e.getMessage());
		} catch (IllegalMoveException e) {
			if (targetBlock != null) {
				cancelMove(targetBlock.getLocation());
			}
			ChessUtils.errorMessage(player, e.getMessage() + ". " + Messages.getString("ChessPlayerListener.moveCancelled")); //$NON-NLS-1$ $NON-NLS-2$ 
		} catch (IllegalStateException e) {
			// player.getTargetBlock() throws this exception occasionally - it appears
			// to be harmless, so we'll ignore it
		}

	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		StringBuilder games = new StringBuilder();
		String who = event.getPlayer().getName();
		for (ChessGame game : ChessGame.listGames()) {
			if (game.isPlayerInGame(who)) {
				ChessCraft.getInstance().playerRejoined(who);
				game.alert(game.getOtherPlayer(who),
						Messages.getString("ChessPlayerListener.playerBack", who)); //$NON-NLS-1$
				games.append(" ").append(game.getName()); //$NON-NLS-1$
			}
		}
		if (games.length() > 0) {
			ChessUtils.alertMessage(event.getPlayer(), Messages.getString("ChessPlayerListener.currentGames", games)); //$NON-NLS-1$
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String who = event.getPlayer().getName();
		int timeout = ChessConfig.getConfig().getInt("forfeit_timeout"); //$NON-NLS-1$
		for (ChessGame game : ChessGame.listGames()) {
			if (game.isPlayerInGame(who)) {
				ChessCraft.getInstance().playerLeft(who);
				if (timeout > 0 && game.getState() == GameState.RUNNING) {
					game.alert(Messages.getString("ChessPlayerListener.playerQuit", who, timeout)); //$NON-NLS-1$
				}
			}
		}
		MessagePager.deletePager(event.getPlayer());
	}

	private void cancelMove(Location loc) {
		BoardView bv = BoardView.onChessBoard(loc);
		if (bv == null) {
			bv = BoardView.aboveChessBoard(loc);
		}
		if (bv != null && bv.getGame() != null) {
			bv.getGame().setFromSquare(Chess.NO_SQUARE);
		}
	}

	private void pieceClicked(Player player, Location loc, BoardView bv) throws IllegalMoveException, ChessException {
		ChessGame game = bv.getGame();
		if (game == null || game.getState() != GameState.RUNNING) {
			return;
		}

		if (game.isPlayerToMove(player.getName())) {
			if (game.getFromSquare() == Chess.NO_SQUARE) {
				int sqi = game.getView().getSquareAt(loc);
				int colour = game.getPosition().getColor(sqi);
				if (colour == game.getPosition().getToPlay()) {
					game.setFromSquare(sqi);
					int piece = game.getPosition().getPiece(sqi);
					String what = ChessUtils.pieceToStr(piece).toUpperCase();
					ChessUtils.statusMessage(player,
							Messages.getString("ChessPlayerListener.pieceSelected", what, Chess.sqiToStr(sqi))); //$NON-NLS-1$
				}
			} else {
				int sqi = game.getView().getSquareAt(loc);
				if (sqi == game.getFromSquare()) {
					game.setFromSquare(Chess.NO_SQUARE);
					ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.moveCancelled")); //$NON-NLS-1$
				} else if (sqi >= 0 && sqi < Chess.NUM_OF_SQUARES) {
					game.doMove(player.getName(), sqi);
					ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed",
							game.getPosition().getLastMove().getLAN())); //$NON-NLS-1$
				}
			}
		} else if (game.isPlayerInGame(player.getName())) {
			ChessUtils.errorMessage(player, Messages.getString("ChessPlayerListener.notYourTurn")); //$NON-NLS-1$
		}
	}

	private void boardClicked(Player player, Location loc, BoardView bv) throws IllegalMoveException, ChessException {
		int sqi = bv.getSquareAt(loc);
		ChessGame game = bv.getGame();
		if (game != null && game.getFromSquare() != Chess.NO_SQUARE) {
			game.doMove(player.getName(), sqi);
			ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed", //$NON-NLS-1$
					game.getPosition().getLastMove().getLAN()));
		} else {
			ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.squareMessage", //$NON-NLS-1$
					Chess.sqiToStr(sqi), bv.getName()));
			if (bv.isPartOfBoard(player.getLocation())) {
				// allow teleporting around the board, but only if the player is 
				// already on the board
				Location newLoc = loc.clone().add(0, 1.0, 0);
				newLoc.setPitch(player.getLocation().getPitch());
				newLoc.setYaw(player.getLocation().getYaw());
				player.teleport(newLoc);
			}
		}
	}

	static void expectingClick(Player p, String name, String style) {
		List<String> list = new ArrayList<String>();
		list.add(name);
		list.add(style);
		expecting.put(p.getName(), list);
	}


}

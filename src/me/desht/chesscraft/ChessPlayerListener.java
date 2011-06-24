package me.desht.chesscraft;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

public class ChessPlayerListener extends PlayerListener {
	ChessCraft plugin;
	
	public ChessPlayerListener(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
			
		Player player = event.getPlayer();
		
		try {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Block b = event.getClickedBlock();
				if (b == null) return;
				Location loc = b.getLocation();
				BoardView bv;
				if ((bv = onChessBoard(loc)) != null) {
					int sqi = bv.getSquareAt(loc);
					Game game = bv.getGame();
					if (game != null && game.getFromSquare() != Chess.NO_SQUARE) {
						game.doMove(player, sqi);
						plugin.statusMessage(player, "You played " + game.getPosition().getLastMove().getLAN() + ".");
					} else {
						plugin.statusMessage(player, "Square [" + Chess.sqiToStr(sqi) + "], board '" + bv.getName() + "'");
					}
				} else if ((bv = aboveChessBoard(loc)) != null) {
					Game game = bv.getGame();
					if (game == null)
						return;
					pieceClicked(player, game, loc);
				} else {
					// nothing?
				}
			}
		} catch (ChessException e) {
			plugin.errorMessage(player, e.getMessage());
		} catch (IllegalMoveException e) {
			cancelMove(event);
			plugin.errorMessage(player, e.getMessage() + " - move cancelled.");
		}
	}
	
	private void cancelMove(PlayerInteractEvent event) {
		BoardView bv = onChessBoard(event.getClickedBlock().getLocation());
		if (bv == null) 
			bv = aboveChessBoard(event.getClickedBlock().getLocation());
		if (bv != null && bv.getGame() != null) {
			bv.getGame().setFromSquare(Chess.NO_SQUARE);
		}
	}

	private void pieceClicked(Player player, Game game, Location loc) throws IllegalMoveException, ChessException {
		if (game.getState() != GameState.RUNNING)
			return;
		
		if (game.isPlayerToMove(player)) {
			if (game.getFromSquare() == Chess.NO_SQUARE) {
				int sqi = game.getView().getSquareAt(loc);
				int colour = game.getPosition().getColor(sqi);
				if (colour == game.getPosition().getToPlay()) {
					game.setFromSquare(sqi);
					int piece = game.getPosition().getPiece(sqi);
					String what = ChessCraft.pieceToStr(piece).toUpperCase();
					plugin.statusMessage(player, "Selected your " + what + " at " + Chess.sqiToStr(sqi) + ".");
					plugin.statusMessage(player, "- Right-click a square or another piece to move your " + what);
					plugin.statusMessage(player, "- Right-click the " + what + " again to cancel.");
				}
			} else {
				int sqi = game.getView().getSquareAt(loc);
				if (sqi == game.getFromSquare()) {
					game.setFromSquare(Chess.NO_SQUARE);
					plugin.statusMessage(player, "Move cancelled.");
				} else {
					game.doMove(player, sqi);
					plugin.statusMessage(player, "You played " + game.getPosition().getLastMove().getLAN() + ".");
				}
			}
		} else if (game.isPlayerInGame(player)) {
			plugin.errorMessage(player, "It is not your turn!");
		}
	}
	
	private BoardView aboveChessBoard(Location loc) {
		for (BoardView bv : plugin.listBoardViews()) {
			if (bv.isAboveBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	BoardView onChessBoard(Location loc) {
		for (BoardView bv : plugin.listBoardViews()) {
			if (bv.isOnBoard(loc)) {
				return bv;
			}
		}
		return null;
	}
}

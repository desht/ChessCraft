package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.chesscraft.ExpectResponse.ExpectAction;
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
	private ChessCraft plugin;
	private static final Map<String,List<String>> expecting = new HashMap<String,List<String>>();
	
	public ChessPlayerListener(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
			
		Player player = event.getPlayer();
		
		try {
			Block b = event.getClickedBlock();
			if (b == null) return;
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Location loc = b.getLocation();
				BoardView bv;
				if (plugin.expecter.isExpecting(player, ExpectAction.BoardCreation)) {
					plugin.expecter.cancelAction(player, ExpectAction.BoardCreation);
					plugin.statusMessage(player, "Board creation cancelled.");
				} else if ((bv = plugin.onChessBoard(loc)) != null) {
					boardClicked(player, loc, bv);
				} else if ((bv = plugin.aboveChessBoard(loc)) != null) {
					pieceClicked(player, loc, bv);
				} else {
					// nothing?
				}
			} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (plugin.expecter.isExpecting(player, ExpectAction.BoardCreation)) {
					ExpectBoardCreation a = (ExpectBoardCreation) plugin.expecter.getAction(player, ExpectAction.BoardCreation);
					a.setLocation(b.getLocation());
					plugin.expecter.handleAction(player, ExpectAction.BoardCreation);
					return;
				}
			}
		} catch (ChessException e) {
			plugin.errorMessage(player, e.getMessage());
			if (plugin.expecter.isExpecting(player, ExpectAction.BoardCreation)) {
				plugin.expecter.cancelAction(player, ExpectAction.BoardCreation);
				plugin.errorMessage(player, "Board creation cancelled.");
			}
		} catch (IllegalMoveException e) {
			cancelMove(event);
			plugin.errorMessage(player, e.getMessage() + ".  Move cancelled.");
		}
	}

	private void cancelMove(PlayerInteractEvent event) {
		BoardView bv = plugin.onChessBoard(event.getClickedBlock().getLocation());
		if (bv == null) 
			bv = plugin.aboveChessBoard(event.getClickedBlock().getLocation());
		if (bv != null && bv.getGame() != null) {
			bv.getGame().setFromSquare(Chess.NO_SQUARE);
		}
	}

	private void pieceClicked(Player player, Location loc, BoardView bv) throws IllegalMoveException, ChessException {
		Game game = bv.getGame();
		if (game == null || game.getState() != GameState.RUNNING)
			return;
		
		if (game.isPlayerToMove(player)) {
			if (game.getFromSquare() == Chess.NO_SQUARE) {
				int sqi = game.getView().getSquareAt(loc);
				int colour = game.getPosition().getColor(sqi);
				if (colour == game.getPosition().getToPlay()) {
					game.setFromSquare(sqi);
					int piece = game.getPosition().getPiece(sqi);
					String what = ChessCraft.pieceToStr(piece).toUpperCase();
					plugin.statusMessage(player, "Selected your &f" + what + "&- at &f" + Chess.sqiToStr(sqi) + "&-.");
					plugin.statusMessage(player, "&5-&- Right-click a square or another piece to move your &f" + what);
					plugin.statusMessage(player, "&5-&- Right-click the &f" + what + "&- again to cancel.");
				}
			} else {
				int sqi = game.getView().getSquareAt(loc);
				if (sqi == game.getFromSquare()) {
					game.setFromSquare(Chess.NO_SQUARE);
					plugin.statusMessage(player, "Move cancelled.");
				} else if (sqi >= 0 && sqi < Chess.NUM_OF_SQUARES) {
					game.doMove(player, sqi);
					plugin.statusMessage(player, "You played " + game.getPosition().getLastMove().getLAN() + ".");
				}
			}
		} else if (game.isPlayerInGame(player)) {
			plugin.errorMessage(player, "It is not your turn!");
		}
	}
	
	private void boardClicked(Player player, Location loc, BoardView bv)
			throws IllegalMoveException, ChessException {
		int sqi = bv.getSquareAt(loc);
		Game game = bv.getGame();
		if (game != null && game.getFromSquare() != Chess.NO_SQUARE) {
			game.doMove(player, sqi);
			plugin.statusMessage(player, "You played &f[" + game.getPosition().getLastMove().getLAN() + "]&-.");
		} else {
			plugin.statusMessage(player, "Square &6[" + Chess.sqiToStr(sqi) + "]&-, board &6" + bv.getName() + "&-");
		}
	}

	static void expectingClick(Player p, String name, String style) {
		List<String> list = new ArrayList<String>();
		list.add(name);
		list.add(style);
		expecting.put(p.getName(), list);
	}
}

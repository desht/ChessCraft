package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import me.desht.chesscraft.ExpectResponse.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

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
				if (plugin.expecter.isExpecting(player, ExpectAction.BoardCreation)) {
					plugin.expecter.cancelAction(player, ExpectAction.BoardCreation);
					plugin.statusMessage(player, "Board creation cancelled.");
				} else {
					// nothing?
				}
			} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (plugin.expecter.isExpecting(player, ExpectAction.BoardCreation)) {
					ExpectBoardCreation a = (ExpectBoardCreation) plugin.expecter.getAction(player, ExpectAction.BoardCreation);
					a.setLocation(b.getLocation());
					plugin.expecter.handleAction(player, ExpectAction.BoardCreation);
					return;
				} else {
					BoardView bv = plugin.partOfChessBoard(b.getLocation());
					if (bv != null && b.getState() instanceof Sign) {
						bv.getControlPanel().signClicked(player, b, bv);
					} 
				}
			}
		} catch (ChessException e) {
			plugin.errorMessage(player, e.getMessage());
			if (plugin.expecter.isExpecting(player, ExpectAction.BoardCreation)) {
				plugin.expecter.cancelAction(player, ExpectAction.BoardCreation);
				plugin.errorMessage(player, "Board creation cancelled.");
			}
		}
	}

	@Override
	public void onPlayerAnimation(PlayerAnimationEvent event) {	
		Player player = event.getPlayer();
		
		Block targetBlock = null;
		
		try {
			if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
				String wand = plugin.getConfiguration().getString("wand_item");
				int wandId = new MaterialWithData(wand).material;
				if (player.getItemInHand().getTypeId() == wandId) {
					HashSet<Byte> transparent = new HashSet<Byte>();
					transparent.add((byte) 0);	// air
					transparent.add((byte) 20);	// glass
					targetBlock = player.getTargetBlock(transparent, 100);
					Location loc = targetBlock.getLocation();
					BoardView bv;
					if ((bv = plugin.onChessBoard(loc)) != null) {
						boardClicked(player, loc, bv);
					} else if ((bv = plugin.aboveChessBoard(loc)) != null) {
						pieceClicked(player, loc, bv);
					} else if ((bv = plugin.partOfChessBoard(loc)) != null) {
						if (bv.isControlPanel(loc)) {
							Location corner = bv.getBounds().getUpperSW();
							Location loc2 = new Location(corner.getWorld(), corner.getX() - 4 * bv.getSquareSize(), corner.getY() + 1, corner.getZ() - 2.5);
							player.teleport(loc2);
						}
					}
				}
			}
		} catch (ChessException e) {
			plugin.errorMessage(player, e.getMessage());
		} catch (IllegalMoveException e) {
			if (targetBlock != null) {
				cancelMove(targetBlock.getLocation());
			}
			plugin.errorMessage(player, e.getMessage() + ".  Move cancelled.");
		}
		
	}
	
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		String games = "";
		String who = event.getPlayer().getName();
		for (Game game : plugin.listGames()) {
			if (game.isPlayerInGame(who)) {
				plugin.playerRejoined(who);
				game.alert(game.getOtherPlayer(who), who + " is back in the game!");
				games = games + " " + game.getName();
			}
		}
		if (!games.isEmpty())
			plugin.alertMessage(event.getPlayer(), "Your current chess games: " + games);
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		String who = event.getPlayer().getName();
		int timeout = plugin.getConfiguration().getInt("forfeit_timeout", 60);
		for (Game game : plugin.listGames()) {
			if (game.isPlayerInGame(who)) {
				plugin.playerLeft(who);
				if (timeout > 0) {
					game.alert(who + " quit.  If they don't rejoin within");
					game.alert(timeout + " seconds, you can type &f/chess win&-");
					game.alert("to win by default.");
				}
			}
		}
	}

	private void cancelMove(Location loc) {
		BoardView bv = plugin.onChessBoard(loc);
		if (bv == null) 
			bv = plugin.aboveChessBoard(loc);
		if (bv != null && bv.getGame() != null) {
			bv.getGame().setFromSquare(Chess.NO_SQUARE);
		}
	}

	private void pieceClicked(Player player, Location loc, BoardView bv) throws IllegalMoveException, ChessException {
		Game game = bv.getGame();
		if (game == null || game.getState() != GameState.RUNNING)
			return;
		
		if (game.isPlayerToMove(player.getName())) {
			if (game.getFromSquare() == Chess.NO_SQUARE) {
				int sqi = game.getView().getSquareAt(loc);
				int colour = game.getPosition().getColor(sqi);
				if (colour == game.getPosition().getToPlay()) {
					game.setFromSquare(sqi);
					int piece = game.getPosition().getPiece(sqi);
					String what = ChessCraft.pieceToStr(piece).toUpperCase();
					plugin.statusMessage(player, "Selected your &f" + what + "&- at &f" + Chess.sqiToStr(sqi) + "&-.");
					plugin.statusMessage(player, "&5-&- Left-click a square or another piece to move your &f" + what);
					plugin.statusMessage(player, "&5-&- Left-click the &f" + what + "&- again to cancel.");
				}
			} else {
				int sqi = game.getView().getSquareAt(loc);
				if (sqi == game.getFromSquare()) {
					game.setFromSquare(Chess.NO_SQUARE);
					plugin.statusMessage(player, "Move cancelled.");
				} else if (sqi >= 0 && sqi < Chess.NUM_OF_SQUARES) {
					game.doMove(player.getName(), sqi);
					plugin.statusMessage(player, "You played " + game.getPosition().getLastMove().getLAN() + ".");
				}
			}
		} else if (game.isPlayerInGame(player.getName())) {
			plugin.errorMessage(player, "It is not your turn!");
		}
	}
	
	private void boardClicked(Player player, Location loc, BoardView bv)
			throws IllegalMoveException, ChessException {
		int sqi = bv.getSquareAt(loc);
		Game game = bv.getGame();
		if (game != null && game.getFromSquare() != Chess.NO_SQUARE) {
			game.doMove(player.getName(), sqi);
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

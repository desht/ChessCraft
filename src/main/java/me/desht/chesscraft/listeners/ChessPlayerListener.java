package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldedit.blocks.BlockType;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.chesscraft.expector.ExpectInvitePlayer;
import me.desht.chesscraft.expector.ResponseHandler;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessagePager;

public class ChessPlayerListener implements Listener {
	
	// block ids to be considered transparent when calling player.getTargetBlock()
	private static HashSet<Byte> transparent = new HashSet<Byte>();
	static {
		transparent.add((byte) 0); // air
		transparent.add((byte) 20); // glass
	}
	
	private static final long MIN_ANIMATION_WAIT = 200; // milliseconds
	private final Map<String,Long> lastAnimation = new HashMap<String, Long>();

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		ResponseHandler resp = ChessCraft.getResponseHandler();
		
		// a left or right-click cancels any pending player invite response
		if (resp.isExpecting(player, ExpectInvitePlayer.class)) {
			resp.cancelAction(player, ExpectInvitePlayer.class);
			ChessUtils.alertMessage(player, Messages.getString("ChessPlayerListener.playerInviteCancelled"));
			event.setCancelled(true);
			return;
		}

		try {
			Block b = event.getClickedBlock();
			if (b == null) {
				return;
			}
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (resp.isExpecting(player, ExpectBoardCreation.class)) {
					resp.cancelAction(player, ExpectBoardCreation.class);
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
				if (resp.isExpecting(player, ExpectBoardCreation.class)) {
					ExpectBoardCreation a = (ExpectBoardCreation) resp.getAction(player, ExpectBoardCreation.class);
					a.setLocation(b.getLocation());
					resp.handleAction(player, ExpectBoardCreation.class);
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
			if (resp.isExpecting(player, ExpectBoardCreation.class)) {
				resp.cancelAction(player, ExpectBoardCreation.class);
				ChessUtils.errorMessage(player, Messages.getString("ChessPlayerListener.boardCreationCancelled")); //$NON-NLS-1$
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		Player player = event.getPlayer();

		// We seem to get multiple events very close together, leading to unwanted double actions sometimes.
		// So ignore events that happen too soon after the last one for a player.
		if (System.currentTimeMillis() - lastAnimationEvent(player) < MIN_ANIMATION_WAIT) {
			return;
		}
		lastAnimation.put(player.getName(), System.currentTimeMillis());
		
		Block targetBlock = null;

		try {
			if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
				int wandId = ChessUtils.getWandId();
				if (wandId < 0 || player.getItemInHand().getTypeId() == wandId) {
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
			// targetBlock must be non-null at this point
			cancelMove(targetBlock.getLocation());
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

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (BoardView.partOfChessBoard(event.getBlockClicked().getLocation()) != null) {
			event.setCancelled(true);
			// seems just cancelling the event doesn't stop the bucket getting filled?
			event.setItemStack(new ItemStack(Material.BUCKET, 1));
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (BoardView.partOfChessBoard(event.getBlockClicked().getLocation()) != null) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (BoardView.partOfChessBoard(event.getFrom()) != null) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority=EventPriority.HIGH)
	public void onPlayerChat(PlayerChatEvent event) {
		Player player = event.getPlayer();
		ResponseHandler resp = ChessCraft.getResponseHandler();
		if (resp.isExpecting(event.getPlayer(), ExpectInvitePlayer.class)) {
			try {
				ExpectInvitePlayer ip = (ExpectInvitePlayer) resp.getAction(player, ExpectInvitePlayer.class);
				ip.setInviteeName(event.getMessage());
				event.setCancelled(true);
				resp.handleAction(player, ip.getClass());
			} catch (ChessException e) {
				ChessUtils.errorMessage(player, e.getMessage());
				resp.cancelAction(player, ExpectInvitePlayer.class);
			}
		}
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

		int sqi = game.getView().getSquareAt(loc);
		
		if (player.isSneaking()) {
			// shift-clicked a piece - try to teleport the player onto the piece
			teleportToPiece(player, bv, loc);
		} else if (game.isPlayerToMove(player.getName())) {
			if (game.getFromSquare() == Chess.NO_SQUARE) {
				// select the piece for moving (if it belongs to the player)
				int colour = game.getPosition().getColor(sqi);
				if (colour == game.getPosition().getToPlay()) {
					game.setFromSquare(sqi);
					int piece = game.getPosition().getPiece(sqi);
					String what = ChessUtils.pieceToStr(piece).toUpperCase();
					ChessUtils.statusMessage(player,
							Messages.getString("ChessPlayerListener.pieceSelected", what, Chess.sqiToStr(sqi))); //$NON-NLS-1$
				}
			} else {
				if (sqi == game.getFromSquare()) {
					// cancel a selected piece
					game.setFromSquare(Chess.NO_SQUARE);
					ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.moveCancelled")); //$NON-NLS-1$
				} else if (sqi >= 0 && sqi < Chess.NUM_OF_SQUARES) {
					// try to move the selected piece
					game.doMove(player.getName(), sqi);
					ChessUtils.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed",
							game.getPosition().getLastMove().getLAN())); //$NON-NLS-1$
				}
			}
		} else if (game.isPlayerInGame(player.getName())) {
			ChessUtils.errorMessage(player, Messages.getString("ChessPlayerListener.notYourTurn")); //$NON-NLS-1$
		}
	}

	private void teleportToPiece(Player player, BoardView bv, Location loc) {
		Block b = loc.getBlock();
		Block b1 = b.getRelative(BlockFace.UP);
		boolean isSolid = !BlockType.canPassThrough(bv.getEnclosureMaterial().getMaterial());
		int max = isSolid ? bv.getOuterBounds().getUpperY() - 2 : loc.getWorld().getMaxHeight();
		while (b.getType() != Material.AIR && b1.getType() != Material.AIR && b1.getLocation().getY() < max) {
			b = b.getRelative(BlockFace.UP);
			b1 = b1.getRelative(BlockFace.UP);
		}
		if (b1.getY() < max) {
			Location dest = b1.getLocation();
			dest.setYaw(player.getLocation().getYaw());
			dest.setPitch(player.getLocation().getPitch());
			player.teleport(dest);
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

	private long lastAnimationEvent(Player player) {
		if (!lastAnimation.containsKey(player.getName())) {
			lastAnimation.put(player.getName(), 0L);
		}
		return lastAnimation.get(player.getName());
	}


}

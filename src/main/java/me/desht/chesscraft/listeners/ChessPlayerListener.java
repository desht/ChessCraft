package me.desht.chesscraft.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.chesscraft.expector.ExpectInvitePlayer;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

public class ChessPlayerListener extends ChessListenerBase {
	
	public ChessPlayerListener(ChessCraft plugin) {
		super(plugin);
	}

	// block ids to be considered transparent when calling player.getTargetBlock()
	private static HashSet<Byte> transparent = new HashSet<Byte>();
	static {
		transparent.add((byte) 0); // air
		transparent.add((byte) 20); // glass
	}
	
	private static final long MIN_ANIMATION_WAIT = 200; // milliseconds
	private final Map<String,Long> lastAnimation = new HashMap<String, Long>();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		
		ResponseHandler resp = ChessCraft.getInstance().responseHandler;
		
		// a left or right-click (even air, where the event is cancelled) cancels any pending player invite response
		if (resp.isExpecting(playerName, ExpectInvitePlayer.class)) {
			resp.cancelAction(playerName, ExpectInvitePlayer.class);
			MiscUtil.alertMessage(player, Messages.getString("ChessPlayerListener.playerInviteCancelled"));
			event.setCancelled(true);
			return;
		}

		if (event.isCancelled()) {
			return;
		}
		
		try {
			Block b = event.getClickedBlock();
			if (b == null) {
				return;
			}
			if (resp.isExpecting(playerName, ExpectBoardCreation.class)) {
				ExpectBoardCreation a = resp.getAction(playerName, ExpectBoardCreation.class);
				switch (event.getAction()) {
				case LEFT_CLICK_BLOCK:
					a.setLocation(b.getLocation());
					a.handleAction();
					break;
				case RIGHT_CLICK_BLOCK:
					MiscUtil.alertMessage(player,  Messages.getString("ChessPlayerListener.boardCreationCancelled")); //$NON-NLS-1$
					a.cancelAction();
					break;
				}
				event.setCancelled(true);
			} else {
				BoardView bv = BoardView.partOfChessBoard(b.getLocation());
				if (bv != null && bv.getControlPanel().isSignButton(b.getLocation())) {
					bv.getControlPanel().signClicked(event);
					event.setCancelled(true);
				}
			}

		} catch (ChessException e) {
			MiscUtil.errorMessage(player, e.getMessage());
			if (resp.isExpecting(playerName, ExpectBoardCreation.class)) {
				resp.cancelAction(playerName, ExpectBoardCreation.class);
				MiscUtil.errorMessage(player, Messages.getString("ChessPlayerListener.boardCreationCancelled")); //$NON-NLS-1$
			}
		} catch (DHUtilsException e) {
			MiscUtil.errorMessage(player, e.getMessage());			
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
					targetBlock = player.getTargetBlock(transparent, 120);
					LogUtils.finer("Player " + player.getName() + " waved at block " + targetBlock);
					Location loc = targetBlock.getLocation();
					BoardView bv;
					if ((bv = BoardView.onChessBoard(loc)) != null) {
						boardClicked(player, loc, bv);
					} else if ((bv = BoardView.aboveChessBoard(loc)) != null) {
						pieceClicked(player, loc, bv);
					} else if ((bv = BoardView.partOfChessBoard(loc)) != null) {
						if (bv.isControlPanel(loc)) {
							Location tpLoc = bv.getControlPanel().getTeleportLocation();
							Cuboid zone = bv.getControlPanel().getPanelBlocks().outset(Direction.Horizontal, 4);
							if (!zone.contains(player.getLocation()) && bv.isPartOfBoard(player.getLocation())) {
								teleportPlayer(player, tpLoc);
							}
						}
					}
				}
			}
		} catch (ChessException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		} catch (IllegalMoveException e) {
			// targetBlock must be non-null at this point
			cancelMove(targetBlock.getLocation());
			MiscUtil.errorMessage(player, e.getMessage() + ". " + Messages.getString("ChessPlayerListener.moveCancelled"));
			ChessUtils.playEffect(player.getLocation(), "piece_unselected");
		} catch (IllegalStateException e) {
			// player.getTargetBlock() throws this exception occasionally - it appears
			// to be harmless, so we'll ignore it
		}

	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		StringBuilder games = new StringBuilder();
		String who = event.getPlayer().getName();
		for (ChessGame game : ChessGameManager.getManager().listGames()) {
			int colour = game.getPlayerColour(who);
			if (colour != Chess.NOBODY) {
				plugin.getPlayerTracker().playerRejoined(who);
				ChessPlayer other = game.getPlayer(Chess.otherPlayer(colour));
				if (other != null) {
					other.alert(Messages.getString("ChessPlayerListener.playerBack", who));
				}
				games.append(" ").append(game.getName());
			}
		}
		if (games.length() > 0) {
			MiscUtil.alertMessage(event.getPlayer(), Messages.getString("ChessPlayerListener.currentGames", games)); //$NON-NLS-1$
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String who = event.getPlayer().getName();
		int timeout = plugin.getConfig().getInt("forfeit_timeout"); //$NON-NLS-1$
		for (ChessGame game : ChessGameManager.getManager().listGames()) {
			if (game.isPlayerInGame(who)) {
				game.playerLeft(who);
				plugin.getPlayerTracker().playerLeft(who);
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
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		ResponseHandler resp = ChessCraft.getInstance().responseHandler;
		ExpectInvitePlayer ip = resp.getAction(player.getName(), ExpectInvitePlayer.class);
		
		if (ip != null) {
			try {
				ip.setInviteeName(event.getMessage());
				event.setCancelled(true);
				ip.handleAction();
			} catch (ChessException e) {
				MiscUtil.errorMessage(player, e.getMessage());
				ip.cancelAction();
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
		if (player.isSneaking()) {
			// shift-clicked a piece - try to teleport the player onto the piece
			teleportToPiece(player, bv, loc);
			return;
		}
		
		ChessGame game = bv.getGame();
		if (game == null || game.getState() != GameState.RUNNING) {
			return;
		}
		
		ChessGameManager.getManager().setCurrentGame(player.getName(), game);

		int sqi = game.getView().getSquareAt(loc);
		if (game.isPlayerToMove(player.getName())) {
			if (game.getFromSquare() == Chess.NO_SQUARE) {
				// select the piece for moving (if it belongs to the player)
				int colour = game.getPosition().getColor(sqi);
				if (colour == game.getPosition().getToPlay()) {
					game.setFromSquare(sqi);
					int piece = game.getPosition().getPiece(sqi);
					String what = ChessUtils.pieceToStr(piece).toUpperCase();
					MiscUtil.statusMessage(player,
							Messages.getString("ChessPlayerListener.pieceSelected", what, Chess.sqiToStr(sqi))); //$NON-NLS-1$
					ChessUtils.playEffect(player.getLocation(), "piece_selected");
				}
			} else {
				if (sqi == game.getFromSquare()) {
					// cancel a selected piece
					game.setFromSquare(Chess.NO_SQUARE);
					MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.moveCancelled")); //$NON-NLS-1$
					ChessUtils.playEffect(player.getLocation(), "piece_unselected");
				} else if (sqi >= 0 && sqi < Chess.NUM_OF_SQUARES) {
					// try to move the selected piece
					game.doMove(player.getName(), sqi);
					MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed",
							game.getPosition().getLastMove().getLAN())); //$NON-NLS-1$
				}
			}
		} else if (game.isPlayerInGame(player.getName())) {
			MiscUtil.errorMessage(player, Messages.getString("ChessPlayerListener.notYourTurn")); //$NON-NLS-1$
		}
	}

	private void teleportToPiece(Player player, BoardView bv, Location loc) {
		Block b = loc.getBlock();
		Block b1 = b.getRelative(BlockFace.UP);
		boolean isSolid = !BlockType.canPassThrough(bv.getEnclosureMaterial().getId());
		int max = isSolid ? bv.getOuterBounds().getUpperY() - 2 : loc.getWorld().getMaxHeight();
		while (b.getType() != Material.AIR && b1.getType() != Material.AIR && b1.getLocation().getY() < max) {
			b = b.getRelative(BlockFace.UP);
			b1 = b1.getRelative(BlockFace.UP);
		}
		if (b1.getY() < max) {
			Location dest = b1.getLocation();
			dest.setYaw(player.getLocation().getYaw());
			dest.setPitch(player.getLocation().getPitch());
			teleportPlayer(player, dest);
		}
	}

	private void boardClicked(Player player, Location loc, BoardView bv) throws IllegalMoveException, ChessException {
		int sqi = bv.getSquareAt(loc);
		ChessGame game = bv.getGame();
		if (game != null && game.getFromSquare() != Chess.NO_SQUARE) {
			game.doMove(player.getName(), sqi);
			MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed", //$NON-NLS-1$
					game.getPosition().getLastMove().getLAN()));
		} else {
			if (player.isSneaking()) {
				MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.squareMessage", //$NON-NLS-1$
				                                                    Chess.sqiToStr(sqi), bv.getName()));
			}
			if (bv.isPartOfBoard(player.getLocation())) {
				// allow teleporting around the board, but only if the player is 
				// already on the board
				Location newLoc = loc.clone().add(0, 1.0, 0);
				newLoc.setPitch(player.getLocation().getPitch());
				newLoc.setYaw(player.getLocation().getYaw());
				teleportPlayer(player, newLoc);
			}
		}
	}
	
	private void teleportPlayer(Player player, Location dest) {
		ChessUtils.playEffect(player.getLocation(), "teleport_from");
		player.teleport(dest);
		ChessUtils.playEffect(dest, "teleport_to");
	}

	private long lastAnimationEvent(Player player) {
		if (!lastAnimation.containsKey(player.getName())) {
			lastAnimation.put(player.getName(), 0L);
		}
		return lastAnimation.get(player.getName());
	}


}

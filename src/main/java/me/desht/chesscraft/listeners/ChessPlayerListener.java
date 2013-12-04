package me.desht.chesscraft.listeners;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.chesscraft.expector.ExpectInvitePlayer;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import me.desht.dhutils.responsehandler.ResponseHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ChessPlayerListener extends ChessListenerBase {

	public ChessPlayerListener(ChessCraft plugin) {
		super(plugin);
	}

	// block ids to be considered transparent when calling player.getTargetBlock()
	private static final HashSet<Byte> transparent = new HashSet<Byte>();
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
				default:
					break;
				}
				event.setCancelled(true);
			} else {
				BoardView bv = BoardViewManager.getManager().partOfChessBoard(b.getLocation(), 0);
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
				Material wandMat = ChessUtils.getWandMaterial();
				if (wandMat == null || player.getItemInHand().getType() == wandMat) {
					targetBlock = player.getTargetBlock(transparent, 120);
					LogUtils.finer("Player " + player.getName() + " waved at block " + targetBlock);
					Location loc = targetBlock.getLocation();
					BoardView bv;
					if ((bv = BoardViewManager.getManager().onChessBoard(loc)) != null) {
						boardClicked(player, loc, bv);
					} else if ((bv = BoardViewManager.getManager().aboveChessBoard(loc)) != null) {
						pieceClicked(player, loc, bv);
					} else if ((bv = BoardViewManager.getManager().partOfChessBoard(loc, 0)) != null) {
						if (bv.isControlPanel(loc)) {
							Location tpLoc = bv.getControlPanel().getTeleportLocation();
							Cuboid zone = bv.getControlPanel().getPanelBlocks().outset(CuboidDirection.Horizontal, 4);
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
			ChessCraft.getInstance().getFX().playEffect(player.getLocation(), "piece_unselected");
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
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlockClicked().getLocation(), 0) != null) {
			event.setCancelled(true);
			// seems just cancelling the event doesn't stop the bucket getting filled?
			event.setItemStack(new ItemStack(Material.BUCKET, 1));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlockClicked().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getFrom(), 0) != null) {
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
		BoardView bv = BoardViewManager.getManager().onChessBoard(loc);
		if (bv == null) {
			bv = BoardViewManager.getManager().aboveChessBoard(loc);
		}
		if (bv != null && bv.getGame() != null) {
			bv.getChessBoard().setSelectedSquare(Chess.NO_SQUARE);
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

		int clickedSqi = game.getView().getSquareAt(loc);
		int selectedSqi = bv.getChessBoard().getSelectedSquare();
		if (game.isPlayerToMove(player.getName())) {
			if (selectedSqi == Chess.NO_SQUARE) {
				// select the piece for moving (if it belongs to the player)
				int colour = game.getPosition().getColor(clickedSqi);
				if (colour == game.getPosition().getToPlay()) {
					bv.getChessBoard().setSelectedSquare(clickedSqi);
					int piece = game.getPosition().getPiece(clickedSqi);
					String what = ChessUtils.pieceToStr(piece).toUpperCase();
					if (plugin.getConfig().getBoolean("verbose")) {
						MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.pieceSelected", what, Chess.sqiToStr(clickedSqi))); //$NON-NLS-1$
					}
					plugin.getFX().playEffect(player.getLocation(), "piece_selected");
				}
			} else {
				if (clickedSqi == selectedSqi) {
					// cancel a selected piece
					bv.getChessBoard().setSelectedSquare(Chess.NO_SQUARE);
					if (plugin.getConfig().getBoolean("verbose")) {
						MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.moveCancelled")); //$NON-NLS-1$
					}
					plugin.getFX().playEffect(player.getLocation(), "piece_unselected");
				} else if (clickedSqi >= 0 && clickedSqi < Chess.NUM_OF_SQUARES) {
					// try to move the selected piece
					game.doMove(player.getName(), selectedSqi, clickedSqi);
					if (plugin.getConfig().getBoolean("verbose")) {
						MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed",
						                                                  game.getPosition().getLastMove().getSAN())); //$NON-NLS-1$
					}
				}
			}
		} else if (game.isPlayerInGame(player.getName())) {
			MiscUtil.errorMessage(player, Messages.getString("ChessPlayerListener.notYourTurn")); //$NON-NLS-1$
		} else {
			MiscUtil.errorMessage(player, Messages.getString("Game.notInGame")); //$NON-NLS-1$
		}
	}

	private void boardClicked(Player player, Location loc, BoardView bv) throws IllegalMoveException, ChessException {
		ChessGame game = bv.getGame();
		int clickedSqi = bv.getSquareAt(loc);
		int colour = game == null ? Chess.NOBODY : game.getPosition().getColor(clickedSqi);
		int selectedSqi = bv.getChessBoard().getSelectedSquare();

		if (game != null && selectedSqi != Chess.NO_SQUARE && selectedSqi != clickedSqi) {
			// a square is already selected; attempt to move the piece in that square to the clicked square
			game.doMove(player.getName(), selectedSqi, clickedSqi);
			if (plugin.getConfig().getBoolean("verbose")) {
				MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.youPlayed", //$NON-NLS-1$
				                                                  game.getPosition().getLastMove().getSAN()));
			}
		} else if (game != null && game.isPlayerToMove(player.getName()) && colour == game.getPosition().getToPlay()) {
			// clicking the square that a piece of our colour is on is equivalent to selecting the piece, if it's our move
			pieceClicked(player, loc, bv);
		} else {
			// just try to teleport to the square, if the player is already on the board, and the square is empty
			if (player.isSneaking()) {
				MiscUtil.statusMessage(player, Messages.getString("ChessPlayerListener.squareMessage", //$NON-NLS-1$
				                                                  Chess.sqiToStr(clickedSqi), bv.getName()));
			}
			if (bv.isPartOfBoard(player.getLocation()) && player.getLocation().distanceSquared(loc) >= 16 && colour == Chess.NOBODY) {
				Location newLoc = loc.clone().add(0, 1.0, 0);
				newLoc.setPitch(player.getLocation().getPitch());
				newLoc.setYaw(player.getLocation().getYaw());
				teleportPlayer(player, newLoc);
			}
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

	private void teleportPlayer(Player player, Location dest) {
		plugin.getFX().playEffect(player.getLocation(), "teleport_out");
		player.teleport(dest);
		plugin.getFX().playEffect(dest, "teleport_in");
	}

	private long lastAnimationEvent(Player player) {
		if (!lastAnimation.containsKey(player.getName())) {
			lastAnimation.put(player.getName(), 0L);
		}
		return lastAnimation.get(player.getName());
	}


}

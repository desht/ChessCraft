package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TeleportCommand extends AbstractCommand {

	public TeleportCommand() {
		super("chess tp", 0, 2);
		setPermissionNode("chesscraft.commands.teleport");
		setUsage(new String[] {
				"/chess tp [<game-name>]",
				"/chess tp -b <board-name>",
				"/chess tp -set [<board-name>]",
				"/chess tp -clear [<board-name>]",
				"/chess tp -list"
		});
		setOptions(new String[] { "b", "set", "clear", "list" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		if (getBooleanOption("list")) {
			showTeleportDests(sender);
			return true;
		}
		notFromConsole(sender);
		
		if (!ChessCraft.getInstance().getConfig().getBoolean("teleporting")) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.noTeleporting"));
		}
		
		Player player = (Player)sender;
		
		if (getBooleanOption("set")) {
			PermissionUtils.requirePerms(sender, "chesscraft.commands.teleport.set");
			if (args.length == 0) {
				// set global teleport-out location
				BoardViewManager.getManager().setGlobalTeleportOutDest(player.getLocation());
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.globalTeleportSet")); 
			} else {
				// set per-board teleport-out location
				BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
				bv.setTeleportDestination(player.getLocation());
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.boardTeleportSet", bv.getName()));
			}
		} else if (getBooleanOption("clear")) {
			PermissionUtils.requirePerms(sender, "chesscraft.commands.teleport.set");
			if (args.length == 0) {
				// clear global teleport-out location
				BoardViewManager.getManager().setGlobalTeleportOutDest(null);
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.globalTeleportCleared"));
			} else {
				// clear per-board teleport-out location
				BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
				bv.setTeleportDestination(null);
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.boardTeleportCleared", bv.getName()));
			}
		} else if (getBooleanOption("b") && args.length > 0) {
			// teleport to board
			PermissionUtils.requirePerms(sender, "chesscraft.commands.teleport.board");
			BoardViewManager.getManager().getBoardView(args[0]).summonPlayer(player);
		} else if (args.length == 0) {
			// teleport out of (or back to) current game
			BoardViewManager.getManager().teleportOut(player);
		} else {
			// teleport to game
			ChessGame game = ChessGameManager.getManager().getGame(args[0], true);
			game.getView().summonPlayer(player);
		}
		
		return true;
	}

	private void showTeleportDests(CommandSender sender) {
		String bullet = MessagePager.BULLET + ChatColor.DARK_PURPLE;
		MessagePager pager = MessagePager.getPager(sender).clear();
		Location loc = BoardViewManager.getManager().getGlobalTeleportOutDest();
		if (loc != null) {
			pager.add(bullet + ChatColor.YELLOW + "[GLOBAL]" + ChatColor.WHITE + ": " + MiscUtil.formatLocation(loc));
		}
		for (BoardView bv : BoardViewManager.getManager().listBoardViewsSorted()) {
			if (bv.hasTeleportDestination()) {
				loc = bv.getTeleportDestination();
				pager.add(bullet + ChatColor.YELLOW + bv.getName() + ChatColor.WHITE + ": " + MiscUtil.formatLocation(loc));
			}
		}
		pager.showPage();
	}

}

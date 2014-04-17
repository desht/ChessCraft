package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ListGameCommand extends ChessAbstractCommand {

	private static final String TO_MOVE = ChatColor.GOLD + "\u261e " + ChatColor.RESET;

	public ListGameCommand() {
		super("chess list game", 0, 1);
		setPermissionNode("chesscraft.commands.list.game");
		setUsage("/chess list game");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (ChessGameManager.getManager().listGames().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.noCurrentGames"));
			return true;
		}

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);

		if (args.length >= 1) {
			List<String> l = ChessGameManager.getManager().getGame(args[0]).getGameDetail();
			pager.add(l);
		} else {
			for (ChessGame game : ChessGameManager.getManager().listGamesSorted()) {
				String name = game.getName();
				if (sender instanceof Player && game == ChessGameManager.getManager().getCurrentGame((Player) sender)) {
					name = ChatColor.BOLD + ChatColor.ITALIC.toString() + name + ChatColor.RESET;
				}
				String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? TO_MOVE : "";
				String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? TO_MOVE : "";
				String white = game.hasPlayer(Chess.WHITE) ? game.getPlayer(Chess.WHITE).getDisplayName() : "?";
				String black = game.hasPlayer(Chess.BLACK) ? game.getPlayer(Chess.BLACK).getDisplayName() : "?";
				String line = String.format(MessagePager.BULLET + "%s: &f%s%s (%s) v %s%s (%s)",
				                            name,
				                            curMoveW, white, ChessUtils.getDisplayColour(Chess.WHITE),
				                            curMoveB, black, ChessUtils.getDisplayColour(Chess.BLACK));
				if (game.getInvited() != null) {
					line += Messages.getString("ChessCommandExecutor.invited", game.getInvited());
				}
				pager.add(line);
			}
		}
		pager.showPage();

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getGameCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}

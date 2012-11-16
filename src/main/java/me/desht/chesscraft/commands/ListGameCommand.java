package me.desht.chesscraft.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

public class ListGameCommand extends AbstractCommand {

	private static final String TO_MOVE = ChatColor.GOLD + "\u261e " + ChatColor.RESET;

	public ListGameCommand() {
		super("chess l g", 0, 1);
		setPermissionNode("chesscraft.commands.list.game");
		setUsage("/chess list game");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (ChessGameManager.getManager().listGames().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.noCurrentGames")); //$NON-NLS-1$
			return true;
		}

		MessagePager pager = MessagePager.getPager(sender).clear();

		if (args.length >= 1) {
			List<String> l = ChessGameManager.getManager().getGame(args[0]).getGameDetail();
			pager.add(l);
		} else {
			for (ChessGame game : ChessGameManager.getManager().listGames(true)) {
				String name = game.getName();
				if (game == ChessGameManager.getManager().getCurrentGame(sender.getName())) {
					name = ChatColor.BOLD + ChatColor.ITALIC.toString() + name + ChatColor.RESET;
				}
				String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? TO_MOVE : ""; //$NON-NLS-1$ //$NON-NLS-2$
				String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? TO_MOVE : ""; //$NON-NLS-1$ //$NON-NLS-2$
				String white = game.hasPlayer(Chess.WHITE) ? game.getPlayer(Chess.WHITE).getDisplayName() : "?"; //$NON-NLS-1$
				String black = game.hasPlayer(Chess.BLACK) ? game.getPlayer(Chess.BLACK).getDisplayName() : "?"; //$NON-NLS-1$
				String line = String.format(MessagePager.BULLET + "%s: &f%s%s (%s) v %s%s (%s)",
				                            name,
				                            curMoveW, white, ChessUtils.getDisplayColour(Chess.WHITE),
				                            curMoveB, black, ChessUtils.getDisplayColour(Chess.BLACK));
				if (game.getInvited().length() > 0) {
					line += Messages.getString("ChessCommandExecutor.invited", game.getInvited()); //$NON-NLS-1$
				}
				pager.add(line);
			}
		}
		pager.showPage();

		return true;
	}

}

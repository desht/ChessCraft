package me.desht.chesscraft.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

public class ListGameCommand extends AbstractCommand {

	public ListGameCommand() {
		super("chess l g", 0, 1);
		setPermissionNode("chesscraft.commands.list.game");
		setUsage("/chess list game");
	}
	
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (ChessGame.listGames().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.noCurrentGames")); //$NON-NLS-1$
			return true;
		}
		
		if (args.length >= 1) {
			ChessGame.getGame(args[0]).showGameDetail(sender);
			return true;
		}
	
		MessagePager pager = MessagePager.getPager(sender).clear();
		for (ChessGame game : ChessGame.listGames(true)) {
			String name = game.getName();
			String curGameMarker = "  "; //$NON-NLS-1$
			if (sender != null) {
				curGameMarker = game == ChessGame.getCurrentGame(sender.getName()) ? "+ " : "  "; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? "&4*&-" : ""; //$NON-NLS-1$ //$NON-NLS-2$
			String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? "&4*&-" : ""; //$NON-NLS-1$ //$NON-NLS-2$
			String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite(); //$NON-NLS-1$
			String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack(); //$NON-NLS-1$
			StringBuilder info = new StringBuilder(": &f" + curMoveW + white + " (W) v " + curMoveB + black + " (B) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			info.append("&e[").append(game.getState()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			if (game.getInvited().length() > 0) {
				info.append(Messages.getString("ChessCommandExecutor.invited", game.getInvited())); //$NON-NLS-1$
			}
			pager.add(curGameMarker + name + info);
		}
		pager.showPage();
		
		return true;
	}
	
}

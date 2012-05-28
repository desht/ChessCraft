package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class PageCommand extends AbstractCommand {

	public PageCommand() {
		super("chess pa", 0, 1);
		setUsage("/chess page");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		MessagePager pager = MessagePager.getPager(sender);
		if (args.length < 1) {
			// default is to advance one page and display
			pager.nextPage();
			pager.showPage();
		} else if (args[0].startsWith("n")) { //$NON-NLS-1$
			pager.nextPage();
			pager.showPage();
		} else if (args[0].startsWith("p")) { //$NON-NLS-1$
			pager.prevPage();
			pager.showPage();
		} else {
			try {
				int pageNum = Integer.parseInt(args[0]);
				pager.showPage(pageNum);
			} catch (NumberFormatException e) {
				MiscUtil.errorMessage(sender, Messages.getString("ChessCommandExecutor.invalidNumeric", args[0])); //$NON-NLS-1$
			}
		}
		return true;
	}

}


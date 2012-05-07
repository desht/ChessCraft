package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessagePager;

public class PageCommand extends AbstractCommand {

	public PageCommand() {
		super("chess pa", 0, 1);
		setUsage("/chess page");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		MessagePager pager = MessagePager.getPager(player);
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
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.invalidNumeric", args[0])); //$NON-NLS-1$
			}
		}
		return true;
	}

}


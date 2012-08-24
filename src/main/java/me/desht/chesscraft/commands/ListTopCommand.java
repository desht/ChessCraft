package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.results.ScoreRecord;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListTopCommand extends AbstractCommand {

	public ListTopCommand() {
		super("chess l t", 0, 3);
		setPermissionNode("chesscraft.commands.list.top");
		setUsage("/chess list top [<n>] [ladder|list] [-ai]");
	}
	
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		Results results = Results.getResultsHandler();
		if (results == null) {
			throw new ChessException("Results are not available.");
		}
		int n = 5;
		if (args.length > 0) {
			try {
				n = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", args[1]));
			}
		}
		String viewName = "ladder";
		if (args.length > 1) {
			viewName = args[1];
		}
		boolean excludeAI = false;
		if (args.length > 2 && args[2].equals("-ai")) {
			excludeAI = true;
		}
		
		MessagePager pager = MessagePager.getPager(sender).clear();
		int row = 1;
		for (ScoreRecord sr : results.getView(viewName).getScores(n, excludeAI)) {
			pager.add(Messages.getString("ChessCommandExecutor.scoreRecord", row, sr.getPlayer(), sr.getScore()));
			row++;
		}
		pager.showPage();
		
		return true;
	}

}

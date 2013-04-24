package me.desht.chesscraft.commands;

import java.util.Arrays;
import java.util.List;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.results.ScoreRecord;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListTopCommand extends ChessAbstractCommand {

	public ListTopCommand() {
		super("chess list top", 0, 3);
		setPermissionNode("chesscraft.commands.list.top");
		setUsage("/chess list top [<n>] [ladder|league] [-ai] [-r]");
		setOptions(new String[] { "ai", "r" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		Results results = Results.getResultsHandler();
		if (results == null) {
			throw new ChessException("Results are not available.");
		}
		if (getBooleanOption("r")) {
			results.rebuildViews();
		}
		int n = 5;
		if (args.length > 0) {
			try {
				n = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", args[0]));
			}
		}
		String viewName = args.length > 1 ? args[1] : "ladder";
		boolean excludeAI = getBooleanOption("ai");

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
		int row = 1;
		for (ScoreRecord sr : results.getView(viewName).getScores(n, excludeAI)) {
			pager.add(MessagePager.BULLET + Messages.getString("ChessCommandExecutor.scoreRecord", row, sr.getPlayer(), sr.getScore()));
			row++;
		}
		pager.showPage();

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 2) {
			return filterPrefix(sender, Arrays.asList(new String[] { "ladder", "league" }), args[1]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}

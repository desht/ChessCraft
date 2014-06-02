package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.ai.AIFactory.AIDefinition;
import me.desht.chesscraft.util.EconomyUtil;
import me.desht.dhutils.MessagePager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ListAICommand extends ChessAbstractCommand {

	public ListAICommand() {
		super("chess list ai", 0, 1);
		setPermissionNode("chesscraft.commands.list.ai");
		setUsage("/chess list ai");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);

		if (args.length == 0) {
			List<AIDefinition> aiDefs = AIFactory.getInstance().listAIDefinitions(true);
			List<String> lines = new ArrayList<String>(aiDefs.size());
			for (AIDefinition aiDef : aiDefs) {
				if (!aiDef.isEnabled())
					continue;
				StringBuilder sb = new StringBuilder(Messages.getString("ChessCommandExecutor.AIList",
				                                                        aiDef.getName(), aiDef.getImplClassName(),
				                                                        aiDef.getComment()));
				if (EconomyUtil.enabled()) {
					sb.append(", ").append(Messages.getString("ChessCommandExecutor.AIpayout", (int) (aiDef.getPayoutMultiplier() * 100)));
				}

				lines.add(MessagePager.BULLET +  sb.toString());
			}
			pager.add(lines);
		} else {
			AIDefinition aiDef = AIFactory.getInstance().getAIDefinition(args[0], true);
			pager.add(aiDef.getDetails());
		}

		pager.showPage();
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getPlayerCompletions(plugin, sender, args[0], true);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}

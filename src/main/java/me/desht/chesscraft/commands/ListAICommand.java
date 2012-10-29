package me.desht.chesscraft.commands;

import java.util.LinkedList;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.ai.AIFactory.AIDefinition;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MinecraftChatStr;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ListAICommand extends AbstractCommand {

	public ListAICommand() {
		super("chess l a", 0, 1);
		setPermissionNode("chesscraft.commands.list.ai");
		setUsage("/chess list ai");
	}
	
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		
		MessagePager pager = MessagePager.getPager(sender).clear();
		
		if (args.length == 0) {
			LinkedList<String> lines = new LinkedList<String>();
			for (AIDefinition aiDef : AIFactory.instance.listAIDefinitions(true)) {
				StringBuilder sb = new StringBuilder(Messages.getString("ChessCommandExecutor.AIList", //$NON-NLS-1$
				                                                        aiDef.getName(), aiDef.getEngine(),
				                                                        aiDef.getParams().getInt("depth", 0)));
				if (ChessCraft.economy != null) {
					sb.append(sender instanceof Player ? "<l>" : ", "); //$NON-NLS-1$ //$NON-NLS-2$
					sb.append(Messages.getString("ChessCommandExecutor.AIpayout", (int) (aiDef.getPayoutMultiplier() * 100))); //$NON-NLS-1$
				}

				if (aiDef.getComment() != null && sender != null && ((lines.size() + 1) % MessagePager.getPageSize()) == 0) {
					lines.add(""); // ensure description and comment are on the same page  $NON-NLS-1$
				}
				lines.add(sb.toString());
				if (aiDef.getComment() != null) {
					lines.add("  &2 - " + aiDef.getComment()); //$NON-NLS-1$
				}
			}
			lines = MinecraftChatStr.alignTags(lines, true);
			pager.add(lines);
		} else {
			AIDefinition aiDef = AIFactory.instance.getAIDefinition(args[0], true);
			pager.add(aiDef.getDetails());
		}

		pager.showPage();
		return true;
	}

}

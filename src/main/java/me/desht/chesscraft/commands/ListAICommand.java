package me.desht.chesscraft.commands;

import java.util.LinkedList;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessAI;
import me.desht.chesscraft.chess.ChessAI.AI_Def;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MinecraftChatStr;
import me.desht.dhutils.commands.AbstractCommand;

public class ListAICommand extends AbstractCommand {

	public ListAICommand() {
		super("chess l a", 0, 0);
		setPermissionNode("chesscraft.commands.list.ai");
		setUsage("/chess list ai");
	}
	
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		
		MessagePager pager = MessagePager.getPager(sender).clear();
		LinkedList<String> lines = new LinkedList<String>();
		for (AI_Def ai : ChessAI.listAIs(true)) {
			StringBuilder sb = new StringBuilder(Messages.getString("ChessCommandExecutor.AIList", //$NON-NLS-1$
			                                                        ai.getName(), ai.getEngine(), ai.getSearchDepth()));
			if (ChessCraft.economy != null) {
				sb.append(sender != null ? "<l>" : ", "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(Messages.getString("ChessCommandExecutor.AIpayout", (int) (ai.getPayoutMultiplier() * 100))); //$NON-NLS-1$
			}

			if (ai.getComment() != null && sender != null && ((lines.size() + 1) % MessagePager.getPageSize()) == 0) {
				lines.add(""); // ensure description and comment are on the same page  $NON-NLS-1$
			}
			lines.add(sb.toString());
			if (ai.getComment() != null) {
				lines.add("  &2 - " + ai.getComment()); //$NON-NLS-1$
			}
		}
		lines = MinecraftChatStr.alignTags(lines, true);
		pager.add(lines);
		pager.showPage();
		
		return true;
	}

}

package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.dhutils.MessagePager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ListStylesCommand extends ChessAbstractCommand {

	public ListStylesCommand() {
		super("chess list style", 0, 1);
		setPermissionNode("chesscraft.commands.list.style");
		setUsage("/chess list style [-b] [-p]");
		setOptions(new String[] { "b", "p" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		boolean showAll = !hasOption("b") && !hasOption("p");

		MessagePager p = MessagePager.getPager(sender).clear().setParseColours(true);

		if (showAll || hasOption("b")) {
			List<BoardStyle> l = getAllBoardStyles();
			p.add(ChatColor.AQUA.toString() + l.size() + " board styles:");
			for (BoardStyle b : l) {
				int sq = b.getSquareSize();
				int h = b.getHeight();
				int f = b.getFrameWidth();
				String cus = b.isCustom() ? ChatColor.GOLD + " [c]" : "";
				p.add(MessagePager.BULLET + String.format("%s%s&e: sq=%d, h=%d, f=%d, (%dx%dx%d), piece=%s",
				                                          b.getName(), cus, sq, h, f, (sq * 8) + (f * 2), (sq * 8) + (f * 2),
				                                          h, b.getPieceStyleName()));
			}
		}
		if (showAll || hasOption("p")) {
			List<ChessSet> l = getAllPieceStyles();
			p.add(ChatColor.AQUA.toString() + l.size() + " piece styles:");
			for (ChessSet cs : l) {
				int w = cs.getMaxWidth();
				int h = cs.getMaxHeight();
				String cus = cs.isCustom() ? ChatColor.GOLD + " [c]" : "";
				p.add(MessagePager.BULLET + String.format("%s%s&e: w=%d, h=%d", cs.getName(), cus, w, h));
			}
		}
		p.showPage();

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}
}

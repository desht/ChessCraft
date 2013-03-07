package me.desht.chesscraft.commands;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.chess.pieces.ChessSetFactory;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListStylesCommand extends AbstractCommand {

	public ListStylesCommand() {
		super("chess l s", 0, 1);
		setPermissionNode("chesscraft.commands.list.style");
		setUsage("/chess list style");
		setOptions(new String[] { "b", "p" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		boolean showAll = !hasOption("b") && !hasOption("p");

		MessagePager p = MessagePager.getPager(sender).clear();

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

	private List<ChessSet> getAllPieceStyles() {
		Map<String,ChessSet> res = new HashMap<String, ChessSet>();

		File dir = DirectoryStructure.getPieceStyleDirectory();
		File customDir = new File(dir, "custom");

		for (File f : customDir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			res.put(styleName, ChessSetFactory.getChessSet(styleName));
		}
		for (File f : dir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			if (res.containsKey(styleName)) continue;
			res.put(styleName, ChessSetFactory.getChessSet(styleName));
		}
		return MiscUtil.asSortedList(res.values());
	}

	private List<BoardStyle> getAllBoardStyles() {
		Map<String, BoardStyle> res = new HashMap<String, BoardStyle>();

		File dir = DirectoryStructure.getBoardStyleDirectory();
		File customDir = new File(dir, "custom");

		for (File f : customDir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			res.put(styleName, BoardStyle.loadStyle(styleName));
		}
		for (File f : dir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			if (res.containsKey(styleName)) continue;
			res.put(styleName, BoardStyle.loadStyle(styleName));
		}
		return MiscUtil.asSortedList(res.values());
	}
}

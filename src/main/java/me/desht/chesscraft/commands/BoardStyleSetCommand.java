package me.desht.chesscraft.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.event.ChessBoardModifiedEvent;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.AttributeCollection;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BoardStyleSetCommand extends ChessAbstractCommand {

	public BoardStyleSetCommand() {
		super("chess board set", 2);
		setPermissionNode("chesscraft.commands.board.set");
		setUsage("/chess board set <attribute> <value> [<attribute> <value>...]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		if (args.length % 2 != 0) {
			showUsage(sender);
			return true;
		}

		BoardView bv = BoardViewManager.getManager().partOfChessBoard(((Player)sender).getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		if (bv.isDesigning()) {
			throw new ChessException(Messages.getString("Game.boardInDesignMode"));
		}
		BoardStyle style = bv.getChessBoard().getBoardStyle();
		AttributeCollection viewAttrs = bv.getAttributes();
		AttributeCollection styleAttrs = style.getAttributes();
		boolean styleHasChanged = false;
		Set<String> changedAttrs = new HashSet<String>();

		for (int i = 0; i < args.length; i += 2) {
			String attr = args[i];
			String val = args[i + 1];

			if (styleAttrs.contains(attr)) {
				styleAttrs.set(attr, val);
				styleHasChanged = true;
			} else if (viewAttrs.contains(attr)) {
				viewAttrs.set(attr, val);
			} else {
				throw new ChessException("Unknown attribute '" + attr + "'.");
			}

			changedAttrs.add(attr);
		}

		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardStyleChanged", bv.getName()));
		if (styleHasChanged) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardStyleSuggestSave"));
			bv.paintAll();
		} else if (bv.getChessBoard().isRedrawNeeded()) {
			bv.paintAll();
		} else {
			bv.getControlPanel().repaintAll(null);
		}

		bv.save();

		Bukkit.getPluginManager().callEvent(new ChessBoardModifiedEvent(bv, changedAttrs));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(((Player)sender).getLocation());
		if (bv == null) {
			return noCompletions(sender);
		}
		int l = args.length;
		AttributeCollection styleAttrs = bv.getChessBoard().getBoardStyle().getAttributes();
		AttributeCollection viewAttrs = bv.getAttributes();
		if (args.length % 2 == 1) {
			// provide attribute completions
			List<String> attrs = new ArrayList<String>(styleAttrs.listAttributeKeys(false));
			attrs.addAll(new ArrayList<String>(viewAttrs.listAttributeKeys(false)));
			return filterPrefix(sender, attrs, args[l - 1]);
		} else {
			// provide value completions for last attribute
			String attr = args[l - 2];
			String desc = styleAttrs.contains(attr) ? styleAttrs.getDescription(attr) : viewAttrs.getDescription(attr);
			Object o = styleAttrs.contains(attr) ? styleAttrs.get(attr) : viewAttrs.get(attr);
			if (!desc.isEmpty())
				desc = ChatColor.GRAY.toString() + ChatColor.ITALIC + " [" + desc + "]";
			return getConfigValueCompletions(sender, attr, o, desc, args[l - 1]);
		}
	}
}

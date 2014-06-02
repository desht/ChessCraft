package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.chess.*;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.ai.AIFactory.AIDefinition;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.chess.pieces.ChessSetFactory;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public abstract class ChessAbstractCommand extends AbstractCommand {

	public ChessAbstractCommand(String label, int minArgs, int maxArgs) {
		super(label, minArgs, maxArgs);
	}

	public ChessAbstractCommand(String label, int minArgs) {
		super(label, minArgs);
	}

	public ChessAbstractCommand(String label) {
		super(label);
	}

	protected List<String> getGameCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();

		for (ChessGame game : ChessGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix)) {
				res.add(game.getName());
			}
		}
		return getResult(res, sender, true);
	}


	protected List<String> getPlayerInGameCompletions(Plugin plugin, CommandSender sender, String prefix) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			List<String> res = new ArrayList<String>();
			for (ChessGame game : ChessGameManager.getManager().listGames()) {
				if (game.getName().startsWith(prefix) && game.getPlayer(player.getUniqueId().toString()) != null) {
					res.add(game.getName());
				}
			}
			return getResult(res, sender, true);
		} else {
			return noCompletions(sender);
		}
	}

	protected List<String> getBoardCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();

		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			if (bv.getName().startsWith(prefix)) {
				res.add(bv.getName());
			}
		}
		return getResult(res, sender, true);
	}

	protected List<String> getBoardStyleCompletions(Plugin plugin, CommandSender sender, String prefix) {
		return filterPrefix(sender, getAllBoardStyleNames(), prefix);
	}

	protected List<String> getPieceStyleCompletions(Plugin plugin, CommandSender sender, String prefix) {
		return filterPrefix(sender, getAllPieceStyleNames(), prefix);
	}

	protected List<String> getPlayerCompletions(Plugin plugin, CommandSender sender, String prefix, boolean aiOnly) {
		List<String> res = new ArrayList<String>();
		if (!aiOnly) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				res.add(p.getName());
			}
		}
		for (AIDefinition aiDef : AIFactory.getInstance().listAIDefinitions()) {
			if (!aiDef.isEnabled())
				continue;
			res.add(aiDef.getName());
		}
		return filterPrefix(sender, res, prefix);
	}

	protected List<ChessSet> getAllPieceStyles() {
		Map<String,ChessSet> res = new HashMap<String, ChessSet>();
		for (String styleName : getAllPieceStyleNames()) {
			try {
				res.put(styleName, ChessSetFactory.getChessSet(styleName));
			} catch (ChessException e) {
				// bad piece style?
			}
		}
		return MiscUtil.asSortedList(res.values());
	}

	protected List<BoardStyle> getAllBoardStyles() {
		Map<String, BoardStyle> res = new HashMap<String, BoardStyle>();
		for (String styleName : getAllBoardStyleNames()) {
			try {
				res.put(styleName, BoardStyle.loadStyle(styleName));
			} catch (ChessException e) {
				// bad board style?
			}
		}
		return MiscUtil.asSortedList(res.values());
	}

	protected List<String> getAllBoardStyleNames() {
		return getAllStyleNames(DirectoryStructure.getBoardStyleDirectory());
	}

	protected List<String> getAllPieceStyleNames() {
		return getAllStyleNames(DirectoryStructure.getPieceStyleDirectory());
	}

	private List<String> getAllStyleNames(File dir) {
		Set<String> res = new HashSet<String>();
		for (File f : new File(dir, "custom").listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			res.add(styleName);
		}
		for (File f : dir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			res.add(styleName);
		}
		return MiscUtil.asSortedList(res);
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}
}

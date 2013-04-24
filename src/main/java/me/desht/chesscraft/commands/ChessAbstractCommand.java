package me.desht.chesscraft.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.ai.AIFactory.AIDefinition;
import me.desht.chesscraft.chess.pieces.ChessSet;
import me.desht.chesscraft.chess.pieces.ChessSetFactory;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

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
		List<String> res = new ArrayList<String>();

		for (ChessGame game : ChessGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix) && game.getPlayerColour(sender.getName()) != Chess.NOBODY) {
				res.add(game.getName());
			}
		}
		return getResult(res, sender, true);
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
		List<String> styleNames = new ArrayList<String>();
		for (BoardStyle style : getAllBoardStyles()) {
			styleNames.add(style.getName());
		}
		return filterPrefix(sender, styleNames, prefix);
	}


	protected List<String> getPieceStyleCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> styleNames = new ArrayList<String>();
		for (ChessSet style : getAllPieceStyles()) {
			styleNames.add(style.getName());
		}
		return filterPrefix(sender, styleNames, prefix);
	}

	protected List<String> getPlayerCompletions(Plugin plugin, CommandSender sender, String prefix, boolean aiOnly) {
		List<String> res = new ArrayList<String>();
		if (!aiOnly) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				res.add(p.getName());
			}
		}
		for (AIDefinition aiDef : AIFactory.instance.listAIDefinitions()) {
			if (!aiDef.isEnabled())
				continue;
			res.add(aiDef.getName());
		}
		return filterPrefix(sender, res, prefix);
	}

	protected List<ChessSet> getAllPieceStyles() {
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

	protected List<BoardStyle> getAllBoardStyles() {
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

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}
}

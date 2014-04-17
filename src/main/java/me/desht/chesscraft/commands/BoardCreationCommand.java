package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class BoardCreationCommand extends ChessAbstractCommand {

	public BoardCreationCommand() {
		super("chess board create", 1);
		addAlias("chess create board");	// backwards compat
		setPermissionNode("chesscraft.commands.create.board");
		setUsage("/chess board create <board-name> [-style <style-name>] [-pstyle <style-name>]");
		setOptions("style:s", "pstyle:s");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player) sender;

		if (args.length == 0 || args[0].startsWith("-")) {
			showUsage(sender);
			return true;
		}

		String name = args[0];

		String boardStyleName = getStringOption("style", "Standard");
		String pieceStyleName = getStringOption("pstyle", "");

		// this will throw an exception if the styles are in any way invalid or incompatible
		BoardStyle.verifyCompatibility(boardStyleName, pieceStyleName);

		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardCreationPrompt", name));
		ChessCraft.getInstance().responseHandler.expect(player, new ExpectBoardCreation(name, boardStyleName, pieceStyleName));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length >= 2 && args[args.length - 2].equals("-style")) {
			return getBoardStyleCompletions(plugin, sender, args[args.length - 1]);
		} else if (args.length >= 2 && args[args.length - 2].equals("-pstyle")) {
			return getPieceStyleCompletions(plugin, sender, args[args.length - 1]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

}

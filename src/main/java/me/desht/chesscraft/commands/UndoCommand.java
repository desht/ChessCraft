package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class UndoCommand extends ChessAbstractCommand {

	public UndoCommand() {
		super("chess undo");
		setPermissionNode("chesscraft.commands.undo");
		setUsage("/chess undo");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(sender.getName(), true);

		game.offerUndoMove(sender.getName());

		return true;
	}

}

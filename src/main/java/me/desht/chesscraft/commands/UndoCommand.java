package me.desht.chesscraft.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.commands.AbstractCommand;

public class UndoCommand extends AbstractCommand {

	public UndoCommand() {
		super("chess u");
		setPermissionNode("chesscraft.commands.undo");
		setUsage("/chess undo");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		ChessGame game = ChessGame.getCurrentGame(sender.getName(), true);
		
		game.offerUndoMove(sender.getName());
		
		return true;
	}

}

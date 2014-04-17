package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
		Player player = (Player) sender;

		ChessGame game = ChessGameManager.getManager().getCurrentGame(player, true);

		game.offerUndoMove(player.getUniqueId().toString());

		return true;
	}

}

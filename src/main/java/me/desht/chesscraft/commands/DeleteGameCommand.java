package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DeleteGameCommand extends AbstractCommand {

	public DeleteGameCommand() {
		super("chess d g", 1, 1);
		setUsage("/chess delete game <game-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		String gameName = args[0];
		ChessGame game = ChessGameManager.getManager().getGame(gameName);
		gameName = game.getName();
		
		// bypass permission check if player is deleting their own game and it's still in setup phase
		if (!game.playerCanDelete(sender)) {
			PermissionUtils.requirePerms(sender, "chesscraft.commands.delete.game");
		}
		
		game.alert(Messages.getString("ChessCommandExecutor.gameDeletedAlert", sender.getName())); //$NON-NLS-1$
		game.deletePermanently();
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.gameDeleted", gameName)); //$NON-NLS-1$
		
		return true;
	}

}

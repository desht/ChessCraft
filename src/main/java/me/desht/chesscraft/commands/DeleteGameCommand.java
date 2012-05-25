package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.entity.Player;

public class DeleteGameCommand extends AbstractCommand {

	public DeleteGameCommand() {
		super("chess d g", 1, 1);
//		setPermissionNode("chesscraft.commands.delete.game");
		setUsage("/chess delete game <game-name>");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		String gameName = args[0];
		ChessGame game = ChessGame.getGame(gameName);
		gameName = game.getName();
		
		// bypass permission check if player is deleting their own game and it's still in setup phase
		if (!game.playerCanDelete(player)) {
			PermissionUtils.requirePerms(player, "chesscraft.commands.delete.game");
		}
		
		String deleter = player == null ? "CONSOLE" : player.getName(); //$NON-NLS-1$
		game.alert(Messages.getString("ChessCommandExecutor.gameDeletedAlert", deleter)); //$NON-NLS-1$
		game.deletePermanently();
		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.gameDeleted", gameName)); //$NON-NLS-1$
		
		return true;
	}

}

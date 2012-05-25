package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class JoinCommand extends AbstractCommand {

	public JoinCommand() {
		super("chess j", 0, 1);
		setPermissionNode("chesscraft.commands.join");
		setUsage("/chess join [<game-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);

		String gameName = null;
		if (args.length >= 1) {
			gameName = args[0];
			ChessGame.getGame(gameName).addPlayer(player.getName());
		} else {
			// find a game (or games) with an invitation for us
			for (ChessGame game : ChessGame.listGames()) {
				if (game.getInvited().equalsIgnoreCase(player.getName())) {
					game.addPlayer(player.getName());
					gameName = game.getName();
				}
			}
			if (gameName == null) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.noPendingInvitation")); //$NON-NLS-1$
			}
		}

		ChessGame game = ChessGame.getGame(gameName);
		ChessGame.setCurrentGame(player.getName(), game);
		int playingAs = game.playingAs(player.getName());
		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.joinedGame", //$NON-NLS-1$
		                                                    game.getName(), ChessUtils.getColour(playingAs)));
		
		if (plugin.getConfig().getBoolean("auto_teleport_on_join")) { //$NON-NLS-1$
			game.summonPlayers();
		} else {
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.canTeleport", game.getName())); //$NON-NLS-1$
		}
		return true;
	}

}

package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class InvitePlayerCommand extends AbstractCommand {

	public InvitePlayerCommand() {
		super("chess i", 0, 1);
		setPermissionNode("chesscraft.commands.invite");
		setUsage("/chess invite [<player-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(player.getName(), true);
		String invitee = args.length > 0 ? args[0] : null;
		game.invitePlayer(player.getName(), invitee);
		
		return true;
	}

}

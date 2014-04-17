package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class InvitePlayerCommand extends ChessAbstractCommand {

	public InvitePlayerCommand() {
		super("chess invite", 0, 1);
		setPermissionNode("chesscraft.commands.invite");
		setUsage("/chess invite [<player-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player) sender;

		ChessGame game = ChessGameManager.getManager().getCurrentGame((Player) sender, true);
		String invitee = args.length > 0 ? args[0] : null;
		game.invitePlayer(player, invitee);

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getPlayerCompletions(plugin, sender, args[0], false);
		default:
			return noCompletions(sender);
		}
	}

}

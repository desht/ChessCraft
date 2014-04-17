package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ResignCommand extends ChessAbstractCommand {

	public ResignCommand() {
		super("chess resign", 0, 1);
		setPermissionNode("chesscraft.commands.resign");
		setUsage("/chess resign [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player) sender;

		ChessGame game = null;
		if (args.length >= 1) {
			game = ChessGameManager.getManager().getGame(args[0]);
		} else {
			game = ChessGameManager.getManager().getCurrentGame(player, true);
		}

		if (game != null) {
			game.resign(player.getUniqueId().toString());
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getPlayerInGameCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}


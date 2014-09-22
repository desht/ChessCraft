package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.ChessValidate;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class JoinCommand extends ChessAbstractCommand {

	public JoinCommand() {
		super("chess join", 0, 1);
		setPermissionNode("chesscraft.commands.join");
		setUsage("/chess join [<game-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player) sender;

		ChessGameManager cMgr = ChessGameManager.getManager();

		String gameName = null;
		int colour = Chess.NOBODY;
		if (args.length >= 1) {
			gameName = args[0];
			colour = cMgr.getGame(gameName).addPlayer(player.getUniqueId().toString(), player.getDisplayName());
		} else {
			// find a game (or games) with an invitation for us
			for (ChessGame game : cMgr.listGames()) {
				if (game.getInvitedId().equals(player.getUniqueId())) {
					colour = game.addPlayer(player.getUniqueId().toString(), player.getDisplayName());
					gameName = game.getName();
				}
			}
			ChessValidate.notNull(gameName, Messages.getString("ChessCommandExecutor.noPendingInvitation"));
		}

		ChessGame game = cMgr.getGame(gameName);
		cMgr.setCurrentGame(player, game);
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.joinedGame",
		                                                  game.getName(), ChessUtils.getDisplayColour(colour)));

//		if (plugin.getConfig().getBoolean("auto_teleport_on_join")) {
//			game.getPlayer(colour).teleport();
//		} else {
//			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.canTeleport", game.getName()));
//		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1 && sender instanceof Player) {
			return getInvitedGameCompletions((Player) sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

	private List<String> getInvitedGameCompletions(Player player, String prefix) {
		List<String> res = new ArrayList<String>();

		for (ChessGame game : ChessGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix) && game.isOpenInvite() || player.getUniqueId().equals(game.getInvitedId())) {
				res.add(game.getName());
			}
		}
		return getResult(res, player, true);
	}
}

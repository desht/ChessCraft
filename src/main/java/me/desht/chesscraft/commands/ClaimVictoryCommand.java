package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessValidate;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class ClaimVictoryCommand extends ChessAbstractCommand {

	public ClaimVictoryCommand() {
		super("chess win", 0, 0);
		setPermissionNode("chesscraft.commands.win");
		setUsage("/chess win");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player) sender;

		ChessGame game = ChessGameManager.getManager().getCurrentGame(player, true);
		game.ensureGameState(GameState.RUNNING);
		ChessPlayer cp = game.getPlayer(player.getUniqueId().toString());
		if (cp == null) {
			return true;
		}
		ChessPlayer other = game.getPlayer(Chess.otherPlayer(cp.getColour()));
		if (other == null || !other.isHuman()) {
			return true;
		}

		int timeout = plugin.getConfig().getInt("timeout_forfeit", 60);
		long leftAt = ((ChessCraft)plugin).getPlayerTracker().getPlayerLeftAt(UUID.fromString(other.getId()));
		ChessValidate.isTrue(leftAt > 0, Messages.getString("ChessCommandExecutor.otherPlayerMustBeOffline"));
		long now = System.currentTimeMillis();
		long elapsed = (now - leftAt) / 1000;
		if (elapsed >= timeout) {
			game.winByDefault(player.getUniqueId().toString());
		} else {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.needToWait", timeout - elapsed));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}
}

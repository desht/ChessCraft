package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ClaimVictoryCommand extends AbstractCommand {

	public ClaimVictoryCommand() {
		super("chess w", 0, 0);
		setPermissionNode("chesscraft.commands.win");
		setUsage("/chess win");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(sender.getName(), true);

		game.ensureGameState(GameState.RUNNING);

		String other = game.getOtherPlayerName(sender.getName());
		if (other.isEmpty()) {
			return true;
		}

		int timeout = plugin.getConfig().getInt("forfeit_timeout"); //$NON-NLS-1$
		long leftAt = ((ChessCraft)plugin).getPlayerTracker().getPlayerLeftAt(other);
		if (leftAt == 0) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.otherPlayerMustBeOffline")); //$NON-NLS-1$
		}
		long now = System.currentTimeMillis();
		long elapsed = (now - leftAt) / 1000;
		if (elapsed >= timeout) {
			game.winByDefault(sender.getName());
		} else {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.needToWait", timeout - elapsed)); //$NON-NLS-1$
		}
		
		return true;
	}

}

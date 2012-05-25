package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class ClaimVictoryCommand extends AbstractCommand {

	public ClaimVictoryCommand() {
		super("chess w", 0, 0);
		setPermissionNode("chesscraft.commands.win");
		setUsage("/chess win");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);

		ChessGame game = ChessGame.getCurrentGame(player, true);

		game.ensureGameState(GameState.RUNNING);

		String other = game.getOtherPlayer(player.getName());
		if (other.isEmpty()) {
			return true;
		}

		int timeout = plugin.getConfig().getInt("forfeit_timeout"); //$NON-NLS-1$
		long leftAt = plugin.getPlayerLeftAt(other);
		if (leftAt == 0) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.otherPlayerMustBeOffline")); //$NON-NLS-1$
		}
		long now = System.currentTimeMillis();
		long elapsed = (now - leftAt) / 1000;
		if (elapsed >= timeout) {
			game.winByDefault(player.getName());
		} else {
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.needToWait", timeout - elapsed)); //$NON-NLS-1$
		}
		
		return true;
	}

}

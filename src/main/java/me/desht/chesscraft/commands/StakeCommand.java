package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class StakeCommand extends AbstractCommand {

	public StakeCommand() {
		super("chess stak", 1, 1);
		setPermissionNode("chesscraft.commands.stake");
		setUsage("/chess stake <amount>");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		String stakeStr = args[0];
		try {
			ChessGame game = ChessGame.getCurrentGame(player);
			if (game == null) {
				return true;
			}
			double amount = Double.parseDouble(stakeStr);
			if (amount <= 0.0) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.noNegativeStakes")); //$NON-NLS-1$
			}
			if (!ChessCraft.economy.has(player.getName(), amount)) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.cantAffordStake")); //$NON-NLS-1$
			}
			game.setStake(amount);
			game.getView().getControlPanel().repaintSignButtons();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.stakeChanged", ChessCraft.economy.format(amount))); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", stakeStr)); //$NON-NLS-1$
		}
		return true;
	}

}


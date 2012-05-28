package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
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
		if (ChessCraft.economy == null) {
			return true;
		}
		
		String stakeStr = args[0];
		try {
			ChessGame game = ChessGame.getCurrentGame(player);
			if (game == null) {
				return true;
			}
			double amount = Double.parseDouble(stakeStr);
			game.setStake(player.getName(), amount);
			game.getView().getControlPanel().repaintSignButtons();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.stakeChanged", ChessUtils.formatStakeStr(amount))); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", stakeStr)); //$NON-NLS-1$
		}
		return true;
	}

}


package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StakeCommand extends ChessAbstractCommand {

	public StakeCommand() {
		super("chess stake", 1, 1);
		setPermissionNode("chesscraft.commands.stake");
		setUsage("/chess stake <amount>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		if (ChessCraft.economy == null) {
			return true;
		}
		notFromConsole(sender);
		Player player = (Player) sender;

		String stakeStr = args[0];
		try {
			ChessGame game = ChessGameManager.getManager().getCurrentGame(player, true);
			double amount = Double.parseDouble(stakeStr);
			game.setStake(player, amount);
			game.getView().getControlPanel().repaintControls();
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.stakeChanged", ChessUtils.formatStakeStr(amount)));
		} catch (NumberFormatException e) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", stakeStr));
		}
		return true;
	}

}


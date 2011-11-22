package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public class TimeControlCommand extends AbstractCommand {

	public TimeControlCommand() {
		super("chess tc", 1, 1);
		setPermissionNode("chesscraft.commands.tc");
		setUsage("/chess tc <time-control-spec>");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		String tcSpec = args[0];

		ChessGame game = ChessGame.getCurrentGame(player, true);
		game.setTimeControl(tcSpec);
		game.alert(Messages.getString("ChessCommandExecutor.timeControlSet", tcSpec, game.getTcWhite().toString()));
		
		return true;
	}

}

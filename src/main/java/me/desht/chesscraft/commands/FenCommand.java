package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class FenCommand extends AbstractCommand {
	
	public FenCommand() {
		super("chess f", 1, 1);
		setPermissionNode("chesscraft.commands.fen");
		setUsage("/chess fen <fen-string>");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);

		ChessGame game = ChessGame.getCurrentGame(player, true);

		game.setFen(combine(args, 1));

		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.positionUpdatedFEN", //$NON-NLS-1$ 
		                                                    game.getName(), ChessUtils.getColour(game.getPosition().getToPlay())));
		return true;
	}

}

package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class FenCommand extends ChessAbstractCommand {
	public FenCommand() {
		super("chess fen", 1, 1);
		setPermissionNode("chesscraft.commands.fen");
		setUsage("/chess fen <fen-string>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(sender.getName(), true);

		game.setPositionFEN(combine(args, 1));

		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.positionUpdatedFEN", //$NON-NLS-1$
		                                                    game.getName(), ChessUtils.getDisplayColour(game.getPosition().getToPlay())));
		return true;
	}
}

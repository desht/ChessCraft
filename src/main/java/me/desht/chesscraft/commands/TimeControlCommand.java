package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

public class TimeControlCommand extends AbstractCommand {

	public TimeControlCommand() {
		super("chess tc", 1, 1);
		setPermissionNode("chesscraft.commands.tc");
		setUsage("/chess tc <time-control-spec>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		String tcSpec = args[0];

		ChessGame game = ChessGame.getCurrentGame(sender.getName(), true);
		game.setTimeControl(tcSpec);
		game.getView().getControlPanel().updateClock(Chess.WHITE, game.getTcWhite());
		game.getView().getControlPanel().updateClock(Chess.BLACK, game.getTcBlack());
		
		game.alert(Messages.getString("ChessCommandExecutor.timeControlSet", tcSpec, game.getTcWhite().toString()));
		
		return true;
	}

}

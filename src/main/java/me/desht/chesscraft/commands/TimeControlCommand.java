package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.controlpanel.ControlPanel;
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
		ControlPanel cp = game.getView().getControlPanel();
		cp.getTcDefs().setCustomSpec(tcSpec);
		cp.repaintSignButtons();
		cp.updateClock(Chess.WHITE, game.getTcWhite());
		cp.updateClock(Chess.BLACK, game.getTcBlack());
		
		game.alert(Messages.getString("ChessCommandExecutor.timeControlSet", tcSpec, game.getTcWhite().toString()));
		
		return true;
	}

}

package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.controlpanel.ControlPanel;
import me.desht.chesscraft.controlpanel.TimeControlButton;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TimeControlCommand extends ChessAbstractCommand {

	public TimeControlCommand() {
		super("chess tc", 1, 1);
		addAlias("chess timecontrol");
		setPermissionNode("chesscraft.commands.tc");
		setUsage("/chess tc <time-control-spec>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player)sender;
		String tcSpec = args[0];

		ChessGame game = ChessGameManager.getManager().getCurrentGame(player, true);
		game.setTimeControl(tcSpec);
		ControlPanel cp = game.getView().getControlPanel();
		cp.getTcDefs().addCustomSpec(tcSpec);
		cp.getSignButton(TimeControlButton.class).repaint();
		cp.updateClock(Chess.WHITE, game.getTimeControl(Chess.WHITE));
		cp.updateClock(Chess.BLACK, game.getTimeControl(Chess.BLACK));

		game.alert(Messages.getString("ChessCommandExecutor.timeControlSet", tcSpec, game.getTimeControl(Chess.WHITE).toString()));

		return true;
	}

}

package me.desht.chesscraft.commands;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectYesNoResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class NoCommand extends ChessAbstractCommand {

	public NoCommand() {
		super("chess no", 0, 0);
		setUsage("/chess no");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		ExpectYesNoResponse.handleYesNoResponse((Player)sender, false);
		return true;
	}
}


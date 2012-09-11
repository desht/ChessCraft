package me.desht.chesscraft.commands;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectYesNoResponse;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class YesCommand extends AbstractCommand {

	public YesCommand() {
		super("chess y", 0, 0);
		setUsage("/chess yes");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		ExpectYesNoResponse.handleYesNoResponse((Player)sender, true);
		return true;
	}

}


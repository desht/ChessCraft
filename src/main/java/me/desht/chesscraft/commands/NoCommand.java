package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class NoCommand extends AbstractCommand {

	public NoCommand() {
		super("chess n", 0, 0);
		setUsage("/chess no");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		ChessCraft.handleYesNoResponse((Player)sender, false);
		return true;
	}

}


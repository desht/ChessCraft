package me.desht.chesscraft.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

public class YesCommand extends AbstractCommand {

	public YesCommand() {
		super("chess y", 0, 0);
		setUsage("/chess yes");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		ChessCraft.handleYesNoResponse((Player)sender, true);
		return true;
	}

}


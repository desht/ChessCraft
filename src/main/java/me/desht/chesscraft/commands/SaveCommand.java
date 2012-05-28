package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class SaveCommand extends AbstractCommand {

	public SaveCommand() {
		super("chess sa", 0, 0);
		setPermissionNode("chesscraft.commands.save");
		setUsage("/chess save");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		ChessCraft.getPersistenceHandler().save();
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.chessSaved")); //$NON-NLS-1$
		return true;
	}

}

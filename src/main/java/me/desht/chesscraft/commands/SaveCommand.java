package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class SaveCommand extends AbstractCommand {

	public SaveCommand() {
		super("chess sa", 0, 0);
		setPermissionNode("chesscraft.commands.save");
		setUsage("/chess save");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		ChessCraft.getPersistenceHandler().save();
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.chessSaved")); //$NON-NLS-1$
		return true;
	}

}

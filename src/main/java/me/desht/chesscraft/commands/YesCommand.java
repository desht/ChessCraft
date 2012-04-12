package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;

public class YesCommand extends AbstractCommand {

	public YesCommand() {
		super("chess y", 0, 0);
		setUsage("/chess yes");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		ChessCraft.handleYesNoResponse(player, true);
		return true;
	}

}


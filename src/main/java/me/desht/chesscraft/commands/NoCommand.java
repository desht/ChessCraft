package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;

public class NoCommand extends AbstractCommand {

	public NoCommand() {
		super("chess n", 0, 0);
		setUsage("/chess no");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		ChessCraft.handleExpectedResponse(player, false);
		return true;
	}

}


package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

public class OfferSwapCommand extends AbstractCommand {

	public OfferSwapCommand() {
		super("chess o s", 0, 1);
		setPermissionNode("chesscraft.commands.offer.swap");
		setUsage("/chess offer swap");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		ChessGame game = ChessGame.getCurrentGame(player, true);
		game.offerSwap(player);
		
		return true;
	}

}


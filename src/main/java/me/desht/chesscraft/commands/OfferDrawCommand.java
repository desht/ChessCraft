package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

public class OfferDrawCommand extends AbstractCommand {

	public OfferDrawCommand() {
		super("chess offerdraw", 0, 1);
		setPermissionNode("chesscraft.commands.offer.draw");
		setUsage("/chess offerdraw");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		ChessGame game = ChessGame.getCurrentGame(player, true);
		game.offerDraw(player);
		
		return true;
	}

}


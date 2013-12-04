package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class OfferSwapCommand extends ChessAbstractCommand {

	public OfferSwapCommand() {
		super("chess offer swap", 0, 1);
		setPermissionNode("chesscraft.commands.offer.swap");
		setUsage("/chess offer swap");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(player.getName(), true);
		game.offerSwap(player.getName());

		return true;
	}
}


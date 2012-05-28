package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

public class PromoteCommand extends AbstractCommand {

	public PromoteCommand() {
		super("chess pr", 1, 1);
		setPermissionNode("chesscraft.commands.promote");
		setUsage("/chess promote");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);

		ChessGame game = ChessGame.getCurrentGame(player.getName(), true);
		int piece = Chess.charToPiece(Character.toUpperCase(args[0].charAt(0)));
		game.setPromotionPiece(player.getName(), piece);
		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.promotionPieceSet", //$NON-NLS-1$
		                                                    game.getName(),ChessUtils.pieceToStr(piece).toUpperCase()));
		game.getView().getControlPanel().repaintSignButtons();

		return true;
	}

}


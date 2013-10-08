package me.desht.chesscraft.commands;

import java.util.Arrays;
import java.util.List;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.controlpanel.PromoteBlackButton;
import me.desht.chesscraft.controlpanel.PromoteWhiteButton;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

public class PromoteCommand extends ChessAbstractCommand {

	public PromoteCommand() {
		super("chess promote", 1, 1);
		setPermissionNode("chesscraft.commands.promote");
		setUsage("/chess promote <Q|R|B|N>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(player.getName(), true);
		game.ensurePlayerInGame(player.getName());

		int piece = Chess.charToPiece(Character.toUpperCase(args[0].charAt(0)));
		int colour = game.getPlayerColour(player.getName());
		game.getPlayer(colour).setPromotionPiece(piece);
		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.promotionPieceSet", //$NON-NLS-1$
		                                                    game.getName(),ChessUtils.pieceToStr(piece).toUpperCase()));
		if (colour == Chess.WHITE) {
			game.getView().getControlPanel().getSignButton(PromoteWhiteButton.class).repaint();
		} else {
			game.getView().getControlPanel().getSignButton(PromoteBlackButton.class).repaint();
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return filterPrefix(sender, Arrays.asList("Q", "R", "B", "N"), args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}


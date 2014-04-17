package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.controlpanel.PromoteBlackButton;
import me.desht.chesscraft.controlpanel.PromoteWhiteButton;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class PromoteCommand extends ChessAbstractCommand {

	public PromoteCommand() {
		super("chess promote", 1, 1);
		setPermissionNode("chesscraft.commands.promote");
		setUsage("/chess promote <Q|R|B|N>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player) sender;

		int piece = Chess.charToPiece(Character.toUpperCase(args[0].charAt(0)));
		ChessGame game = ChessGameManager.getManager().getCurrentGame(player, true);
		int colour = game.getPlayerColour(player.getUniqueId().toString());
		if (colour != Chess.NOBODY) {
			game.getPlayer(colour).setPromotionPiece(piece);
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.promotionPieceSet",
					game.getName(),ChessUtils.pieceToStr(piece).toUpperCase()));
			game.getView().getControlPanel().getSignButton(colour == Chess.WHITE ? PromoteWhiteButton.class : PromoteBlackButton.class).repaint();
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


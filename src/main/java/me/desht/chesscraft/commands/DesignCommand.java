package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class DesignCommand extends AbstractCommand {

	public DesignCommand() {
		super("chess des");
		setPermissionNode("chesscraft.designer");
		setUsage(new String[] {
				"/chess design",
				"/chess design save <style-name>"
		});
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		BoardView bv = BoardView.partOfChessBoard(player.getLocation());
		if (bv == null) {
			throw new ChessException("You are not standing on a chess board.");
		}
		if (bv.getGame() != null) {
			throw new ChessException("You cannot design on a board with a game in progress.");
		}
		
		if (args.length == 0) {
			// toggle in and out of design mode
			bv.setDesigning(!bv.isDesigning());
			ChessUtils.statusMessage(player, Messages.getString(bv.isDesigning() ? "Designer.inDesignMode" : "Designer.outOfDesignMode", bv.getName()));
			bv.paintAll();
		} else if (args[0].equalsIgnoreCase("save")) {
			
		}
		
		return true;
	}

}

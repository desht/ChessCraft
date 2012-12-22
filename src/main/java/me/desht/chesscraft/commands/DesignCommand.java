package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DesignCommand extends AbstractCommand {

	public DesignCommand() {
		super("chess des");
		setPermissionNode("chesscraft.designer");
		setUsage(new String[] {
				"/chess design",
				"/chess design clear",
				"/chess design exit",
				"/chess design save [<style-name>]",
				"/chess design load [<style-name>]",
		});
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		Player player = (Player)sender;
		
		BoardView bv = BoardViewManager.getManager().partOfChessBoard(player.getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		if (bv.getGame() != null) {
			throw new ChessException(Messages.getString("Designer.gameRunning"));
		}

		PieceDesigner designer = bv.getChessBoard().getDesigner();
		if (designer == null && args.length > 0) {
			throw new ChessException(Messages.getString("Designer.mustBeInDesignMode"));
		}
		
		if (args.length == 0) {
			if (bv.isDesigning()) {
				showUsage(sender);
				return true;
			} else {
				// toggle into design mode
				designer = new PieceDesigner(bv, bv.getPieceStyleName(), player.getName());
				bv.getChessBoard().setDesigner(designer);
				MiscUtil.statusMessage(sender, Messages.getString("Designer.inDesignMode", bv.getName()));
				if (ChessCraft.getInstance().getConfig().getBoolean("designer.auto_load")) {
					designer.load();
					MiscUtil.statusMessage(sender, Messages.getString("Designer.styleLoaded", designer.getSetName()));
				}
			}
			bv.paintAll();
		} else if (args[0].startsWith("e")) {	// exit
			if (bv.isDesigning()) {
				bv.getChessBoard().setDesigner(null);
				MiscUtil.statusMessage(sender, Messages.getString("Designer.outOfDesignMode", bv.getName()));
				bv.paintAll();
			}
		} else if (args[0].startsWith("c")) {	// clear
			designer.clear();
			MiscUtil.statusMessage(sender, Messages.getString("Designer.cleared", designer.getSetName()));
		} else if (args[0].startsWith("s")) {	// save
			if (args.length >= 2) {
				designer.setSetName(args[1]);
			}
			designer.scan();
			designer.save();
			MiscUtil.statusMessage(sender, Messages.getString("Designer.styleSaved", designer.getSetName()));
		} else if (args[0].startsWith("l")) {	// load
			if (args.length >= 2) {
				designer.setSetName(args[1]);
			}
			designer.load();
			MiscUtil.statusMessage(sender, Messages.getString("Designer.styleLoaded", designer.getSetName()));
		}
		
		return true;
	}

}

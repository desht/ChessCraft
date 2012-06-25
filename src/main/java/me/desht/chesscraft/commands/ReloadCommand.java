package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessAI;
import me.desht.chesscraft.chess.TimeControlDefs;
import me.desht.chesscraft.controlpanel.TimeControlButton;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand extends AbstractCommand {

	public ReloadCommand() {
		super("chess rel", 1, 1);
		setPermissionNode("chesscraft.commands.reload");
		setUsage("/chess reload <ai|config|persist>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		boolean reloadPersisted = false;
		boolean reloadAI = false;
		boolean reloadConfig = false;
		boolean reloadTimeControls = false;

		if (args[0].startsWith("a")) { //$NON-NLS-1$
			reloadAI = true;
		} else if (args[0].startsWith("c")) { //$NON-NLS-1$
			reloadConfig = true;
		} else if (args[0].startsWith("p")) { //$NON-NLS-1$
			reloadPersisted = true;
		} else if (args[0].startsWith("p")) { //$NON-NLS-1$
			reloadTimeControls = true;
		} else {
			showUsage(player);
		}

		if (reloadConfig) {
			plugin.reloadConfig();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.configReloaded")); //$NON-NLS-1$
		}
		if (reloadAI) {
			ChessAI.initAINames();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.AIdefsReloaded")); //$NON-NLS-1$
		}
		if (reloadPersisted) {
			ChessCraft.getPersistenceHandler().reload();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.persistedReloaded")); //$NON-NLS-1$
		}
		if (reloadTimeControls) {
			TimeControlDefs.reInit();
			for (BoardView bv : BoardView.listBoardViews()) {
//				bv.getControlPanel().repaintSignButtons();
				bv.getControlPanel().getSignButton(TimeControlButton.class).reloadDefs();
			}
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.timeControlsReloaded")); //$NON-NLS-1$
		}
		return true;
	}

}

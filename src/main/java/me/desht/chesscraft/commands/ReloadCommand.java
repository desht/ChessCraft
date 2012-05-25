package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessAI;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class ReloadCommand extends AbstractCommand {

	public ReloadCommand() {
		super("chess rel", 1, 1);
		setPermissionNode("chesscraft.commands.reload");
		setUsage("/chess reload <ai|config|persist>");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		boolean reloadPersisted = false;
		boolean reloadAI = false;
		boolean reloadConfig = false;

		if (partialMatch(args, 0, "a")) { //$NON-NLS-1$
			reloadAI = true;
		} else if (partialMatch(args, 0, "c")) { //$NON-NLS-1$
			reloadConfig = true;
		} else if (partialMatch(args, 0, "p")) { //$NON-NLS-1$
			reloadPersisted = true;
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
		return true;
	}

}

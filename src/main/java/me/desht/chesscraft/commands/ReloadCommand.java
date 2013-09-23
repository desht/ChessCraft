package me.desht.chesscraft.commands;

import java.util.Arrays;
import java.util.List;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.TimeControlDefs;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.controlpanel.TimeControlButton;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand extends ChessAbstractCommand {

	public ReloadCommand() {
		super("chess reload", 1, 1);
		setPermissionNode("chesscraft.commands.reload");
		setUsage("/chess reload <ai|config|gamedata|timecontrols>");
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
		} else if (args[0].startsWith("g")) { //$NON-NLS-1$
			reloadPersisted = true;
		} else if (args[0].startsWith("t")) { //$NON-NLS-1$
			reloadTimeControls = true;
		} else {
			showUsage(player);
		}

		if (reloadConfig) {
			plugin.reloadConfig();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.configReloaded")); //$NON-NLS-1$
		}
		if (reloadAI) {
			AIFactory.getInstance().loadAIDefinitions();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.AIdefsReloaded")); //$NON-NLS-1$
		}
		if (reloadPersisted) {
			((ChessCraft)plugin).getPersistenceHandler().reload();
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.persistedReloaded")); //$NON-NLS-1$
		}
		if (reloadTimeControls) {
			TimeControlDefs.loadBaseDefs();
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
				bv.getControlPanel().getSignButton(TimeControlButton.class).reloadDefs();
			}
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.timeControlsReloaded")); //$NON-NLS-1$
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return filterPrefix(sender, Arrays.asList(new String[] { "ai", "config", "gamedata", "timecontrols" }), args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}

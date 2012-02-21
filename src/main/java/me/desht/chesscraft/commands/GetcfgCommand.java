package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.MessagePager;

public class GetcfgCommand extends AbstractCommand {

	public GetcfgCommand() {
		super("chess get", 0, 1);
		setPermissionNode("chesscraft.commands.getcfg");
		setUsage("/chess getcfg");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		if (args.length < 1) {
			MessagePager pager = MessagePager.getPager(player).clear();
			for (String line : ChessConfig.getPluginConfiguration()) {
				pager.add(line);
			}
			pager.showPage();
		} else {
			String res = plugin.getConfig().getString(args[0]);
			if (res != null) {
				ChessUtils.statusMessage(player, "&f" + args[0] + " = '&e" + res + "&-'"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.noSuchKey", args[0])); //$NON-NLS-1$
			}
		}
		return true;
	}

}


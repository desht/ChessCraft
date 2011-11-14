package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

public class SetcfgCommand extends AbstractCommand {

	public SetcfgCommand() {
		super("chess set", 2, 2);
		setPermissionNode("chesscraft.commands.setcfg");
		setUsage("/chess setcfg");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		String key = args[0];
		String val = args[1];
		ChessConfig.setConfigItem(player, key, val);

		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.configKeySet", key, ChessConfig.getConfig().get(key))); //$NON-NLS-1$

		return true;
	}

}


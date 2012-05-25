package me.desht.chesscraft.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;

public class SetcfgCommand extends AbstractCommand {

	public SetcfgCommand() {
		super("chess set", 2, 2);
		setPermissionNode("chesscraft.commands.setcfg");
		setUsage("/chess setcfg");
		setQuotedArgs(true);
	}


	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) {
		String key = args[0], val = args[1];
		
		ConfigurationManager configManager = ((ChessCraft) plugin).getConfigManager();
		
		try {
			if (args.length > 2) {
				List<String> list = new ArrayList<String>(args.length - 1);
				for (int i = 1; i < args.length; i++)
					list.add(args[i]);
				configManager.set(key, list);	
			} else {
				configManager.set(key, val);
			}
			Object res = configManager.get(key);
			MiscUtil.statusMessage(player, key + " is now set to '&e" + res + "&-'");
		} catch (ChessException e) {
			MiscUtil.errorMessage(player, e.getMessage());
			MiscUtil.errorMessage(player, "Use /chess getcfg to list all valid keys");
		} catch (IllegalArgumentException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
		return true;
	}
}


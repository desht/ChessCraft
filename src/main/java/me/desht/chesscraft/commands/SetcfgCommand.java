package me.desht.chesscraft.commands;

import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class SetcfgCommand extends AbstractCommand {

	public SetcfgCommand() {
		super("chess set", 2, 2);
		setPermissionNode("chesscraft.commands.setcfg");
		setUsage("/chess setcfg");
		setQuotedArgs(true);
	}


	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
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
			MiscUtil.statusMessage(sender, key + " is now set to '&e" + res + "&-'");
		} catch (ChessException e) {
			MiscUtil.errorMessage(sender, e.getMessage());
			MiscUtil.errorMessage(sender, "Use /chess getcfg to list all valid keys");
		} catch (IllegalArgumentException e) {
			MiscUtil.errorMessage(sender, e.getMessage());
		}
		return true;
	}
}


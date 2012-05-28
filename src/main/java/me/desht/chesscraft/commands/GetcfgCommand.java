package me.desht.chesscraft.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;

public class GetcfgCommand extends AbstractCommand {

	public GetcfgCommand() {
		super("chess get", 0, 1);
		setPermissionNode("chesscraft.commands.getcfg");
		setUsage("/chess getcfg");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		if (args.length < 1) {
			MessagePager pager = MessagePager.getPager(player).clear();
			for (String line : getPluginConfiguration()) {
				pager.add(line);
			}
			pager.showPage();
		} else {
			String res = plugin.getConfig().getString(args[0]);
			if (res != null) {
				MiscUtil.statusMessage(player, "&f" + args[0] + " = '&e" + res + "&-'"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				MiscUtil.errorMessage(player, Messages.getString("ChessConfig.noSuchKey", args[0])); //$NON-NLS-1$
			}
		}
		return true;
	}

	public static List<String> getPluginConfiguration() {
		ArrayList<String> res = new ArrayList<String>();
		Configuration config = ChessCraft.getInstance().getConfig();
		for (String k : config.getDefaults().getKeys(true)) {
			if (config.isConfigurationSection(k))
				continue;
			res.add("&f" + k + "&- = '&e" + config.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}
}


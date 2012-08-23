package me.desht.chesscraft.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class GetcfgCommand extends AbstractCommand {

	public GetcfgCommand() {
		super("chess get", 0, 1);
		setPermissionNode("chesscraft.commands.getcfg");
		setUsage("/chess getcfg");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		List<String> lines = getPluginConfiguration(args.length >= 1 ? args[0] : null);
		if (lines.size() > 1) {
			MessagePager pager = MessagePager.getPager(player).clear();
			for (String line : lines) {
				pager.add(line);
			}
			pager.showPage();		
		} else if (lines.size() == 1) {
			MiscUtil.statusMessage(player, lines.get(0));
		} else {
			MiscUtil.errorMessage(player, Messages.getString("ChessConfig.noSuchKey", args[0])); //$NON-NLS-1$	
		}
		return true;
	}

	public static List<String> getPluginConfiguration() {
		return getPluginConfiguration(null);
	}

	public static List<String> getPluginConfiguration(String section) {
		ArrayList<String> res = new ArrayList<String>();
		Configuration config = ChessCraft.getInstance().getConfig();
		ConfigurationSection cs = config.getDefaultSection();
		
		Set<String> items;
		if (section == null) {
			items = config.getDefaults().getKeys(true);
		} else {
			if (config.getDefaults().isConfigurationSection(section)) {
				cs = config.getConfigurationSection(section);
				items = config.getDefaults().getConfigurationSection(section).getKeys(true);
			} else {
				items = new HashSet<String>();
				if (config.getDefaults().contains(section))
					items.add(section);
			}
		}

		for (String k : items) {
			if (cs.isConfigurationSection(k))
				continue;
			res.add("&f" + k + "&- = '&e" + cs.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}
}


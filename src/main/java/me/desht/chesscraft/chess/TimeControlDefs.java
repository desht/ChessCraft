package me.desht.chesscraft.chess;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class TimeControlDefs {
	private static final String TIME_CONTROLS_FILE = "timecontrols.yml";
	
	private static List<TCDef> baseDefs;
	
	private int idx;
	private final List<TCDef> allDefs;
	private final List<TCDef> extraDefs;
	
	public static void loadBaseDefs() {
		baseDefs = new ArrayList<TimeControlDefs.TCDef>();
		
		File f = new File(ChessCraft.getInstance().getDataFolder(), TIME_CONTROLS_FILE);
		Configuration c = YamlConfiguration.loadConfiguration(f);
		List<?> l = c.getList("time_controls");
		if (l == null) {
			throw new ChessException(TIME_CONTROLS_FILE + " is missing the 'time_controls' section!");
		} else {
			for (Object o : l) {
				@SuppressWarnings("unchecked")
				Map<String,String> map = (HashMap<String, String>) o;
				String label = map.get("label");
				String spec = map.get("spec");
				if (label == null) {
					LogUtils.warning("missing label in " + TIME_CONTROLS_FILE);
					continue;
				}
				if (spec == null) {
					LogUtils.warning("missing spec in " + TIME_CONTROLS_FILE);
					continue;
				}
				try {
					baseDefs.add(new TCDef(label, spec));
				} catch (Exception e) {
					LogUtils.warning(e.getMessage());
				}
			}
		}
	}
	
	public TimeControlDefs() {
		if (baseDefs == null)
			loadBaseDefs();
		
		idx = 0;
		allDefs = new ArrayList<TCDef>(baseDefs);
		extraDefs = new ArrayList<TCDef>();
	}

	public void reload() {
		idx = 0;
		allDefs.clear();
		loadBaseDefs();
		for (TCDef def : baseDefs) {
			allDefs.add(def);
		}
		for (TCDef def : extraDefs) {
			allDefs.add(def);
		}
	}

	public void addCustomSpec(String customSpec) {
		// see if the custom spec. is one we know about already - if it is,
		// we'll use that spec. rather than add a custom spec.
		for (int i = 0; i < allDefs.size(); i++) {
			TCDef def = allDefs.get(i);
			if (def.getSpec().equalsIgnoreCase(customSpec)) {
				idx = i;
				return;
			}
		}
		
		TCDef tcd = new TCDef("Custom;" + customSpec, customSpec);
		allDefs.add(tcd);
		extraDefs.add(tcd);
		idx = allDefs.size() - 1;
	}

	/**
	 * Get the next time control definition in the list, wrapping round at the end of the list.
	 * 
	 * @return
	 */
	public TCDef nextDef() {
		if (allDefs.isEmpty()) {
			return new TCDef("None", "None");
		}
		idx++;
		if (idx >= allDefs.size()) {
			idx = 0;
		}
		return allDefs.get(idx);
	}
	
	/**
	 * Get the previous time control definition in the list, wrapping round at the start of the list.
	 * @return
	 */
	public TCDef prevDef() {
		if (allDefs.isEmpty()) {
			return new TCDef("None", "None");
		}
		idx--;
		if (idx < 0) {
			idx = allDefs.size() - 1;
		}
		return allDefs.get(idx);
	}
	
	public TCDef currentDef() {
		if (allDefs.isEmpty()) {
			return new TCDef("None", "None");
		}
		if (idx < 0) {
			idx = 0;
		} else if (idx >= allDefs.size()) {
			idx = allDefs.size() - 1;
		}
		return allDefs.get(idx);
	}
	
	public static class TCDef {
		private final String[] label;
		private final String spec;
		
		public TCDef(String label, String spec) {
			this.label = label.split(";");
			this.spec = spec;
			if (this.label.length > 3) {
				throw new IllegalArgumentException("label can have max. 3 lines: " + label);
			}
			new TimeControl(spec);	// ensure the spec. is valid
		}
		
		public String getSpec() {
			return spec;
		}

		public String[] getLabel() {
			return label;
		}
		
		public TimeControl createTimeControl() {
			return new TimeControl(spec);
		}
	}
}

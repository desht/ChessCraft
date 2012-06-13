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
	
	private final List<TCDef> defs;
	private int idx = 0;
	
	public TimeControlDefs() {
		defs = new ArrayList<TCDef>();
		
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
				}
				if (spec == null) {
					LogUtils.warning("missing spec in " + TIME_CONTROLS_FILE);
				}
				try {
					defs.add(new TCDef(label, spec));
				} catch (Exception e) {
					LogUtils.warning(e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Get the next time control definition in the list, wrapping round at the end of the list.
	 * 
	 * @return
	 */
	public TCDef nextDef() {
		if (defs.isEmpty()) {
			return new TCDef("None", "None");
		}
		idx++;
		if (idx >= defs.size()) {
			idx = 0;
		}
		return defs.get(idx);
	}
	
	/**
	 * Get the previous time control definition in the list, wrapping round at the start of the list.
	 * @return
	 */
	public TCDef prevDef() {
		if (defs.isEmpty()) {
			return new TCDef("None", "None");
		}
		idx--;
		if (idx < 0) {
			idx = defs.size() - 1;
		}
		return defs.get(idx);
	}
	
	public TCDef currentDef() {
		if (defs.isEmpty()) {
			return new TCDef("None", "None");
		}
		if (idx < 0) {
			idx = 0;
		} else if (idx >= defs.size()) {
			idx = defs.size() - 1;
		}
		return defs.get(idx);
	}
	
	public class TCDef {
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

package me.desht.chesscraft.controlpanel;

import me.desht.dhutils.MiscUtil;

public abstract class CounterLabel extends AbstractSignLabel {

	private int count;

	public CounterLabel(ControlPanel panel, String labelKey, int x, int y) {
		super(panel, labelKey, x, y);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int plyNumber) {
		this.count = plyNumber;
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public String[] getCustomSignText() {
		String[] res = getSignText();
		res[2] = MiscUtil.parseColourSpec("&4" + count);
		return res;
	}
}

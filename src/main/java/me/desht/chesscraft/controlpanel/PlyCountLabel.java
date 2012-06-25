package me.desht.chesscraft.controlpanel;

public class PlyCountLabel extends CounterLabel {

	public PlyCountLabel(ControlPanel panel) {
		super(panel, "playNumber", 5, 0);
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

}

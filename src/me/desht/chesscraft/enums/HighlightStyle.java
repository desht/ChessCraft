package me.desht.chesscraft.enums;

import me.desht.chesscraft.ChessUtils;

public enum HighlightStyle {

	NONE, CORNERS, EDGES, CHEQUERED, LINE, CHECKERED;

	public static HighlightStyle getStyle(String style) {
		if (style == null) {
			return null;
		}
		style = style.trim().toUpperCase();
		for (HighlightStyle h : values()) {
			if (h.name().equals(style) || ChessUtils.getLevenshteinDistance(h.name(), style) < 2) {
				return h;
			}
		}
		return null;
	}
}

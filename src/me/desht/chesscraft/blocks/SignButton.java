package me.desht.chesscraft.blocks;

import me.desht.chesscraft.ChessUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class SignButton {

    private Location location;
    private boolean enabled;
    private String text;
    private MaterialWithData mat;
    private String name;
    private final static ChatColor enabledCol = ChatColor.DARK_BLUE;
    private final static ChatColor disabledCol = ChatColor.DARK_GRAY;

    public SignButton(String name, Location loc, String text, MaterialWithData mat, boolean enabled) {
        this.name = name;
        this.text = text;
        this.location = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        this.enabled = enabled;
        this.mat = mat;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }
    
    public void setText(String text) {
        this.text = text;
    }

    public static ChatColor getEnabledcol() {
        return enabledCol;
    }

    public static ChatColor getDisabledcol() {
        return disabledCol;
    }
    
    public void repaint() {
        Block block = location.getBlock();
        mat.applyToBlock(block);
        if (block.getState() instanceof Sign) {
            String[] lines = text.split(";");
            Sign s = (Sign) block.getState();
            for (int i = 0; i < 4 && i < lines.length; ++i) {
                if (lines[i].equals("=")) {
                    continue;
                }
                String col = enabled ? enabledCol.toString() : disabledCol.toString();
                if (lines[i].matches("^&[0-9a-f]")) {
                    col = "";
                }
                if (!enabled) {
                    lines[i] = lines[i].replaceFirst("^&[0-9a-f]", "");
                }
                s.setLine(i, ChessUtils.parseColourSpec(col + lines[i]));
            }
            s.update();
        }
    }

}

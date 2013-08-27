package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.dhutils.LogUtils;

import org.bukkit.Location;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class ProtocolLibIntegration {
	public static void registerPlibPacketHandler(ChessCraft plugin) {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(PacketAdapter.params(plugin, Packets.Server.NAMED_SOUND_EFFECT).serverSide()) {
			@Override
			public void onPacketSending(PacketEvent event) {
				switch (event.getPacketID()) {
				case Packets.Server.NAMED_SOUND_EFFECT: // 0x3E
					// silence all mob noises if they're on a chess board
					// this preserves player sanity when using entity chess sets
					String soundName = event.getPacket().getStrings().read(0);
					int x = event.getPacket().getIntegers().read(0) >> 3;
					int y = event.getPacket().getIntegers().read(1) >> 3;
					int z = event.getPacket().getIntegers().read(2) >> 3;
					Location loc = new Location(event.getPlayer().getWorld(), x, y, z);
					if (BoardViewManager.getManager().partOfChessBoard(loc) != null) {
						if (soundName.matches("^mob\\.[a-z]+\\.(say|idle|bark)$")) {
							LogUtils.finer("cancel sound " + soundName + " -> " + event.getPlayer().getName() + " @ " + loc);
							event.setCancelled(true);
						}
					}
					break;
				}
			}
		});
	}
}

package me.Fupery.ArtMap.IO.Protocol.In;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.IO.Protocol.In.Packet.ArtistPacket;
import me.Fupery.ArtMap.Painting.ArtistHandler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import static me.Fupery.ArtMap.IO.Protocol.In.Packet.ArtistPacket.PacketInteract;
import static me.Fupery.ArtMap.IO.Protocol.In.Packet.ArtistPacket.PacketInteract.InteractType;

public class ProtocolLibReceiver extends PacketReceiver {

    public ProtocolLibReceiver() {
        registerListeners(ArtMap.instance());
    }

    private void registerListeners(JavaPlugin plugin) {
        PacketAdapter.AdapterParameteters options = new PacketAdapter.AdapterParameteters();
        options.plugin(plugin);
        options.optionAsync();
        options.connectionSide(ConnectionSide.CLIENT_SIDE);
        options.listenerPriority(ListenerPriority.HIGH);
        options.types(PacketType.Play.Client.ARM_ANIMATION, PacketType.Play.Client.LOOK,
                PacketType.Play.Client.USE_ENTITY);
        ProtocolLibrary.getProtocolManager().addPacketListener(new DefaultPacketAdapter(options));
    }

    private ArtistPacket getPacketType(PacketContainer packet) {
        if (packet.getType() == PacketType.Play.Client.LOOK) {
            float yaw = packet.getFloat().read(0);
            float pitch = packet.getFloat().read(1);
            return new ArtistPacket.PacketLook(yaw, pitch);

        } else if (packet.getType() == PacketType.Play.Client.ARM_ANIMATION) {
            return new ArtistPacket.PacketArmSwing();

        } else if (packet.getType() == PacketType.Play.Client.USE_ENTITY) {
            EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
            return new PacketInteract(
                action == EnumWrappers.EntityUseAction.ATTACK ? InteractType.ATTACK : InteractType.INTERACT);
        }
        return null;
    }

    @Override
    public void close() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(ArtMap.instance());
    }

    class DefaultPacketAdapter extends PacketAdapter {
        DefaultPacketAdapter(AdapterParameteters options) {
            super(options);
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            ArtistHandler handler = ArtMap.instance().getArtistHandler();
            if (!handler.containsPlayer(event.getPlayer()))
                return;
            ArtistPacket packet = getPacketType(event.getPacket());
            if (packet == null)
                return;
            if (!onPacketPlayIn(handler, event.getPlayer(), packet))
                event.setCancelled(true);
        }
    }

    @Override
    public void injectPlayer(Player player) throws ReflectiveOperationException {}

    @Override
    public void uninjectPlayer(Player player) {}
}

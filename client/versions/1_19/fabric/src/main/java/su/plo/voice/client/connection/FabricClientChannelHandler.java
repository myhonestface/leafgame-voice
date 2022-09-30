package su.plo.voice.client.connection;

import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import su.plo.voice.api.client.connection.ServerConnection;
import su.plo.voice.client.BaseVoiceClient;
import su.plo.voice.proto.packets.tcp.PacketTcpCodec;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public final class FabricClientChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

    private final BaseVoiceClient voiceClient;

    private ModServerConnection connection;

    @Override
    public void receive(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
        if (connection == null || handler.getConnection() != connection.getHandler()) {
            if (connection != null) close();
            this.connection = new ModServerConnection(handler.getConnection(), voiceClient);
            voiceClient.getEventBus().register(voiceClient, connection);
        }

        byte[] data = new byte[buf.readableBytes()];
        buf.duplicate().readBytes(data);

        try {
            PacketTcpCodec.decode(ByteStreams.newDataInput(data))
                    .ifPresent(connection::handle);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (connection != null) {
            voiceClient.getEventBus().unregister(voiceClient, connection);
            this.connection = null;
        }
    }

    public Optional<ServerConnection> getConnection() {
        return Optional.ofNullable(connection);
    }
}

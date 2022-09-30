package su.plo.voice.server;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.voice.BaseVoice;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivationManager;
import su.plo.voice.api.server.audio.line.ServerSourceLineManager;
import su.plo.voice.api.server.audio.source.ServerSourceManager;
import su.plo.voice.api.server.connection.TcpServerConnectionManager;
import su.plo.voice.api.server.connection.UdpServerConnectionManager;
import su.plo.voice.api.server.entity.EntityManager;
import su.plo.voice.api.server.event.VoiceServerInitializeEvent;
import su.plo.voice.api.server.event.VoiceServerShutdownEvent;
import su.plo.voice.api.server.event.socket.UdpServerCreateEvent;
import su.plo.voice.api.server.event.socket.UdpServerStartedEvent;
import su.plo.voice.api.server.event.socket.UdpServerStoppedEvent;
import su.plo.voice.api.server.pos.WorldManager;
import su.plo.voice.api.server.socket.UdpServer;
import su.plo.voice.server.audio.capture.VoiceServerActivationManager;
import su.plo.voice.server.audio.line.VoiceServerSourceLineManager;
import su.plo.voice.server.audio.source.VoiceServerSourceManager;
import su.plo.voice.server.config.ServerConfig;
import su.plo.voice.server.connection.VoiceTcpConnectionManager;
import su.plo.voice.server.connection.VoiceUdpConnectionManager;
import su.plo.voice.server.player.BasePlayerManager;
import su.plo.voice.server.player.LuckPermsListener;
import su.plo.voice.server.player.PermissionSupplier;
import su.plo.voice.server.socket.NettyUdpServer;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public abstract class BaseVoiceServer extends BaseVoice implements PlasmoVoiceServer {

    public static final String CHANNEL_STRING = "plasmo:voice";

    protected static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    protected final Logger logger = LogManager.getLogger();
    @Getter
    protected final TcpServerConnectionManager tcpConnectionManager = new VoiceTcpConnectionManager(this);
    @Getter
    protected final UdpServerConnectionManager udpConnectionManager = new VoiceUdpConnectionManager(this);
    @Getter
    protected final ServerSourceManager sourceManager = new VoiceServerSourceManager(this);

    protected UdpServer udpServer;
    @Getter
    protected PermissionSupplier permissionSupplier;
    @Getter
    protected BasePlayerManager playerManager;
    @Getter
    protected EntityManager entityManager;
    @Getter
    protected WorldManager worldManager;
    @Getter
    protected ServerActivationManager activationManager;
    @Getter
    protected ServerSourceLineManager sourceLineManager;

    protected LuckPermsListener luckPermsListener;

    @Getter
    protected ServerConfig config;

    @Override
    protected void onInitialize() {
        eventBus.call(new VoiceServerInitializeEvent(this));
        eventBus.register(this, sourceManager);
        eventBus.register(this, udpConnectionManager);

        try {
            this.config = toml.load(ServerConfig.class, new File(configFolder(), "config.toml"), true);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config", e);
        }

        this.permissionSupplier = createPermissionSupplier();

        this.playerManager = createPlayerManager(permissionSupplier);
        eventBus.register(this, playerManager);
        this.entityManager = createEntityManager();

        this.worldManager = createWorldManager();

        this.activationManager = new VoiceServerActivationManager(this, config.getVoice());
        this.sourceLineManager = new VoiceServerSourceLineManager(this);

        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            this.luckPermsListener = new LuckPermsListener(this, playerManager);
            luckPermsListener.subscribe();
        } catch (IllegalStateException | ClassNotFoundException ignored) {
            // luckperms not found
        }

        UdpServer server = new NettyUdpServer(this, config);

        UdpServerCreateEvent createEvent = new UdpServerCreateEvent(server);
        eventBus.call(createEvent);
        if (createEvent.isCancelled()) return;

        server = createEvent.getServer();

        try {
            int port = config.getHost().getPort();
            if (port == 0) {
                port = getMinecraftServerPort();
                if (port <= 0) port = 0;
            }

            server.start(config.getHost().getIp(), port);
            eventBus.call(new UdpServerStartedEvent(server));
            this.udpServer = server;
        } catch (Exception e) {
            getLogger().error("Failed to start the udp server", e);
            return;
        }
    }

    @Override
    protected void onShutdown() {
        eventBus.call(new VoiceServerShutdownEvent(this));

        if (luckPermsListener != null) {
            luckPermsListener.unsubscribe();
            this.luckPermsListener = null;
        }

        if (udpServer != null) {
            udpConnectionManager.clearConnections();
            udpServer.stop();
            eventBus.call(new UdpServerStoppedEvent(udpServer));

            this.udpServer = null;
        }

        // cleanup sources
        sourceManager.clear();

        // cleanup source lines
        sourceLineManager.clear();

        // cleanup activations
        activationManager.clear();

        // cleanup players
        playerManager.clear();

        eventBus.unregister(this);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Optional<UdpServer> getUdpServer() {
        return Optional.ofNullable(udpServer);
    }

    public abstract int getMinecraftServerPort();

    protected abstract PermissionSupplier createPermissionSupplier();

    protected abstract BasePlayerManager createPlayerManager(@NotNull PermissionSupplier permissionSupplier);

    protected abstract EntityManager createEntityManager();

    protected abstract WorldManager createWorldManager();
}

package mike.cutil.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mike.cutil.client.payload.CosmicApiRawPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CutilClient implements ClientModInitializer {

    public static final Identifier CHANNEL_ID = Identifier.of("cosmicapi", "cutil");
    private static final String CLIENT_ID = "client_mtm2jln6d1j9ed2knf";
    private static final String MOD_ID = "cutil";
    private static final String MOD_VERSION = "1.0.0";
    private static final String MC_VERSION = "1.21.11";

    private static final List<String> REQUESTED_SCOPES = List.of(
            "gang.messages:read",
            "gang.pings:read"
    );
    private static final List<String> REQUESTED_HOOKS = List.of(
            "gang.chat.message.created",
            "gang.ping.created",
            "player.enchant_proc",
            "player.cooldowns.changed",
            "player.effects.changed",
            "player.absorber.used",
            "player.command.succeeded",
            "player.trinket.changed",
            "player.chat_channel.changed",
            "bandit.killed",
            "player.library.changed",
            "player.vault.contribution.changed",
            "player.emblem.changed",
            "player.emblem.progress.changed",
            "player.emblem.unlocked",
            "player.top_credits.changed",
            "player.satchel_backpack.changed",
            "player.payday.changed",
            "server.event.changed",
            "server.merchant.spawned",
            "server.merchant.despawned",
            "server.weather.changed",
            "server.vault.raid.started",
            "server.vault.raid.ended"
    );

    public static final Text PREFIX = Text.literal("CUtil: ").formatted(Formatting.WHITE);

    private static final Gson GSON = new GsonBuilder().create();

    private boolean connected = false;
    private long lastConnectionTime = System.currentTimeMillis();

    @Override
    public void onInitializeClient() {

        PayloadTypeRegistry.playC2S().register(CosmicApiRawPayload.ID, CosmicApiRawPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CosmicApiRawPayload.ID, CosmicApiRawPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(CosmicApiRawPayload.ID, ((payload, context) -> {
            String json = new String(payload.payloadBytes(), StandardCharsets.UTF_8);
            context.client().execute(() -> handleServerMessage(context.client(), json));
        }));

        ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
            this.connected = false;
        }));

        ClientTickEvents.END_CLIENT_TICK.register(this::tryConnect);

    }

    private record ClientHello(
            String type,
            int protocolVersion,
            String clientId,
            String modId,
            String modLoader,
            String minecraftVersion,
            String modVersion,
            List<String> requestedScopes,
            List<String> requestedHooks
    ) {

    }

    private void tryConnect(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (connected || player == null) return;
        long currTime = System.currentTimeMillis();
        if (currTime - lastConnectionTime < 10000) return;
        if (!ClientPlayNetworking.canSend(CosmicApiRawPayload.ID)) {
            Text message = PREFIX.copy().append(Text.literal("Could not connect to CosmicAPI, retrying in 10s...").formatted(Formatting.RESET).formatted(Formatting.RED));
            player.sendMessage(message, false);
            lastConnectionTime = currTime;
            return;
        }
        sendHello();
        connected = true;
        Text connectedMessage = PREFIX.copy().append(Text.literal("Connected to the CosmicAPI!").formatted(Formatting.GREEN));
        player.sendMessage(connectedMessage, false);
    }

    private void sendHello() {
        ClientHello clientHello = new ClientHello(
                "client_hello",
                1,
                CLIENT_ID,
                MOD_ID,
                "fabric",
                MC_VERSION,
                MOD_VERSION,
                REQUESTED_SCOPES,
                REQUESTED_HOOKS
        );
        String hello = GSON.toJson(clientHello);
        ClientPlayNetworking.send(new CosmicApiRawPayload(hello.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleServerMessage(MinecraftClient client, String message) {
        if (client.player == null) return;
        client.player.sendMessage(Text.literal(message), false);
    }

}

package xyz.nikitacartes.easywhitelist;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Locale;

public final class LoginListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final EasyWhitelist plugin;

    public LoginListener(EasyWhitelist plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (plugin.getWhitelistStore() == null || !plugin.getWhitelistStore().isReady()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, LEGACY.deserialize(plugin.getConfig().getString("messages.init-not-ready", "&cWhitelist is still initializing. Try again in a moment.")));
            return;
        }

        String playerName = event.getName().toLowerCase(Locale.ROOT);
        if (!plugin.getWhitelistStore().isWhitelisted(playerName)) {
            String kickMessage = plugin.getConfig().getString("messages.kick-message", "&cYou are not on the whitelist.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, LEGACY.deserialize(kickMessage));
        }
    }
}
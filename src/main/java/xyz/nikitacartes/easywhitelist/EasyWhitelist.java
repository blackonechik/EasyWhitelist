package xyz.nikitacartes.easywhitelist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nikitacartes.easywhitelist.storage.WhitelistStore;

import java.util.Set;
import java.util.stream.Collectors;

public final class EasyWhitelist extends JavaPlugin implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private WhitelistStore whitelistStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            whitelistStore = new WhitelistStore(this);
            whitelistStore.initialize();
        } catch (Exception exception) {
            getLogger().severe("Failed to initialize PostgreSQL whitelist storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        if (getCommand("easywhitelist") != null) {
            getCommand("easywhitelist").setExecutor(this);
        }

        long refreshTicks = Math.max(20L, getConfig().getLong("refresh-interval-seconds", 15L) * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                whitelistStore.reloadCache();
            } catch (Exception exception) {
                getLogger().warning("Failed to refresh whitelist cache: " + exception.getMessage());
            }
        }, refreshTicks, refreshTicks);

        getLogger().info("EasyWhitelist enabled with PostgreSQL storage.");
    }

    @Override
    public void onDisable() {
        if (whitelistStore != null) {
            whitelistStore.close();
        }
    }

    public WhitelistStore getWhitelistStore() {
        return whitelistStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(message("messages.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        try {
            switch (subCommand) {
                case "reload" -> {
                    if (!hasPermission(sender, "easywhitelist.admin.reload")) {
                        sender.sendMessage(message("messages.no-permission"));
                        return true;
                    }

                    whitelistStore.reloadCache();
                    sender.sendMessage(message("messages.reload-success"));
                }
                case "add" -> {
                    if (args.length < 2) {
                        sender.sendMessage(message("messages.usage-add"));
                        return true;
                    }

                    if (!hasPermission(sender, "easywhitelist.admin.add")) {
                        sender.sendMessage(message("messages.no-permission"));
                        return true;
                    }

                    whitelistStore.upsert(args[1], true);
                    sender.sendMessage(message("messages.add-success", "{nick}", args[1]));
                }
                case "remove" -> {
                    if (args.length < 2) {
                        sender.sendMessage(message("messages.usage-remove"));
                        return true;
                    }

                    if (!hasPermission(sender, "easywhitelist.admin.remove")) {
                        sender.sendMessage(message("messages.no-permission"));
                        return true;
                    }

                    whitelistStore.remove(args[1]);
                    sender.sendMessage(message("messages.remove-success", "{nick}", args[1]));
                }
                case "list" -> {
                    if (!hasPermission(sender, "easywhitelist.admin.list")) {
                        sender.sendMessage(message("messages.no-permission"));
                        return true;
                    }

                    Set<String> entries = whitelistStore.snapshot();
                    String joined = entries.stream().sorted().limit(20).collect(Collectors.joining("&7, &f"));
                    sender.sendMessage(message("messages.list-header", "{count}", String.valueOf(entries.size()), "{list}", joined.isBlank() ? messageText("messages.list-empty") : joined));
                }
                default -> sender.sendMessage(message("messages.usage"));
            }
        } catch (Exception exception) {
            sender.sendMessage(message("messages.error", "{error}", exception.getMessage()));
        }

        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission("easywhitelist.admin") || sender.hasPermission(permission);
    }

    private Component message(String text) {
        return LEGACY.deserialize(messageText(text));
    }

    private Component message(String path, String... replacements) {
        String text = messageText(path);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            text = text.replace(replacements[index], replacements[index + 1]);
        }
        return LEGACY.deserialize(text);
    }

    private String messageText(String path) {
        return getConfig().getString(path, path);
    }
}

package kr.wolfool.wolfcraft.command;

import kr.wolfool.wolfcraft.WolfCraft;
import kr.wolfool.wolfcraft.gui.CraftGUI;
import kr.wolfool.wolfcraft.model.CraftRecipe;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CraftCommand implements CommandExecutor, TabCompleter {

    private final WolfCraft plugin;
    private final CraftGUI gui;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CraftCommand(WolfCraft plugin) {
        this.plugin = plugin;
        this.gui = new CraftGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("craftadmin")) {
            if (!player.hasPermission("wolfcraft.admin")) {
                player.sendMessage(mm.deserialize(plugin.getMessage("no-permission")));
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.reload();
                player.sendMessage(mm.deserialize(plugin.getMessage("reloaded")));
            }
            return true;
        }

        // /craft queue
        if (args.length > 0 && args[0].equalsIgnoreCase("queue")) {
            gui.openQueue(player);
            return true;
        }

        // /craft <recipe_id>
        if (args.length > 0) {
            CraftRecipe recipe = plugin.getRecipes().get(args[0].toLowerCase());
            if (recipe != null) {
                plugin.getCraftManager().startCraft(player, recipe);
                return true;
            }
        }

        gui.openMain(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(plugin.getRecipes().keySet());
            completions.add("queue");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

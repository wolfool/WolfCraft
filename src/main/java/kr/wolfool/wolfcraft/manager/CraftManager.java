package kr.wolfool.wolfcraft.manager;

import kr.wolfool.wolfcraft.WolfCraft;
import kr.wolfool.wolfcraft.model.CraftRecipe;
import kr.wolfool.wolfcraft.model.CraftTask;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages crafting queue per player.
 */
public class CraftManager {

    private final WolfCraft plugin;
    private final Map<UUID, List<CraftTask>> queues = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CraftManager(WolfCraft plugin) {
        this.plugin = plugin;
        startCheckTask();
    }

    /**
     * Start crafting a recipe for a player.
     */
    public boolean startCraft(Player player, CraftRecipe recipe) {
        UUID uuid = player.getUniqueId();
        List<CraftTask> queue = queues.computeIfAbsent(uuid, k -> new ArrayList<>());

        int maxQueue = plugin.getConfig().getInt("max-queue", 3);
        // Remove completed tasks that have been collected
        queue.removeIf(CraftTask::isCompleted);

        if (queue.size() >= maxQueue) {
            player.sendMessage(mm.deserialize(plugin.getMessage("queue-full")
                    .replace("{max}", String.valueOf(maxQueue))));
            return false;
        }

        // Check permission
        if (!player.hasPermission(recipe.getPermission())) {
            player.sendMessage(mm.deserialize(plugin.getMessage("no-permission")));
            return false;
        }

        // Check experience
        if (player.getLevel() < recipe.getExpCost()) {
            player.sendMessage(mm.deserialize(plugin.getMessage("not-enough-exp")
                    .replace("{exp}", String.valueOf(recipe.getExpCost()))));
            return false;
        }

        // Check and consume ingredients
        if (!hasIngredients(player, recipe)) {
            player.sendMessage(mm.deserialize(plugin.getMessage("not-enough-ingredients")));
            return false;
        }

        consumeIngredients(player, recipe);
        player.setLevel(player.getLevel() - recipe.getExpCost());

        CraftTask task = new CraftTask(uuid, recipe.getId(), recipe.getDisplay(),
                recipe.buildResult(), recipe.getTime());
        queue.add(task);

        player.sendMessage(mm.deserialize(plugin.getMessage("crafting-started")
                .replace("{recipe}", recipe.getDisplay())
                .replace("{time}", String.valueOf(recipe.getTime()))));

        return true;
    }

    /**
     * Collect a completed craft task.
     */
    public boolean collectTask(Player player, int index) {
        List<CraftTask> queue = queues.get(player.getUniqueId());
        if (queue == null || index < 0 || index >= queue.size()) return false;

        CraftTask task = queue.get(index);
        if (!task.isFinished()) return false;

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(mm.deserialize("<red>인벤토리가 가득 찼습니다!</red>"));
            return false;
        }

        player.getInventory().addItem(task.getResult());
        task.setCompleted(true);
        queue.remove(index);
        player.sendMessage(mm.deserialize(plugin.getMessage("crafting-collected")));
        return true;
    }

    /**
     * Get a player's crafting queue.
     */
    public List<CraftTask> getQueue(UUID uuid) {
        return queues.getOrDefault(uuid, new ArrayList<>());
    }

    private boolean hasIngredients(Player player, CraftRecipe recipe) {
        for (var entry : recipe.getIngredients().entrySet()) {
            int has = countMaterial(player, entry.getKey());
            if (has < entry.getValue()) return false;
        }
        return true;
    }

    private void consumeIngredients(Player player, CraftRecipe recipe) {
        for (var entry : recipe.getIngredients().entrySet()) {
            removeMaterial(player, entry.getKey(), entry.getValue());
        }
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == material) {
                count += is.getAmount();
            }
        }
        return count;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        int toRemove = amount;
        for (int i = 0; i < player.getInventory().getSize() && toRemove > 0; i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is != null && is.getType() == material) {
                if (is.getAmount() <= toRemove) {
                    toRemove -= is.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - toRemove);
                    toRemove = 0;
                }
            }
        }
    }

    /**
     * Periodic task to notify players of completed crafts.
     */
    private void startCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (var entry : queues.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null) continue;

                    for (CraftTask task : entry.getValue()) {
                        if (task.isFinished() && !task.isCompleted()) {
                            player.sendActionBar(mm.deserialize(
                                    "<gold>✅ " + task.getRecipeDisplay() + " 제작 완료!</gold>"));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Every 5 seconds
    }

    public void cleanup() {
        queues.clear();
    }
}

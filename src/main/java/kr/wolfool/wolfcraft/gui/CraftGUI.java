package kr.wolfool.wolfcraft.gui;

import kr.wolfool.wolfcraft.WolfCraft;
import kr.wolfool.wolfcraft.model.CraftRecipe;
import kr.wolfool.wolfcraft.model.CraftTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CraftGUI implements Listener {

    private final WolfCraft plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final String MAIN_TITLE = "🔨 제작대";
    private static final String CAT_PREFIX = "제작 - ";
    private static final String QUEUE_TITLE = "⏰ 제작 대기열";

    public CraftGUI(WolfCraft plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open main category selection.
     */
    public void openMain(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, Component.text(MAIN_TITLE));

        // Categories
        var catSection = plugin.getConfig().getConfigurationSection("categories");
        if (catSection != null) {
            for (String cat : catSection.getKeys(false)) {
                int slot = catSection.getInt(cat + ".slot", 0);
                Material mat;
                try {
                    mat = Material.valueOf(catSection.getString(cat + ".material", "CHEST").toUpperCase());
                } catch (Exception e) { mat = Material.CHEST; }

                long count = plugin.getRecipes().values().stream()
                        .filter(r -> r.getCategory().equals(cat)).count();

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text(cat, NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text(count + "개의 레시피", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("▶ 클릭하여 열기", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                ));
                item.setItemMeta(meta);
                gui.setItem(slot, item);
            }
        }

        // Queue button
        ItemStack queueBtn = new ItemStack(Material.CLOCK);
        ItemMeta qMeta = queueBtn.getItemMeta();
        int queueSize = plugin.getCraftManager().getQueue(player.getUniqueId()).size();
        qMeta.displayName(Component.text("⏰ 제작 대기열 (" + queueSize + ")", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        queueBtn.setItemMeta(qMeta);
        gui.setItem(7, queueBtn);

        player.openInventory(gui);
    }

    /**
     * Open category recipe list.
     */
    public void openCategory(Player player, String category) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(CAT_PREFIX + category));
        int slot = 0;

        for (CraftRecipe recipe : plugin.getRecipes().values()) {
            if (!recipe.getCategory().equals(category)) continue;
            if (slot >= 45) break;

            boolean hasPerm = player.hasPermission(recipe.getPermission());
            ItemStack item = new ItemStack(hasPerm ? recipe.getGuiMaterial() : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(mm.deserialize(recipe.getDisplay())
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("재료:", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            for (var entry : recipe.getIngredients().entrySet()) {
                String matName = entry.getKey().name().toLowerCase().replace("_", " ");
                int has = countMaterial(player, entry.getKey());
                NamedTextColor color = has >= entry.getValue() ? NamedTextColor.GREEN : NamedTextColor.RED;
                lore.add(Component.text("  " + matName + ": " + has + "/" + entry.getValue(), color)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("⏱ 제작 시간: " + recipe.getTime() + "초", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (recipe.getExpCost() > 0) {
                lore.add(Component.text("✨ 경험치: " + recipe.getExpCost() + "레벨", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            if (hasPerm) {
                lore.add(Component.text("▶ 클릭하여 제작", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("✖ 권한 없음", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← 돌아가기", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        gui.setItem(49, back);

        player.openInventory(gui);
    }

    /**
     * Open crafting queue.
     */
    public void openQueue(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text(QUEUE_TITLE));

        List<CraftTask> queue = plugin.getCraftManager().getQueue(player.getUniqueId());
        for (int i = 0; i < queue.size() && i < 9; i++) {
            CraftTask task = queue.get(i);

            ItemStack item;
            if (task.isFinished()) {
                item = task.getResult().clone();
                ItemMeta meta = item.getItemMeta();
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("✅ 완료! 클릭하여 수령", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            } else {
                // Progress bar
                int remaining = task.getRemainingSeconds();
                double progress = task.getProgress();
                int bars = (int) (progress * 20);
                StringBuilder progressBar = new StringBuilder();
                for (int b = 0; b < 20; b++) {
                    progressBar.append(b < bars ? "█" : "░");
                }

                item = new ItemStack(Material.CLOCK);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(mm.deserialize(task.getRecipeDisplay())
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.empty(),
                        Component.text(progressBar.toString(), NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("남은 시간: " + remaining + "초", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text(String.format("%.0f%%", progress * 100), NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(i + 9, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("← 돌아가기", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(bm);
        gui.setItem(22, back);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.getView().title());

        // Main menu
        if (title.equals(MAIN_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.CLOCK) {
                openQueue(player);
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;
            String catName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
            openCategory(player, catName);
            return;
        }

        // Category menu
        if (title.startsWith(CAT_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (clicked.getType() == Material.ARROW) { openMain(player); return; }
            if (clicked.getType() == Material.GRAY_DYE) return;

            String category = title.substring(CAT_PREFIX.length());
            int slot = event.getSlot();
            int index = 0;
            for (CraftRecipe recipe : plugin.getRecipes().values()) {
                if (!recipe.getCategory().equals(category)) continue;
                if (index == slot) {
                    player.closeInventory();
                    plugin.getCraftManager().startCraft(player, recipe);
                    return;
                }
                index++;
            }
            return;
        }

        // Queue menu
        if (title.equals(QUEUE_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (clicked.getType() == Material.ARROW) { openMain(player); return; }

            int queueIndex = event.getSlot() - 9;
            if (queueIndex >= 0) {
                plugin.getCraftManager().collectTask(player, queueIndex);
                openQueue(player); // Refresh
            }
        }
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == material) count += is.getAmount();
        }
        return count;
    }
}

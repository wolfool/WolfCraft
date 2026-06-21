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

/**
 * AddCook-style crafting GUI.
 *
 * Layout (6 rows = 54 slots):
 * Row 0: [CAT_SCROLL] [R01] [R02] [R03] [R04] [R05] [ ] [BOOK] [OUTPUT]
 * Row 1: [CAT_SCROLL] [R06] [R07] [R08] [R09] [R10] [ ] [ ->] [HAMMER]
 * Row 2: [CAT_SCROLL] [R11] [R12] [R13] [R14] [R15] [ ] [ ]  [ ]
 * Row 3: [CAT_SCROLL] [R16] [R17] [R18] [R19] [R20] [ ] [ ]  [ ]
 * Row 4: [CAT_SCROLL] [R21] [R22] [R23] [R24] [R25] [ ] [ ]  [ ]
 * Row 5: [PAGE<]  [Q1 ] [Q2 ] [Q3 ] [Q4 ] [Q5 ] [Q6] [Q7] [PAGE>]
 */
public class CraftGUI implements Listener {

    private final WolfCraft plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Player state
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, CraftRecipe> playerSelected = new HashMap<>();

    private static final String GUI_TITLE = "⚒ 제작대";
    private static final int GUI_SIZE = 54;

    // Slot positions
    private static final int[] CATEGORY_SLOTS = {0, 9, 18, 27, 36};
    private static final int[] RECIPE_SLOTS = {
            1, 2, 3, 4, 5,
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31, 32,
            37, 38, 39, 40, 41
    };
    private static final int BOOK_SLOT = 7;
    private static final int OUTPUT_SLOT = 8;
    private static final int ARROW_SLOT = 16;
    private static final int HAMMER_SLOT = 17;
    private static final int[] QUEUE_SLOTS = {46, 47, 48, 49, 50, 51, 52};
    private static final int PAGE_PREV = 45;
    private static final int PAGE_NEXT = 53;

    public CraftGUI(WolfCraft plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open crafting table GUI for a player.
     */
    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerCategory.containsKey(uuid)) {
            // Default to first category
            var cats = plugin.getCategories();
            if (!cats.isEmpty()) {
                playerCategory.put(uuid, cats.keySet().iterator().next());
            }
        }
        playerPage.putIfAbsent(uuid, 0);

        render(player);
    }

    private void render(Player player) {
        UUID uuid = player.getUniqueId();
        String category = playerCategory.getOrDefault(uuid, "");
        int page = playerPage.getOrDefault(uuid, 0);
        CraftRecipe selected = playerSelected.get(uuid);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, Component.text(GUI_TITLE));

        // Fill background with dark panes
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < GUI_SIZE; i++) gui.setItem(i, bg);

        // === CATEGORIES (left column) ===
        int catIdx = 0;
        for (var entry : plugin.getCategories().entrySet()) {
            if (catIdx >= CATEGORY_SLOTS.length) break;
            String catName = entry.getKey();
            Material catMat = entry.getValue();
            boolean isSelected = catName.equals(category);

            ItemStack catItem = createItem(
                    catMat,
                    (isSelected ? "§a▶ " : "§7") + catName,
                    isSelected ? List.of("§e현재 선택됨") : List.of("§7클릭하여 선택")
            );
            gui.setItem(CATEGORY_SLOTS[catIdx], catItem);
            catIdx++;
        }

        // === RECIPE GRID (5x5) ===
        List<CraftRecipe> catRecipes = plugin.getRecipes().values().stream()
                .filter(r -> r.getCategory().equals(category))
                .toList();

        int startIdx = page * RECIPE_SLOTS.length;
        for (int i = 0; i < RECIPE_SLOTS.length; i++) {
            int recipeIdx = startIdx + i;
            if (recipeIdx >= catRecipes.size()) {
                gui.setItem(RECIPE_SLOTS[i], createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ", null));
                continue;
            }

            CraftRecipe recipe = catRecipes.get(recipeIdx);
            boolean hasPerm = player.hasPermission(recipe.getPermission());
            boolean isSelected = selected != null && selected.getId().equals(recipe.getId());

            Material displayMat = hasPerm ? recipe.getGuiMaterial() : Material.BARRIER;
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (hasPerm) {
                // Show ingredient summary
                for (var ing : recipe.getIngredients().entrySet()) {
                    int has = countMaterial(player, ing.getKey());
                    String color = has >= ing.getValue() ? "§a" : "§c";
                    lore.add(color + formatMat(ing.getKey()) + ": " + has + "/" + ing.getValue());
                }
                lore.add("");
                lore.add("§7⏱ " + recipe.getTime() + "초");
                if (recipe.getExpCost() > 0) lore.add("§a✨ " + recipe.getExpCost() + " 레벨");
                lore.add("");
                lore.add(isSelected ? "§a✔ 선택됨" : "§e▶ 클릭하여 선택");
            } else {
                lore.add("§c✖ 권한 없음");
            }

            String name = (isSelected ? "§b§l▶ " : "§f") + recipe.getDisplay();
            // Strip minimessage tags for inventory name
            name = name.replaceAll("<[^>]+>", "");

            gui.setItem(RECIPE_SLOTS[i], createItem(displayMat, name, lore));
        }

        // === BOOK (recipe detail) ===
        gui.setItem(BOOK_SLOT, createItem(Material.KNOWLEDGE_BOOK, "§a📖 레시피 보기",
                selected != null ? List.of("§7선택된 레시피의 상세 정보") : List.of("§7레시피를 먼저 선택하세요")));

        // === OUTPUT PREVIEW ===
        if (selected != null) {
            ItemStack result = selected.buildResult();
            gui.setItem(OUTPUT_SLOT, result);

            // Arrow
            gui.setItem(ARROW_SLOT, createItem(Material.ARROW, "§7→", null));

            // Hammer (craft button)
            boolean canCraft = canCraft(player, selected);
            gui.setItem(HAMMER_SLOT, createItem(
                    canCraft ? Material.ANVIL : Material.BARRIER,
                    canCraft ? "§6§l⚒ 제작하기!" : "§c⚒ 제작 불가",
                    canCraft ? List.of("§e클릭하여 제작 시작") : List.of("§c재료 또는 경험치 부족")
            ));
        } else {
            gui.setItem(OUTPUT_SLOT, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7결과물", List.of("§7레시피를 선택하세요")));
            gui.setItem(HAMMER_SLOT, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7⚒ 제작", List.of("§7레시피를 선택하세요")));
        }

        // === QUEUE (bottom row) ===
        List<CraftTask> queue = plugin.getCraftManager().getQueue(player.getUniqueId());
        for (int i = 0; i < QUEUE_SLOTS.length; i++) {
            if (i < queue.size()) {
                CraftTask task = queue.get(i);
                if (task.isFinished()) {
                    ItemStack qItem = task.getResult().clone();
                    ItemMeta qm = qItem.getItemMeta();
                    List<Component> lore2 = qm.lore() != null ? new ArrayList<>(qm.lore()) : new ArrayList<>();
                    lore2.add(Component.empty());
                    lore2.add(Component.text("✅ 완료! 클릭하여 수령", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                    qm.lore(lore2);
                    qItem.setItemMeta(qm);
                    gui.setItem(QUEUE_SLOTS[i], qItem);
                } else {
                    double progress = task.getProgress();
                    int bars = (int) (progress * 16);
                    StringBuilder pb = new StringBuilder();
                    for (int b = 0; b < 16; b++) pb.append(b < bars ? "§a█" : "§8░");

                    String taskName = task.getRecipeDisplay().replaceAll("<[^>]+>", "");
                    gui.setItem(QUEUE_SLOTS[i], createItem(Material.CLOCK,
                            "§e⏳ " + taskName,
                            List.of("", pb.toString(),
                                    "§7남은 시간: §f" + task.getRemainingSeconds() + "초",
                                    "§7진행: §f" + String.format("%.0f%%", progress * 100))
                    ));
                }
            } else {
                gui.setItem(QUEUE_SLOTS[i], createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                        "§8대기열 슬롯", List.of("§7비어있음")));
            }
        }

        // === PAGE BUTTONS ===
        int totalPages = (int) Math.ceil((double) catRecipes.size() / RECIPE_SLOTS.length);
        if (page > 0) {
            gui.setItem(PAGE_PREV, createItem(Material.ARROW, "§a◀ 이전 페이지", List.of("§7페이지 " + page + "/" + totalPages)));
        }
        if (page < totalPages - 1) {
            gui.setItem(PAGE_NEXT, createItem(Material.ARROW, "§a다음 페이지 ▶", List.of("§7페이지 " + (page + 2) + "/" + totalPages)));
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.getView().title());
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;
        UUID uuid = player.getUniqueId();

        // Category click
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (slot == CATEGORY_SLOTS[i]) {
                int catIdx = 0;
                for (String catName : plugin.getCategories().keySet()) {
                    if (catIdx == i) {
                        playerCategory.put(uuid, catName);
                        playerPage.put(uuid, 0);
                        playerSelected.remove(uuid);
                        render(player);
                        return;
                    }
                    catIdx++;
                }
                return;
            }
        }

        // Recipe click
        for (int i = 0; i < RECIPE_SLOTS.length; i++) {
            if (slot == RECIPE_SLOTS[i]) {
                String category = playerCategory.getOrDefault(uuid, "");
                List<CraftRecipe> catRecipes = plugin.getRecipes().values().stream()
                        .filter(r -> r.getCategory().equals(category)).toList();
                int page = playerPage.getOrDefault(uuid, 0);
                int recipeIdx = page * RECIPE_SLOTS.length + i;
                if (recipeIdx < catRecipes.size()) {
                    CraftRecipe recipe = catRecipes.get(recipeIdx);
                    if (player.hasPermission(recipe.getPermission())) {
                        playerSelected.put(uuid, recipe);
                        render(player);
                    }
                }
                return;
            }
        }

        // Hammer click (craft)
        if (slot == HAMMER_SLOT) {
            CraftRecipe selected = playerSelected.get(uuid);
            if (selected != null && canCraft(player, selected)) {
                plugin.getCraftManager().startCraft(player, selected);
                render(player);
            }
            return;
        }

        // Book click (recipe detail)
        if (slot == BOOK_SLOT) {
            CraftRecipe selected = playerSelected.get(uuid);
            if (selected != null) {
                showRecipeDetail(player, selected);
            }
            return;
        }

        // Queue click (collect)
        for (int i = 0; i < QUEUE_SLOTS.length; i++) {
            if (slot == QUEUE_SLOTS[i]) {
                List<CraftTask> queue = plugin.getCraftManager().getQueue(uuid);
                if (i < queue.size() && queue.get(i).isFinished()) {
                    plugin.getCraftManager().collectTask(player, i);
                    render(player);
                }
                return;
            }
        }

        // Page buttons
        if (slot == PAGE_PREV) {
            int page = playerPage.getOrDefault(uuid, 0);
            if (page > 0) {
                playerPage.put(uuid, page - 1);
                render(player);
            }
            return;
        }
        if (slot == PAGE_NEXT) {
            int page = playerPage.getOrDefault(uuid, 0);
            playerPage.put(uuid, page + 1);
            render(player);
        }
    }

    private void showRecipeDetail(Player player, CraftRecipe recipe) {
        Inventory detail = Bukkit.createInventory(null, 27, Component.text("📖 " + recipe.getDisplay().replaceAll("<[^>]+>", "")));

        // Fill background
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) detail.setItem(i, bg);

        // Ingredients (left side)
        int slot = 0;
        for (var entry : recipe.getIngredients().entrySet()) {
            if (slot > 5) break;
            int has = countMaterial(player, entry.getKey());
            String color = has >= entry.getValue() ? "§a" : "§c";
            ItemStack item = new ItemStack(entry.getKey(), entry.getValue());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(formatMat(entry.getKey()), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("필요: " + entry.getValue() + "개", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("보유: " + has + "개",
                            has >= entry.getValue() ? NamedTextColor.GREEN : NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            detail.setItem(slot < 3 ? slot : slot + 6, item);
            slot++;
        }

        // Arrow
        detail.setItem(13, createItem(Material.ARROW, "§7→", null));

        // Result
        detail.setItem(15, recipe.buildResult());

        // Info
        detail.setItem(16, createItem(Material.PAPER, "§e정보",
                List.of("§7⏱ 제작 시간: §f" + recipe.getTime() + "초",
                        recipe.getExpCost() > 0 ? "§a✨ 경험치: §f" + recipe.getExpCost() + " 레벨" : "§7경험치 불필요")));

        // Back button
        detail.setItem(22, createItem(Material.BARRIER, "§c← 돌아가기", List.of("§7클릭하여 제작대로")));

        player.openInventory(detail);
    }

    private boolean canCraft(Player player, CraftRecipe recipe) {
        if (!player.hasPermission(recipe.getPermission())) return false;
        if (player.getLevel() < recipe.getExpCost()) return false;
        for (var entry : recipe.getIngredients().entrySet()) {
            if (countMaterial(player, entry.getKey()) < entry.getValue()) return false;
        }
        int maxQueue = plugin.getConfig().getInt("max-queue", 3);
        List<CraftTask> queue = plugin.getCraftManager().getQueue(player.getUniqueId());
        long active = queue.stream().filter(t -> !t.isCompleted()).count();
        return active < maxQueue;
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == material) count += is.getAmount();
        }
        return count;
    }

    private String formatMat(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        if (lore != null) {
            meta.lore(lore.stream()
                    .map(l -> (Component) Component.text(l).decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}

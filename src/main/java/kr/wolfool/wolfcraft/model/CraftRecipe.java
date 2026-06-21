package kr.wolfool.wolfcraft.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A custom recipe with ingredients, result, time, and requirements.
 */
public class CraftRecipe {

    private final String id;
    private final String display;
    private final String category;
    private final Map<Material, Integer> ingredients;
    private final Material resultMaterial;
    private final int resultAmount;
    private final String resultName;
    private final List<String> resultLore;
    private final Map<Enchantment, Integer> enchantments;
    private final int time; // seconds
    private final String permission;
    private final int expCost;
    private final Material guiMaterial;

    public CraftRecipe(String id, String display, String category,
                       Map<Material, Integer> ingredients,
                       Material resultMaterial, int resultAmount,
                       String resultName, List<String> resultLore,
                       Map<Enchantment, Integer> enchantments,
                       int time, String permission, int expCost,
                       Material guiMaterial) {
        this.id = id;
        this.display = display;
        this.category = category;
        this.ingredients = ingredients;
        this.resultMaterial = resultMaterial;
        this.resultAmount = resultAmount;
        this.resultName = resultName;
        this.resultLore = resultLore;
        this.enchantments = enchantments;
        this.time = time;
        this.permission = permission.isEmpty() ? "wolfcraft.use" : permission;
        this.expCost = expCost;
        this.guiMaterial = guiMaterial;
    }

    /**
     * Build the result ItemStack.
     */
    public ItemStack buildResult() {
        MiniMessage mm = MiniMessage.miniMessage();
        ItemStack item = new ItemStack(resultMaterial, resultAmount);
        ItemMeta meta = item.getItemMeta();

        if (resultName != null && !resultName.isEmpty()) {
            meta.displayName(mm.deserialize(resultName));
        }

        if (resultLore != null && !resultLore.isEmpty()) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : resultLore) {
                lore.add(mm.deserialize(line));
            }
            meta.lore(lore);
        }

        item.setItemMeta(meta);

        if (enchantments != null) {
            for (var entry : enchantments.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        return item;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplay() { return display; }
    public String getCategory() { return category; }
    public Map<Material, Integer> getIngredients() { return ingredients; }
    public int getTime() { return time; }
    public String getPermission() { return permission; }
    public int getExpCost() { return expCost; }
    public Material getGuiMaterial() { return guiMaterial; }
}

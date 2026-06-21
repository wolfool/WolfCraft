package kr.wolfool.wolfcraft;

import kr.wolfool.wolfcraft.command.CraftCommand;
import kr.wolfool.wolfcraft.manager.CraftManager;
import kr.wolfool.wolfcraft.model.CraftRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WolfCraft extends JavaPlugin {

    private static WolfCraft instance;
    private final Map<String, CraftRecipe> recipes = new LinkedHashMap<>();
    private final Map<String, Material> categories = new LinkedHashMap<>();
    private CraftManager craftManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadCategories();
        loadRecipes();

        craftManager = new CraftManager(this);

        CraftCommand cmd = new CraftCommand(this);
        var craftCmd = getCommand("craft");
        if (craftCmd != null) {
            craftCmd.setExecutor(cmd);
            craftCmd.setTabCompleter(cmd);
        }
        var adminCmd = getCommand("craftadmin");
        if (adminCmd != null) {
            adminCmd.setExecutor(cmd);
        }

        getLogger().info("WolfCraft v" + getDescription().getVersion() + " 활성화!");
    }

    @Override
    public void onDisable() {
        if (craftManager != null) craftManager.cleanup();
        getLogger().info("WolfCraft 비활성화!");
    }

    @SuppressWarnings("deprecation")
    private void loadRecipes() {
        recipes.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("recipes");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            ConfigurationSection r = sec.getConfigurationSection(key);
            if (r == null) continue;

            try {
                String display = r.getString("display", key);
                String category = r.getString("category", "기타");

                // Parse ingredients
                Map<Material, Integer> ingredients = new LinkedHashMap<>();
                ConfigurationSection ingr = r.getConfigurationSection("ingredients");
                if (ingr != null) {
                    for (String matName : ingr.getKeys(false)) {
                        Material mat = Material.valueOf(matName.toUpperCase());
                        ingredients.put(mat, ingr.getInt(matName));
                    }
                }

                // Parse result
                ConfigurationSection result = r.getConfigurationSection("result");
                Material resultMat = Material.valueOf(result.getString("material", "STONE").toUpperCase());
                int resultAmount = result.getInt("amount", 1);
                String resultName = result.getString("name", "");
                List<String> resultLore = result.getStringList("lore");

                // Parse enchantments
                Map<Enchantment, Integer> enchants = new HashMap<>();
                ConfigurationSection enchSec = result.getConfigurationSection("enchantments");
                if (enchSec != null) {
                    for (String enchName : enchSec.getKeys(false)) {
                        Enchantment ench = Registry.ENCHANTMENT.get(
                                NamespacedKey.minecraft(enchName.toLowerCase()));
                        if (ench != null) {
                            enchants.put(ench, enchSec.getInt(enchName));
                        }
                    }
                }

                int time = r.getInt("time", 30);
                String permission = r.getString("permission", "");
                int expCost = r.getInt("exp-cost", 0);
                Material guiMat;
                try {
                    guiMat = Material.valueOf(r.getString("gui-material", "CRAFTING_TABLE").toUpperCase());
                } catch (Exception e) { guiMat = Material.CRAFTING_TABLE; }

                recipes.put(key, new CraftRecipe(key, display, category, ingredients,
                        resultMat, resultAmount, resultName, resultLore, enchants,
                        time, permission, expCost, guiMat));
            } catch (Exception e) {
                getLogger().warning("레시피 로드 실패: " + key + " - " + e.getMessage());
            }
        }
        getLogger().info(recipes.size() + "개의 레시피가 로드되었습니다.");
    }

    private void loadCategories() {
        categories.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("categories");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            Material mat;
            try {
                mat = Material.valueOf(sec.getString(key + ".material", "CHEST").toUpperCase());
            } catch (Exception e) { mat = Material.CHEST; }
            categories.put(key, mat);
        }
    }

    public void reload() {
        reloadConfig();
        loadCategories();
        loadRecipes();
    }

    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "<red>메시지 없음: " + key + "</red>");
    }

    public static WolfCraft getInstance() { return instance; }
    public Map<String, CraftRecipe> getRecipes() { return recipes; }
    public Map<String, Material> getCategories() { return categories; }
    public CraftManager getCraftManager() { return craftManager; }
}

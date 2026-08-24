package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.ShapedRecipe;
 import org.bukkit.inventory.ShapelessRecipe;
 import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecipeManager {

    // ================================================================
    //  제작 횟수 추적
    // ================================================================

    private static final Map<UUID, Integer> apprenticeCrafts = new HashMap<>();
    private static final Map<UUID, Integer> swordCrafts = new HashMap<>();
    private static final Map<UUID, Integer> bowCrafts = new HashMap<>();
    private static final Map<UUID, Integer> compassCrafts = new HashMap<>();
    private static final Map<UUID, Integer> sharpnessBookCrafts = new HashMap<>();
    private static final Map<UUID, Integer> powerBookCrafts = new HashMap<>();
    private static final Map<UUID, Integer> dragonSwordCrafts = new HashMap<>();
    private static final Map<UUID, Integer> leatherSavingCrafts = new HashMap<>();
    private static final Map<UUID, Integer> protectionBookCrafts = new HashMap<>();
    private static final Map<UUID, Integer> dragonArmorCrafts = new HashMap<>();
    private static final Map<UUID, Integer> nectarCrafts = new HashMap<>();
    private static final Map<UUID, Integer> burningArtifactCrafts = new HashMap<>();
    private static final Map<UUID, Integer> deliciousMealCrafts = new HashMap<>();
    private static final Map<UUID, Integer> toughnessPotionCrafts = new HashMap<>();
    private static final Map<UUID, Integer> sevenLeagueBootsCrafts = new HashMap<>();
    private static final Map<UUID, Integer> ironPackCrafts = new HashMap<>();
    private static final Map<UUID, Integer> obsidianMixCrafts = new HashMap<>();
    private static final Map<UUID, Integer> tarnhelmCrafts = new HashMap<>();
    private static final Map<UUID, Integer> philosopherPickaxeCrafts = new HashMap<>();
    private static final Map<UUID, Integer> awakeningPackCrafts = new HashMap<>();
    private static final Map<UUID, Integer> lightAnvilCrafts = new HashMap<>();
    private static final Map<UUID, Integer> lightEnchantingTableCrafts = new HashMap<>();
    private static final Map<UUID, Integer> evesTemptationCrafts = new HashMap<>();
    private static final Map<UUID, Integer> fruitOfRecoveryCrafts = new HashMap<>();
    private static final Map<UUID, Integer> lightAppleCrafts = new HashMap<>();
    private static final Map<UUID, Integer> goldenHeadCrafts = new HashMap<>();

    public static int getApprenticeCrafts(UUID uuid)      { return apprenticeCrafts.getOrDefault(uuid, 0); }
    public static int getSwordCrafts(UUID uuid)           { return swordCrafts.getOrDefault(uuid, 0); }
    public static int getBowCrafts(UUID uuid)             { return bowCrafts.getOrDefault(uuid, 0); }
    public static int getCompassCrafts(UUID uuid)         { return compassCrafts.getOrDefault(uuid, 0); }
    public static int getSharpnessBookCrafts(UUID uuid)   { return sharpnessBookCrafts.getOrDefault(uuid, 0); }
    public static int getPowerBookCrafts(UUID uuid)       { return powerBookCrafts.getOrDefault(uuid, 0); }
    public static int getDragonSwordCrafts(UUID uuid)     { return dragonSwordCrafts.getOrDefault(uuid, 0); }
    public static int getLeatherSavingCrafts(UUID uuid)   { return leatherSavingCrafts.getOrDefault(uuid, 0); }
    public static int getProtectionBookCrafts(UUID uuid)  { return protectionBookCrafts.getOrDefault(uuid, 0); }
    public static int getDragonArmorCrafts(UUID uuid)     { return dragonArmorCrafts.getOrDefault(uuid, 0); }
    public static int getNectarCrafts(UUID uuid)          { return nectarCrafts.getOrDefault(uuid, 0); }
    public static int getBurningArtifactCrafts(UUID uuid) { return burningArtifactCrafts.getOrDefault(uuid, 0); }
    public static int getDeliciousMealCrafts(UUID uuid)   { return deliciousMealCrafts.getOrDefault(uuid, 0); }
    public static int getToughnessPotionCrafts(UUID uuid) { return toughnessPotionCrafts.getOrDefault(uuid, 0); }
    public static int getSevenLeagueBootsCrafts(UUID uuid){ return sevenLeagueBootsCrafts.getOrDefault(uuid, 0); }
    public static int getIronPackCrafts(UUID uuid)        { return ironPackCrafts.getOrDefault(uuid, 0); }
    public static int getObsidianMixCrafts(UUID uuid)     { return obsidianMixCrafts.getOrDefault(uuid, 0); }
    public static int getTarnhelmCrafts(UUID uuid)        { return tarnhelmCrafts.getOrDefault(uuid, 0); }
    public static int getPhilosopherPickaxeCrafts(UUID uuid){ return philosopherPickaxeCrafts.getOrDefault(uuid, 0); }
    public static int getAwakeningPackCrafts(UUID uuid)   { return awakeningPackCrafts.getOrDefault(uuid, 0); }
    public static int getLightAnvilCrafts(UUID uuid)      { return lightAnvilCrafts.getOrDefault(uuid, 0); }
    public static int getLightEnchantingTableCrafts(UUID uuid){ return lightEnchantingTableCrafts.getOrDefault(uuid, 0); }
    public static int getEvesTemptationCrafts(UUID uuid)  { return evesTemptationCrafts.getOrDefault(uuid, 0); }
    public static int getFruitOfRecoveryCrafts(UUID uuid) { return fruitOfRecoveryCrafts.getOrDefault(uuid, 0); }
    public static int getLightAppleCrafts(UUID uuid)      { return lightAppleCrafts.getOrDefault(uuid, 0); }
    public static int getGoldenHeadCrafts(UUID uuid)      { return goldenHeadCrafts.getOrDefault(uuid, 0); }

    public static void addApprenticeCraft(UUID uuid)      { apprenticeCrafts.put(uuid, getApprenticeCrafts(uuid) + 1); }
    public static void addSwordCraft(UUID uuid)           { swordCrafts.put(uuid, getSwordCrafts(uuid) + 1); }
    public static void addBowCraft(UUID uuid)             { bowCrafts.put(uuid, getBowCrafts(uuid) + 1); }
    public static void addCompassCraft(UUID uuid)         { compassCrafts.put(uuid, getCompassCrafts(uuid) + 1); }
    public static void addSharpnessBookCraft(UUID uuid)   { sharpnessBookCrafts.put(uuid, getSharpnessBookCrafts(uuid) + 1); }
    public static void addPowerBookCraft(UUID uuid)       { powerBookCrafts.put(uuid, getPowerBookCrafts(uuid) + 1); }
    public static void addDragonSwordCraft(UUID uuid)     { dragonSwordCrafts.put(uuid, getDragonSwordCrafts(uuid) + 1); }
    public static void addLeatherSavingCraft(UUID uuid)   { leatherSavingCrafts.put(uuid, getLeatherSavingCrafts(uuid) + 1); }
    public static void addProtectionBookCraft(UUID uuid)  { protectionBookCrafts.put(uuid, getProtectionBookCrafts(uuid) + 1); }
    public static void addDragonArmorCraft(UUID uuid)     { dragonArmorCrafts.put(uuid, getDragonArmorCrafts(uuid) + 1); }
    public static void addNectarCraft(UUID uuid)          { nectarCrafts.put(uuid, getNectarCrafts(uuid) + 1); }
    public static void addBurningArtifactCraft(UUID uuid) { burningArtifactCrafts.put(uuid, getBurningArtifactCrafts(uuid) + 1); }
    public static void addDeliciousMealCraft(UUID uuid)   { deliciousMealCrafts.put(uuid, getDeliciousMealCrafts(uuid) + 1); }
    public static void addToughnessPotionCraft(UUID uuid) { toughnessPotionCrafts.put(uuid, getToughnessPotionCrafts(uuid) + 1); }
    public static void addSevenLeagueBootsCraft(UUID uuid){ sevenLeagueBootsCrafts.put(uuid, getSevenLeagueBootsCrafts(uuid) + 1); }
    public static void addIronPackCraft(UUID uuid)        { ironPackCrafts.put(uuid, getIronPackCrafts(uuid) + 1); }
    public static void addObsidianMixCraft(UUID uuid)     { obsidianMixCrafts.put(uuid, getObsidianMixCrafts(uuid) + 1); }
    public static void addTarnhelmCraft(UUID uuid)        { tarnhelmCrafts.put(uuid, getTarnhelmCrafts(uuid) + 1); }
    public static void addPhilosopherPickaxeCraft(UUID uuid){ philosopherPickaxeCrafts.put(uuid, getPhilosopherPickaxeCrafts(uuid) + 1); }
    public static void addAwakeningPackCraft(UUID uuid)   { awakeningPackCrafts.put(uuid, getAwakeningPackCrafts(uuid) + 1); }
    public static void addLightAnvilCraft(UUID uuid)      { lightAnvilCrafts.put(uuid, getLightAnvilCrafts(uuid) + 1); }
    public static void addLightEnchantingTableCraft(UUID uuid){ lightEnchantingTableCrafts.put(uuid, getLightEnchantingTableCrafts(uuid) + 1); }
    public static void addEvesTemptationCraft(UUID uuid)  { evesTemptationCrafts.put(uuid, getEvesTemptationCrafts(uuid) + 1); }
    public static void addFruitOfRecoveryCraft(UUID uuid) { fruitOfRecoveryCrafts.put(uuid, getFruitOfRecoveryCrafts(uuid) + 1); }
    public static void addLightAppleCraft(UUID uuid)      { lightAppleCrafts.put(uuid, getLightAppleCrafts(uuid) + 1); }
    public static void addGoldenHeadCraft(UUID uuid)      { goldenHeadCrafts.put(uuid, getGoldenHeadCrafts(uuid) + 1); }

    public static void resetCrafts() {
        apprenticeCrafts.clear();
        swordCrafts.clear();
        bowCrafts.clear();
        compassCrafts.clear();
        sharpnessBookCrafts.clear();
        powerBookCrafts.clear();
        dragonSwordCrafts.clear();
        leatherSavingCrafts.clear();
        protectionBookCrafts.clear();
        dragonArmorCrafts.clear();
        nectarCrafts.clear();
        burningArtifactCrafts.clear();
        deliciousMealCrafts.clear();
        toughnessPotionCrafts.clear();
        sevenLeagueBootsCrafts.clear();
        ironPackCrafts.clear();
        obsidianMixCrafts.clear();
        tarnhelmCrafts.clear();
        philosopherPickaxeCrafts.clear();
        awakeningPackCrafts.clear();
        lightAnvilCrafts.clear();
        lightEnchantingTableCrafts.clear();
        evesTemptationCrafts.clear();
        fruitOfRecoveryCrafts.clear();
        lightAppleCrafts.clear();
        goldenHeadCrafts.clear();
    }

    // ================================================================
    //  레시피 등록
    // ================================================================

    public static void registerRecipes(JavaPlugin plugin) {
        registerApprenticeHelmet(plugin);
        registerApprenticeSword(plugin);
        registerApprenticeBow(plugin);
        registerMasterCompass(plugin);
        registerSharpnessBook(plugin);
        registerPowerBook(plugin);
        registerDragonSword(plugin);
        registerLeatherSaving(plugin);
        registerProtectionBook(plugin);
        registerDragonArmor(plugin);
        registerNectar(plugin);
        registerBurningArtifact(plugin);
        registerDeliciousMeal(plugin);
        registerToughnessPotion(plugin);
        registerSevenLeagueBoots(plugin);
        registerIronPack(plugin);
        registerObsidianMix(plugin);
        registerTarnhelm(plugin);
        registerPhilosopherPickaxe(plugin);
        registerAwakeningPack(plugin);
        registerLightAnvil(plugin);
        registerLightEnchantingTable(plugin);
        registerEvesTemptation(plugin);
        registerFruitOfRecovery(plugin);
        registerLightApple(plugin);
        registerGoldenHead(plugin);
    }

    // ================================================================
    //  견습용 투구 (Apprentice Helmet)
    // ================================================================

    private static void registerApprenticeHelmet(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "apprentice_helmet");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getApprenticeHelmet());
        recipe.shape("III", "IRI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE_TORCH);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getApprenticeHelmet() {
        ItemStack item = new ItemStack(Material.IRON_HELMET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "견습용 투구");
            item.setItemMeta(meta);
        }
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 1);
        item.addUnsafeEnchantment(Enchantment.FIRE_PROTECTION, 1);
        item.addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, 1);
        item.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 1);
        return item;
    }

    public static ItemStack getApprenticeHelmetForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getApprenticeCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getApprenticeHelmet();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(ChatColor.GRAY + "제작 한도: " + limitText));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final String APPRENTICE_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + "견습용 투구";

    public static void openApprenticeRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, APPRENTICE_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        // 조합법 그리드 (0-indexed): 11,12,13 / 20,21,22 / 29,30,31
        gui.setItem(11, new ItemStack(Material.IRON_INGOT));
        gui.setItem(12, new ItemStack(Material.IRON_INGOT));
        gui.setItem(13, new ItemStack(Material.IRON_INGOT));
        gui.setItem(20, new ItemStack(Material.IRON_INGOT));
        gui.setItem(21, new ItemStack(Material.REDSTONE_TORCH));
        gui.setItem(22, new ItemStack(Material.IRON_INGOT));
        gui.setItem(29, air);
        gui.setItem(30, air);
        gui.setItem(31, air);

        // 화살표 (0-indexed 23), 결과물 (0-indexed 24)
        gui.setItem(23, makeArrow());
        gui.setItem(24, getApprenticeHelmetForDisplay(p.getUniqueId()));

        // 뒤로가기 (0-indexed 49 = 6행 가운데)
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  견습용 검 (Apprentice Sword)
    // ================================================================

    public static final String SWORD_TAG = "견습용 검";

    private static void registerApprenticeSword(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "apprentice_sword");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getApprenticeSword());
        // 레드스톤블록 / 철검 / 레드스톤블록 (세로 배치)
        recipe.shape(
            " R ",
            " S ",
            " R "
        );
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
        recipe.setIngredient('S', Material.IRON_SWORD);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getApprenticeSword() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + SWORD_TAG);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "이 검은 시간이 지날수록 강해집니다.",
                ChatColor.YELLOW + "PVP 시작 시 날카로움 I",
                ChatColor.GOLD + "25분 후 날카로움 II"
            ));
            // 모루 · 마법부여대 사용 불가
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getApprenticeSwordForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getSwordCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getApprenticeSword();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(meta.getLore() != null ? meta.getLore() : new ArrayList<>());
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final String APPRENTICE_SWORD_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + "견습용 검";

    public static void openApprenticeSwordRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, APPRENTICE_SWORD_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        // 세로 배치: 빈칸/레드블록/빈칸 × 3행 (0-indexed 11,12,13 / 20,21,22 / 29,30,31)
        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(13, air);
        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.IRON_SWORD));
        gui.setItem(22, air);
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(31, air);

        // 화살표 (0-indexed 23), 결과물 (0-indexed 24)
        gui.setItem(23, makeArrow());
        gui.setItem(24, getApprenticeSwordForDisplay(p.getUniqueId()));

        // 뒤로가기 (0-indexed 49)
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  견습용 활 (Apprentice Bow)
    // ================================================================

    public static final String BOW_TAG = "견습용 활";

    private static void registerApprenticeBow(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "apprentice_bow");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getApprenticeBow());
        // 활 모양: 막대기 대신 레드스톤 횃불
        recipe.shape(
            " RS",
            "R S",
            " RS"
        );
        recipe.setIngredient('R', Material.REDSTONE_TORCH);
        recipe.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getApprenticeBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + BOW_TAG);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "이 활은 시간이 지날수록 강해집니다.",
                ChatColor.YELLOW + "PVP 시작 시 힘 I",
                ChatColor.GOLD + "15분 후 힘 II",
                ChatColor.RED + "25분 후 힘 III"
            ));
            // 모루 · 마법부여대 사용 불가 플래그
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getApprenticeBowForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getBowCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getApprenticeBow();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(meta.getLore() != null ? meta.getLore() : new ArrayList<>());
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final String APPRENTICE_BOW_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + "견습용 활";

    public static void openApprenticeBowRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, APPRENTICE_BOW_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        // 조합법 (0-indexed 11,12,13 / 20,21,22 / 29,30,31)
        // " RS", "R S", " RS"
        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.REDSTONE_TORCH));
        gui.setItem(13, new ItemStack(Material.STRING));
        
        gui.setItem(20, new ItemStack(Material.REDSTONE_TORCH));
        gui.setItem(21, air);
        gui.setItem(22, new ItemStack(Material.STRING));
        
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.REDSTONE_TORCH));
        gui.setItem(31, new ItemStack(Material.STRING));

        // 화살표 (0-indexed 23), 결과물 (0-indexed 24)
        gui.setItem(23, makeArrow());
        gui.setItem(24, getApprenticeBowForDisplay(p.getUniqueId()));

        // 뒤로가기 (0-indexed 49)
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  마스터의 나침반 (Master Compass)
    // ================================================================

    public static final String COMPASS_TAG = "\ub9c8\uc2a4\ud130\uc758 \ub098\uce68\ubc18";

    private static void registerMasterCompass(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "master_compass");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getMasterCompass());
        // U\uc790(\uac00\ub9c8\uc194) \ubaa8\uc591: \ub808\ub4dc\uc2a4\ud1a4 7 + \ub098\uce68\ubc18 + \ud683\ubd88
        recipe.shape(
            "RTR",
            "RCR",
            "RRR"
        );
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('T', Material.REDSTONE_TORCH);
        recipe.setIngredient('C', Material.COMPASS);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getMasterCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + COMPASS_TAG);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "우클릭시 가장 가까운 상대를 추적합니다.",
                ChatColor.DARK_GRAY + "상대가 없으면 정면 방향으로 입자가 퍼집니다.",
                ChatColor.RED + "한 번 사용 후 소멸됩니다."
            ));
            // 인챈트 효과 (받는 효과)
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getCompassForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getCompassCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getMasterCompass();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(meta.getLore() != null ? meta.getLore() : new ArrayList<>());
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final String MASTER_COMPASS_RECIPE_TITLE = ChatColor.GOLD + "\uc870\ud569\ubc95: " + ChatColor.YELLOW + "\ub9c8\uc2a4\ud130\uc758 \ub098\uce68\ubc18";

    public static void openMasterCompassRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, MASTER_COMPASS_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        // \uc870\ud569\ubc95 \uc2ac\ub86f (11~13 / 20~22 / 29~31)
        // RTR
        gui.setItem(11, new ItemStack(Material.REDSTONE));
        gui.setItem(12, new ItemStack(Material.REDSTONE_TORCH));
        gui.setItem(13, new ItemStack(Material.REDSTONE));
        // RCR
        gui.setItem(20, new ItemStack(Material.REDSTONE));
        gui.setItem(21, new ItemStack(Material.COMPASS));
        gui.setItem(22, new ItemStack(Material.REDSTONE));
        // RRR
        gui.setItem(29, new ItemStack(Material.REDSTONE));
        gui.setItem(30, new ItemStack(Material.REDSTONE));
        gui.setItem(31, new ItemStack(Material.REDSTONE));

        // 화살표 & 결과물
        gui.setItem(23, makeArrow());
        gui.setItem(24, getCompassForDisplay(p.getUniqueId()));

        // \ub4a4\ub85c\uac00\uae30
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  날카로움의 책
    // ================================================================

    public static final String SHARPNESS_BOOK_TAG = "날카로움의 책";
    public static final String SHARPNESS_BOOK_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.YELLOW + SHARPNESS_BOOK_TAG;

    private static void registerSharpnessBook(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "sharpness_book");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getSharpnessBook());
        // F
        //  PP
        //  PI
        recipe.shape(
            "F  ",
            " PP",
            " PI"
        );
        recipe.setIngredient('F', Material.FLINT);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('I', Material.IRON_SWORD);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getSharpnessBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + SHARPNESS_BOOK_TAG);
            meta.addStoredEnchant(Enchantment.SHARPNESS, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getSharpnessBookForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getSharpnessBookCrafts(viewer);
        String limitText = (crafts >= 4) ? ChatColor.RED + "" + crafts + "/4 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/4 (제작 가능)";
        ItemStack item = getSharpnessBook();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openSharpnessBookRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, SHARPNESS_BOOK_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, new ItemStack(Material.FLINT));
        gui.setItem(12, air);
        gui.setItem(13, air);
        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.PAPER));
        gui.setItem(22, new ItemStack(Material.PAPER));
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.PAPER));
        gui.setItem(31, new ItemStack(Material.IRON_SWORD));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getSharpnessBookForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  힘의 책
    // ================================================================

    public static final String POWER_BOOK_TAG = "힘의 책";
    public static final String POWER_BOOK_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.YELLOW + POWER_BOOK_TAG;

    private static void registerPowerBook(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "power_book");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getPowerBook());
        // F
        //  PP
        //  PB
        recipe.shape(
            "F  ",
            " PP",
            " PB"
        );
        recipe.setIngredient('F', Material.FLINT);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('B', Material.BONE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getPowerBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + POWER_BOOK_TAG);
            meta.addStoredEnchant(Enchantment.POWER, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPowerBookForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getPowerBookCrafts(viewer);
        String limitText = (crafts >= 4) ? ChatColor.RED + "" + crafts + "/4 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/4 (제작 가능)";
        ItemStack item = getPowerBook();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openPowerBookRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, POWER_BOOK_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, new ItemStack(Material.FLINT));
        gui.setItem(12, air);
        gui.setItem(13, air);
        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.PAPER));
        gui.setItem(22, new ItemStack(Material.PAPER));
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.PAPER));
        gui.setItem(31, new ItemStack(Material.BONE));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getPowerBookForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  용의 검 (Dragon Sword)
    // ================================================================

    public static final String DRAGON_SWORD_TAG = "용의 검";
    public static final String DRAGON_SWORD_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.LIGHT_PURPLE + DRAGON_SWORD_TAG;

    private static void registerDragonSword(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "dragon_sword");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getDragonSword());
        // 공백 / 블레이즈 가루 / 공백
        // 공백 / 다이아 검 / 공백
        // 흑요석 / 블레이즈 가루 / 흑요석
        recipe.shape(
            " B ",
            " D ",
            "OBO"
        );
        recipe.setIngredient('B', Material.BLAZE_POWDER);
        recipe.setIngredient('D', Material.DIAMOND_SWORD);
        recipe.setIngredient('O', Material.OBSIDIAN);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getDragonSword() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + DRAGON_SWORD_TAG);
            
            // Base Damage 8, Attack Speed 1.6 (which is -2.4 from base 4.0)
            org.bukkit.attribute.AttributeModifier damageModifier = new org.bukkit.attribute.AttributeModifier(
                UUID.randomUUID(), "generic.attack_damage", 8.0, 
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, 
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.ATTACK_DAMAGE, damageModifier);
            
            org.bukkit.attribute.AttributeModifier speedModifier = new org.bukkit.attribute.AttributeModifier(
                UUID.randomUUID(), "generic.attack_speed", -2.4, 
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, 
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.ATTACK_SPEED, speedModifier);

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getDragonSwordForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getDragonSwordCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getDragonSword();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openDragonSwordRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, DRAGON_SWORD_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.BLAZE_POWDER));
        gui.setItem(13, air);
        
        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.DIAMOND_SWORD));
        gui.setItem(22, air);
        
        gui.setItem(29, new ItemStack(Material.OBSIDIAN));
        gui.setItem(30, new ItemStack(Material.BLAZE_POWDER));
        gui.setItem(31, new ItemStack(Material.OBSIDIAN));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getDragonSwordForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  가죽 절약 (Leather Saving)
    // ================================================================

    public static final String LEATHER_SAVING_TAG = "가죽 절약";
    public static final String LEATHER_SAVING_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.GREEN + LEATHER_SAVING_TAG;

    private static void registerLeatherSaving(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "leather_saving");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getLeatherSaving());
        // 막대기 1,3,4,6,7,9 / 가죽 2,5,8
        recipe.shape(
            "SLS",
            "SLS",
            "SLS"
        );
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('L', Material.LEATHER);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getLeatherSaving() {
        return new ItemStack(Material.LEATHER, 6);
    }

    public static ItemStack getLeatherSavingForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getLeatherSavingCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = new ItemStack(Material.LEATHER, 6);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + LEATHER_SAVING_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "가죽 방어구를 만들 때 재료를 아낄 수 있습니다.");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openLeatherSavingRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, LEATHER_SAVING_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, new ItemStack(Material.STICK));
        gui.setItem(12, new ItemStack(Material.LEATHER));
        gui.setItem(13, new ItemStack(Material.STICK));

        gui.setItem(20, new ItemStack(Material.STICK));
        gui.setItem(21, new ItemStack(Material.LEATHER));
        gui.setItem(22, new ItemStack(Material.STICK));

        gui.setItem(29, new ItemStack(Material.STICK));
        gui.setItem(30, new ItemStack(Material.LEATHER));
        gui.setItem(31, new ItemStack(Material.STICK));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getLeatherSavingForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  보호의 책 (Protection Book)
    // ================================================================

    public static final String PROTECTION_BOOK_TAG = "보호의 책";
    public static final String PROTECTION_BOOK_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + PROTECTION_BOOK_TAG;

    private static void registerProtectionBook(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "protection_book");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getProtectionBook());
        // 네모 모양 2x2: 종이 1,2,4 / 철 6 (3x3 기준)
        // [P][P]
        // [P][I]
        recipe.shape(
            "PP",
            "PI"
        );
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('I', Material.IRON_INGOT);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getProtectionBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + PROTECTION_BOOK_TAG);
            meta.addStoredEnchant(Enchantment.PROTECTION, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getProtectionBookForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getProtectionBookCrafts(viewer);
        String limitText = (crafts >= 4) ? ChatColor.RED + "" + crafts + "/4 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/4 (제작 가능)";
        ItemStack item = getProtectionBook();
        org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openProtectionBookRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, PROTECTION_BOOK_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        // 네모 모양 2x2: [P][P] / [P][I]
        gui.setItem(11, new ItemStack(Material.PAPER));
        gui.setItem(12, new ItemStack(Material.PAPER));
        gui.setItem(13, air);

        gui.setItem(20, new ItemStack(Material.PAPER));
        gui.setItem(21, new ItemStack(Material.IRON_INGOT));
        gui.setItem(22, air);

        gui.setItem(29, air);
        gui.setItem(30, air);
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getProtectionBookForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  용의 갑옷 (Dragon Armor)
    // ================================================================

    public static final String DRAGON_ARMOR_TAG = "용의 갑옷";
    public static final String DRAGON_ARMOR_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.DARK_PURPLE + DRAGON_ARMOR_TAG;

    private static void registerDragonArmor(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "dragon_armor");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getDragonArmor());
        // 마그마크림 2 / 다이아흉갑 5 / 흑요석 7,9 / 모루 8
        recipe.shape(
            " M ",
            " C ",
            "OAO"
        );
        recipe.setIngredient('M', Material.MAGMA_CREAM);
        recipe.setIngredient('C', Material.DIAMOND_CHESTPLATE);
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('A', Material.ANVIL);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getDragonArmor() {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + DRAGON_ARMOR_TAG);
            item.setItemMeta(meta);
        }
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        return item;
    }

    public static ItemStack getDragonArmorForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getDragonArmorCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getDragonArmor();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openDragonArmorRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, DRAGON_ARMOR_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.MAGMA_CREAM));
        gui.setItem(13, air);

        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.DIAMOND_CHESTPLATE));
        gui.setItem(22, air);

        gui.setItem(29, new ItemStack(Material.OBSIDIAN));
        gui.setItem(30, new ItemStack(Material.ANVIL));
        gui.setItem(31, new ItemStack(Material.OBSIDIAN));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getDragonArmorForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  넥타르 (Nectar)
    // ================================================================

    public static final String NECTAR_TAG = "넥타르";
    public static final String NECTAR_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.YELLOW + NECTAR_TAG;

    private static void registerNectar(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "nectar");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getNectar());
        // 2: 에메랄드, 4/6: 금, 5: 수박조각, 8: 유리병
        recipe.shape(
            " E ",
            "GMG",
            " B "
        );
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('M', Material.MELON_SLICE);
        recipe.setIngredient('B', Material.GLASS_BOTTLE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getNectar() {
        ItemStack item = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + NECTAR_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "마시면 재생 II 효과를 10초간 얻습니다.");
            meta.setLore(lore);
            meta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 200, 1), true);
            meta.setColor(org.bukkit.Color.YELLOW);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getNectarForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getNectarCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getNectar();
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openNectarRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, NECTAR_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.EMERALD));
        gui.setItem(13, air);

        gui.setItem(20, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(21, new ItemStack(Material.MELON_SLICE));
        gui.setItem(22, new ItemStack(Material.GOLD_INGOT));

        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.GLASS_BOTTLE));
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getNectarForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  불타는 아티팩트 (Burning Artifact)
    // ================================================================

    public static final String BURNING_ARTIFACT_TAG = "불타는 아티팩트";
    public static final String BURNING_ARTIFACT_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.RED + BURNING_ARTIFACT_TAG;

    private static void registerBurningArtifact(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "burning_artifact");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getBurningArtifact());
        // 1,3,4,6,7,9: 주황색 색유리, 2,8: 용암 양동이, 5: 폭죽 로켓
        recipe.shape(
            "OLO",
            "OFO",
            "OLO"
        );
        recipe.setIngredient('O', Material.ORANGE_STAINED_GLASS);
        recipe.setIngredient('L', Material.LAVA_BUCKET);
        recipe.setIngredient('F', Material.FIREWORK_ROCKET);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getBurningArtifact() {
        return new ItemStack(Material.BLAZE_ROD, 1);
    }

    public static ItemStack getBurningArtifactForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getBurningArtifactCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "" + crafts + "/1 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/1 (제작 가능)";
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + BURNING_ARTIFACT_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "조합 후 블레이즈 막대기 1개를 얻습니다.");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openBurningArtifactRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, BURNING_ARTIFACT_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        gui.setItem(11, new ItemStack(Material.ORANGE_STAINED_GLASS));
        gui.setItem(12, new ItemStack(Material.LAVA_BUCKET));
        gui.setItem(13, new ItemStack(Material.ORANGE_STAINED_GLASS));

        gui.setItem(20, new ItemStack(Material.ORANGE_STAINED_GLASS));
        gui.setItem(21, new ItemStack(Material.FIREWORK_ROCKET));
        gui.setItem(22, new ItemStack(Material.ORANGE_STAINED_GLASS));

        gui.setItem(29, new ItemStack(Material.ORANGE_STAINED_GLASS));
        gui.setItem(30, new ItemStack(Material.LAVA_BUCKET));
        gui.setItem(31, new ItemStack(Material.ORANGE_STAINED_GLASS));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getBurningArtifactForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  맛있는 식사 (Delicious Meal)
    // ================================================================

    public static final String DELICIOUS_MEAL_TAG = "맛있는 식사";
    public static final String DELICIOUS_MEAL_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + DELICIOUS_MEAL_TAG;

    private static void registerDeliciousMeal(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "delicious_meal");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getDeliciousMeal());
        // 1,2,3,4,6,7,8,9: 익히지 않은 고기, 5: 석탄
        recipe.shape(
            "MMM",
            "MCM",
            "MMM"
        );
        recipe.setIngredient('M', new org.bukkit.inventory.RecipeChoice.MaterialChoice(Material.PORKCHOP, Material.BEEF, Material.MUTTON));
        recipe.setIngredient('C', Material.COAL);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getDeliciousMeal() {
        return new ItemStack(Material.COOKED_BEEF, 10);
    }

    public static ItemStack getDeliciousMealForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getDeliciousMealCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = new ItemStack(Material.COOKED_BEEF, 10);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + DELICIOUS_MEAL_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "조합 후 스테이크 10개를 얻습니다.");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openDeliciousMealRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, DELICIOUS_MEAL_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack meatInfo = new ItemStack(Material.BEEF);
        ItemMeta meta = meatInfo.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "생고기 (소, 돼지, 양 중 1개)");
            meatInfo.setItemMeta(meta);
        }

        gui.setItem(11, meatInfo);
        gui.setItem(12, meatInfo);
        gui.setItem(13, meatInfo);

        gui.setItem(20, meatInfo);
        gui.setItem(21, new ItemStack(Material.COAL));
        gui.setItem(22, meatInfo);

        gui.setItem(29, meatInfo);
        gui.setItem(30, meatInfo);
        gui.setItem(31, meatInfo);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getDeliciousMealForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  강인함의 포션 (Toughness Potion)
    // ================================================================

    public static final String TOUGHNESS_POTION_TAG = "강인함의 포션";
    public static final String TOUGHNESS_POTION_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.LIGHT_PURPLE + TOUGHNESS_POTION_TAG;

    private static void registerToughnessPotion(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "toughness_potion");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getToughnessPotion());
        // 2: 슬라임볼, 5: 눈블록, 8: 유리병
        recipe.shape(
            " S ",
            " N ",
            " B "
        );
        recipe.setIngredient('S', Material.SLIME_BALL);
        recipe.setIngredient('N', Material.SNOW_BLOCK);
        recipe.setIngredient('B', Material.GLASS_BOTTLE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getToughnessPotion() {
        ItemStack item = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + TOUGHNESS_POTION_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "마시면 저항 II 효과를 2분간 얻습니다.");
            meta.setLore(lore);
            meta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 2400, 1), true);
            meta.setColor(org.bukkit.Color.PURPLE);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getToughnessPotionForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getToughnessPotionCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getToughnessPotion();
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openToughnessPotionRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, TOUGHNESS_POTION_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);

        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.SLIME_BALL));
        gui.setItem(13, air);

        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.SNOW_BLOCK));
        gui.setItem(22, air);

        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.GLASS_BOTTLE));
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getToughnessPotionForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  공통 유틸리티
    // ================================================================

    public static ItemStack makeBg() {
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = bg.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            try { m.setHideTooltip(true); } catch (Exception ignored) {}
            bg.setItemMeta(m);
        }
        return bg;
    }

    private static ItemStack makeArrow() {
        ItemStack arrow = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta m = arrow.getItemMeta();
        if (m != null) { m.setDisplayName(ChatColor.GREEN + "→"); arrow.setItemMeta(m); }
        return arrow;
    }

    private static ItemStack makeBack() {
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta m = back.getItemMeta();
        if (m != null) { m.setDisplayName(ChatColor.RED + "뒤로 가기"); back.setItemMeta(m); }
        return back;
    }

    // ================================================================
    //  세븐 리그 부츠 (Seven League Boots)
    // ================================================================

    public static final String SEVEN_LEAGUE_BOOTS_TAG = "세븐 리그 부츠";
    public static final String SEVEN_LEAGUE_BOOTS_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + SEVEN_LEAGUE_BOOTS_TAG;

    private static void registerSevenLeagueBoots(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "seven_league_boots");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getSevenLeagueBoots());
        // 1,3,4,6,7,9: 깃털, 2: 엔더진주, 5: 다이아부츠, 8: 물양동이
        recipe.shape(
            "FEF",
            "FDF",
            "FWF"
        );
        recipe.setIngredient('F', Material.FEATHER);
        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('D', Material.DIAMOND_BOOTS);
        recipe.setIngredient('W', Material.WATER_BUCKET);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getSevenLeagueBoots() {
        ItemStack item = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + SEVEN_LEAGUE_BOOTS_TAG);
            item.setItemMeta(meta);
        }
        item.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 3);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 3);
        return item;
    }

    public static ItemStack getSevenLeagueBootsForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getSevenLeagueBootsCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "1/1 (제작 완료)" : ChatColor.GREEN + "0/1 (제작 가능)";
        ItemStack item = getSevenLeagueBoots();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "다이아부츠 + 가벼운 착지 III + 보호 III");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openSevenLeagueBootsRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, SEVEN_LEAGUE_BOOTS_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        gui.setItem(11, new ItemStack(Material.FEATHER));
        gui.setItem(12, new ItemStack(Material.ENDER_PEARL));
        gui.setItem(13, new ItemStack(Material.FEATHER));

        gui.setItem(20, new ItemStack(Material.FEATHER));
        gui.setItem(21, new ItemStack(Material.DIAMOND_BOOTS));
        gui.setItem(22, new ItemStack(Material.FEATHER));

        gui.setItem(29, new ItemStack(Material.FEATHER));
        gui.setItem(30, new ItemStack(Material.WATER_BUCKET));
        gui.setItem(31, new ItemStack(Material.FEATHER));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getSevenLeagueBootsForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  아이언 팩 (Iron Pack)
    // ================================================================

    public static final String IRON_PACK_TAG = "아이언 팩";
    public static final String IRON_PACK_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.WHITE + IRON_PACK_TAG;

    private static void registerIronPack(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "iron_pack");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getIronPack());
        // 1,2,3,4,6,7,8,9: 철 원석, 5: 석탄
        recipe.shape(
            "RRR",
            "RCR",
            "RRR"
        );
        recipe.setIngredient('R', Material.RAW_IRON);
        recipe.setIngredient('C', Material.COAL);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getIronPack() {
        return new ItemStack(Material.IRON_INGOT, 10);
    }

    public static ItemStack getIronPackForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getIronPackCrafts(viewer);
        String limitText = (crafts >= 4) ? ChatColor.RED + "" + crafts + "/4 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/4 (제작 가능)";
        ItemStack item = new ItemStack(Material.IRON_INGOT, 10);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + IRON_PACK_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "조합 후 철 주괴 10개를 얻습니다.");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openIronPackRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, IRON_PACK_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        gui.setItem(11, new ItemStack(Material.RAW_IRON));
        gui.setItem(12, new ItemStack(Material.RAW_IRON));
        gui.setItem(13, new ItemStack(Material.RAW_IRON));

        gui.setItem(20, new ItemStack(Material.RAW_IRON));
        gui.setItem(21, new ItemStack(Material.COAL));
        gui.setItem(22, new ItemStack(Material.RAW_IRON));

        gui.setItem(29, new ItemStack(Material.RAW_IRON));
        gui.setItem(30, new ItemStack(Material.RAW_IRON));
        gui.setItem(31, new ItemStack(Material.RAW_IRON));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getIronPackForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  흑요석 만들기 (Obsidian Mix)
    // ================================================================

    public static final String OBSIDIAN_MIX_TAG = "흑요석 만들기";
    public static final String OBSIDIAN_MIX_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.DARK_PURPLE + OBSIDIAN_MIX_TAG;

    private static void registerObsidianMix(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "obsidian_mix");
        Bukkit.removeRecipe(key);
        ShapelessRecipe recipe = new ShapelessRecipe(key, getObsidianMix());
        recipe.addIngredient(Material.WATER_BUCKET);
        recipe.addIngredient(Material.LAVA_BUCKET);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getObsidianMix() {
        return new ItemStack(Material.OBSIDIAN, 1);
    }

    public static ItemStack getObsidianMixForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getObsidianMixCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = new ItemStack(Material.OBSIDIAN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + OBSIDIAN_MIX_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "조합 후 흑요석 1개를 얻습니다.");
            lore.add(ChatColor.GRAY + "물 양동이 + 용암 양동이 (위치 상관없음)");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openObsidianMixRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, OBSIDIAN_MIX_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        // 위치 상관없음(셔플리스)이라 2개만 표시
        gui.setItem(20, new ItemStack(Material.WATER_BUCKET));
        gui.setItem(22, new ItemStack(Material.LAVA_BUCKET));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getObsidianMixForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  탄헬름 (Tarnhelm)
    // ================================================================

    public static final String TARNHELM_TAG = "탄헬름";
    public static final String TARNHELM_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + TARNHELM_TAG;

    private static void registerTarnhelm(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "tarnhelm");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getTarnhelm());
        // 1,3,4,6 다이아몬드, 2 철 주괴, 5 레드스톤 블록
        recipe.shape(
            "DID",
            "DRD",
            "   "
        );
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getTarnhelm() {
        ItemStack item = new ItemStack(Material.DIAMOND_HELMET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + TARNHELM_TAG);
            meta.addEnchant(Enchantment.PROTECTION, 1, true);
            meta.addEnchant(Enchantment.FIRE_PROTECTION, 1, true);
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getTarnhelmForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getTarnhelmCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getTarnhelm();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openTarnhelmRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, TARNHELM_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);
        gui.setItem(11, new ItemStack(Material.DIAMOND));
        gui.setItem(12, new ItemStack(Material.IRON_INGOT));
        gui.setItem(13, new ItemStack(Material.DIAMOND));
        gui.setItem(20, new ItemStack(Material.DIAMOND));
        gui.setItem(21, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(22, new ItemStack(Material.DIAMOND));
        gui.setItem(29, air);
        gui.setItem(30, air);
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getTarnhelmForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  철학자의 곡괭이 (Philosopher's Pickaxe)
    // ================================================================

    public static final String PHILOSOPHER_PICKAXE_TAG = "철학자의 곡괭이";
    public static final String PHILOSOPHER_PICKAXE_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.YELLOW + PHILOSOPHER_PICKAXE_TAG;

    private static void registerPhilosopherPickaxe(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "philosopher_pickaxe");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getPhilosopherPickaxe());
        // 1,3 철 원석 / 2 금 원석 / 4,6 청금석 블록 / 5,8 막대기
        recipe.shape(
            "IGI",
            "LSL",
            " S "
        );
        recipe.setIngredient('I', Material.RAW_IRON);
        recipe.setIngredient('G', Material.RAW_GOLD);
        recipe.setIngredient('L', Material.LAPIS_BLOCK);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getPhilosopherPickaxe() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + PHILOSOPHER_PICKAXE_TAG);
            meta.addEnchant(Enchantment.FORTUNE, 2, true);
            if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                // max durability of diamond pickaxe is 1561. Remaining is 2. So damage is 1559.
                ((org.bukkit.inventory.meta.Damageable) meta).setDamage(1559);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getPhilosopherPickaxeForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getPhilosopherPickaxeCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "" + crafts + "/1 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/1 (제작 가능)";
        ItemStack item = getPhilosopherPickaxe();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "내구도가 2밖에 남지 않았습니다.");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openPhilosopherPickaxeRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, PHILOSOPHER_PICKAXE_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);
        gui.setItem(11, new ItemStack(Material.RAW_IRON));
        gui.setItem(12, new ItemStack(Material.RAW_GOLD));
        gui.setItem(13, new ItemStack(Material.RAW_IRON));
        gui.setItem(20, new ItemStack(Material.LAPIS_BLOCK));
        gui.setItem(21, new ItemStack(Material.STICK));
        gui.setItem(22, new ItemStack(Material.LAPIS_BLOCK));
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.STICK));
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getPhilosopherPickaxeForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  깨우침의 팩 (Awakening Pack)
    // ================================================================

    public static final String AWAKENING_PACK_TAG = "깨우침의 팩";
    public static final String AWAKENING_PACK_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.LIGHT_PURPLE + AWAKENING_PACK_TAG;

    private static void registerAwakeningPack(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "awakening_pack");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getAwakeningPack());
        // 2,4,6,8 레드스톤 블록 / 5 유리병
        recipe.shape(
            " R ",
            "RBR",
            " R "
        );
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
        recipe.setIngredient('B', Material.GLASS_BOTTLE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getAwakeningPack() {
        return new ItemStack(Material.EXPERIENCE_BOTTLE, 8);
    }

    public static ItemStack getAwakeningPackForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getAwakeningPackCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getAwakeningPack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + AWAKENING_PACK_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "조합 후 경험치병 8개를 얻습니다.");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openAwakeningPackRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, AWAKENING_PACK_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);
        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(13, air);
        gui.setItem(20, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(21, new ItemStack(Material.GLASS_BOTTLE));
        gui.setItem(22, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.REDSTONE_BLOCK));
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getAwakeningPackForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  가벼운 모루 (Light Anvil)
    // ================================================================

    public static final String LIGHT_ANVIL_TAG = "가벼운 모루";
    public static final String LIGHT_ANVIL_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.WHITE + LIGHT_ANVIL_TAG;

    private static void registerLightAnvil(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "light_anvil");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getLightAnvil());
        // 1,2,3,7,8,9 철 주괴 / 5 철 블록
        recipe.shape(
            "III",
            " B ",
            "III"
        );
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('B', Material.IRON_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getLightAnvil() {
        return new ItemStack(Material.ANVIL, 1);
    }

    public static ItemStack getLightAnvilForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getLightAnvilCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getLightAnvil();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + LIGHT_ANVIL_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openLightAnvilRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, LIGHT_ANVIL_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);
        gui.setItem(11, new ItemStack(Material.IRON_INGOT));
        gui.setItem(12, new ItemStack(Material.IRON_INGOT));
        gui.setItem(13, new ItemStack(Material.IRON_INGOT));
        gui.setItem(20, air);
        gui.setItem(21, new ItemStack(Material.IRON_BLOCK));
        gui.setItem(22, air);
        gui.setItem(29, new ItemStack(Material.IRON_INGOT));
        gui.setItem(30, new ItemStack(Material.IRON_INGOT));
        gui.setItem(31, new ItemStack(Material.IRON_INGOT));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getLightAnvilForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  가벼운 마법 부여대 (Light Enchanting Table)
    // ================================================================

    public static final String LIGHT_ENCHANTING_TABLE_TAG = "가벼운 마법 부여대";
    public static final String LIGHT_ENCHANTING_TABLE_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.LIGHT_PURPLE + LIGHT_ENCHANTING_TABLE_TAG;

    private static void registerLightEnchantingTable(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "light_enchanting_table");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getLightEnchantingTable());
        // 2 책장 / 4,6 흑요석 / 5 다이아몬드 / 7,9 흑요석 / 8 경험치병
        recipe.shape(
            " B ",
            "ODO",
            "OEO"
        );
        recipe.setIngredient('B', Material.BOOKSHELF);
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('E', Material.EXPERIENCE_BOTTLE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getLightEnchantingTable() {
        return new ItemStack(Material.ENCHANTING_TABLE, 1);
    }

    public static ItemStack getLightEnchantingTableForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getLightEnchantingTableCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getLightEnchantingTable();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + LIGHT_ENCHANTING_TABLE_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openLightEnchantingTableRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, LIGHT_ENCHANTING_TABLE_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);
        // 1번칸(idx11)=공백, 2번칸(idx12)=책장, 3번칸(idx13)=공백
        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.BOOKSHELF));
        gui.setItem(13, air);
        // 4번칸(idx20)=흑요석, 5번칸(idx21)=다이아몬드, 6번칸(idx22)=흑요석
        gui.setItem(20, new ItemStack(Material.OBSIDIAN));
        gui.setItem(21, new ItemStack(Material.DIAMOND));
        gui.setItem(22, new ItemStack(Material.OBSIDIAN));
        // 7번칸(idx29)=흑요석, 8번칸(idx30)=경험치병, 9번칸(idx31)=흑요석
        gui.setItem(29, new ItemStack(Material.OBSIDIAN));
        gui.setItem(30, new ItemStack(Material.EXPERIENCE_BOTTLE));
        gui.setItem(31, new ItemStack(Material.OBSIDIAN));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getLightEnchantingTableForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  이브의 유혹 (Eve's Temptation)
    // ================================================================

    public static final String EVES_TEMPTATION_TAG = "이브의 유혹";
    public static final String EVES_TEMPTATION_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.RED + EVES_TEMPTATION_TAG;

    private static void registerEvesTemptation(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "eves_temptation");
        Bukkit.removeRecipe(key);
        ShapelessRecipe recipe = new ShapelessRecipe(key, getEvesTemptation());
        recipe.addIngredient(Material.APPLE);
        recipe.addIngredient(Material.BONE_MEAL);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getEvesTemptation() {
        return new ItemStack(Material.APPLE, 2);
    }

    public static ItemStack getEvesTemptationForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getEvesTemptationCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getEvesTemptation();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + EVES_TEMPTATION_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openEvesTemptationRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, EVES_TEMPTATION_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        gui.setItem(20, new ItemStack(Material.APPLE));
        gui.setItem(22, new ItemStack(Material.BONE_MEAL));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getEvesTemptationForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  회복의 과일 (Fruit of Recovery)
    // ================================================================

    public static final String FRUIT_OF_RECOVERY_TAG = "회복의 과일";
    public static final String FRUIT_OF_RECOVERY_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.AQUA + FRUIT_OF_RECOVERY_TAG;

    private static void registerFruitOfRecovery(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "fruit_of_recovery");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getFruitOfRecovery());
        // 1,3,7,9 뼛가루 / 2,4,6,8 씨앗 / 5 사과
        recipe.shape(
            "BSB",
            "SAS",
            "BSB"
        );
        recipe.setIngredient('B', Material.BONE_MEAL);
        recipe.setIngredient('S', Material.WHEAT_SEEDS);
        recipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getFruitOfRecovery() {
        return new ItemStack(Material.MELON_SLICE, 1);
    }

    public static ItemStack getFruitOfRecoveryForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getFruitOfRecoveryCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getFruitOfRecovery();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + FRUIT_OF_RECOVERY_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openFruitOfRecoveryRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, FRUIT_OF_RECOVERY_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        gui.setItem(11, new ItemStack(Material.BONE_MEAL));
        gui.setItem(12, new ItemStack(Material.WHEAT_SEEDS));
        gui.setItem(13, new ItemStack(Material.BONE_MEAL));
        gui.setItem(20, new ItemStack(Material.WHEAT_SEEDS));
        gui.setItem(21, new ItemStack(Material.APPLE));
        gui.setItem(22, new ItemStack(Material.WHEAT_SEEDS));
        gui.setItem(29, new ItemStack(Material.BONE_MEAL));
        gui.setItem(30, new ItemStack(Material.WHEAT_SEEDS));
        gui.setItem(31, new ItemStack(Material.BONE_MEAL));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getFruitOfRecoveryForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  가벼운 사과 (Light Apple)
    // ================================================================

    public static final String LIGHT_APPLE_TAG = "가벼운 사과";
    public static final String LIGHT_APPLE_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.YELLOW + LIGHT_APPLE_TAG;

    private static void registerLightApple(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "light_apple");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getLightApple());
        // 2,4,6,8 금 / 5 사과
        recipe.shape(
            " G ",
            "GAG",
            " G "
        );
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getLightApple() {
        return new ItemStack(Material.GOLDEN_APPLE, 1);
    }

    public static ItemStack getLightAppleForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getLightAppleCrafts(viewer);
        String limitText = (crafts >= 1) ? ChatColor.RED + "" + crafts + "/1 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/1 (제작 가능)";
        ItemStack item = getLightApple();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + LIGHT_APPLE_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openLightAppleRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, LIGHT_APPLE_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        ItemStack air = new ItemStack(Material.AIR);
        gui.setItem(11, air);
        gui.setItem(12, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(13, air);
        gui.setItem(20, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(21, new ItemStack(Material.APPLE));
        gui.setItem(22, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(29, air);
        gui.setItem(30, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(31, air);

        gui.setItem(23, makeArrow());
        gui.setItem(24, getLightAppleForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  황금 머리 (Golden Head)
    // ================================================================

    public static final String GOLDEN_HEAD_TAG = "황금 머리";
    public static final String GOLDEN_HEAD_RECIPE_TITLE = ChatColor.GOLD + "조합법: " + ChatColor.GOLD + GOLDEN_HEAD_TAG;

    private static void registerGoldenHead(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, getGoldenHead());
        // 1,2,3,4,6,7,8,9 금 주괴 / 5 플레이어 머리
        recipe.shape(
            "GGG",
            "GHG",
            "GGG"
        );
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getGoldenHead() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + GOLDEN_HEAD_TAG);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "우클릭 시 즉시 발동:");
            lore.add(ChatColor.AQUA + "흡수 I (2:00)");
            lore.add(ChatColor.AQUA + "재생 II (0:05)");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getGoldenHeadForDisplay(UUID viewer) {
        int crafts = viewer == null ? 0 : getGoldenHeadCrafts(viewer);
        String limitText = (crafts >= 3) ? ChatColor.RED + "" + crafts + "/3 (제한 도달)" : ChatColor.GREEN + "" + crafts + "/3 (제작 가능)";
        ItemStack item = getGoldenHead();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openGoldenHeadRecipeGUI(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, GOLDEN_HEAD_RECIPE_TITLE);
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        gui.setItem(11, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(12, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(13, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(20, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(21, new ItemStack(Material.PLAYER_HEAD));
        gui.setItem(22, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(29, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(30, new ItemStack(Material.GOLD_INGOT));
        gui.setItem(31, new ItemStack(Material.GOLD_INGOT));

        gui.setItem(23, makeArrow());
        gui.setItem(24, getGoldenHeadForDisplay(p.getUniqueId()));
        gui.setItem(49, makeBack());

        p.openInventory(gui);
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}

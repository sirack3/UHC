package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
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

    public static int getApprenticeCrafts(UUID uuid) { return apprenticeCrafts.getOrDefault(uuid, 0); }
    public static void addApprenticeCraft(UUID uuid)  { apprenticeCrafts.put(uuid, getApprenticeCrafts(uuid) + 1); }

    public static int getSwordCrafts(UUID uuid)       { return swordCrafts.getOrDefault(uuid, 0); }
    public static void addSwordCraft(UUID uuid)        { swordCrafts.put(uuid, getSwordCrafts(uuid) + 1); }

    public static int getBowCrafts(UUID uuid)         { return bowCrafts.getOrDefault(uuid, 0); }
    public static void addBowCraft(UUID uuid)         { bowCrafts.put(uuid, getBowCrafts(uuid) + 1); }

    public static int getCompassCrafts(UUID uuid)     { return compassCrafts.getOrDefault(uuid, 0); }
    public static void addCompassCraft(UUID uuid)     { compassCrafts.put(uuid, getCompassCrafts(uuid) + 1); }

    public static int getSharpnessBookCrafts(UUID uuid) { return sharpnessBookCrafts.getOrDefault(uuid, 0); }
    public static void addSharpnessBookCraft(UUID uuid) { sharpnessBookCrafts.put(uuid, getSharpnessBookCrafts(uuid) + 1); }

    public static int getPowerBookCrafts(UUID uuid)     { return powerBookCrafts.getOrDefault(uuid, 0); }
    public static void addPowerBookCraft(UUID uuid)     { powerBookCrafts.put(uuid, getPowerBookCrafts(uuid) + 1); }

    public static void resetCrafts() {
        apprenticeCrafts.clear();
        swordCrafts.clear();
        bowCrafts.clear();
        compassCrafts.clear();
        sharpnessBookCrafts.clear();
        powerBookCrafts.clear();
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
}

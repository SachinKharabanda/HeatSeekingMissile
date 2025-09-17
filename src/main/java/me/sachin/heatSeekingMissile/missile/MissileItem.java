package me.sachin.heatSeekingMissile.missile;

import me.sachin.heatSeekingMissile.ConfigManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MissileItem {

    private final JavaPlugin plugin;
    @SuppressWarnings("unused")
    private final ConfigManager configs; // kept in case you want messages later

    // Config snapshot
    private Material material;
    private String displayName;
    private List<String> lore;
    private String subtitleTemplate = "";
    private Integer customModelData; // nullable -> only set if present
    private boolean glow;
    private boolean unbreakable;
    private List<ItemFlag> flags;
    private int defaultAmount;
    private int maxAmount = 1;
    private NamespacedKey pdcKey;
    private NamespacedKey ammoKey;
    private NamespacedKey reloadingKey; // (optional, used only for display)


    public MissileItem(JavaPlugin plugin, ConfigManager configs) {
        this.plugin = plugin;
        this.configs = configs;
        reload(); // read initial values from config.yml
    }

    /** Re-read missile item settings from config.yml (call this after /reload). */
    public final void reload() {
        var cfg = plugin.getConfig();
        final String base = "Missile-settings.";

        // --- material ---
        String matStr = cfg.getString(base + "Item", "FIREWORK_ROCKET");
        Material m = Material.matchMaterial(matStr);
        this.material = (m != null) ? m : Material.FIREWORK_ROCKET;

        // --- display name & lore ---
        this.displayName = cfg.getString(base + "Display-name", "&cHeat Seeking Missile");
        this.lore = new ArrayList<>(cfg.getStringList(base + "Lore"));

        // --- quantities ---
        this.maxAmount = Math.max(1, cfg.getInt(base + "Maximum-amount", 1));
        int defAmt = cfg.getInt(base + "Default-amount", 1);
        this.defaultAmount = Math.max(1, defAmt);

        // --- extras (all optional) ---
        int cmd = cfg.getInt(base + "Custom-model-data", -1);
        this.customModelData = cmd > 0 ? cmd : null;

        this.glow = cfg.getBoolean(base + "Glow", false);
        this.unbreakable = cfg.getBoolean(base + "Unbreakable", true);

        this.flags = new ArrayList<>();
        for (String f : cfg.getStringList(base + "Flags")) {
            try { this.flags.add(ItemFlag.valueOf(f.toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }

        // PDC tag (used to identify the item)
        String key = cfg.getString(base + "PDC-key", "hsm_missile");
        this.pdcKey = new NamespacedKey(plugin, key);

        this.ammoKey = new NamespacedKey(plugin, "hsm_ammo");
        this.reloadingKey = new NamespacedKey(plugin, "hsm_reloading");

        // Optional subtitle template (if you use it elsewhere)
        this.subtitleTemplate = cfg.getString(base + "Subtitle-display", "");
    }



    /** Build a missile ItemStack with the default amount from config. */
    public ItemStack buildItem() {
        return buildItem(this.defaultAmount);
    }

    /** Build a missile ItemStack with a specific amount. */
    public ItemStack buildItem(int amount) {
        int amt = Math.min(Math.max(1, amount), Math.max(1, maxAmount));
        ItemStack stack = new ItemStack(material, amt);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) meta.setDisplayName(color(displayName));
            if (!lore.isEmpty()) {
                List<String> colored = new ArrayList<>(lore.size());
                for (String line : lore) colored.add(color(line));
                meta.setLore(colored);
            }
            if (customModelData != null) meta.setCustomModelData(customModelData);
            meta.setUnbreakable(unbreakable);
            for (ItemFlag f : flags) meta.addItemFlags(f);

            // PDC tag
            meta.getPersistentDataContainer().set(pdcKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);

            if (glow) {
                stack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                ItemMeta m2 = stack.getItemMeta();
                if (m2 != null) { m2.addItemFlags(ItemFlag.HIDE_ENCHANTS); stack.setItemMeta(m2); }
            }
        }

        // inside buildItem(int amount) after you fetched meta:
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(pdcKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(ammoKey, PersistentDataType.INTEGER, 1);        // capacity 1
        pdc.set(reloadingKey, PersistentDataType.BYTE, (byte) 0);

        return stack;
    }


    /** Give the missile item to a player (uses default amount). */
    public void give(Player player) {
        give(player, this.defaultAmount);
    }

    /** Give the missile item with a specific amount. */
    public void give(Player player, int amount) {
        ItemStack toGive = buildItem(amount);
        var left = player.getInventory().addItem(toGive);
        // Drop extras at feet if inventory full
        if (!left.isEmpty()) {
            left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    /** Check if an ItemStack is our missile (by PDC tag). */
    public boolean isMissile(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        Byte flag = pdc.get(pdcKey, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public boolean isHolding(Player p) {
        return isMissile(p.getInventory().getItemInMainHand())
                || isMissile(p.getInventory().getItemInOffHand());
    }

    /** Shows only the subtitle (no title). Tweaks are optional. */
    public void showSubtitle(Player p) {
        if (subtitleTemplate == null || subtitleTemplate.isBlank()) return;
        String msg = color(subtitleTemplate); // supports &-colors
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    /** Convenience: only show if currently holding the missile. */
    public void showSubtitleIfHolding(Player p) {
        if (isHolding(p)) showSubtitle(p);
    }

    // ---- Ammo helpers ----
    public int getAmmo(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        Integer v = stack.getItemMeta().getPersistentDataContainer().get(ammoKey, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public void setAmmo(ItemStack stack, int v) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta m = stack.getItemMeta();
        m.getPersistentDataContainer().set(ammoKey, PersistentDataType.INTEGER, Math.max(0, Math.min(1, v)));
        stack.setItemMeta(m);
    }

    public void setReloadingFlag(ItemStack stack, boolean reloading) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta m = stack.getItemMeta();
        m.getPersistentDataContainer().set(reloadingKey, PersistentDataType.BYTE, (byte) (reloading ? 1 : 0));
        stack.setItemMeta(m);
    }

    public boolean isReloading(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        Byte v = stack.getItemMeta().getPersistentDataContainer().get(reloadingKey, PersistentDataType.BYTE);
        return v != null && v == (byte)1;
    }

    // Action-bar subtitle with %ammo% and optional ᴿ
    public void showSubtitleFor(Player p, ItemStack held) {
        if (subtitleTemplate == null || subtitleTemplate.isBlank()) return;
        boolean reloading = isReloading(held);
        String msg = subtitleTemplate.replace("%ammo%", String.valueOf(getAmmo(held)));
        if (reloading) msg = msg + " ᴿ";
        // Bungee action bar (works on Paper/Spigot)
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(color(msg)));
    }

    /** Get the maximum amount of missiles allowed in inventory from config. */
    public int getMaxAmount() {
        return maxAmount;
    }

}
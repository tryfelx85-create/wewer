package com.example.pvptournament.kit;

import com.example.pvptournament.PvPTournamentPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class KitManager {

    private final PvPTournamentPlugin plugin;
    private final File kitsFolder;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private final Map<UUID, String> selectedKit = new HashMap<>(); // player -> kit id

    public KitManager(PvPTournamentPlugin plugin) {
        this.plugin = plugin;
        this.kitsFolder = new File(plugin.getDataFolder(), "kits");
        if (!kitsFolder.exists()) kitsFolder.mkdirs();
    }

    public void loadAll() {
        kits.clear();
        File[] files = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                Kit kit = loadKit(file);
                kits.put(kit.getId(), kit);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load kit " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private Kit loadKit(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = cfg.getString("id", file.getName().replace(".yml", ""));
        Kit kit = new Kit(id);
        kit.setDisplayName(cfg.getString("display-name", id));
        kit.setPermission(cfg.getString("permission", null));
        kit.setCooldownMillis(cfg.getLong("cooldown-millis", 0));

        List<ItemStack> contents = new ArrayList<>();
        List<?> rawContents = cfg.getList("contents");
        if (rawContents != null) {
            for (Object o : rawContents) {
                contents.add(o instanceof ItemStack stack ? stack : null);
            }
        }
        kit.setContents(contents);

        kit.setHelmet((ItemStack) cfg.get("armor.helmet"));
        kit.setChestplate((ItemStack) cfg.get("armor.chestplate"));
        kit.setLeggings((ItemStack) cfg.get("armor.leggings"));
        kit.setBoots((ItemStack) cfg.get("armor.boots"));

        return kit;
    }

    public void saveKit(Kit kit) {
        File file = new File(kitsFolder, kit.getId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", kit.getId());
        cfg.set("display-name", kit.getDisplayName());
        if (kit.getPermission() != null) cfg.set("permission", kit.getPermission());
        cfg.set("cooldown-millis", kit.getCooldownMillis());
        cfg.set("contents", kit.getContents());
        cfg.set("armor.helmet", kit.getHelmet());
        cfg.set("armor.chestplate", kit.getChestplate());
        cfg.set("armor.leggings", kit.getLeggings());
        cfg.set("armor.boots", kit.getBoots());
        try {
            cfg.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save kit " + kit.getId() + ": " + ex.getMessage());
        }
    }

    public void registerKit(Kit kit) {
        kits.put(kit.getId(), kit);
        saveKit(kit);
    }

    public void deleteKit(String id) {
        kits.remove(id);
        File file = new File(kitsFolder, id + ".yml");
        if (file.exists()) file.delete();
    }

    public Kit getKit(String id) {
        return kits.get(id);
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    public void setSelectedKit(UUID playerUuid, String kitId) {
        selectedKit.put(playerUuid, kitId);
    }

    public Kit getSelectedKit(UUID playerUuid) {
        String kitId = selectedKit.get(playerUuid);
        return kitId != null ? kits.get(kitId) : null;
    }
}

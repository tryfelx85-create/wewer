package com.example.pvptournament.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;

/**
 * Thin wrapper around WorldEdit's API for pasting pre-built SkyWars island schematics.
 * Schematics live in plugins/PvPTournament/schematics/<name>.schem
 *
 * Requires WorldEdit or FastAsyncWorldEdit as a softdepend.
 */
public final class SchematicUtil {

    private SchematicUtil() {
    }

    public static File schematicsFolder(JavaPlugin plugin) {
        File folder = new File(plugin.getDataFolder(), "schematics");
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    /**
     * Pastes the given schematic file so its origin lands at targetLocation.
     * Runs synchronously — call from the main thread; for large islands consider
     * wrapping in an async task with FAWE's queue if performance becomes an issue.
     */
    public static boolean paste(JavaPlugin plugin, File schematicFile, Location targetLocation) {
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
            return false;
        }
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            plugin.getLogger().warning("Unknown schematic format: " + schematicFile.getName());
            return false;
        }

        try (FileInputStream fis = new FileInputStream(schematicFile);
             ClipboardReader reader = format.getReader(fis)) {

            Clipboard clipboard = reader.read();
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(
                    BukkitAdapter.adapt(targetLocation.getWorld()))) {

                BlockVector3 to = BukkitAdapter.asBlockVector(targetLocation);
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(to)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
            return true;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to paste schematic " + schematicFile.getName(), ex);
            return false;
        }
    }
}

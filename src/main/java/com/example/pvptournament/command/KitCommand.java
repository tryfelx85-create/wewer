package com.example.pvptournament.command;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.gui.KitSelectGui;
import com.example.pvptournament.kit.Kit;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public final class KitCommand {

    private KitCommand() {
    }

    public static List<String> aliases() {
        return List.of();
    }

    public static LiteralCommandNode<CommandSourceStack> create(PvPTournamentPlugin plugin) {
        return Commands.literal("kit")
                .requires(src -> src.getSender().hasPermission("pvpt.use"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                    KitSelectGui gui = new KitSelectGui();
                    player.openInventory(gui.build(player));
                    return 1;
                })
                .then(Commands.literal("select")
                        .then(Commands.argument("kitId", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                    String kitId = StringArgumentType.getString(ctx, "kitId");
                                    Kit kit = plugin.getKitManager().getKit(kitId);
                                    if (kit == null) {
                                        player.sendMessage("§cNo such kit.");
                                        return 0;
                                    }
                                    if (!kit.canUse(player)) {
                                        player.sendMessage("§cYou don't have permission for that kit.");
                                        return 0;
                                    }
                                    plugin.getKitManager().setSelectedKit(player.getUniqueId(), kitId);
                                    player.sendMessage("§aSelected kit: §f" + kit.getDisplayName());
                                    return 1;
                                })))
                .then(Commands.literal("create")
                        .requires(src -> src.getSender().hasPermission("pvpt.admin"))
                        .then(Commands.argument("kitId", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                    String kitId = StringArgumentType.getString(ctx, "kitId");
                                    Kit kit = new Kit(kitId);
                                    kit.captureFrom(player);
                                    plugin.getKitManager().registerKit(kit);
                                    player.sendMessage("§aCreated kit §f" + kitId + " §afrom your current inventory.");
                                    return 1;
                                })))
                .then(Commands.literal("delete")
                        .requires(src -> src.getSender().hasPermission("pvpt.admin"))
                        .then(Commands.argument("kitId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String kitId = StringArgumentType.getString(ctx, "kitId");
                                    plugin.getKitManager().deleteKit(kitId);
                                    ctx.getSource().getSender().sendMessage("§aDeleted kit §f" + kitId);
                                    return 1;
                                })))
                .build();
    }
}

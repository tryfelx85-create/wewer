package com.example.pvptournament.command;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.mode.GameModeType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class AdminCommand {

    private AdminCommand() {
    }

    public static List<String> aliases() {
        return List.of("pvpta");
    }

    public static LiteralCommandNode<CommandSourceStack> create(PvPTournamentPlugin plugin) {
        return Commands.literal("pvptournament")
                .requires(src -> src.getSender().hasPermission("pvpt.admin"))
                .then(Commands.literal("arena")
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                                    String id = StringArgumentType.getString(ctx, "id");
                                                    String modeStr = StringArgumentType.getString(ctx, "mode").toUpperCase();
                                                    GameModeType type;
                                                    try {
                                                        type = GameModeType.valueOf(modeStr);
                                                    } catch (IllegalArgumentException ex) {
                                                        player.sendMessage("§cInvalid mode.");
                                                        return 0;
                                                    }
                                                    Arena arena = new Arena(id, type);
                                                    arena.setWorld(player.getWorld());
                                                    plugin.getArenaManager().registerArena(arena);
                                                    player.sendMessage("§aCreated arena §f" + id + " §a(" + type + ")");
                                                    return 1;
                                                }))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            plugin.getArenaManager().deleteArena(id);
                                            ctx.getSource().getSender().sendMessage("§aDeleted arena §f" + id);
                                            return 1;
                                        })))
                        .then(Commands.literal("setlobby")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                            Arena arena = requireArena(ctx, player);
                                            if (arena == null) return 0;
                                            arena.setLobbySpawn(player.getLocation());
                                            plugin.getArenaManager().saveArena(arena);
                                            player.sendMessage("§aLobby spawn set for §f" + arena.getId());
                                            return 1;
                                        })))
                        .then(Commands.literal("setspectator")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                            Arena arena = requireArena(ctx, player);
                                            if (arena == null) return 0;
                                            arena.setSpectatorSpawn(player.getLocation());
                                            plugin.getArenaManager().saveArena(arena);
                                            player.sendMessage("§aSpectator spawn set for §f" + arena.getId());
                                            return 1;
                                        })))
                        .then(Commands.literal("addspawn")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("team", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                                    Arena arena = requireArena(ctx, player);
                                                    if (arena == null) return 0;
                                                    String team = StringArgumentType.getString(ctx, "team");
                                                    arena.getSpawnPoints()
                                                            .computeIfAbsent(team, k -> new ArrayList<>())
                                                            .add(player.getLocation());
                                                    plugin.getArenaManager().saveArena(arena);
                                                    player.sendMessage("§aAdded spawn point for team §f" + team
                                                            + " §ain §f" + arena.getId());
                                                    return 1;
                                                }))))
                        .then(Commands.literal("setisland")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                                    Arena arena = requireArena(ctx, player);
                                                    if (arena == null) return 0;
                                                    String key = StringArgumentType.getString(ctx, "key");
                                                    arena.getIslandSpawns().put(key, player.getLocation());
                                                    plugin.getArenaManager().saveArena(arena);
                                                    player.sendMessage("§aSet island spawn §f" + key
                                                            + " §afor §f" + arena.getId());
                                                    return 1;
                                                }))))
                        .then(Commands.literal("setschematic")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("fileName", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    Arena arena = requireArena(ctx, ctx.getSource().getSender() instanceof Player p ? p : null);
                                                    if (arena == null) return 0;
                                                    arena.setSchematicName(StringArgumentType.getString(ctx, "fileName"));
                                                    plugin.getArenaManager().saveArena(arena);
                                                    ctx.getSource().getSender().sendMessage("§aSchematic set for §f" + arena.getId());
                                                    return 1;
                                                }))))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    var sender = ctx.getSource().getSender();
                                    for (Arena a : plugin.getArenaManager().getArenas()) {
                                        sender.sendMessage("§f" + a.getId() + " §7- " + a.getModeType()
                                                + " §7[" + a.getState() + "] §7players=" + a.getPlayers().size());
                                    }
                                    return 1;
                                })))
                .build();
    }

    private static Arena requireArena(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, Player fallbackSender) {
        String id = StringArgumentType.getString(ctx, "id");
        Arena arena = PvPTournamentPlugin.get().getArenaManager().getArena(id);
        if (arena == null) {
            ctx.getSource().getSender().sendMessage("§cNo such arena: " + id);
        }
        return arena;
    }
}

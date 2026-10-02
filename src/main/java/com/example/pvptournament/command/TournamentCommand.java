package com.example.pvptournament.command;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.mode.GameModeType;
import com.example.pvptournament.tournament.Tournament;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public final class TournamentCommand {

    private TournamentCommand() {
    }

    public static List<String> aliases() {
        return List.of("t", "tourney");
    }

    public static LiteralCommandNode<CommandSourceStack> create(PvPTournamentPlugin plugin) {
        return Commands.literal("tournament")
                .then(Commands.literal("create")
                        .requires(src -> src.getSender().hasPermission("pvpt.admin"))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("format", StringArgumentType.word())
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .then(Commands.argument("arenaCsv", StringArgumentType.string())
                                                        .executes(ctx -> {
                                                            String id = StringArgumentType.getString(ctx, "id");
                                                            String formatStr = StringArgumentType.getString(ctx, "format").toUpperCase();
                                                            String modeStr = StringArgumentType.getString(ctx, "mode").toUpperCase();
                                                            String arenaCsv = StringArgumentType.getString(ctx, "arenaCsv");

                                                            Tournament.Format format;
                                                            GameModeType modeType;
                                                            try {
                                                                format = Tournament.Format.valueOf(formatStr);
                                                                modeType = GameModeType.valueOf(modeStr);
                                                            } catch (IllegalArgumentException ex) {
                                                                ctx.getSource().getSender().sendMessage("§cInvalid format or mode.");
                                                                return 0;
                                                            }

                                                            List<String> arenaIds = List.of(arenaCsv.split(","));
                                                            plugin.getTournamentManager().create(id, format, modeType, arenaIds);
                                                            ctx.getSource().getSender().sendMessage("§aTournament §f" + id + " §acreated. Players can /tournament join " + id);
                                                            return 1;
                                                        }))))))
                .then(Commands.literal("join")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");
                                    Tournament t = plugin.getTournamentManager().get(id);
                                    if (t == null) {
                                        player.sendMessage("§cNo such tournament.");
                                        return 0;
                                    }
                                    if (t.register(player.getUniqueId())) {
                                        player.sendMessage("§aRegistered for tournament §f" + id);
                                    } else {
                                        player.sendMessage("§cCould not register (already registered, or registration closed).");
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");
                                    Tournament t = plugin.getTournamentManager().get(id);
                                    if (t != null && t.unregister(player.getUniqueId())) {
                                        player.sendMessage("§aUnregistered from §f" + id);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("start")
                        .requires(src -> src.getSender().hasPermission("pvpt.admin"))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    Tournament t = plugin.getTournamentManager().get(id);
                                    if (t == null) {
                                        ctx.getSource().getSender().sendMessage("§cNo such tournament.");
                                        return 0;
                                    }
                                    if (t.getRegisteredPlayers().size() < 2) {
                                        ctx.getSource().getSender().sendMessage("§cNeed at least 2 registered players.");
                                        return 0;
                                    }
                                    plugin.getTournamentManager().startTournament(t);
                                    ctx.getSource().getSender().sendMessage("§aTournament §f" + id + " §astarted with "
                                            + t.getRegisteredPlayers().size() + " players.");
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var sender = ctx.getSource().getSender();
                            if (plugin.getTournamentManager().getAll().isEmpty()) {
                                sender.sendMessage("§eNo tournaments exist.");
                                return 1;
                            }
                            for (Tournament t : plugin.getTournamentManager().getAll()) {
                                sender.sendMessage("§f" + t.getId() + " §7- " + t.getPhase()
                                        + " §7(" + t.getRegisteredPlayers().size() + " registered)");
                            }
                            return 1;
                        }))
                .build();
    }
}

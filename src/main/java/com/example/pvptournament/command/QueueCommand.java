package com.example.pvptournament.command;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.mode.GameModeType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class QueueCommand {

    private QueueCommand() {
    }

    public static List<String> aliases() {
        return List.of("q");
    }

    public static LiteralCommandNode<CommandSourceStack> create(PvPTournamentPlugin plugin) {
        return Commands.literal("queue")
                .requires(src -> src.getSender().hasPermission("pvpt.use"))
                .then(Commands.literal("join")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(QueueCommand::suggestModes)
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage("§cPlayers only.");
                                        return 0;
                                    }
                                    String raw = StringArgumentType.getString(ctx, "mode").toUpperCase();
                                    GameModeType type;
                                    try {
                                        type = GameModeType.valueOf(raw);
                                    } catch (IllegalArgumentException ex) {
                                        player.sendMessage("§cUnknown mode. Try: " + modeNames());
                                        return 0;
                                    }
                                    if (plugin.getQueueManager().isQueued(player)) {
                                        player.sendMessage("§cYou are already queued. Use /queue leave first.");
                                        return 0;
                                    }
                                    plugin.getQueueManager().joinQueue(player, type);
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                ctx.getSource().getSender().sendMessage("§cPlayers only.");
                                return 0;
                            }
                            plugin.getQueueManager().leaveQueue(player);
                            return 1;
                        }))
                .build();
    }

    private static CompletableFuture<Suggestions> suggestModes(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        Arrays.stream(GameModeType.values())
                .map(Enum::name)
                .filter(name -> name.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static String modeNames() {
        return String.join(", ", Arrays.stream(GameModeType.values()).map(Enum::name).toList());
    }
}

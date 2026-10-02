package com.example.pvptournament;

import com.example.pvptournament.arena.ArenaManager;
import com.example.pvptournament.command.AdminCommand;
import com.example.pvptournament.command.KitCommand;
import com.example.pvptournament.command.QueueCommand;
import com.example.pvptournament.command.TournamentCommand;
import com.example.pvptournament.kit.KitManager;
import com.example.pvptournament.listener.CombatListener;
import com.example.pvptournament.listener.PlayerConnectionListener;
import com.example.pvptournament.queue.QueueManager;
import com.example.pvptournament.tournament.TournamentManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPTournamentPlugin extends JavaPlugin {

    private static PvPTournamentPlugin instance;

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private QueueManager queueManager;
    private TournamentManager tournamentManager;

    public static PvPTournamentPlugin get() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Order matters: arenas -> kits -> queue -> tournament
        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);
        this.queueManager = new QueueManager(this, arenaManager);
        this.tournamentManager = new TournamentManager(this, arenaManager);

        arenaManager.loadAll();
        kitManager.loadAll();

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();
            registrar.register(QueueCommand.create(this), "Join/leave casual PvP queues", QueueCommand.aliases());
            registrar.register(KitCommand.create(this), "Select or edit kits", KitCommand.aliases());
            registrar.register(TournamentCommand.create(this), "Tournament bracket management", TournamentCommand.aliases());
            registrar.register(AdminCommand.create(this), "PvPTournament admin commands", AdminCommand.aliases());
        });

        getLogger().info("PvPTournament enabled: " + arenaManager.getArenas().size() + " arenas loaded.");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) arenaManager.shutdown();
        if (queueManager != null) queueManager.shutdown();
        if (tournamentManager != null) tournamentManager.shutdown();
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }
}

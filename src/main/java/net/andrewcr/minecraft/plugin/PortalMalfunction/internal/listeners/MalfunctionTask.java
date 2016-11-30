package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.listeners;

import net.andrewcr.minecraft.plugin.BasePluginLib.util.ChestUtil;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.RandomUtil;
import net.andrewcr.minecraft.plugin.PlayerPortals.api.types.IPortal;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.Plugin;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.integration.playerportals.PlayerPortalsIntegration;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.ConfigStore;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.PlayerConfig;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MalfunctionTask extends BukkitRunnable {
    private final Map<Integer, List<TaskStep>> steps;
    private final Player player;
    private final Location origin;
    private final Location destination;
    private final IPortal startPortal;
    private final PlayerConfig config;

    private int count = 0;

    public MalfunctionTask(Player player, Location origin) {
        this.player = player;
        this.origin = origin;
        this.startPortal = PlayerPortalsIntegration.getInstance().getPortalByName(ConfigStore.getInstance().getTriggerPortal());
        this.destination = PlayerPortalsIntegration.getInstance().getTeleportLocation(player, ConfigStore.getInstance().getRedirectPortal());
        this.config = ConfigStore.getInstance().getPlayerConfig(player);

        this.steps = new HashMap<>();

        // Chat messages at source
        this.addChatStep(0, "Attempting compensation...");
        this.addChatStep(40, "Compensation failed!");
        this.addChatStep(80, "Phase error: " + RandomUtil.getRandomInt(1, 25) + "%");
        this.addChatStep(120, "Phase error: " + RandomUtil.getRandomInt(26, 50) + "%");
        this.addChatStep(140, ChatColor.YELLOW + "WARNING: " + ChatColor.WHITE + "Phase error above safety limit!");
        this.addChatStep(160, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(51, 75) + "%");
        this.addChatStep(200, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(75, 100) + "%");
        this.addChatStep(201, "Preparing emergency shunt...");
        this.addChatStep(240, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(101, 125) + "%");
        this.addChatStep(260, ChatColor.RED + "ERROR: " + ChatColor.WHITE + "Emergency shunt target not set!");
        this.addChatStep(280, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(126, 150) + "%");
        this.addChatStep(300, ChatColor.RED + "WARNING: " + ChatColor.WHITE + "Phase error above critical limit!");
        this.addChatStep(320, "Phase error: " + ChatColor.RED + RandomUtil.getRandomInt(151, 175) + "%");
        this.addChatStep(340, ChatColor.RED + "WARNING: " + ChatColor.WHITE + "Pattern buffer corruption detected!");
        this.addChatStep(360, "Phase error: " + ChatColor.RED + RandomUtil.getRandomInt(176, 200) + "%");
        this.addChatStep(370, "Emergency shunt online");
        this.addChatStep(380, "Engaging emergency shunt...");

        // Remove player's inventory, if possible
        this.addStep(340, new TaskStep() {
            @Override
            void execute() {
                MalfunctionTask.doInventoryStep(player);
                player.spawnParticle(Particle.CLOUD, player.getLocation(), 200);
            }
        });

        // Lightning strikes at source
        int zapStep = 0;
        for (int i = 1; i < 40; i++) {
            this.addZapStep(zapStep, (i / 10) + 1);
            zapStep += (40 - i) / 2;
        }

        // Teleport the player to their spawn in the world
        this.addStep(390, new TaskStep() {
            @Override
            void execute() {
                MalfunctionTask.doTeleportStep(player);
            }
        });

        // Chat messages at destination
        this.addChatStep(400, "Emergency shunt engaged");
        this.addChatStep(440, "Phase error: " + ChatColor.RED + RandomUtil.getRandomInt(176, 200) + "%");
        this.addChatStep(480, "Phase error: " + ChatColor.RED + RandomUtil.getRandomInt(151, 175) + "%");
        this.addChatStep(500, "Phase error within critical limit");
        this.addChatStep(520, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(126, 150) + "%");
        this.addChatStep(560, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(101, 125) + "%");
        this.addChatStep(600, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(75, 100) + "%");
        this.addChatStep(640, "Phase error: " + ChatColor.YELLOW + RandomUtil.getRandomInt(51, 75) + "%");
        this.addChatStep(660, "Phase error within safety limit");
        this.addChatStep(680, "Phase error: " + RandomUtil.getRandomInt(26, 50) + "%");
        this.addChatStep(720, "Phase error: " + RandomUtil.getRandomInt(1, 25) + "%");
        this.addChatStep(760, "Phase error nominal");

        // Reset everything to normal
        this.addStep(770, new TaskStep() {
            @Override
            void execute() {
                // Allow use of source portal again
                startPortal.setProperty(PortalListener.PORTAL_MALFUNCTIONING_KEY, null);

                MalfunctionTask.doCleanupStep(player, config);
            }
        });

        this.addChatStep(780, "Emergency shunt complete.");
        this.addChatStep(800, "Please report problems to portal administrator: " + ChatColor.GREEN + "null@null" + ChatColor.WHITE + ".");

        // Lightning strikes at destination
        zapStep = 400;
        for (int i = 39; i > 0; i--) {
            this.addZapStep(zapStep, (i / 10) + 1);
            zapStep += (40 - i) / 2;
        }
    }

    static void doTeleportStep(Player player) {
        PlayerPortalsIntegration.getInstance().teleportPlayer(player, ConfigStore.getInstance().getRedirectPortal(), false);
        player.setBedSpawnLocation(null);
    }

    static void doInventoryStep(Player player) {
        Location chestLocation = ConfigStore.getInstance().getChestLocation();
        if (chestLocation != null && ChestUtil.getChest(chestLocation) != null) {
            ChestListener.addPlayerInventory(player);
        }
    }

    static void doCleanupStep(Player player, PlayerConfig config) {
        // Don't limit the player's speed anymore
        player.removeMetadata(PortalListener.MALFUNCTION_IN_PROGRESS_KEY, Plugin.getInstance());
        player.setWalkSpeed(config.getPreviousSpeed());

        // Clear the config flag
        config.setIsInProgress(false);

        // Don't malfunction twice for the same player
        config.setIsTriggered(true);
    }

    private void addChatStep(int tick, String message) {
        this.addStep(tick, new TaskStep() {
            @Override
            void execute() {
                player.sendMessage(message);
            }
        });
    }

    private void addZapStep(int tick, int strikes) {
        this.addStep(tick, new TaskStep() {
            int radius = 6;

            @Override
            void execute() {
                for (int i = 0; i < strikes; i++) {
                    double phi = 2 * Math.PI * Math.random();
                    double r = Math.random() * this.radius;

                    // Convert from polar to rectangular
                    Location strikeLocation = player.getLocation().clone();
                    strikeLocation.setX(strikeLocation.getX() + (r * Math.cos(phi)));
                    strikeLocation.setZ(strikeLocation.getZ() + (r * Math.sin(phi)));
                    strikeLocation.setY(strikeLocation.getWorld().getHighestBlockYAt(strikeLocation));

                    strikeLocation.getWorld().strikeLightningEffect(strikeLocation);
                }
            }
        });
    }

    private void addStep(int tick, TaskStep step) {
        List<TaskStep> tickSteps;

        if (this.steps.containsKey(tick)) {
            tickSteps = this.steps.get(tick);
        } else {
            tickSteps = new ArrayList<>();
            this.steps.put(tick, tickSteps);
        }

        tickSteps.add(step);
    }

    @Override
    public void run() {
        this.count++;

        if (!this.player.isOnline()) {
            // Player disconnected in the middle of the sequence - reset the portal and wait for them to log back in
            Plugin.getInstance().getLogger().info("Player " + this.player.getName() + " signed off in the middle of a malfunction, cancelling...");
            startPortal.setProperty(PortalListener.PORTAL_MALFUNCTIONING_KEY, null);
            this.cancel();
            return;
        }

        for (Integer tick : this.steps.keySet()) {
            if (tick < count) {
                Plugin.getInstance().getLogger().info("Executing malfunction step " + tick + " for player " + this.player.getName());
                List<TaskStep> taskSteps = this.steps.get(tick);
                taskSteps.forEach(TaskStep::execute);

                this.steps.remove(tick);
                break;
            }
        }

        if (this.steps.isEmpty()) {
            Plugin.getInstance().getLogger().info("Done malfunctioning for player " + this.player.getName());
            // Out of messages, end the task
            this.cancel();
        }
    }

    private abstract class TaskStep {
        abstract void execute();
    }
}
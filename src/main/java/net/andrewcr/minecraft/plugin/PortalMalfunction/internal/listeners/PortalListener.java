package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.listeners;

import net.andrewcr.minecraft.plugin.BasePluginLib.util.StringUtil;
import net.andrewcr.minecraft.plugin.PlayerPortals.api.events.PlayerPortalsEntityPortalExitEvent;
import net.andrewcr.minecraft.plugin.PlayerPortals.api.events.PlayerPortalsPlayerPortalEvent;
import net.andrewcr.minecraft.plugin.PlayerPortals.api.types.IPortal;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.Plugin;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.ConfigStore;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.PlayerConfig;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class PortalListener implements Listener {
    static final String PORTAL_MALFUNCTIONING_KEY = "Malfunctioning";
    static final String MALFUNCTION_ORIGIN_KEY = "MalfunctionOrigin";
    static final String MALFUNCTION_IN_PROGRESS_KEY = "MalfunctionInProgress";

    private static final float MALFUNCTION_WALK_SPEED = 0.02f;

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        PlayerConfig config = ConfigStore.getInstance().getPlayerConfig(player);

        if (config.isInProgress()) {
            // Player logged back in after being disconnected in the middle of a malfunction - wait until
            //  the login finishes, then complete the malfunction tasks.
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        // Login hasn't finished yet - try again in a bit
                        return;
                    }

                    // Send player to redirect location
                    MalfunctionTask.doTeleportStep(player);

                    // Remove player's inventory, if possible
                    MalfunctionTask.doInventoryStep(player);

                    // Reset everything else to normal
                    MalfunctionTask.doCleanupStep(player, config);

                    this.cancel();
                }
            }.runTaskTimer(Plugin.getInstance(), 20, 2);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata(MALFUNCTION_ORIGIN_KEY)) {
            MetadataValue value = player.getMetadata(MALFUNCTION_ORIGIN_KEY).stream()
                .filter(m -> m.getOwningPlugin() == Plugin.getInstance())
                .findAny()
                .orElse(null);

            if (value != null && value.value() instanceof Location) {
                Location origin = (Location) value.value();
                if (origin.getWorld() != event.getTo().getWorld()) {
                    // Player somehow left the world before they moved far enough
                    player.removeMetadata(MALFUNCTION_ORIGIN_KEY, Plugin.getInstance());
                    return;
                }

                if (origin.distance(event.getTo()) > 2) {
                    // Player has moved far enough, start the fun
                    player.removeMetadata(MALFUNCTION_ORIGIN_KEY, Plugin.getInstance());
                    player.setMetadata(MALFUNCTION_IN_PROGRESS_KEY, new FixedMetadataValue(Plugin.getInstance(), true));

                    // Make sure the player moves too slowly to escape the area of effect
                    PlayerConfig config = ConfigStore.getInstance().getPlayerConfig(event.getPlayer());
                    config.setPreviousSpeed(event.getPlayer().getWalkSpeed());
                    event.getPlayer().setWalkSpeed(MALFUNCTION_WALK_SPEED);

                    // Start the task that creates the malfunction events
                    MalfunctionTask malfunctionTask = new MalfunctionTask(event.getPlayer(), origin);
                    malfunctionTask.runTaskTimer(Plugin.getInstance(), 0, 1);
                }
            }
        }

        if (player.hasMetadata(MALFUNCTION_IN_PROGRESS_KEY)) {
            // Setting the player's walk speed doesn't keep them from moving faster while jumping, so
            //  clamp their velocity vector.

            if (event.getPlayer().getVelocity().length() > MALFUNCTION_WALK_SPEED) {
                org.bukkit.util.Vector newVelocity = event.getPlayer().getVelocity();
                newVelocity.multiply(MALFUNCTION_WALK_SPEED / newVelocity.length());

                event.getPlayer().setVelocity(newVelocity);
            }
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalsPlayerPortalEvent event) {
        IPortal destination = event.getDestinationPortal();
        if (destination == null || !StringUtil.equals(destination.getName(), ConfigStore.getInstance().getTriggerPortal())) {
            // Not the portal we want
            return;
        }

        if (StringUtil.equalsIgnoreCase(destination.getProperty(PORTAL_MALFUNCTIONING_KEY), "true")) {
            event.getPlayer().sendMessage("This portal is currently down for maintenance.  Please try again later.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExitPortal(PlayerPortalsEntityPortalExitEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            // Not a player
            return;
        }

        IPortal destPortal = event.getDestinationPortal();
        if (destPortal == null || !StringUtil.equals(destPortal.getName(), ConfigStore.getInstance().getTriggerPortal())) {
            // Not the portal we want
            return;
        }

        Player player = (Player) event.getEntity();

        PlayerConfig config = ConfigStore.getInstance().getPlayerConfig(player);
        if (config.isTriggered()) {
            // Already triggered for this player
            return;
        }

        config.setIsInProgress(true);
        destPortal.setProperty(PORTAL_MALFUNCTIONING_KEY, "true");

        player.sendMessage(ChatColor.YELLOW + "WARNING: " + ChatColor.WHITE + "Quantum instability detected.  Compensator active.");

        // Wait for the player to move a bit before starting the fun
        player.setMetadata(MALFUNCTION_ORIGIN_KEY, new FixedMetadataValue(Plugin.getInstance(), event.getTo()));
    }
}

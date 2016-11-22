package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.integration.playerportals;

import net.andrewcr.minecraft.plugin.PlayerPortals.api.IPlayerPortalsApi;
import net.andrewcr.minecraft.plugin.PlayerPortals.api.types.IPortal;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerPortalsIntegration {
    private IPlayerPortalsApi ppApi = null;

    public static PlayerPortalsIntegration getInstance() {
        return Plugin.getInstance().getPlayerPortalsIntegration();
    }

    public PlayerPortalsIntegration() {
        org.bukkit.plugin.Plugin playerPortalsPlugin = Bukkit.getPluginManager().getPlugin("PlayerPortals");

        if (playerPortalsPlugin == null) {
            Plugin.getInstance().getLogger().info("PlayerPortals integration disabled - failed to get plugin instance!");
            return;
        }

        if (!playerPortalsPlugin.isEnabled()) {
            Plugin.getInstance().getLogger().info("PlayerPortals integration disabled - Dynmap plugin is not enabled!");
            return;
        }

        if (!(playerPortalsPlugin instanceof IPlayerPortalsApi)) {
            Plugin.getInstance().getLogger().info("PlayerPortals integration disabled - plugin instance not of expected type!");
            return;
        }

        this.ppApi = (IPlayerPortalsApi) playerPortalsPlugin;
    }

    public IPortal getPortalByName(String name) {
        if (this.ppApi != null) {
            return this.ppApi.getPortalByName(name);
        }

        return null;
    }

    public boolean teleportPlayer(Player player, String destination, boolean sendMessage) {
        if (this.ppApi != null) {
            return this.ppApi.teleportPlayer(player, destination, sendMessage);
        }

        return false;
    }

    public Location getTeleportLocation(Player player, String destinationName) {
        if (this.ppApi != null) {
            return this.ppApi.getTeleportLocation(player, destinationName);
        }

        return null;
    }
}

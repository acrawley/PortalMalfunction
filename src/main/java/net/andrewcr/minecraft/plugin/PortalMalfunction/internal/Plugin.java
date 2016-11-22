package net.andrewcr.minecraft.plugin.PortalMalfunction.internal;

import lombok.Getter;
import net.andrewcr.minecraft.plugin.BasePluginLib.plugin.PluginBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.Version;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.integration.playerportals.PlayerPortalsIntegration;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.listeners.ChestListener;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.listeners.PortalListener;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.ConfigStore;

public class Plugin extends PluginBase {
    @Getter private static Plugin instance;

    @Getter private ConfigStore configStore;
    @Getter private PlayerPortalsIntegration playerPortalsIntegration;

    @Override
    protected Version getRequiredBPLVersion() {
        return new Version(1, 3);
    }

    @Override
    public void onEnableCore() {
        Plugin.instance = this;

        this.configStore = new ConfigStore();
        this.configStore.load();

        this.registerListener(new PortalListener());
        this.registerListener(new ChestListener());

        this.playerPortalsIntegration = new PlayerPortalsIntegration();
    }

    @Override
    public void onDisableCore() {
        this.configStore.save();

        Plugin.instance = null;
    }
}

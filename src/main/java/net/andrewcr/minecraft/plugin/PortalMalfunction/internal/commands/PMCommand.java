package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.commands;

import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandExecutorBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.GroupCommandExecutorBase;

public class PMCommand extends CommandBase {

    @Override
    public CommandExecutorBase getExecutor() {
        return new PMCommandExecutor();
    }

    private class PMCommandExecutor extends GroupCommandExecutorBase {

        public PMCommandExecutor() {
            super("pm");
        }
    }
}

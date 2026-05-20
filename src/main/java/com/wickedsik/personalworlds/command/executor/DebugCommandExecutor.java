package com.wickedsik.personalworlds.command.executor;

import com.wickedsik.personalworlds.command.CommandResult;
import com.wickedsik.personalworlds.util.PerformanceMonitor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DebugCommandExecutor {

    public CommandResult enablePerf() {
        PerformanceMonitor.enable();
        return CommandResult.successBroadcast(
            Text.translatable("pocketislands.command.perf.enabled").formatted(Formatting.GREEN)
        );
    }

    public CommandResult disablePerf() {
        PerformanceMonitor.disable();
        return CommandResult.successBroadcast(
            Text.translatable("pocketislands.command.perf.disabled").formatted(Formatting.GRAY)
        );
    }

    public void showStatus(ServerCommandSource source, MinecraftServer server) {
        String status = PerformanceMonitor.getStatusSummary(server);
        for (String line : status.split("\n")) {
            source.sendFeedback(() -> Text.literal(line), false);
        }
    }

    public CommandResult resetCounters() {
        PerformanceMonitor.resetCounters();
        return CommandResult.successBroadcast(
            Text.translatable("pocketislands.command.perf.reset").formatted(Formatting.GRAY)
        );
    }
}
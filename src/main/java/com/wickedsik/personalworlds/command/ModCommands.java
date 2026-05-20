package com.wickedsik.personalworlds.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.wickedsik.personalworlds.command.executor.AdminCommandExecutor;
import com.wickedsik.personalworlds.command.executor.DebugCommandExecutor;
import com.wickedsik.personalworlds.command.executor.DevCommandExecutor;
import com.wickedsik.personalworlds.command.executor.PlayerCommandExecutor;
import com.wickedsik.personalworlds.command.service.PlayerLookupService;
import com.wickedsik.personalworlds.compat.CommandCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.util.PermissionHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ModCommands {
    private static DevCommandExecutor devExecutor;
    private static PlayerCommandExecutor playerExecutor;
    private static AdminCommandExecutor adminExecutor;
    private static DebugCommandExecutor debugExecutor;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            initializeExecutors();
            registerCommands(dispatcher);
        });
    }

    private static void initializeExecutors() {
        PlayerLookupService playerLookup = new PlayerLookupService();
        devExecutor = new DevCommandExecutor();
        playerExecutor = new PlayerCommandExecutor(playerLookup);
        adminExecutor = new AdminCommandExecutor(playerLookup);
        debugExecutor = new DebugCommandExecutor();
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Основная команда: /pi
        dispatcher.register(
            CommandManager.literal("pi")
                // === Команды игрока ===
                .then(CommandManager.literal("create")
                    .requires(PermissionHelper.require("pocketislands.player.create", 2))
                    .executes(ctx -> handleCreate(ctx.getSource(), "OVERWORLD"))
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .executes(ctx -> handleCreate(ctx.getSource(), StringArgumentType.getString(ctx, "type")))))
                .then(CommandManager.literal("enter")
                    .requires(PermissionHelper.require("pocketislands.player.create", 2))
                    .executes(ctx -> handleEnter(ctx.getSource())))
                .then(CommandManager.literal("leave")
                    .requires(PermissionHelper.require("pocketislands.player.create", 2))
                    .executes(ctx -> handleLeave(ctx.getSource())))
                .then(buildInviteCommand())
                .then(CommandManager.literal("uninvite")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(ctx -> handleUninvite(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                .then(buildToggleWelcomeCommand())
                .then(CommandManager.literal("invites")
                    .executes(ctx -> handleInvites(ctx.getSource())))
                .then(CommandManager.literal("portals")
                    .executes(ctx -> handlePortals(ctx.getSource())))

                // === Админ-команды: /pi admin ===
                .then(CommandManager.literal("admin")
                    .then(CommandManager.literal("list")
                        .requires(PermissionHelper.require("pocketislands.admin.list", 2))
                        .executes(ctx -> handleAdminList(ctx.getSource())))
                    .then(CommandManager.literal("info")
                        .requires(PermissionHelper.require("pocketislands.admin.info", 2))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> handleAdminInfo(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                    .then(CommandManager.literal("delete")
                        .requires(PermissionHelper.require("pocketislands.admin.delete", 4))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> handleAdminDeletePrompt(ctx.getSource(), StringArgumentType.getString(ctx, "player")))
                            .then(CommandManager.literal("confirm")
                                .executes(ctx -> handleAdminDeleteConfirm(ctx.getSource(), StringArgumentType.getString(ctx, "player"))))))
                    .then(CommandManager.literal("tp")
                        .requires(PermissionHelper.require("pocketislands.admin.teleport", 2))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> handleAdminTeleport(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
                    .then(CommandManager.literal("reload")
                        .requires(PermissionHelper.require("pocketislands.admin.reload", 3))
                        .executes(ctx -> handleAdminReload(ctx.getSource()))))

                // === Отладочные команды: /pi debug ===
                .then(CommandManager.literal("debug")
                    .requires(CommandCompat.requiresLevel(4))
                    .then(CommandManager.literal("perf")
                        .then(CommandManager.literal("enable")
                            .executes(ctx -> debugExecutor.enablePerf().applyTo(ctx.getSource())))
                        .then(CommandManager.literal("disable")
                            .executes(ctx -> debugExecutor.disablePerf().applyTo(ctx.getSource())))
                        .then(CommandManager.literal("status")
                            .executes(ctx -> {
                                debugExecutor.showStatus(ctx.getSource(), ctx.getSource().getServer());
                                return 1;
                            })))
                    .then(CommandManager.literal("reset")
                        .executes(ctx -> debugExecutor.resetCounters().applyTo(ctx.getSource()))))
        );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildInviteCommand() {
        RequiredArgumentBuilder<ServerCommandSource, ServerPlayerEntity> playerArg = CommandManager
            .argument("player", EntityArgumentType.player())
            .executes(ctx -> handleInvite(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), false));

        if (ModConfig.get().enableAlwaysWelcome) {
            playerArg = playerArg.then(
                CommandManager.literal("always")
                    .executes(ctx -> handleInvite(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), true))
            );
        }

        return CommandManager.literal("invite").then(playerArg);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildToggleWelcomeCommand() {
        if (!ModConfig.get().enableAlwaysWelcome) {
            // Если функция отключена в конфиге — команда не будет доступна никому
            return CommandManager.literal("togglewelcome").requires(source -> false);
        } else {
            return CommandManager.literal("togglewelcome")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .executes(ctx -> handleToggleWelcome(ctx.getSource(), StringArgumentType.getString(ctx, "player"))));
        }
    }

    // === Обработчики команд ===

    private static int handleCreate(ServerCommandSource source, String typeStr) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity player) {
            return devExecutor.createDimension(player, typeStr).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleEnter(ServerCommandSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity player) {
            return devExecutor.enterDimension(player).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleLeave(ServerCommandSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity player) {
            return devExecutor.leaveDimension(player).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleInvite(ServerCommandSource source, ServerPlayerEntity guest, boolean alwaysWelcome) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity owner) {
            return playerExecutor.invite(owner, guest, alwaysWelcome).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleToggleWelcome(ServerCommandSource source, String guestName) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity owner) {
            return playerExecutor.toggleWelcome(owner, guestName).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleUninvite(ServerCommandSource source, String guestName) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity owner) {
            return playerExecutor.uninvite(owner, guestName).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleInvites(ServerCommandSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity player) {
            return playerExecutor.showInvitations(player).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handlePortals(ServerCommandSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity player) {
            playerExecutor.showPortals(player, source);
            return 1;
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleAdminList(ServerCommandSource source) {
        adminExecutor.list(source);
        return 1;
    }

    private static int handleAdminInfo(ServerCommandSource source, String playerName) {
        return adminExecutor.info(source, playerName).applyTo(source);
    }

    private static int handleAdminDeletePrompt(ServerCommandSource source, String playerName) {
        return adminExecutor.deletePrompt(source, playerName).applyTo(source);
    }

    private static int handleAdminDeleteConfirm(ServerCommandSource source, String playerName) {
        return adminExecutor.deleteConfirm(source, playerName).applyTo(source);
    }

    private static int handleAdminTeleport(ServerCommandSource source, String playerName) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity admin) {
            return adminExecutor.teleport(admin, playerName).applyTo(source);
        } else {
            source.sendError(Text.translatable("pocketislands.command.error.must_be_player"));
            return 0;
        }
    }

    private static int handleAdminReload(ServerCommandSource source) {
        return adminExecutor.reload(source).applyTo(source);
    }
}
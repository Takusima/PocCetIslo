package com.wickedsik.personalworlds.command.executor;

import com.wickedsik.personalworlds.command.CommandResult;
import com.wickedsik.personalworlds.command.service.PlayerLookupService;
import com.wickedsik.personalworlds.compat.EntityCompat;
import com.wickedsik.personalworlds.compat.IdentifierCompat;
import com.wickedsik.personalworlds.config.ModConfig;
import com.wickedsik.personalworlds.dimension.DimensionRegistry;
import com.wickedsik.personalworlds.dimension.PlayerDimensionData;
import com.wickedsik.personalworlds.player.InvitationManager;
import com.wickedsik.personalworlds.player.PlayerDataManager;
import com.wickedsik.personalworlds.portal.PortalColor;
import com.wickedsik.personalworlds.registry.ModBlocks;
import com.wickedsik.personalworlds.registry.ModItems;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.List;
import java.util.Optional;

public class PlayerCommandExecutor {
    private final PlayerLookupService playerLookup;

    public PlayerCommandExecutor(PlayerLookupService playerLookup) {
        this.playerLookup = playerLookup;
    }

    public CommandResult invite(ServerPlayerEntity owner, ServerPlayerEntity guest, boolean alwaysWelcome) {
        InvitationManager.invite(EntityCompat.getServer(owner), owner, guest, alwaysWelcome);
        return CommandResult.silent();
    }

    public CommandResult uninvite(ServerPlayerEntity owner, String guestName) {
        MinecraftServer server = EntityCompat.getServer(owner);
        Optional<PlayerLookupService.PlayerReference> playerRef = this.playerLookup.findInInvitations(server, owner.getUuid(), guestName);
        
        if (playerRef.isEmpty()) {
            return CommandResult.error(Text.translatable("pocketislands.command.error.player_not_found", guestName));
        } else {
            PlayerLookupService.PlayerReference ref = playerRef.get();
            InvitationManager.uninvite(server, owner, ref.uuid(), ref.resolvedName());
            return CommandResult.silent();
        }
    }

    public CommandResult toggleWelcome(ServerPlayerEntity owner, String guestName) {
        MinecraftServer server = EntityCompat.getServer(owner);
        Optional<PlayerLookupService.PlayerReference> playerRef = this.playerLookup.findInInvitations(server, owner.getUuid(), guestName);
        
        if (playerRef.isEmpty()) {
            return CommandResult.error(Text.translatable("pocketislands.command.error.not_invited_by_you", guestName));
        } else {
            PlayerLookupService.PlayerReference ref = playerRef.get();
            PlayerDataManager dataManager = PlayerDataManager.get(server);
            Optional<Boolean> newValue = dataManager.toggleAlwaysWelcome(owner.getUuid(), ref.uuid());
            
            if (newValue.isEmpty()) {
                return CommandResult.error(Text.translatable("pocketislands.command.error.not_invited_by_you", ref.resolvedName()));
            } else {
                String messageKey = newValue.get() ? "pocketislands.command.toggle_welcome_on" : "pocketislands.command.toggle_welcome_off";
                return CommandResult.success(Text.translatable(messageKey, ref.resolvedName()));
            }
        }
    }

    public CommandResult showInvitations(ServerPlayerEntity player) {
        InvitationManager.showInvitations(player);
        return CommandResult.silent();
    }

    public void showPortals(ServerPlayerEntity player, ServerCommandSource source) {
        List<ModConfig.PortalConfig> portalTypes = ModConfig.get().portalTypes;
        
        source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.header").formatted(Formatting.WHITE), false);
        
        if (portalTypes.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.no_portals").formatted(Formatting.DARK_GRAY), false);
        } else {
            for (int i = 0; i < portalTypes.size(); ++i) {
                this.sendPortalTypeInfo(source, i, portalTypes.get(i));
            }
        }

        source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.your_island_header").formatted(Formatting.WHITE), false);
        
        DimensionRegistry registry = DimensionRegistry.get(source.getServer());
        Optional<PlayerDimensionData> islandData = registry.getDimensionData(player.getUuid());
        
        if (islandData.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.no_island").formatted(Formatting.DARK_GRAY), false);
        } else {
            PlayerDimensionData data = islandData.get();
            int typeIndex = data.portalTypeIndex();
            List<ModConfig.PortalConfig> currentTypes = ModConfig.get().portalTypes;
            
            if (typeIndex >= 0 && typeIndex < currentTypes.size()) {
                Block frameBlock = ModBlocks.getFrameBlock(typeIndex);
                Text frameName = Text.translatable(frameBlock.getTranslationKey());
                PortalColor color = ModBlocks.getPortalColor(typeIndex);
                String colorName = formatColorName(color);
                
                source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.your_type", frameName, colorName).formatted(Formatting.GREEN), false);
            } else {
                source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.your_type_unknown").formatted(Formatting.RED), false);
            }
        }
    }

    private void sendPortalTypeInfo(ServerCommandSource source, int index, ModConfig.PortalConfig config) {
        Block frameBlock = ModBlocks.getFrameBlock(index);
        Text frameName = Text.translatable(frameBlock.getTranslationKey());
        PortalColor color = ModBlocks.getPortalColor(index);
        String colorName = formatColorName(color);
        
        source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.type_header", index + 1, frameName, colorName).formatted(Formatting.GRAY), false);
        
        Item activationItem = ModItems.getActivationItem(index);
        Text itemName = Text.translatable(activationItem.getTranslationKey());
        source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.activate", itemName).formatted(Formatting.DARK_GRAY), false);
        
        Text layersText = buildLayersText(config.islandLayers);
        source.sendFeedback(() -> Text.translatable("pocketislands.command.portals.layers", layersText).formatted(Formatting.DARK_GRAY), false);
    }

    private static Text buildLayersText(String[] layers) {
        if (layers != null && layers.length != 0) {
            MutableText result = Text.empty();
            for (int i = 0; i < layers.length; ++i) {
                if (i > 0) result.append(", ");
                Identifier id = IdentifierCompat.tryParse(layers[i]);
                if (id != null) {
                    Block block = Registries.BLOCK.get(id);
                    if (block == Blocks.AIR && !"minecraft:air".equals(layers[i])) {
                        result.append(Text.literal(layers[i]));
                    } else {
                        result.append(Text.translatable(block.getTranslationKey()));
                    }
                } else {
                    result.append(Text.literal(layers[i]));
                }
            }
            return result;
        } else {
            return Text.literal("None");
        }
    }

    private static String formatColorName(PortalColor color) {
        return color.getDisplayName();
    }
}
package com.wickedsik.personalworlds.dimension.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Генератор чанков для пустого мира с островом.
 */
public class VoidIslandChunkGenerator extends ChunkGenerator {
   public static final Codec<VoidIslandChunkGenerator> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
         BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource),
         BlockState.CODEC.listOf().xmap(list -> list.toArray(new BlockState[0]), Arrays::asList)
            .fieldOf("island_layers").forGetter(gen -> gen.islandLayers)
      ).apply(instance, VoidIslandChunkGenerator::new)
   );

   private final BlockState[] islandLayers;

   public VoidIslandChunkGenerator(BiomeSource biomeSource, BlockState[] islandLayers) {
      super(biomeSource);
      this.islandLayers = islandLayers;
   }

   @Override
   protected Codec<? extends ChunkGenerator> getCodec() { return CODEC; }

   @Override
   public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
      int chunkX = chunk.getPos().x;
      int chunkZ = chunk.getPos().z;
      if (isIslandChunk(chunkX, chunkZ)) {
         generateIslandSection(chunk);
      }
      return CompletableFuture.completedFuture(chunk);
   }

   private boolean isIslandChunk(int chunkX, int chunkZ) {
      return chunkX >= -4 && chunkX <= 3 && chunkZ >= -4 && chunkZ <= 3;
   }

   private void generateIslandSection(Chunk chunk) {
      for (int x = 0; x < 16; ++x) {
         for (int z = 0; z < 16; ++z) {
            for (int layerIdx = 0; layerIdx < this.islandLayers.length; ++layerIdx) {
               BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, 64 - layerIdx, chunk.getPos().getStartZ() + z);
               chunk.setBlockState(pos, this.islandLayers[layerIdx], false);
            }
         }
      }
   }

   @Override
   public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}

   @Override
   public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {}

   @Override
   public void populateEntities(ChunkRegion region) {}

   @Override
   public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
      int chunkX = x >> 4;
      int chunkZ = z >> 4;
      return isIslandChunk(chunkX, chunkZ) ? 65 : world.getBottomY();
   }

   @Override
   public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
      int chunkX = x >> 4;
      int chunkZ = z >> 4;
      int height = world.getHeight();
      int bottomY = world.getBottomY();
      BlockState[] states = new BlockState[height];
      Arrays.fill(states, Blocks.AIR.getDefaultState());
      if (isIslandChunk(chunkX, chunkZ)) {
         for (int layerIdx = 0; layerIdx < this.islandLayers.length; ++layerIdx) {
            int y = 64 - layerIdx;
            int stateIndex = y - bottomY;
            if (stateIndex >= 0 && stateIndex < height) {
               states[stateIndex] = this.islandLayers[layerIdx];
            }
         }
      }
      return new VerticalBlockSample(bottomY, states);
   }

   @Override
   public int getWorldHeight() { return 384; }

   @Override
   public int getMinimumY() { return -64; }

   @Override
   public int getSeaLevel() { return 63; }

   @Override
   public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
      text.add("VoidIsland Generator");
   }
}

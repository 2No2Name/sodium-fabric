package me.jellysquid.mods.sodium.client.render.chunk.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import java.util.*;

/**
 * The render data for a chunk render container containing all the information about which meshes are attached, the
 * block entities contained by it, and any data used for occlusion testing.
 */
public class ChunkRenderData {
    public static final ChunkRenderData ABSENT = new ChunkRenderData.Builder()
            .build();
    public static final ChunkRenderData EMPTY = createEmptyData();

    private List<BlockEntity> globalBlockEntities;
    private List<BlockEntity> blockEntities;

    private Map<BlockRenderPass, ChunkMeshData> meshes;

    private ChunkOcclusionData occlusionData;
    private ChunkRenderBounds bounds;

    private List<Sprite> animatedSprites;

    private boolean isEmpty;
    private int meshByteSize;
    private int facesWithData;

    /**
     * @return True if the chunk has no renderables, otherwise false
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    public ChunkRenderBounds getBounds() {
        return this.bounds;
    }

    /**
     * @param from The direction from which this node is being traversed through on the graph
     * @param to The direction from this node into the adjacent to be tested
     * @return True if this chunk can cull the neighbor given the incoming direction
     */
    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.occlusionData != null && this.occlusionData.isVisibleThrough(from, to);
    }

    public List<Sprite> getAnimatedSprites() {
        return this.animatedSprites;
    }

    /**
     * The collection of block entities contained by this rendered chunk.
     */
    public Collection<BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    /**
     * The collection of block entities contained by this rendered chunk section which are not part of its culling
     * volume. These entities should always be rendered regardless of the render being visible in the frustum.
     */
    public Collection<BlockEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

    /**
     * The collection of chunk meshes belonging to this render.
     */
    public ChunkMeshData getMesh(BlockRenderPass pass) {
        return this.meshes.get(pass);
    }

    public int getMeshSize() {
        return this.meshByteSize;
    }

    public int getFacesWithData() {
        return this.facesWithData;
    }

    public static class Builder {
        private final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();
        private final Set<Sprite> animatedSprites = new ObjectOpenHashSet<>();

        private final Map<BlockRenderPass, ChunkMeshData> meshes = new Reference2ReferenceArrayMap<>();

        private ChunkOcclusionData occlusionData;
        private ChunkRenderBounds bounds;

        public void setBounds(ChunkRenderBounds bounds) {
            this.bounds = bounds;
        }

        public void setOcclusionData(ChunkOcclusionData data) {
            this.occlusionData = data;
        }

        /**
         * Adds a sprite to this data container for tracking. If the sprite is tickable, it will be ticked every frame
         * before rendering as necessary.
         * @param sprite The sprite
         */
        public void addSprite(Sprite sprite) {
            if (sprite.isAnimated()) {
                this.animatedSprites.add(sprite);
            }
        }

        public void setMesh(BlockRenderPass pass, ChunkMeshData data) {
            this.meshes.put(pass, data);
        }

        /**
         * Adds a block entity to the data container.
         * @param entity The block entity itself
         * @param cull True if the block entity can be culled to this chunk render's volume, otherwise false
         */
        public void addBlockEntity(BlockEntity entity, boolean cull) {
            (cull ? this.blockEntities : this.globalBlockEntities).add(entity);
        }

        public ChunkRenderData build() {
            ChunkRenderData data = new ChunkRenderData();
            data.globalBlockEntities = this.globalBlockEntities;
            data.blockEntities = this.blockEntities;
            data.occlusionData = this.occlusionData;
            data.meshes = this.meshes;
            data.bounds = this.bounds;
            data.animatedSprites = new ObjectArrayList<>(this.animatedSprites);

            int facesWithData = 0;
            int size = 0;

            for (ChunkMeshData meshData : this.meshes.values()) {
                size += meshData.getVertexDataSize();

                for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
                    facesWithData |= 1 << entry.getKey().ordinal();
                }
            }

            data.isEmpty = this.globalBlockEntities.isEmpty() && this.blockEntities.isEmpty() && facesWithData == 0;
            data.meshByteSize = size;
            data.facesWithData = facesWithData;

            return data;
        }
    }

    private static ChunkRenderData createEmptyData() {
        ChunkOcclusionData occlusionData = new ChunkOcclusionData();
        occlusionData.addOpenEdgeFaces(EnumSet.allOf(Direction.class));

        ChunkRenderData.Builder meshInfo = new ChunkRenderData.Builder();
        meshInfo.setOcclusionData(occlusionData);

        return meshInfo.build();
    }
}
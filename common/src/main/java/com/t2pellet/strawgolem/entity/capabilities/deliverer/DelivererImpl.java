package com.t2pellet.strawgolem.entity.capabilities.deliverer;

import com.t2pellet.strawgolem.entity.capabilities.held_item.HeldItem;
import com.t2pellet.strawgolem.util.container.ContainerUtil;
import com.t2pellet.strawgolem.util.octree.Octree;
import com.t2pellet.tlib.common.entity.capability.AbstractCapability;
import com.t2pellet.tlib.common.entity.capability.ICapabilityHaver;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DelivererImpl<E extends LivingEntity & ICapabilityHaver> extends AbstractCapability<E> implements Deliverer {

    private Octree tree = new Octree(new AABB(Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    private ResourceLocation level;

    protected DelivererImpl(E e) {
        super(e);
        level = e.level.dimension().location();
    }

    @Override
    public BlockPos getDeliverPos() {
        // Clear memory if we change dimensions
        if (!e.level.dimension().location().equals(level)) {
            tree = new Octree(new AABB(Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
            level = e.level.dimension().location();
        }
        BlockPos query = e.blockPosition();
        BlockPos cachedPos = tree.findNearest(query);
        if (cachedPos == null) return scanForDeliverable(query);
        if (ContainerUtil.isContainer(e.level, cachedPos)) {
            return cachedPos;
        } else tree.remove(cachedPos);
        return null;
    }

    private BlockPos scanForDeliverable(BlockPos query) {
        for (int x = -24; x <= 24; ++x) {
            for (int y = -12; y <= 12; ++y) {
                for (int z = -24; z <= 24; ++z) {
                    BlockPos pos = query.offset(x, y, z);
                    if (ContainerUtil.isContainer(e.level, pos)) {
                        tree.insert(pos);
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void deliver(BlockPos pos) {
        HeldItem heldItem = e.getCapabilityManager().getCapability(HeldItem.class);
        ItemStack stack = heldItem.get().copy();
        if (ContainerUtil.isContainer(e.level, pos)) {
            Container container = (Container) e.level.getBlockEntity(pos);
            for (int i = 0; i < container.getContainerSize(); ++i) {
                ItemStack containerStack = container.getItem(i);
                if (containerStack.isEmpty()) {
                    container.setItem(i, stack);
                    stack = ItemStack.EMPTY;
                } else if (containerStack.is(stack.getItem())) {
                    int placeableCount = containerStack.getMaxStackSize() - containerStack.getCount();
                    int placingCount = Math.min(stack.getCount(), placeableCount);
                    containerStack.grow(placingCount);
                    stack.shrink(placingCount);
                }
                if (stack.isEmpty()) break;
            }
            e.level.gameEvent(e, GameEvent.CONTAINER_OPEN, pos);
            e.level.playSound(null, e, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        e.level.addFreshEntity(new ItemEntity(e.level, pos.getX(), pos.getY() + 1, pos.getZ(), stack));
        heldItem.set(ItemStack.EMPTY);
    }

    @Override
    public Tag writeTag() {
        ListTag positionsTag = new ListTag();
        List<BlockPos> crops = tree.getAll();
        for (BlockPos pos : crops) {
            if (ContainerUtil.isContainer(e.level, pos)) {
                positionsTag.add(NbtUtils.writeBlockPos(pos));
            }
        }
        return positionsTag;
    }

    @Override
    public void readTag(Tag tag) {
        ListTag positions = (ListTag) tag;
        for (Tag position : positions) {
            BlockPos pos = NbtUtils.readBlockPos((CompoundTag) position);
            if (ContainerUtil.isContainer(e.level, pos)) {
                tree.insert(pos);
            }
        }
    }
}
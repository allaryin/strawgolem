package com.t2pellet.strawgolem.entity.capabilities.held_item;

import com.t2pellet.strawgolem.entity.StrawGolem;
import com.t2pellet.tlib.common.entity.capability.AbstractCapability;
import com.t2pellet.tlib.common.entity.capability.ICapabilityHaver;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

class HeldItemImpl<E extends LivingEntity & ICapabilityHaver> extends AbstractCapability<E> implements HeldItem {

    private final SimpleContainer container = new SimpleContainer(1);

    protected HeldItemImpl(E e) {
        super(e);
    }

    @Override
    public void set(ItemStack stack) {
        container.setItem(0, stack);
        synchronize();
    }

    @Override
    public boolean has() {
        ItemStack result = get();
        return result != null && !result.isEmpty();
    }

    @Override
    public ItemStack get() {
        return container.getItem(0);
    }

    @Override
    public Tag writeTag() {
        return container.createTag();
    }

    @Override
    public void readTag(Tag tag) {
        container.fromTag((ListTag) tag);
    }
}
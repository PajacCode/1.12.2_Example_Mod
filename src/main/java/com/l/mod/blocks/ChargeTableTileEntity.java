package com.l.mod.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Arrays;

public class ChargeTableTileEntity extends TileEntity implements IInventory {

    private final int SLOTS = 3;

    private ItemStack[] items;

    public ChargeTableTileEntity()
    {
        items = new ItemStack[SLOTS];
        clear();
    }

    @Override
    public int getSizeInventory()
    {
        return SLOTS;
    }

    @Override
    public boolean isEmpty()
    {
        for(ItemStack stack : items)
            if(!stack.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {

        return items[index];
    }

    @Override
    public ItemStack decrStackSize(int slotIndex, int count) {
        ItemStack itemStackInSlot = getStackInSlot(slotIndex);
        if (itemStackInSlot.isEmpty()) return ItemStack.EMPTY;  // isEmpt();   EMPTY_ITEM

        ItemStack itemStackRemoved;
        if (itemStackInSlot.getCount() <= count) {  // getStackSize()
            itemStackRemoved = itemStackInSlot;
            setInventorySlotContents(slotIndex, ItemStack.EMPTY);   // EMPTY_ITEM
        } else {
            itemStackRemoved = itemStackInSlot.splitStack(count);
            if (itemStackInSlot.getCount() == 0) { // getStackSize
                setInventorySlotContents(slotIndex, ItemStack.EMPTY);   // EMPTY_ITEM
            }
        }
        markDirty();
        return itemStackRemoved;
    }

    // overwrites the stack in the given slotIndex with the given stack
    @Override
    public void setInventorySlotContents(int slotIndex, ItemStack itemstack) {
        items[slotIndex] = itemstack;
        if (itemstack.isEmpty() && itemstack.getCount() > getInventoryStackLimit()) { //  isEmpty(); getStackSize()
            itemstack.setCount(getInventoryStackLimit());  //setStackSize
        }
        markDirty();
    }

    // This is the maximum number if items allowed in each slot
    // This only affects things such as hoppers trying to insert items you need to use the container to enforce this for players
    // inserting items via the gui
    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    // Return true if the given player is able to use this block. In this case it checks that
    // 1) the world tileentity hasn't been replaced in the meantime, and
    // 2) the player isn't too far away from the centre of the block
    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (this.world.getTileEntity(this.pos) != this) return false;
        final double X_CENTRE_OFFSET = 0.5;
        final double Y_CENTRE_OFFSET = 0.5;
        final double Z_CENTRE_OFFSET = 0.5;
        final double MAXIMUM_DISTANCE_SQ = 8.0 * 8.0;
        return player.getDistanceSq(pos.getX() + X_CENTRE_OFFSET, pos.getY() + Y_CENTRE_OFFSET, pos.getZ() + Z_CENTRE_OFFSET) < MAXIMUM_DISTANCE_SQ;
    }

    // Return true if the given stack is allowed to go in the given slot.  In this case, we can insert anything.
    // This only affects things such as hoppers trying to insert items you need to use the container to enforce this for players
    // inserting items via the gui
    @Override
    public boolean isItemValidForSlot(int slotIndex, ItemStack itemstack) {
        return true;
    }

    // This is where you save any data that you don't want to lose when the tile entity unloads
    // In this case, it saves the items stored in the container
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound parentNBTTagCompound)
    {
        super.writeToNBT(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        // to use an analogy with Java, this code generates an array of hashmaps
        // The itemStack in each slot is converted to an NBTTagCompound, which is effectively a hashmap of key->value pairs such
        //   as slot=1, id=2353, count=1, etc
        // Each of these NBTTagCompound are then inserted into NBTTagList, which is similar to an array.
        NBTTagList dataForAllSlots = new NBTTagList();
        for (int i = 0; i < this.items.length; ++i) {
            if (!this.items[i].isEmpty())	{ //isEmpty()
                NBTTagCompound dataForThisSlot = new NBTTagCompound();
                dataForThisSlot.setByte("Slot", (byte) i);
                this.items[i].writeToNBT(dataForThisSlot);
                dataForAllSlots.appendTag(dataForThisSlot);
            }
        }
        // the array of hashmaps is then inserted into the parent hashmap for the container
        parentNBTTagCompound.setTag("Items", dataForAllSlots);
        // return the NBT Tag Compound
        return parentNBTTagCompound;
    }

    // This is where you load the data that you saved in writeToNBT
    @Override
    public void readFromNBT(NBTTagCompound parentNBTTagCompound)
    {
        super.readFromNBT(parentNBTTagCompound); // The super call is required to save and load the tiles location
        final byte NBT_TYPE_COMPOUND = 10;       // See NBTBase.createNewByType() for a listing
        NBTTagList dataForAllSlots = parentNBTTagCompound.getTagList("Items", NBT_TYPE_COMPOUND);

        Arrays.fill(items, ItemStack.EMPTY);           // set all slots to empty EMPTY_ITEM
        for (int i = 0; i < dataForAllSlots.tagCount(); ++i) {
            NBTTagCompound dataForOneSlot = dataForAllSlots.getCompoundTagAt(i);
            int slotIndex = dataForOneSlot.getByte("Slot") & 255;

            if (slotIndex >= 0 && slotIndex < this.items.length) {
                this.items[slotIndex] = new ItemStack(dataForOneSlot);
            }
        }
    }

    // set all slots to empty
    @Override
    public void clear() {
        Arrays.fill(items, ItemStack.EMPTY);  //empty item
    }

    // will add a key for this container to the lang file so we can name it in the GUI
    @Override
    public String getName() {
        return "container.mbe30_inventory_basic.name";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    // standard code to look up what the human-readable name is
    @Override
    public ITextComponent getDisplayName() {
        return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
    }

    // -----------------------------------------------------------------------------------------------------------
    // The following methods are not needed for this example but are part of IInventory so they must be implemented

    /**
     * This method removes the entire contents of the given slot and returns it.
     * Used by containers such as crafting tables which return any items in their slots when you close the GUI
     * @param slotIndex
     * @return
     */
    @Override
    public ItemStack removeStackFromSlot(int slotIndex) {
        ItemStack itemStack = getStackInSlot(slotIndex);
        if (!itemStack.isEmpty()) setInventorySlotContents(slotIndex, ItemStack.EMPTY);  //isEmpty(), EMPTY_ITEM
        return itemStack;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }
}

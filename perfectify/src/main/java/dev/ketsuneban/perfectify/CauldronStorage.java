package dev.ketsuneban.perfectify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class CauldronStorage extends SavedData {
    public record Contents(String mode, int color, String potion, String item, int units) {
        public static Contents colored(int color) {
            return new Contents("dye", color & 0xffffff, "", "", 0);
        }
        public static Contents potion(String potion, String item, int units) {
            return new Contents("potion", -1, potion, item, units);
        }
        public boolean isPotion() { return mode.equals("potion"); }
    }
    private final Map<Long, Contents> contents = new HashMap<>();
    private final Set<Long> initializedHuts = new HashSet<>();
    private static final Factory<CauldronStorage> FACTORY =
        new Factory<>(CauldronStorage::new, CauldronStorage::read);

    public static CauldronStorage of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "perfectify_cauldrons");
    }
    public Contents get(BlockPos pos) {
        return contents.get(pos.asLong());
    }
    public void put(BlockPos pos, Contents contents) {
        this.contents.put(pos.asLong(), contents);
        setDirty();
    }
    public void remove(BlockPos pos) {
        if (contents.remove(pos.asLong()) != null) setDirty();
    }
    public boolean hutSeen(BlockPos pos) { return initializedHuts.contains(pos.asLong()); }
    public void markHut(BlockPos pos) {
        if (initializedHuts.add(pos.asLong())) setDirty();
    }
    @Override
    public CompoundTag save(CompoundTag output, HolderLookup.Provider registries) {
        ListTag data = new ListTag();
        for (var entry : contents.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", entry.getKey());
            c.putString("mode", entry.getValue().mode());
            c.putInt("color", entry.getValue().color());
            c.putString("potion", entry.getValue().potion());
            c.putString("item", entry.getValue().item());
            c.putInt("units", entry.getValue().units());
            data.add(c);
        }
        output.put("contents", data);
        output.putLongArray("hut_seen", initializedHuts.stream().mapToLong(Long::longValue).toArray());
        return output;
    }
    private static CauldronStorage read(CompoundTag input, HolderLookup.Provider registries) {
        CauldronStorage data = new CauldronStorage();
        ListTag entries = input.getList("contents", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag tag = entries.getCompound(i);
            data.contents.put(tag.getLong("pos"), new Contents(
                tag.getString("mode"), tag.getInt("color"), tag.getString("potion"),
                tag.getString("item"), tag.getInt("units")
            ));
        }
        for (long p : input.getLongArray("hut_seen")) data.initializedHuts.add(p);
        return data;
    }
}

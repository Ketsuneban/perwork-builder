package dev.ketsuneban.perfectify;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Perfectify.MOD_ID)
public final class CauldronFeatures {
    private static int ticks;
    private CauldronFeatures() {}

    private static boolean isCauldron(BlockState s) {
        return s.is(Blocks.CAULDRON) || s.is(Blocks.WATER_CAULDRON);
    }
    private static int height(int units) {
        return Math.max(1, Math.min(3, (units + 1) / 2));
    }
    private static void update(ServerLevel level, BlockPos pos, CauldronStorage.Contents value) {
        var data = CauldronStorage.of(level);
        if (value == null || (value.isPotion() && value.units() <= 0)) {
            data.remove(pos);
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        } else {
            data.put(pos, value);
            if (value.isPotion()) {
                level.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, height(value.units())), 3);
            }
        }
    }
    private static Holder<Potion> potion(String id) {
        return BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(id))
            .map(holder -> (Holder<Potion>) holder).orElse(Potions.WATER);
    }
    private static String potionId(PotionContents contents) {
        return contents.potion()
            .map(p -> BuiltInRegistries.POTION.getKey(p.value()).toString())
            .orElse("minecraft:water");
    }
    private static String kind(ItemStack held) {
        if (held.is(Items.SPLASH_POTION)) return "splash";
        if (held.is(Items.LINGERING_POTION)) return "lingering";
        return "normal";
    }
    private static PotionContents potionData(CauldronStorage.Contents contents) {
        return new PotionContents(potion(contents.potion()));
    }
    private static void take(Player player, ItemStack from, int count) {
        if (!player.getAbilities().instabuild) from.shrink(count);
    }
    private static void give(Player player, ItemStack result) {
        if (!player.getInventory().add(result)) player.drop(result, false);
    }
    private static void interactDone(PlayerInteractEvent.RightClickBlock e) {
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        var level = e.getLevel();
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        if (!isCauldron(state)) return;

        ItemStack held = e.getItemStack();
        var item = held.getItem();
        boolean addingPotion = held.is(Items.POTION) || held.is(Items.SPLASH_POTION) ||
            held.is(Items.LINGERING_POTION);

        var incoming = held.get(DataComponents.POTION_CONTENTS);
        boolean actualPotion = addingPotion && incoming != null &&
            !incoming.is(Potions.WATER);

        CauldronStorage.Contents stored = null;
        if (level instanceof ServerLevel sl) stored = CauldronStorage.of(sl).get(pos);

        if (actualPotion) {
            interactDone(e);
            if (!(level instanceof ServerLevel sl)) return;
            var content = CauldronStorage.of(sl);
            String id = potionId(incoming);
            var old = content.get(pos);
            if (old != null && (!old.isPotion() || !old.potion().equals(id) || !old.item().equals(kind(held)))) {
                update(sl, pos, null);
                sl.levelEvent(2002, pos, incoming.getColor());
                take(e.getEntity(), held, 1);
                if (!e.getEntity().getAbilities().instabuild)
                if (!e.getEntity().getAbilities().instabuild)
                    give(e.getEntity(), new ItemStack(Items.GLASS_BOTTLE));
                return;
            }
            if (old == null && state.is(Blocks.WATER_CAULDRON)) {

                update(sl, pos, null);
                sl.levelEvent(2002, pos, incoming.getColor());
                take(e.getEntity(), held, 1);
                give(e.getEntity(), new ItemStack(Items.GLASS_BOTTLE));
                return;
            }
            int units = old != null ? old.units() : 0;
            if (units >= 6) return;
            update(sl, pos, CauldronStorage.Contents.potion(id, kind(held), Math.min(6, units + 2)));
            take(e.getEntity(), held, 1);
            give(e.getEntity(), new ItemStack(Items.GLASS_BOTTLE));
            return;
        }

        if (stored != null && stored.isPotion() && held.is(Items.GLASS_BOTTLE)) {
            interactDone(e);
            if (!(level instanceof ServerLevel sl) || stored.units() < 2) return;
            var bottleItem = stored.item().equals("splash") ? Items.SPLASH_POTION
                : stored.item().equals("lingering") ? Items.LINGERING_POTION : Items.POTION;
            ItemStack bottle = PotionContents.createItemStack(bottleItem, potion(stored.potion()));
            take(e.getEntity(), held, 1);
            give(e.getEntity(), bottle);
            update(sl, pos, CauldronStorage.Contents.potion(
                stored.potion(), stored.item(), stored.units() - 2));
            return;
        }

        if (stored != null && stored.isPotion() && held.is(Items.ARROW)) {
            interactDone(e);
            if (!(level instanceof ServerLevel sl)) return;
            int count = Math.min(held.getCount(), 16);
            int cost = stored.units() < 4 ? stored.units() : 1;
            if (held.getCount() > 32 && stored.units() >= 6) {
                count = Math.min(64, held.getCount()); cost = 6;
            } else if (held.getCount() > 16 && stored.units() >= 4) {
                count = Math.min(32, held.getCount()); cost = 4;
            }
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, count);
            arrows.set(DataComponents.POTION_CONTENTS, potionData(stored));
            take(e.getEntity(), held, count);
            give(e.getEntity(), arrows);
            update(sl, pos, CauldronStorage.Contents.potion(
                stored.potion(), stored.item(), stored.units() - cost));
            return;
        }

        if (state.is(Blocks.WATER_CAULDRON) && (stored == null || !stored.isPotion())
                && item instanceof DyeItem dye) {
            interactDone(e);
            if (!(level instanceof ServerLevel sl)) return;
            int color = dye.getDyeColor().getFireworkColor();
            if (stored != null && stored.mode().equals("dye")) {
                int prior = stored.color();
                color = (((((prior >> 16) & 255) + ((color >> 16) & 255)) / 2) << 16)
                    | (((((prior >> 8) & 255) + ((color >> 8) & 255)) / 2) << 8)
                    | ((((prior & 255) + (color & 255)) / 2));
            }
            CauldronStorage.of(sl).put(pos, CauldronStorage.Contents.colored(color));
            take(e.getEntity(), held, 1);
            return;
        }

        if (state.is(Blocks.WATER_CAULDRON) && stored != null &&
                stored.mode().equals("dye") && held.is(ItemTags.DYEABLE)) {
            interactDone(e);
            if (!(level instanceof ServerLevel sl)) return;
            ItemStack dyed = held.copy();
            dyed.set(DataComponents.DYED_COLOR, new DyedItemColor(stored.color(), true));
            dyed.setCount(1);
            take(e.getEntity(), held, 1);
            give(e.getEntity(), dyed);
            int levelValue = state.getValue(LayeredCauldronBlock.LEVEL);
            if (levelValue > 1) {
                sl.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, levelValue - 1), 3);
            } else update(sl, pos, null);
            return;
        }

        if (stored != null && stored.isPotion() && (held.is(Items.WATER_BUCKET) ||
                held.is(Items.LAVA_BUCKET) || (addingPotion && incoming != null && incoming.is(Potions.WATER)))) {
            interactDone(e);
            if (level instanceof ServerLevel sl) {
                update(sl, pos, null);
                sl.levelEvent(2002, pos, potionData(stored).getColor());
            }
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isCauldron(event.getState())) {
            CauldronStorage.of(level).remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (++ticks % 40 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            var data = CauldronStorage.of(level);
            for (var player : level.players()) {
                BlockPos inside = player.blockPosition();
                var content = data.get(inside);
                if (content != null && content.isPotion()
                        && level.getBlockState(inside).is(Blocks.WATER_CAULDRON)
                        && player.getY() < inside.getY() + 0.85D) {
                    for (var fx : potionData(content).getAllEffects()) {
                        int duration = Math.max(40, Math.min(160, fx.getDuration() / 10));
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            fx.getEffect(), duration, fx.getAmplifier()));
                    }
                }

                if (ticks % 120 == 0) initializeNearbyHut(level, player.blockPosition(), data);
            }
        }
    }

    private static void initializeNearbyHut(ServerLevel level, BlockPos center,
                                            CauldronStorage data) {
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos pos = center.offset(dx, dy, dz);
                if (!level.hasChunkAt(pos) ||
                    !level.getBlockState(pos).is(Blocks.CAULDRON) || data.hutSeen(pos))
                    continue;

                boolean crafting = false, mushroom = false;
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                    for (int y = -1; y <= 2; y++) {
                        BlockPos sample = pos.offset(x, y, z);
                        if (!level.hasChunkAt(sample)) continue;
                        BlockState s = level.getBlockState(sample);
                        if (s.is(Blocks.CRAFTING_TABLE)) crafting = true;
                        if (s.is(Blocks.POTTED_RED_MUSHROOM)) mushroom = true;
                    }
                }
                if (!crafting || !mushroom) continue;
                data.markHut(pos);

                int roll = level.random.nextInt(100);
                Holder<Potion> selected = roll < 25 ? Potions.HEALING
                    : roll < 50 ? Potions.POISON : roll < 65 ? Potions.SWIFTNESS
                    : roll < 75 ? Potions.SLOWNESS : roll < 85 ? Potions.WEAKNESS
                    : roll < 95 ? Potions.WATER_BREATHING : Potions.FIRE_RESISTANCE;
                int units = (1 + level.random.nextInt(3)) * 2;
                update(level, pos, CauldronStorage.Contents.potion(
                    BuiltInRegistries.POTION.getKey(selected.value()).toString(), "normal", units));
            }
    }
}

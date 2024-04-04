package maxsuperman.addon.meteorconstantiam.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class TreeGrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public Setting<BlockPos> plantPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("location")
        .description("The location of the block to grow tree on.")
        .defaultValue(BlockPos.ORIGIN)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between all operations in ticks")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<List<Block>> saplings = sgGeneral.add(new BlockListSetting.Builder()
        .name("saplings")
        .description("What to grow")
        .defaultValue(Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA,
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.CHERRY_SAPLING,
            Blocks.DARK_OAK_SAPLING)
        .build()
    );

    public TreeGrower() {
        super(Categories.World, "tree-grower", "Grows trees");
    }

    int timer = 0;
    String infoString = "Disabled";

    @Override
    public String getInfoString() {
        return infoString;
    }


    @Override
    public void onActivate() {
        timer = 0;
        super.onActivate();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(timer > 0) {
            timer--;
            return;
        } else {
            timer = delay.get();
        }
        assert mc.world != null;
        assert mc.player != null;
        var saplingPos = plantPos.get().up();
        var saplingBlock = mc.world.getBlockState(saplingPos);
        if(saplingBlock.isAir()) {
            var plantBlock = mc.world.getBlockState(plantPos.get());
            if(!plantBlock.isIn(BlockTags.DIRT)) {
                error("Can not plant on %s", Names.get(plantBlock.getBlock()));
                toggle();
                return;
            }
            FindItemResult sapling = findSapling();
            if (!sapling.found()) {
                error("No saplings in hotbar");
                toggle();
                return;
            }
            InvUtils.swap(sapling.slot(), false);
            var ryaw = Rotations.getYaw(plantPos.get());
            var rpitch = Rotations.getPitch(plantPos.get());
            infoString = "Rotating to place sapling";
            Rotations.rotate(ryaw, rpitch, () -> {
                infoString = "Placing sapling";
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(plantPos.get()), Direction.UP, plantPos.get(), false), 0));
            });
        } else if(saplingBlock.isIn(BlockTags.SAPLINGS)) {
            FindItemResult bonemeal = findBonemeal();
            if (!bonemeal.found()) {
                error("No bonemeal in hotbar");
                toggle();
                return;
            }
            InvUtils.swap(bonemeal.slot(), false);
            infoString = "Rotating to bonemeal sapling";
            var ryaw = Rotations.getYaw(saplingPos);
            var rpitch = Rotations.getPitch(saplingPos);
            Rotations.rotate(ryaw, rpitch, () -> {
                infoString = "Bonemealing sapling";
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(saplingPos), Direction.UP, saplingPos, false), 0));
            });
        } else {
            infoString = "Wrong block";
        }

    }
    private FindItemResult findBonemeal() {
        return InvUtils.findInHotbar(Items.BONE_MEAL);
    }

    private FindItemResult findSapling() {
        return InvUtils.findInHotbar(itemStack -> saplings.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }
}

package me.roundaround.invisibleframes.gametest;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("invisibleframes")
@PrefixGameTestTemplate(false)
public final class InvisibleFramesGameTests {
  @GameTest(template = "empty")
  public static void regularFrameToggles(GameTestHelper helper) {
    toggle(helper, false, InteractionHand.MAIN_HAND);
  }

  @GameTest(template = "empty")
  public static void glowFrameToggles(GameTestHelper helper) {
    toggle(helper, true, InteractionHand.MAIN_HAND);
  }

  @GameTest(template = "empty")
  public static void offhandToggles(GameTestHelper helper) {
    toggle(helper, false, InteractionHand.OFF_HAND);
  }

  private static void toggle(GameTestHelper helper, boolean glow, InteractionHand hand) {
    ItemFrame frame = frame(helper, glow, Direction.NORTH);
    frame.setItem(new ItemStack(Items.DIAMOND));
    frame.setRotation(3);
    Player player = player(helper, GameType.SURVIVAL, true);
    player.setItemInHand(hand, new ItemStack(Items.STICK, 5));
    helper.assertTrue(!frame.isInvisible(), "Frame should start visible");
    InteractionResult result = player.interactOn(frame, hand);
    helper.assertTrue(result == InteractionResult.CONSUME, "Server toggle must consume interaction");
    helper.assertTrue(frame.isInvisible(), "Sneak interaction must hide frame");
    helper.assertTrue(frame.getRotation() == 3, "Toggle must not rotate displayed item");
    helper.assertTrue(player.getItemInHand(hand).getCount() == 5, "Toggle must not consume held items");
    player.interactOn(frame, hand);
    helper.assertTrue(!frame.isInvisible(), "Second interaction must reveal frame");
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void normalRotationIsPreserved(GameTestHelper helper) {
    for (boolean glow : new boolean[]{false, true}) {
      ItemFrame frame = frame(helper, glow, Direction.NORTH);
      frame.setItem(new ItemStack(Items.DIAMOND));
      Player player = player(helper, GameType.SURVIVAL, false);
      player.interactOn(frame, InteractionHand.MAIN_HAND);
      helper.assertTrue(frame.getRotation() == 1 && !frame.isInvisible(), "Normal interaction must rotate");
      frame.discard();
    }
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void survivalInsertionIsPreserved(GameTestHelper helper) {
    insertion(helper, GameType.SURVIVAL, 2);
  }

  @GameTest(template = "empty")
  public static void creativeInsertionIsPreserved(GameTestHelper helper) {
    insertion(helper, GameType.CREATIVE, 3);
  }

  private static void insertion(GameTestHelper helper, GameType mode, int remaining) {
    for (boolean glow : new boolean[]{false, true}) {
      ItemFrame frame = frame(helper, glow, Direction.NORTH);
      Player player = player(helper, mode, true);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 3));
      player.interactOn(frame, InteractionHand.MAIN_HAND);
      helper.assertTrue(frame.getItem().is(Items.DIAMOND), "Sneaking must still insert into empty frame");
      helper.assertTrue(!frame.isInvisible(), "Insertion must not toggle visibility");
      helper.assertTrue(player.getMainHandItem().getCount() == remaining, "Vanilla item consumption must be preserved");
      frame.discard();
    }
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void removingItemRevealsFrame(GameTestHelper helper) {
    for (boolean glow : new boolean[]{false, true}) {
      ItemFrame frame = frame(helper, glow, Direction.NORTH);
      frame.setItem(new ItemStack(Items.DIAMOND));
      Player player = player(helper, GameType.SURVIVAL, true);
      player.interactOn(frame, InteractionHand.MAIN_HAND);
      frame.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
      helper.assertTrue(frame.getItem().isEmpty(), "Attack must remove displayed item");
      helper.assertTrue(!frame.isInvisible() && !frame.isRemoved(), "Empty frame must remain and be visible");
      frame.discard();
    }
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void commandRemovalRevealsFrame(GameTestHelper helper) {
    ItemFrame frame = frame(helper, false, Direction.NORTH);
    frame.setItem(new ItemStack(Items.DIAMOND));
    player(helper, GameType.SURVIVAL, true).interactOn(frame, InteractionHand.MAIN_HAND);
    frame.setItem(ItemStack.EMPTY);
    helper.assertTrue(!frame.isInvisible(), "Direct item removal must reveal frame");
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void visibilityPersistsThroughSerialization(GameTestHelper helper) {
    for (boolean glow : new boolean[]{false, true}) {
      ItemFrame frame = frame(helper, glow, Direction.NORTH);
      frame.setItem(new ItemStack(Items.DIAMOND));
      frame.setRotation(5);
      player(helper, GameType.SURVIVAL, true).interactOn(frame, InteractionHand.MAIN_HAND);
      CompoundTag saved = frame.saveWithoutId(new CompoundTag());
      frame.discard();
      ItemFrame restored = frame(helper, glow, Direction.NORTH);
      restored.load(saved);
      helper.assertTrue(restored.isInvisible(), "Saved invisibility must survive entity reload");
      helper.assertTrue(restored.getItem().is(Items.DIAMOND) && restored.getRotation() == 5,
          "Displayed item and rotation must survive entity reload");
      restored.discard();
    }
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void vanillaEntityDataCarriesVisibility(GameTestHelper helper) {
    ItemFrame frame = frame(helper, false, Direction.NORTH);
    frame.setItem(new ItemStack(Items.DIAMOND));
    // Clear initial updates, then inspect the actual data emitted by the toggle.
    frame.getEntityData().packDirty();
    Player player = player(helper, GameType.SURVIVAL, true);
    player.interactOn(frame, InteractionHand.MAIN_HAND);
    var values = frame.getEntityData().packDirty();
    helper.assertTrue(values != null && !values.isEmpty(), "Toggle must produce vanilla entity-data updates");
    var packet = new ClientboundSetEntityDataPacket(frame.getId(), values);
    ItemFrame replica = new ItemFrame(helper.getLevel(), frame.getPos(), Direction.NORTH);
    replica.getEntityData().assignValues(packet.packedItems());
    helper.assertTrue(replica.isInvisible(), "Vanilla entity-data update must carry invisibility");
    player.interactOn(frame, InteractionHand.MAIN_HAND);
    replica.getEntityData().assignValues(frame.getEntityData().packDirty());
    helper.assertTrue(!replica.isInvisible(), "Vanilla entity-data update must carry visibility reset");
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void mainHandConsumesBeforeOffhand(GameTestHelper helper) {
    ItemFrame frame = frame(helper, false, Direction.NORTH);
    frame.setItem(new ItemStack(Items.DIAMOND));
    Player player = player(helper, GameType.SURVIVAL, true);
    int interactions = 0;
    for (InteractionHand hand : InteractionHand.values()) {
      interactions++;
      if (player.interactOn(frame, hand).consumesAction()) {
        break;
      }
    }
    helper.assertTrue(interactions == 1 && frame.isInvisible(), "Consumed main-hand action must prevent a double toggle");
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void fixedFramesAreUnchanged(GameTestHelper helper) {
    ItemFrame frame = frame(helper, false, Direction.NORTH);
    frame.setItem(new ItemStack(Items.DIAMOND));
    CompoundTag saved = frame.saveWithoutId(new CompoundTag());
    saved.putBoolean("Fixed", true);
    frame.load(saved);
    InteractionResult result = player(helper, GameType.SURVIVAL, true).interactOn(frame, InteractionHand.MAIN_HAND);
    helper.assertTrue(result == InteractionResult.PASS && !frame.isInvisible(), "Fixed frames must not toggle");
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void protectionCancellationIsRespected(GameTestHelper helper) {
    ItemFrame frame = frame(helper, false, Direction.NORTH);
    frame.setItem(new ItemStack(Items.DIAMOND));
    Consumer<PlayerInteractEvent.EntityInteract> protection = event -> {
      if (event.getTarget() == frame) {
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
      }
    };
    NeoForge.EVENT_BUS.addListener(protection);
    try {
      InteractionResult result = player(helper, GameType.SURVIVAL, true).interactOn(frame, InteractionHand.MAIN_HAND);
      helper.assertTrue(result == InteractionResult.FAIL && !frame.isInvisible(), "Canceled protection event must prevent toggle");
    } finally {
      NeoForge.EVENT_BUS.unregister(protection);
    }
    helper.succeed();
  }

  @GameTest(template = "empty")
  public static void allFrameOrientationsToggle(GameTestHelper helper) {
    Player player = player(helper, GameType.SURVIVAL, true);
    for (Direction direction : Direction.values()) {
      for (boolean glow : new boolean[]{false, true}) {
        ItemFrame frame = frame(helper, glow, direction);
        frame.setItem(new ItemStack(Items.MAP));
        player.interactOn(frame, InteractionHand.MAIN_HAND);
        helper.assertTrue(frame.isInvisible(), "Frame must toggle on face " + direction);
        frame.discard();
      }
    }
    helper.succeed();
  }

  private static Player player(GameTestHelper helper, GameType mode, boolean sneaking) {
    Player player = helper.makeMockPlayer(mode);
    player.getAbilities().instabuild = mode.isCreative();
    player.setShiftKeyDown(sneaking);
    return player;
  }

  private static ItemFrame frame(GameTestHelper helper, boolean glow, Direction direction) {
    BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
    helper.getLevel().setBlockAndUpdate(pos.relative(direction.getOpposite()), Blocks.STONE.defaultBlockState());
    ItemFrame frame = glow
        ? new GlowItemFrame(helper.getLevel(), pos, direction)
        : new ItemFrame(helper.getLevel(), pos, direction);
    helper.getLevel().addFreshEntity(frame);
    return frame;
  }
}

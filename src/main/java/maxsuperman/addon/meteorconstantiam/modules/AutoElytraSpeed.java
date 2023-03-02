package maxsuperman.addon.meteorconstantiam.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.ChunkPos;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AutoElytraSpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> defaultSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("default-speed")
        .description("Speed without restrictions")
        .defaultValue(2.2)
        .build()
    );

    private final Setting<Double> restrictedSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("restricted-speed")
        .description("Speed with restrictions")
        .defaultValue(1.4)
        .build()
    );

    private final Setting<String> matchingMessage = sgGeneral.add(new StringSetting.Builder()
        .name("message-filter")
        .description("Regex for filtering speed limiter message")
        .defaultValue("restricted")
        .onChanged((String v) -> compileRegex())
        .build()
    );

    private final Setting<Boolean> suppressMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("suppress-message")
        .description("Cancels OverlayMessageS2CPacket if matched with filter")
        .defaultValue(false)
        .build()
    );

    public enum RecoverTrigger {
        TimerRecovery,
        ChunkBorderRecovery
    };

    public final Setting<RecoverTrigger> recoveryMode = sgGeneral.add(new EnumSetting.Builder<RecoverTrigger>()
        .name("recovery-mode")
        .description("When to relax restriction")
        .defaultValue(RecoverTrigger.TimerRecovery)
        .build()
    );

    private final Setting<Integer> timeoutTicks = sgGeneral.add(new IntSetting.Builder()
        .name("timeout-ticks")
        .description("For how long to restrict speed")
        .defaultValue(20)
        .visible(() -> recoveryMode.get() == RecoverTrigger.TimerRecovery)
        .build()
    );

    private final Setting<Boolean> suppressSound = sgGeneral.add(new BoolSetting.Builder()
        .name("suppress-sound")
        .description("Shuts annoying ding")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<SoundEvent>> soundsToSuppress = sgGeneral.add(new SoundEventListSetting.Builder()
        .name("suppressed-sounds")
        .description("Sounds to block.")
        .visible(suppressSound::get)
        .build()
    );

    private final Setting<Integer> soundDelay = sgGeneral.add(new IntSetting.Builder()
        .name("suppress-time")
        .description("For how long to ignore sounds")
        .defaultValue(20)
        .visible(suppressSound::get)
        .build()
    );

    public AutoElytraSpeed() {
        super(Categories.Movement, "auto-elytra-speed", "Automatically changes elytrafly speed");
    }

    private Pattern triggerPattern = null;

    public void compileRegex() {
        try {
            triggerPattern = Pattern.compile(matchingMessage.get());
        } catch (PatternSyntaxException e) {
            info(String.format("Pattern compile error: %s", e));
        }
    }

    private int timeoutLeft = 0;
    private int soundBlock = 0;

    private ChunkPos triggeredChunk = null;

    private void changeSpeed(double s) {
        Modules.get().get(ElytraFly.class).horizontalSpeed.set(s);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (soundBlock > 0) {
            soundBlock--;
        }
        if (recoveryMode.get() == RecoverTrigger.TimerRecovery) {
            if (timeoutLeft > 0) {
                timeoutLeft--;
                if (timeoutLeft == 0) {
                    changeSpeed(defaultSpeed.get());
                }
            }
        } else if (recoveryMode.get() == RecoverTrigger.ChunkBorderRecovery){
            assert mc.player != null;
            if (mc.player.getChunkPos() != triggeredChunk) {
                changeSpeed(defaultSpeed.get());
            }
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof OverlayMessageS2CPacket &&
            triggerPattern != null && triggerPattern.matcher(((OverlayMessageS2CPacket) event.packet).getMessage().getString()).find()) {
            if (recoveryMode.get() == RecoverTrigger.TimerRecovery) {
                if (timeoutLeft > 0) {
                    info(String.format("Got speed limited while already with restricted speed (%d)", timeoutLeft));
                    toggle();
                } else {
                    timeoutLeft = timeoutTicks.get();
                    changeSpeed(restrictedSpeed.get());
                    soundBlock = soundDelay.get();
                }
            } else if (recoveryMode.get() == RecoverTrigger.ChunkBorderRecovery) {
                triggeredChunk = mc.player.getChunkPos();
                changeSpeed(restrictedSpeed.get());
                soundBlock = soundDelay.get();
            }
            if (suppressMessage.get()) {
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (soundBlock == 0) {
            return;
        }
        for (SoundEvent sound : soundsToSuppress.get()) {
            if (sound.getId().equals(event.sound.getId())) {
                event.cancel();
                break;
            }
        }
    }
}

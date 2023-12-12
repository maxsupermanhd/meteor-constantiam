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

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AutoElytraSpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDetection = settings.createGroup("detection");

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

    private final Setting<Integer> timeoutTicks = sgGeneral.add(new IntSetting.Builder()
        .name("timeout-ticks")
        .description("For how long to restrict speed")
        .defaultValue(20)
        .build()
    );

    public enum restrictionDetectMode {
        HotbarMessage,
        Sound
    }

    private final Setting<Boolean> onlyInFlight = sgDetection.add(new BoolSetting.Builder()
        .name("only-in-flight")
        .description("Triggers restriction only while flying")
        .defaultValue(false)
        .build()
    );

    public final Setting<AutoElytraSpeed.restrictionDetectMode> detectionMode = sgDetection.add(new EnumSetting.Builder<AutoElytraSpeed.restrictionDetectMode>()
        .name("detection-mode")
        .description("Depending on what to trigger restricted mode")
        .defaultValue(AutoElytraSpeed.restrictionDetectMode.HotbarMessage)
        .build()
    );

    private final Setting<String> matchingMessage = sgDetection.add(new StringSetting.Builder()
        .name("message-filter")
        .description("Regex for filtering speed limiter message")
        .defaultValue("restricted")
        .onChanged((String v) -> compileRegex())
        .visible(() -> detectionMode.get() == restrictionDetectMode.HotbarMessage)
        .build()
    );

    private final Setting<Boolean> suppressMessage = sgDetection.add(new BoolSetting.Builder()
        .name("suppress-message")
        .description("Cancels OverlayMessageS2CPacket if matched with filter")
        .defaultValue(false)
        .visible(() -> detectionMode.get() == restrictionDetectMode.HotbarMessage)
        .build()
    );

    private final Setting<List<SoundEvent>> matchingSounds = sgDetection.add(new SoundEventListSetting.Builder()
        .name("sounds-filter")
        .description("Sounds to detect.")
        .visible(() -> detectionMode.get() == restrictionDetectMode.Sound)
        .build()
    );

    private final Setting<Boolean> suppressSound = sgDetection.add(new BoolSetting.Builder()
        .name("suppress-sound")
        .description("Cancels matching sounds")
        .defaultValue(false)
        .visible(() -> detectionMode.get() == restrictionDetectMode.Sound)
        .build()
    );

    private final Setting<Integer> soundDelay = sgDetection.add(new IntSetting.Builder()
        .name("suppress-time")
        .description("For how long to ignore sounds")
        .defaultValue(20)
        .visible(suppressSound::get)
        .visible(() -> detectionMode.get() == restrictionDetectMode.Sound)
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

    private void changeSpeed(double s) {
        Modules.get().get(ElytraFly.class).horizontalSpeed.set(s);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (soundBlock > 0) {
            soundBlock--;
        }
        if (timeoutLeft > 0) {
            timeoutLeft--;
            if (timeoutLeft == 0) {
                changeSpeed(defaultSpeed.get());
            }
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (    detectionMode.get() == restrictionDetectMode.HotbarMessage &&
                event.packet instanceof OverlayMessageS2CPacket &&
                triggerPattern != null &&
                triggerPattern.matcher(((OverlayMessageS2CPacket) event.packet).getMessage().getString()).find()) {
            if (timeoutLeft > 0) {
                info(String.format("Got speed limited while already with restricted speed (%d)", timeoutLeft));
                toggle();
            } else {
                timeoutLeft = timeoutTicks.get();
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
        if (mc.player == null) {
            return;
        }
        if (detectionMode.get() == restrictionDetectMode.Sound) {
            if (onlyInFlight.get()) {
                if (!mc.player.isFallFlying()) {
                    return;
                }
            }
            for (SoundEvent sound : matchingSounds.get()) {
                if (sound.getId().equals(event.sound.getId())) {
                    timeoutLeft = timeoutTicks.get();
                    changeSpeed(restrictedSpeed.get());
                    if (suppressSound.get()) {
                        event.cancel();
                    }
                    break;
                }
            }
        } else {
            if (soundBlock == 0) {
                return;
            }
            for (SoundEvent sound : matchingSounds.get()) {
                if (sound.getId().equals(event.sound.getId())) {
                    event.cancel();
                    break;
                }
            }
        }
    }
}

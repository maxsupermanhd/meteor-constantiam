package maxsuperman.addon.meteorconstantiam.modules;

import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public class AutoElytraSpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDetection = settings.createGroup("Detection");

    private final Setting<Double> defaultSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-speed")
        .description("Speed without restrictions")
        .defaultValue(2.3199)
        .decimalPlaces(4)
        .build()
    );

    private final Setting<Double> speedMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-multiplier")
        .description("Speed multiplier with restrictions")
        .defaultValue(0.912)
        .sliderRange(0.0, 1.0)
        .build()
    );

    private final Setting<Double> cutoffTps = sgGeneral.add(new DoubleSetting.Builder()
        .name("cutoff-tps")
        .description("Minimum TPS to restrict speed")
        .defaultValue(8.0)
        .sliderRange(0.0, 20.0)
        .build()
    );

    private final Setting<Integer> refreshRate = sgGeneral.add(new IntSetting.Builder()
        .name("refresh-rate")
        .description("How often should we set the speed, measured in ticks")
        .defaultValue(50)
        .sliderRange(10, 200)
        .build()
    );

    private final Setting<Boolean> onlyInFlight = sgDetection.add(new BoolSetting.Builder()
        .name("only-in-flight")
        .description("Triggers restriction only while flying")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> suppressSound = sgDetection.add(new BoolSetting.Builder()
        .name("suppress-sound")
        .description("Cancels matching sounds")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<SoundEvent>> matchingSounds = sgDetection.add(new SoundEventListSetting.Builder()
        .name("sounds-filter")
        .description("Sounds to detect.")
        .build()
    );

    public AutoElytraSpeed() {
        super(Categories.Movement, "auto-elytra-speed", "Automatically changes elytrafly speed");
    }

    private int timeoutLeft = 0;

    private void changeSpeed(double s) {
        Modules.get().get(ElytraFly.class).horizontalSpeed.set(s);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (onlyInFlight.get() && !mc.player.isFallFlying()) {
            return;
        }
        timeoutLeft--;
        if (timeoutLeft <= 0) {
            float tps = TickRate.INSTANCE.getTickRate();
            double speed = defaultSpeed.get();
            if (tps >= cutoffTps.get()) {
                speed *= tps / 20.0 * speedMultiplier.get();
            }
            changeSpeed(speed);
            timeoutLeft = refreshRate.get();
        }
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (mc.player == null) {
            return;
        }
        if (onlyInFlight.get() && !mc.player.isFallFlying()) {
            return;
        }
        for (SoundEvent sound : matchingSounds.get()) {
            if (sound.getId().equals(event.sound.getId())) {
                if (suppressSound.get()) {
                    event.cancel();
                }
                break;
            }
        }
    }
}

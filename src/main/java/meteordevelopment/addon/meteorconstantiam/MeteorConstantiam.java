package meteordevelopment.addon.meteorconstantiam;

import meteordevelopment.addon.meteorconstantiam.modules.ConstElytraBoost;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class MeteorConstantiam extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
//    public static final Category CATEGORY = new Category("Example");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        // Modules
        Modules.get().add(new ConstElytraBoost());
    }

    @Override
    public void onRegisterCategories() {
//        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "meteordevelopment.addon.meteorconstantiam";
    }
}

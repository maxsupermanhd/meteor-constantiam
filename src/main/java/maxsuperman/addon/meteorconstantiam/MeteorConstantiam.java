package maxsuperman.addon.meteorconstantiam;

import maxsuperman.addon.meteorconstantiam.modules.AutoElytraSpeed;
import maxsuperman.addon.meteorconstantiam.modules.TreeGrower;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.lang.invoke.MethodHandles;

public class MeteorConstantiam extends MeteorAddon {
//    public static final Category CATEGORY = new Category("Example");

    @Override
    public void onInitialize() {
        MeteorClient.EVENT_BUS.registerLambdaFactory("maxsuperman.addon.meteorconstantiam", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        Modules.get().add(new AutoElytraSpeed());
        Modules.get().add(new TreeGrower());
    }

    @Override
    public void onRegisterCategories() {
//        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "maxsuperman.addon.meteorconstantiam";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("maxsupermanhd", "meteor-villager-roller");
    }
}

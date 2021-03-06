package carpet.forge.mixin;

import carpet.forge.CarpetServer;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_coreMixin
{
    @Inject(
            method = "tick",
            at = @At(value = "FIELD", ordinal = 0, shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/server/MinecraftServer;tickCounter:I")
    )
    private void onTick(CallbackInfo ci)
    {
        CarpetServer.tick((MinecraftServer)(Object)this);
    }
    
    // Dedicated server only
    @Inject(method = "loadAllWorlds", at = @At("HEAD"))
    private void onLoadAllWorlds(String saveName, String worldNameIn, long seed, WorldType type, String generatorOptions, CallbackInfo ci)
    {
        CarpetServer.onServerLoaded((MinecraftServer) (Object) this);
    }
}

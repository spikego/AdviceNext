package cn.advicenext.mixin.authlib;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;

@Mixin(value = MinecraftClient.class, remap = false)
public abstract class MixinMinecraftClient {
}

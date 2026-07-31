package cn.advicenext.mixin.minecraft.texture;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerSkinTextureDownloader.class)
public class MixinPlayerSkinTextureDownloader {

    /**
     * 修复某些离线/第三方皮肤 URL 末尾带逗号导致 404 的问题。
     */
    @ModifyExpressionValue(
            method = "method_65866",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/AssetInfo$SkinAssetInfo;url()Ljava/lang/String;")
    )
    private String fixTextureUrl(String original) {
        if (original != null) {
            String trimmed = original.stripTrailing();
            while (trimmed.endsWith(",") || trimmed.endsWith(";")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).stripTrailing();
            }
            return trimmed;
        }
        return original;
    }
}
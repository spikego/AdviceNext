package cn.advicenext.mixin.blaze3d;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipeline.Builder.class)
public interface RenderPipelineBuilderAccessor {
    @Invoker("withSnippet")
    void advicenext$withSnippet(RenderPipeline.Snippet snippet);
}
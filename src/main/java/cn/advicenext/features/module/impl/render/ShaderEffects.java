package cn.advicenext.features.module.impl.render;

import cn.advicenext.event.impl.Render2DEvent;
import cn.advicenext.features.module.Category;
import cn.advicenext.features.module.Module;
import cn.advicenext.features.value.BooleanSetting;
import cn.advicenext.features.value.slider.FloatSetting;
import cn.advicenext.features.value.slider.IntSetting;
import cn.advicenext.render.shader.ShaderEffect;
import cn.advicenext.render.shader.ShaderManager;

/**
 * 着色器效果模块
 */
public class ShaderEffects extends Module {
    // 模糊设置
    private final BooleanSetting enableBlur = new BooleanSetting("Blur", "Enable blur effect", true);
    private final FloatSetting blurRadius = new FloatSetting("Blur Radius", "Radius of blur effect", 5.0f, 20.0f, 1.0f, 0.5f);
    private final IntSetting blurIterations = new IntSetting("Blur Iterations", "Number of blur iterations", 2, 5, 1, 1);
    
    // 泛光设置
    private final BooleanSetting enableBloom = new BooleanSetting("Bloom", "Enable bloom effect", true);
    private final FloatSetting bloomThreshold = new FloatSetting("Bloom Threshold", "Brightness threshold for bloom", 0.7f, 1.0f, 0.0f, 0.05f);
    private final FloatSetting bloomIntensity = new FloatSetting("Bloom Intensity", "Intensity of bloom effect", 1.0f, 3.0f, 0.1f, 0.1f);
    
    // 着色器效果
    private ShaderEffect.BlurEffect blurEffect;
    private ShaderEffect.BloomEffect bloomEffect;
    private ShaderEffect.CompositeEffect compositeEffect;
    
    public ShaderEffects() {
        super("ShaderEffects", "Apply shader effects to the game", Category.RENDER);
        
        // 添加设置
        this.settings.add(enableBlur);
        if(enableBlur.getValue()) {
            this.settings.add(blurRadius);
            this.settings.add(blurIterations);
        }
        this.settings.add(enableBloom);
        if(enableBloom.getValue()) {
            this.settings.add(bloomThreshold);
            this.settings.add(bloomIntensity);
        }
    }
    
    @Override
    public void onEnable() {
        // 初始化着色器管理器
        ShaderManager.getInstance().init();
        
        // 创建着色器效果
        blurEffect = new ShaderEffect.BlurEffect(blurRadius.getValue(), blurIterations.getValue());
        bloomEffect = new ShaderEffect.BloomEffect(bloomThreshold.getValue(), bloomIntensity.getValue());
        compositeEffect = new ShaderEffect.CompositeEffect(blurEffect, bloomEffect);
    }
    
    @Override
    public void onDisable() {
        // 清理资源
        ShaderManager.getInstance().cleanup();
    }
    
    @Override
    public void onRender2D(Render2DEvent event) {
        // 更新效果参数
        blurEffect.setRadius(blurRadius.getValue());
        blurEffect.setIterations(blurIterations.getValue());
        bloomEffect.setThreshold(bloomThreshold.getValue());
        bloomEffect.setIntensity(bloomIntensity.getValue());
        
        // 应用效果
        if (enableBlur.getValue() && enableBloom.getValue()) {
            compositeEffect.apply();
        } else if (enableBlur.getValue()) {
            blurEffect.apply();
        } else if (enableBloom.getValue()) {
            bloomEffect.apply();
        }
    }
}
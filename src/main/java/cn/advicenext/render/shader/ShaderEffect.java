package cn.advicenext.render.shader;

import net.minecraft.client.MinecraftClient;

/**
 * 着色器效果基类
 */
public abstract class ShaderEffect {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected final ShaderManager shaderManager = ShaderManager.getInstance();
    
    /**
     * 应用着色器效果
     */
    public abstract void apply();
    
    /**
     * 模糊效果
     */
    public static class BlurEffect extends ShaderEffect {
        private float radius;
        private int iterations;
        
        /**
         * 创建模糊效果
         * @param radius 模糊半径
         * @param iterations 迭代次数
         */
        public BlurEffect(float radius, int iterations) {
            this.radius = radius;
            this.iterations = iterations;
        }
        
        @Override
        public void apply() {
            shaderManager.applyBlur(radius, iterations);
        }
        
        /**
         * 设置模糊半径
         * @param radius 半径
         */
        public void setRadius(float radius) {
            this.radius = radius;
        }
        
        /**
         * 设置迭代次数
         * @param iterations 迭代次数
         */
        public void setIterations(int iterations) {
            this.iterations = iterations;
        }
    }
    
    /**
     * 泛光效果
     */
    public static class BloomEffect extends ShaderEffect {
        private float threshold;
        private float intensity;
        
        /**
         * 创建泛光效果
         * @param threshold 亮度阈值
         * @param intensity 强度
         */
        public BloomEffect(float threshold, float intensity) {
            this.threshold = threshold;
            this.intensity = intensity;
        }
        
        @Override
        public void apply() {
            shaderManager.applyBloom(threshold, intensity);
        }
        
        /**
         * 设置亮度阈值
         * @param threshold 阈值
         */
        public void setThreshold(float threshold) {
            this.threshold = threshold;
        }
        
        /**
         * 设置泛光强度
         * @param intensity 强度
         */
        public void setIntensity(float intensity) {
            this.intensity = intensity;
        }
    }
    
    /**
     * 组合效果
     */
    public static class CompositeEffect extends ShaderEffect {
        private final ShaderEffect[] effects;
        
        /**
         * 创建组合效果
         * @param effects 要组合的效果
         */
        public CompositeEffect(ShaderEffect... effects) {
            this.effects = effects;
        }
        
        @Override
        public void apply() {
            for (ShaderEffect effect : effects) {
                effect.apply();
            }
        }
    }
}
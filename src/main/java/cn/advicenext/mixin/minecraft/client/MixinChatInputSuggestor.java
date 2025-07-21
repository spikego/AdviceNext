package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.features.command.CommandManager;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ChatInputSuggestor.class)
public class MixinChatInputSuggestor {

    @Shadow @Final
    private TextFieldWidget textField;

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String text = textField.getText();
        
        // 检查是否是命令前缀开头
        if (text.startsWith(CommandManager.getCommandPrefix())) {
            // 获取命令补全
            List<String> completions = CommandManager.getCompletions(text);
            
            if (!completions.isEmpty()) {
                // 使用反射获取ChatInputSuggestor实例
                ChatInputSuggestor suggestor = (ChatInputSuggestor) (Object) this;
                
                // 显示补全建议
                try {
                    // 使用反射调用showCommandSuggestions方法
                    java.lang.reflect.Method method = ChatInputSuggestor.class.getDeclaredMethod("showCommandSuggestions", List.class);
                    method.setAccessible(true);
                    method.invoke(suggestor, completions);
                    
                    // 取消原始方法执行
                    ci.cancel();
                } catch (Exception e) {
                    // 反射失败，继续执行原始方法
                }
            }
        }
    }
}
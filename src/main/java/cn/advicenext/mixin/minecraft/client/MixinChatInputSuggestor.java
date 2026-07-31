package cn.advicenext.mixin.minecraft.client;

import cn.advicenext.features.command.CommandManager;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class MixinChatInputSuggestor {

    @Shadow @Final
    private TextFieldWidget textField;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private boolean completingSuggestions;

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String text = textField.getText();

        if (!text.startsWith(CommandManager.getCommandPrefix())) {
            return;
        }

        List<String> completions = CommandManager.getCompletions(text);

        if (completions.isEmpty()) {
            return;
        }

        StringRange range = StringRange.between(0, text.length());
        List<Suggestion> suggestions = new ArrayList<>();
        for (String completion : completions) {
            suggestions.add(new Suggestion(range, completion));
        }

        pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, suggestions));
        completingSuggestions = false;

        ci.cancel();

        ((ChatInputSuggestor) (Object) this).show(false);
    }
}
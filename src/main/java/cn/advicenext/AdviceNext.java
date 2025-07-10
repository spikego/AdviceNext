package cn.advicenext;

import cn.advicenext.config.ConfigManager;
import cn.advicenext.features.command.CommandManager;
import cn.advicenext.features.module.ModuleManager;
import cn.advicenext.event.EventManager;
import cn.advicenext.script.ScriptManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdviceNext implements ModInitializer {
	public static final String MOD_ID = "advice-next";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("AdviceNext is initializing!");
		ModuleManager.initialize();
		EventManager.initialize();
		CommandManager.initialize();

		ConfigManager.getInstance().loadConfig("default");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ConfigManager.getInstance().saveConfig("default");
		}));



		// 加载Kotlin脚本
		ScriptManager.getInstance().loadScripts();

		// 注册关闭钩子
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ConfigManager.getInstance().saveConfig("default");
			ScriptManager.getInstance().unloadScripts();
		}));

	}
}
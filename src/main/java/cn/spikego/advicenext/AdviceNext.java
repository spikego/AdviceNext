package cn.spikego.advicenext;

import cn.spikego.advicenext.features.command.CommandManager;
import cn.spikego.advicenext.features.module.ModuleManager;
import cn.spikego.advicenext.event.EventManager;
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
	}
}
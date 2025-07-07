package cn.advicenext.script.api;

/**
 * AdviceNext Kotlin Script API Documentation
 * 
 * 脚本文件位置: ~/.advicenext/scripts/
 * 文件扩展名: .kts
 * 
 * ========================================
 * 基础API使用示例
 * ========================================
 * 
 * // 导入必要的类
 * import cn.advicenext.script.api.*
 * import cn.advicenext.event.*
 * import cn.advicenext.event.impl.*
 * import net.minecraft.client.gui.DrawContext
 * 
 * // 1. 通知系统
 * ScriptAPI.notify("标题", "消息内容")
 * ScriptAPI.notifySuccess("成功", "操作成功")
 * ScriptAPI.notifyWarning("警告", "注意事项")
 * ScriptAPI.notifyError("错误", "出现错误")
 * 
 * // 2. 模块控制
 * val hudModule = ScriptAPI.getModule("HUD")
 * ScriptAPI.toggleModule("HUD")
 * ScriptAPI.enableModule("Sprint")
 * ScriptAPI.disableModule("Fly")
 * 
 * // 3. 渲染API
 * class MyRenderScript {
 *     fun onRender2D(context: DrawContext) {
 *         RenderAPI.drawText(context, "Hello World", 10, 10)
 *         RenderAPI.drawText(context, "Colored Text", 10, 25, 0xFF00FF00)
 *         RenderAPI.drawRect(context, 50, 50, 100, 100, 0x80FF0000)
 *         RenderAPI.drawOutline(context, 50, 50, 100, 100, 0xFFFFFFFF)
 *         
 *         val width = RenderAPI.getStringWidth("Test")
 *         val height = RenderAPI.getFontHeight()
 *         val screenW = RenderAPI.getScreenWidth()
 *         val screenH = RenderAPI.getScreenHeight()
 *         val color = RenderAPI.getCurrentColor()
 *     }
 * }
 * 
 * ========================================
 * 自定义模块示例
 * ========================================
 * 
 * class MyCustomModule : ScriptModule("CustomModule", "My custom module") {
 *     override fun onScriptEnable() {
 *         ScriptAPI.notifySuccess("Module", "CustomModule enabled!")
 *     }
 *     
 *     override fun onScriptDisable() {
 *         ScriptAPI.notify("Module", "CustomModule disabled!")
 *     }
 *     
 *     @Listener
 *     fun onTick(event: TickEvent) {
 *         // 每tick执行的代码
 *         if (ScriptAPI.mc.player != null) {
 *             // 玩家相关操作
 *         }
 *     }
 *     
 *     @Listener
 *     fun onRender2D(event: Render2DEvent) {
 *         RenderAPI.drawText(event.context, "Custom Module Active", 10, 50)
 *     }
 * }
 * 
 * // 注册模块
 * val myModule = MyCustomModule()
 * ScriptAPI.registerEvent(myModule)
 * 
 * ========================================
 * 事件监听示例
 * ========================================
 * 
 * class EventListener {
 *     @Listener
 *     fun onTick(event: TickEvent) {
 *         // 每tick执行
 *     }
 *     
 *     @Listener
 *     fun onRender2D(event: Render2DEvent) {
 *         // 2D渲染
 *         RenderAPI.drawText(event.context, "Script Active", 10, 10)
 *     }
 *     
 *     @Listener
 *     fun onChat(event: ChatEvent) {
 *         // 聊天事件
 *         val message = event.message
 *         if (message.contains("test")) {
 *             ScriptAPI.notify("Chat", "Test message detected!")
 *         }
 *     }
 *     
 *     @Listener
 *     fun onPacket(event: PacketEvent) {
 *         // 数据包事件
 *         if (event.packet is SomePacketType) {
 *             // 处理特定数据包
 *         }
 *     }
 * }
 * 
 * val listener = EventListener()
 * ScriptAPI.registerEvent(listener)
 * 
 * ========================================
 * 完整脚本示例
 * ========================================
 * 
 * // AutoWalk.kts - 自动行走脚本
 * import cn.advicenext.script.api.*
 * import cn.advicenext.event.*
 * import cn.advicenext.event.impl.*
 * import net.minecraft.client.option.KeyBinding
 * 
 * class AutoWalkModule : ScriptModule("AutoWalk", "Automatically walks forward") {
 *     private var walking = false
 *     
 *     override fun onScriptEnable() {
 *         walking = true
 *         ScriptAPI.notifySuccess("AutoWalk", "Started walking")
 *     }
 *     
 *     override fun onScriptDisable() {
 *         walking = false
 *         ScriptAPI.mc.options.forwardKey.pressed = false
 *         ScriptAPI.notify("AutoWalk", "Stopped walking")
 *     }
 *     
 *     @Listener
 *     fun onTick(event: TickEvent) {
 *         if (walking && ScriptAPI.mc.player != null) {
 *             ScriptAPI.mc.options.forwardKey.pressed = true
 *         }
 *     }
 *     
 *     @Listener
 *     fun onRender2D(event: Render2DEvent) {
 *         if (walking) {
 *             RenderAPI.drawText(event.context, "AutoWalk: ON", 10, 100, 0xFF00FF00)
 *         }
 *     }
 * }
 * 
 * val autoWalk = AutoWalkModule()
 * ScriptAPI.registerEvent(autoWalk)
 * 
 * ========================================
 * 可用的事件类型
 * ========================================
 * 
 * - TickEvent: 每游戏tick触发
 * - Render2DEvent: 2D渲染时触发
 * - ChatEvent: 聊天消息时触发
 * - PacketEvent: 网络数据包时触发
 * - KeyboardEvent: 键盘输入时触发
 * 
 * ========================================
 * 可用的API类
 * ========================================
 * 
 * - ScriptAPI: 基础API (通知、模块控制、事件注册)
 * - RenderAPI: 渲染API (文本、矩形、颜色等)
 * - ScriptModule: 自定义模块基类
 * - ScriptEvent: 自定义事件基类
 * 
 * ========================================
 * 注意事项
 * ========================================
 * 
 * 1. 脚本文件必须以.kts结尾
 * 2. 使用@Listener注解标记事件处理方法
 * 3. 记得注册事件监听器: ScriptAPI.registerEvent(listener)
 * 4. 脚本错误会在游戏中显示通知
 * 5. 脚本在游戏启动时自动加载
 * 6. 可以通过重新加载脚本来更新代码
 */
public class ScriptDocumentation {
    // 这个类仅用于文档说明，不包含实际代码
}
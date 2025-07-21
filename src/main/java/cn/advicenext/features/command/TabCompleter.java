package cn.advicenext.features.command;

import java.util.List;

/**
 * 命令补全接口，实现此接口的命令可以提供参数补全功能
 */
public interface TabCompleter {
    /**
     * 获取命令参数的补全建议
     * 
     * @param args 当前已输入的参数（不包括正在输入的参数）
     * @param currentArg 当前正在输入的参数
     * @return 补全建议列表
     */
    List<String> getCompletions(String[] args, String currentArg);
}
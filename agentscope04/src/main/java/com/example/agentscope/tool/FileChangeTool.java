package com.example.agentscope.tool;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 写一个FileChangeTool类去判断 ASK 和 DENY
 * 示例工具类FileChangeTool 类继承 AgentScope 的 ToolBase，在文件写入前增加权限检查。
 */
public class FileChangeTool extends ToolBase {

    private static final Logger log = LoggerFactory.getLogger(FileChangeTool.class);

    public static final String TOOL_NAME = "request_file_change";
    private static final List<String> WRITE_OPERATIONS =
            List.of("create", "write", "update", "modify");

    private final Path projectRoot;
    private final Path notesRoot;

    public FileChangeTool(Path projectRoot) {
        super(ToolBase.builder()
                .name(TOOL_NAME)
                .description("Create or update a UTF-8 text file under the current project's notes directory. Use it only when the user explicitly asks to write a file. Deletion requests are denied.")
                // 注册工具参数结构，供模型生成 operation、path 和 content。
                .inputSchema(inputSchema())
                .readOnly(false)
                .concurrencySafe(false));
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.notesRoot = this.projectRoot.resolve("notes").normalize();
        log.info("FileChangeTool initialized. tool={}, writableDirectory=notes/", TOOL_NAME);
    }

    /**
     * ToolBase 提供工具扩展入口，checkPermissions() 负责返回权限结果。至于“只能写 notes/”“删除必须拒绝”“合法写入需要确认”，由你这个工具自己定义。
     */
    @Override
    public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
        // 这里只判断本次工具调用能否执行，不修改文件。
        String operation = value(toolInput, "operation").toLowerCase(Locale.ROOT);
        String path = value(toolInput, "path");
        log.info("Checking file change permission. operation={}, path={}", operation, path);

        if ("delete".equals(operation) || "remove".equals(operation)) {
            log.warn("File change denied. reason=delete_not_allowed, operation={}, path={}", operation, path);
            return Mono.just(PermissionDecision.builder()
                    .behavior(PermissionBehavior.DENY)
                    .message("删除文件不允许由 Agent 自动执行：" + path)
                    .decisionReason("safety: delete operation is denied")
                    .build());
        }
        if (!WRITE_OPERATIONS.contains(operation)) {
            log.warn("File change denied. reason=unsupported_operation, operation={}, path={}", operation, path);
            return Mono.just(PermissionDecision.builder()
                    .behavior(PermissionBehavior.DENY)
                    .message("不支持的文件操作：" + operation)
                    .decisionReason("safety: unsupported file operation")
                    .build());
        }
        try {
            resolveTarget(path);
            log.info("File change requires confirmation. operation={}, path={}", operation, path);
            return Mono.just(PermissionDecision.builder()
                    .behavior(PermissionBehavior.ASK)
                    .message("写入类操作需要人工确认：" + path)
                    .decisionReason("safety: write operation requires approval")
                    .build());
        } catch (IllegalArgumentException ex) {
            log.warn("File change denied. reason=unsafe_path, operation={}, path={}, detail={}", operation, path, ex.getMessage());
            return Mono.just(PermissionDecision.builder()
                    .behavior(PermissionBehavior.DENY)
                    .message(ex.getMessage())
                    .decisionReason("safety: path is outside notes directory")
                    .build());
        }
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        // 权限放行或用户确认后，框架才会调用这个方法。
        return Mono.fromCallable(
                        () -> {
                            Map<String, Object> input =
                                    param.getInput() == null ? Map.of() : param.getInput();
                            String operation =
                                    value(input, "operation").toLowerCase(Locale.ROOT);
                            String path = value(input, "path");
                            String content = value(input, "content");
                            Path target = resolveTarget(path);

                            log.info("Executing confirmed file change. operation={}, path={}, contentLength={}",
                                    operation,
                                    path,
                                    content.length());
                            prepareSafeParent(target);
                            Files.writeString(
                                    target,
                                    content,
                                    StandardCharsets.UTF_8,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING,
                                    StandardOpenOption.WRITE);

                            String callId =
                                    param.getToolUseBlock() == null
                                            ? null
                                            : param.getToolUseBlock().getId();
                            String relativePath =
                                    projectRoot.relativize(target).toString().replace('\\', '/');
                            log.info("File change completed. operation={}, path={}, contentLength={}",
                                    operation,
                                    relativePath,
                                    content.length());
                            String result = "用户已确认，文件已成功写入：" + relativePath;
                            return new ToolResultBlock(
                                    callId,
                                    getName(),
                                    TextBlock.builder().text(result).build());
                        })
                .doOnError(ex -> log.error("File change execution failed. reason={}", ex.getMessage(), ex));
    }

    private void prepareSafeParent(Path target) throws IOException {
        // 防止通过符号链接把实际写入位置绕到 notes/ 之外。
        if (Files.exists(notesRoot, LinkOption.NOFOLLOW_LINKS)
                && Files.isSymbolicLink(notesRoot)) {
            throw new IllegalArgumentException("notes 目录不能是符号链接");
        }
        Files.createDirectories(notesRoot);

        Path current = notesRoot;
        Path relativeParent = notesRoot.relativize(target.getParent());
        for (Path segment : relativeParent) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current)
                        || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalArgumentException("目标目录包含不安全的路径节点：" + current);
                }
            } else {
                Files.createDirectory(current);
            }
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(target)) {
            throw new IllegalArgumentException("目标文件不能是符号链接：" + target);
        }
    }

    private static String value(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value == null ? "" : String.valueOf(value).strip();
    }

    private Path resolveTarget(String pathValue) {
        // 标准化相对路径，并确保最终位置仍在 notes/ 目录内。
        if (pathValue == null || pathValue.isBlank()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        Path requested = Path.of(pathValue);
        if (requested.isAbsolute()) {
            throw new IllegalArgumentException("只允许使用 notes/ 下的相对路径");
        }
        Path target = projectRoot.resolve(requested).normalize();
        if (!target.startsWith(notesRoot) || target.equals(notesRoot)) {
            throw new IllegalArgumentException("只允许写入 notes/ 目录：" + pathValue);
        }
        return target;
    }

    private static Map<String, Object> inputSchema() {
        // 描述模型调用工具时可以提交的参数、类型和必填字段。
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "operation", Map.of(
                                "type", "string",
                                "description", "Change type, such as create, update, write, modify, or delete."),
                        "path", Map.of(
                                "type", "string",
                                "description", "Project-relative target path. It must be under notes/."),
                        "content", Map.of(
                                "type", "string",
                                "description", "Complete UTF-8 text to write to the target file.")),
                "required", List.of("operation", "path", "content"));
    }
}

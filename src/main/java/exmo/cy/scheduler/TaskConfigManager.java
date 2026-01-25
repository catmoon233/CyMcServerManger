package exmo.cy.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import exmo.cy.config.Constants;
import exmo.cy.util.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 计划任务配置管理器
 * 负责保存和加载计划任务配置
 */
public class TaskConfigManager {
    private static final String TASKS_CONFIG_FILE = "scheduled_tasks.json";
    private final Gson gson;
    
    public TaskConfigManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
    
    /**
     * 保存任务列表到文件
     */
    public void saveTasks(List<ScheduledTask> tasks) {
        try (FileWriter writer = new FileWriter(TASKS_CONFIG_FILE)) {
            gson.toJson(tasks, writer);
            Logger.info("已保存 " + tasks.size() + " 个计划任务到配置文件");
        } catch (IOException e) {
            Logger.error("保存计划任务配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文件加载任务列表
     */
    public List<ScheduledTask> loadTasks() {
        Path path = Paths.get(TASKS_CONFIG_FILE);
        if (!Files.exists(path)) {
            Logger.info("计划任务配置文件不存在，返回空列表");
            return new CopyOnWriteArrayList<>();
        }
        
        try (FileReader reader = new FileReader(TASKS_CONFIG_FILE)) {
            Type listType = new TypeToken<List<ScheduledTask>>(){}.getType();
            List<ScheduledTask> tasks = gson.fromJson(reader, listType);
            if (tasks == null) {
                return new CopyOnWriteArrayList<>();
            }
            Logger.info("已从配置文件加载 " + tasks.size() + " 个计划任务");
            return new CopyOnWriteArrayList<>(tasks);
        } catch (IOException e) {
            Logger.error("加载计划任务配置失败: " + e.getMessage(), e);
            return new CopyOnWriteArrayList<>();
        }
    }
    
    /**
     * 添加单个任务
     */
    public void addTask(ScheduledTask task) {
        List<ScheduledTask> tasks = loadTasks();
        tasks.add(task);
        saveTasks(tasks);
    }
    
    /**
     * 删除任务
     */
    public void removeTask(String taskId) {
        List<ScheduledTask> tasks = loadTasks();
        tasks.removeIf(task -> task.getTaskId().equals(taskId));
        saveTasks(tasks);
    }
}
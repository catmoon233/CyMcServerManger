package exmo.cy.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exmo.cy.config.Constants;
import exmo.cy.exception.ConfigurationException;
import exmo.cy.model.LaunchConfig;
import exmo.cy.model.Server;
import exmo.cy.util.FileUtils;
import exmo.cy.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 配置管理器
 * 负责服务器配置和启动配置的加载、保存和管理
 */
public class ConfigurationManager {
    
    private final Gson gson;
    private final Path configFilePath;
    private final Path lastLaunchConfigPath;
    
    /**
     * 构造函数
     */
    public ConfigurationManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configFilePath = Paths.get(Constants.CONFIG_FILE);
        this.lastLaunchConfigPath = Paths.get(Constants.LAST_LAUNCH_CONFIG);
    }
    
    /**
     * 加载所有服务器配置
     * @return 服务器列表
     * @throws ConfigurationException 如果加载失败
     */
    public List<Server> loadServers() throws ConfigurationException {
        if (!Files.exists(configFilePath)) {
            Logger.debug("配置文件不存在，返回空列表");
            return new ArrayList<>();
        }
        
        try (BufferedReader reader = Files.newBufferedReader(configFilePath)) {
            Server[] servers = gson.fromJson(reader, Server[].class);
            if (servers == null) {
                return new ArrayList<>();
            }
            
            List<Server> serverList = Arrays.asList(servers);
            Logger.info("成功加载 " + serverList.size() + " 个服务器配置");
            return serverList;
        } catch (IOException e) {
            throw new ConfigurationException("加载服务器配置失败", e);
        }
    }
    
    /**
     * 保存单个服务器配置
     * 如果服务器已存在则更新，否则添加
     * @param server 服务器配置
     * @throws ConfigurationException 如果保存失败
     */
    public void saveServer(Server server) throws ConfigurationException {
        if (server == null || !server.isValid()) {
            throw new ConfigurationException("服务器配置无效");
        }
        
        List<Server> servers = loadServers();
        
        // 查找并更新现有服务器，或添加新服务器
        boolean found = false;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getName().equals(server.getName())) {
                servers.set(i, server);
                found = true;
                break;
            }
        }
        
        if (!found) {
            servers.add(server);
        }
        
        saveServers(servers);
        Logger.info("保存服务器配置: " + server.getName());
    }
    
    /**
     * 保存所有服务器配置
     * @param servers 服务器列表
     * @throws ConfigurationException 如果保存失败
     */
    public void saveServers(List<Server> servers) throws ConfigurationException {
        if (servers == null) {
            throw new ConfigurationException("服务器列表不能为null");
        }
        
        try (BufferedWriter writer = Files.newBufferedWriter(configFilePath)) {
            writer.write(gson.toJson(servers.toArray()));
            Logger.debug("保存 " + servers.size() + " 个服务器配置到文件");
        } catch (IOException e) {
            throw new ConfigurationException("保存服务器配置失败", e);
        }
    }
    
    /**
     * 删除服务器配置
     * @param serverName 服务器名称
     * @throws ConfigurationException 如果删除失败
     */
    public void deleteServer(String serverName) throws ConfigurationException {
        List<Server> servers = loadServers();
        boolean removed = servers.removeIf(s -> s.getName().equals(serverName));
        
        if (removed) {
            saveServers(servers);
            Logger.info("删除服务器配置: " + serverName);
        } else {
            Logger.warn("未找到要删除的服务器: " + serverName);
        }
    }
    
    /**
     * 根据名称查找服务器
     * @param name 服务器名称
     * @return 服务器配置，如果不存在返回Optional.empty()
     * @throws ConfigurationException 如果查找失败
     */
    public Optional<Server> findServerByName(String name) throws ConfigurationException {
        return loadServers().stream()
            .filter(s -> s.getName().equals(name))
            .findFirst();
    }
    
    /**
     * 保存启动配置
     * @param config 启动配置
     * @throws ConfigurationException 如果保存失败
     */
    public void saveLastLaunchConfig(LaunchConfig config) throws ConfigurationException {
        if (config == null || !config.isValid()) {
            throw new ConfigurationException("启动配置无效");
        }
        
        try (BufferedWriter writer = Files.newBufferedWriter(lastLaunchConfigPath)) {
            writer.write(gson.toJson(config));
            Logger.debug("保存最后启动配置: " + config.getServerName());
        } catch (IOException e) {
            throw new ConfigurationException("保存启动配置失败", e);
        }
    }
    
    /**
     * 加载启动配置
     * @return 启动配置，如果不存在返回Optional.empty()
     * @throws ConfigurationException 如果加载失败
     */
    public Optional<LaunchConfig> loadLastLaunchConfig() throws ConfigurationException {
        if (!Files.exists(lastLaunchConfigPath)) {
            Logger.debug("启动配置文件不存在");
            return Optional.empty();
        }
        
        try (BufferedReader reader = Files.newBufferedReader(lastLaunchConfigPath)) {
            LaunchConfig config = gson.fromJson(reader, LaunchConfig.class);
            if (config != null && config.isValid()) {
                Logger.debug("加载最后启动配置: " + config.getServerName());
                return Optional.of(config);
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new ConfigurationException("加载启动配置失败", e);
        }
    }
    
    /**
     * 检查服务器名称是否已存在
     * @param name 服务器名称
     * @return 如果存在返回true
     * @throws ConfigurationException 如果检查失败
     */
    public boolean serverExists(String name) throws ConfigurationException {
        return findServerByName(name).isPresent();
    }
}
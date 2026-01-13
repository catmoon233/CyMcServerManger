package exmo.cy.service;

import exmo.cy.exception.ServerOperationException;
import exmo.cy.model.ServerInstance;
import exmo.cy.util.Logger;

import java.io.*;
import java.util.function.Consumer;

/**
 * 进程管理器
 * 负责管理服务器进程的输入输出流
 */
public class ProcessManager {
    
    /**
     * 启动进程并设置输出监听
     * @param processBuilder 进程构建器
     * @return 服务器实例
     * @throws ServerOperationException 如果启动失败
     */
    public ServerInstance startProcess(ProcessBuilder processBuilder) throws ServerOperationException {
        try {
            Logger.info("启动进程: " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            
            ServerInstance instance = new ServerInstance();
            instance.setProcess(process);
            instance.setProcessInput(process.getOutputStream());
            
            // 启动输出监听线程
            startOutputGobbler(process.getInputStream(), System.out::println);
            startOutputGobbler(process.getErrorStream(), System.err::println);
            
            return instance;
        } catch (IOException e) {
            throw new ServerOperationException("启动进程失败", e);
        }
    }
    
    /**
     * 启动输出流读取线程
     * @param inputStream 输入流
     * @param consumer 输出消费者
     */
    private void startOutputGobbler(InputStream inputStream, Consumer<String> consumer) {
        Thread thread = new Thread(new StreamGobbler(inputStream, consumer));
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 向进程发送命令
     * @param instance 服务器实例
     * @param command 命令
     * @throws ServerOperationException 如果发送失败
     */
    public void sendCommand(ServerInstance instance, String command) throws ServerOperationException {
        if (instance == null || !instance.isRunning()) {
            throw new ServerOperationException("服务器实例无效或未运行");
        }
        
        try {
            OutputStream input = instance.getProcessInput();
            input.write((command + "\n").getBytes());
            input.flush();
            Logger.debug("发送命令到服务器: " + command);
        } catch (IOException e) {
            throw new ServerOperationException("发送命令失败", e);
        }
    }
    
    /**
     * 正常停止服务器
     * @param instance 服务器实例
     * @throws ServerOperationException 如果停止失败
     */
    public void stopServer(ServerInstance instance) throws ServerOperationException {
        if (instance == null || !instance.isRunning()) {
            throw new ServerOperationException("服务器实例无效或未运行");
        }
        
        try {
            // 发送stop命令
            sendCommand(instance, "stop");
            Logger.info("已发送停止命令到服务器");
        } catch (ServerOperationException e) {
            // 如果发送stop命令失败，尝试强制停止
            Logger.warn("发送停止命令失败，尝试强制停止");
            forceStopServer(instance);
        }
    }
    
    /**
     * 强制停止服务器
     * @param instance 服务器实例
     * @throws ServerOperationException 如果停止失败
     */
    public void forceStopServer(ServerInstance instance) throws ServerOperationException {
        if (instance == null || instance.getProcess() == null) {
            throw new ServerOperationException("服务器实例无效");
        }
        
        try {
            Logger.info("强制终止服务器进程");
            instance.getProcess().destroyForcibly();
            int exitCode = instance.getProcess().waitFor();
            Logger.info("服务器进程已终止，退出代码: " + exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerOperationException("等待进程终止时被中断", e);
        }
    }
    
    /**
     * 等待进程结束
     * @param instance 服务器实例
     * @return 退出代码
     * @throws ServerOperationException 如果等待失败
     */
    public int waitForProcess(ServerInstance instance) throws ServerOperationException {
        if (instance == null || instance.getProcess() == null) {
            throw new ServerOperationException("服务器实例无效");
        }
        
        try {
            return instance.getProcess().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerOperationException("等待进程结束时被中断", e);
        }
    }
    
    /**
     * 流读取器内部类
     * 用于读取进程的输出流
     */
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;
        
        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }
        
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException e) {
                Logger.debug("读取进程输出时出错: " + e.getMessage());
            }
        }
    }
}
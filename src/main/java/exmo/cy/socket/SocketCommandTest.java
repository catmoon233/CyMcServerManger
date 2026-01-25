package exmo.cy.socket;

import java.io.*;
import java.net.Socket;

/**
 * Socket命令测试工具
 * 用于测试TCP Socket的各种命令
 */
public class SocketCommandTest {
    
    private static final String HOST = "localhost";
    private static final int PORT = 5245;
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法: java SocketCommandTest <command>");
            System.out.println("例如: java SocketCommandTest \"help:\"");
            System.out.println("例如: java SocketCommandTest \"list-servers:\"");
            System.out.println("例如: java SocketCommandTest \"start-server:testServer\"");
            return;
        }
        
        String command = args[0];
        
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            // 发送命令
            writer.println(command);
            
            // 读取响应
            String response = reader.readLine();
            System.out.println("命令: " + command);
            System.out.println("响应: " + response);
            
        } catch (IOException e) {
            System.err.println("执行命令时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
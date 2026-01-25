package exmo.cy.socket;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * TCP Socket客户端示例
 * 用于测试TCP Socket服务器功能
 */
public class TcpClientExample {
    private static final String HOST = "localhost";
    private static final int PORT = 5245;
    
    public static void main(String[] args) {
        System.out.println("TCP Socket客户端示例");
        System.out.println("连接到: " + HOST + ":" + PORT);
        
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            
            // 读取欢迎消息
            String welcomeMessage = input.readLine();
            System.out.println("服务器响应: " + welcomeMessage);
            
            System.out.println("\n支持的命令:");
            System.out.println("  list-servers          - 列出所有服务器");
            System.out.println("  server-status:<name>  - 查询服务器状态");
            System.out.println("  health-check          - 健康检查");
            System.out.println("  quit                  - 退出");
            System.out.println("\n请输入命令:");
            
            String command;
            while (!(command = scanner.nextLine()).equalsIgnoreCase("quit")) {
                if (command.trim().isEmpty()) {
                    continue;
                }
                
                // 发送命令
                output.println(command);
                
                // 读取响应
                String response = input.readLine();
                System.out.println("服务器响应: " + response);
                
                System.out.println("\n请输入下一个命令 (或输入 'quit' 退出):");
            }
            
        } catch (IOException e) {
            System.err.println("客户端错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
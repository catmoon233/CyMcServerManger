package exmo.cy.socket;

import exmo.cy.util.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * TCP Socket客户端示例
 * 演示如何通过TCP Socket与服务器通信
 */
public class TcpSocketClientExample {
    
    private static final String HOST = "localhost";
    private static final int PORT = 5245;
    
    public static void main(String[] args) {
        Logger.println("TCP Socket客户端示例");
        Logger.println("连接到: " + HOST + ":" + PORT);
        
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            // 读取欢迎消息
            String welcomeResponse = reader.readLine();
            Logger.println("服务器响应: " + welcomeResponse);
            
            Scanner scanner = new Scanner(System.in);
            
            Logger.println("\n输入命令 (输入 'help' 查看命令列表，输入 'quit' 退出):");
            
            String inputLine;
            while (!(inputLine = scanner.nextLine()).equalsIgnoreCase("quit")) {
                if (inputLine.trim().isEmpty()) {
                    continue;
                }
                
                // 发送命令到服务器
                writer.println(inputLine);
                
                // 读取响应
                String response = reader.readLine();
                Logger.println("服务器响应: " + response);
                
                if ("quit".equalsIgnoreCase(inputLine.trim())) {
                    break;
                }
                
                Logger.print("\n请输入命令: ");
            }
            
        } catch (IOException e) {
            System.err.println("连接服务器时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        Logger.println("客户端已断开连接");
    }
}
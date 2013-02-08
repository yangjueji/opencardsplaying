package sgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SGSServer {
	
	//玩家名链表
	String users[] = new String[8];
	
	//玩家最大的数目
	final int MAXUSERNUM = 8;
	//进入房间的玩家名数目
	int userNumber = 0;
	//玩家到房间位置编号的map
	Map<String,Integer> userRoomIndex = new HashMap<String, Integer>();
	
	//flash的安全策略
	final String xml = "<cross-domain-policy>"
				+ "<allow-access-from domain=\"*\" to-ports=\"2901,8888\" />"
    			+ "</cross-domain-policy>";
	
	List<Socket> socketsList = new LinkedList<Socket>();
	
	
	
	public SGSServer(){
		ServerSocket server = null;
		for(int i = 0;i < users.length;i++){
			users[i] = null;
		}
		try {
			server = new ServerSocket(2901);
			while(true){
				try {
					Socket socket = null;
					socket = server.accept();
					
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter pw = new PrintWriter(socket.getOutputStream());
                    char[] buffer = new char[22];
                    br.read(buffer,0,22);
                    String head = new String(buffer);
                    //发送安全策略
                    if(head.equals("<policy-file-request/>"))
                    {
                    	pw.print(xml + "\0");
                    	pw.flush();
                    	pw.close();
                    	br.close();
                    }
                    //玩家进入房间，判断房间是否满了
                    else if(head.equals("enter in")){
                    	System.out.println("entering");
                    	if(userNumber >= MAXUSERNUM){
                    		System.out.println("full");
                    		pw.print("failed");
                    		pw.flush();
                    	}
                    	//找一个空位给新玩家
                    	else{
                    		System.out.println("can enter");
                    		userNumber ++;
                    		char[] userNameBuffer = new char[40];
                    		br.read(userNameBuffer,0,40);
                    		String userName = new String(userNameBuffer);
                    		int roomIndex = 0;
                    		for(;roomIndex < MAXUSERNUM;roomIndex ++){
                    			if(users[roomIndex] == null){
                    				users[roomIndex] = userName;
                    				break;
                    			}
                    		}
                    		pw.print("success");
                    		pw.print(roomIndex);
                    		pw.flush();
                    	}
                    	pw.close();
                    	br.close();
                    }
                    //开始游戏
                    else if(head.equals("start game")){
                    }
                    //下面的是测试用的
                    else{
						socketsList.add(socket);
						ServerRun sr = new ServerRun(socket,1);
						Thread thread = new Thread(sr);
						thread.start();
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		new SGSServer();
	}
	
	class ServerRun implements Runnable{
		Socket socket = null;
		PrintWriter out;
		BufferedReader in;
		int number;
		public ServerRun(Socket s , int num){
			socket = s;
			number = num;
		}
		@Override
		public void run() {
			System.out.println("server connected");
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream());
				out.println("test from server");
				out.flush();
				while(true){
					if(in.read()>0){
						String line;
						synchronized(socket){
							line = in.readLine();
						}
						System.out.println(line);
						synchronized(socketsList){
							for(Socket s : socketsList){
								synchronized(s){
									PrintWriter outOfS = new PrintWriter(s.getOutputStream());
									outOfS.print(line);
									outOfS.flush();
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
}

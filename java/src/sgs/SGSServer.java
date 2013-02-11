package sgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SGSServer {
	
	//玩家名链表
	String players[] = new String[8];
	
	//玩家最大的数目
	final int MAXplayerNUM = 8;
	//进入房间的玩家名数目
	int playerNumber = 0;
	//玩家到房间位置编号的map
	Map<String,Integer> playerRoomIndex = new HashMap<String, Integer>();
	//房间请求的Socket列表
	Map<Integer,Socket> roomSockets = new HashMap<Integer,Socket>();
	
	//flash的安全策略
	String xml = "<cross-domain-policy>"
				+ "<allow-access-from domain=\"*\" to-ports=\"2901,8888\" />"
    			+ "</cross-domain-policy>";
	
	List<Socket> socketsList = new LinkedList<Socket>();

	ServerSocket server = null;
	
	int status;

	static final int READY = 1;
	static final int INGAME = 2;
	
	public SGSServer(){
		for(int i = 0;i < players.length;i++){
			players[i] = null;
		}
		try {
			server = new ServerSocket(2901);
		} catch (IOException e) {
			e.printStackTrace();
		}
		status = READY;
	}
	
	public void start(){
		KickOutTask kickOutTask = new KickOutTask();
		Thread koThread = new Thread(kickOutTask);
		koThread.start();
		while(true){
			try {
				Socket socket = null;
				socket = server.accept();
				socket.setSoTimeout(3000);
				
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pw = new PrintWriter(socket.getOutputStream());
                char[] buffer = new char[256];
                br.read(buffer,0,256);
                String request = new String(buffer);
                request = request.trim();
                //发送安全策略
                if(request.startsWith("<policy-file-request/>"))
                {
                	pw.print(xml + "\0");
                	pw.flush();
                	pw.close();
                	br.close();
                }
                //玩家进入房间，判断房间是否满了
                else if(request.startsWith("enter")){
                	System.out.println("entering");
                	if(playerNumber >= MAXplayerNUM){
                		System.out.println("full");
                		pw.print("failed,");
                		pw.flush();
                	}
                	//找一个空位给新玩家
                	else{
                		System.out.println("can enter");
                		playerNumber ++;
                		String splitStrs[] = request.split(",");
                		String playerName = splitStrs[1];
                		playerName = playerName.trim();
                		int roomIndex = 0;
                		for(;roomIndex < MAXplayerNUM;roomIndex ++){
                			if(players[roomIndex] == null){
                				players[roomIndex] = playerName;
                				break;
                			}
                		}
                		pw.print("success," + roomIndex +",");
                		pw.flush();
                		playerRoomIndex.put(playerName, roomIndex);
                		System.out.print(playerName + " enters room with index:" + roomIndex);
                	}
                	pw.close();
                	br.close();
                }
                else if(request.startsWith("sign")){
                	synchronized(roomSockets){
	                	String splitStrs[] = request.split(",");
	                	String playerName = splitStrs[1];
	                	playerName = playerName.trim();
                		System.out.println(playerName + " signs");
	                	assert(playerRoomIndex.containsKey(playerName));
	                	int playerIndex = playerRoomIndex.get(playerName);
	                	for(Socket s:roomSockets.values()){
	                		if(s.isConnected()){
	                    		PrintWriter tmppw = new PrintWriter(s.getOutputStream());
	                    		tmppw.println("playerenter," + playerName + "," + playerIndex + ",");
	                    		tmppw.flush();
	                		}
	                	}
	                	String enterString = "";
	                	for(String str:players){
	                		if(str!=null&&!str.equals(playerName)){
	                    		enterString += "playerenter," + str + "," + playerRoomIndex.get(str) + ",";
	                    		System.out.println(str);
	                		}
	                	}
                		PrintWriter tmppw = new PrintWriter(socket.getOutputStream());
                		tmppw.print(enterString);
                		tmppw.flush();
                		roomSockets.put(playerIndex, socket);
                	}
                }
                //开始游戏
                else if(request.equals("start")){
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
	}
	
	public static void main(String args[]){
		new SGSServer().start();
	}
	
	class KickOutTask implements Runnable{
		@Override
		public void run() {
			try{
				while(true){
					if(status == READY){
						synchronized(roomSockets){
							Set<Integer> removeIndexSet = new HashSet<Integer>();
							for(Entry<Integer, Socket> entry : roomSockets.entrySet()){
								Socket tmpSocket = entry.getValue();
								PrintWriter pw = new PrintWriter(tmpSocket.getOutputStream());
								BufferedReader br = new BufferedReader(new InputStreamReader(tmpSocket.getInputStream()));
								pw.println("check,");
								pw.flush();
								try{
									char buffer[] = new char[256];
									br.read(buffer, 0, 256);
									String str = new String(buffer);
									str = str.split(",")[0].trim();
//									System.out.println(str);
									if(!str.equals("here")){
										removeIndexSet.add(entry.getKey());
									}
								}
								catch(SocketException e){
									removeIndexSet.add(entry.getKey());
								}
								catch(SocketTimeoutException e){
									removeIndexSet.add(entry.getKey());
								}
							}
							for(Integer removeIndex:removeIndexSet){
			                	for(Socket socket:roomSockets.values()){
			                		if(socket.isConnected()){
			                    		PrintWriter tmppw = new PrintWriter(socket.getOutputStream());
			                    		tmppw.print("playerquit," + removeIndex + ",");
			                    		tmppw.flush();
			                		}
			                	}
			                	synchronized(players){
			                		players[removeIndex] = null;
			                		playerNumber --;
			                	}
								roomSockets.remove(removeIndex);
								synchronized(playerRoomIndex){
									Set<String> removePlayerSet = new HashSet<String>();
									for(Entry<String, Integer>entry : playerRoomIndex.entrySet()){
										if(entry.getValue().equals(removeIndex)){
											removePlayerSet.add(entry.getKey());
										}
									}
									for(String player:removePlayerSet){
										playerRoomIndex.remove(player);
									}
								}
							}
						}
					}
					Thread.yield();
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
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

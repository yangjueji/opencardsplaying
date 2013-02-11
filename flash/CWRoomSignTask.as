package
{
	import flash.events.*;
	import flash.net.*;
	import flash.text.*;
	import flash.system.*;
	public class CWRoomSignTask{
		private var ip:String;
		private var port:int;
		private var playerName:String;
		private var socket:Socket;
		private var debugInfo:TextField;
		private var playerEnterFun:Function;
		private var playerQuitFun:Function;
		
		public function CWRoomSignTask(ip:String,port:int,playerName:String){
			this.socket = new Socket();
			this.ip = ip;
			this.port = port;
			this.playerName = playerName;
			this.debugInfo = null;
			socket.addEventListener(Event.CONNECT,connectHandler);
			socket.addEventListener(Event.CLOSE, closeHandler);
			socket.addEventListener(IOErrorEvent.IO_ERROR, ioErrorHandler);
			socket.addEventListener(SecurityErrorEvent.SECURITY_ERROR, securityErrorHandler);
			socket.addEventListener(ProgressEvent.SOCKET_DATA, socketDataHandler);
		}
		
		public function setDebugInfo(debugInfo:TextField):void{
			this.debugInfo = debugInfo;
		}
		
		public function startTask():void{
			socket.connect(ip,port);
		}
		
		public function setPlayerEnterFunction(playerEnterFun:Function):void{
			this.playerEnterFun = playerEnterFun;
		}
		
		public function setPlayerQuitFunction(playerQuitFun:Function):void{
			this.playerQuitFun = playerQuitFun;
		}
		
		private function connectHandler(event:Event):void{
			showDebugInfo(event.toString());
			socket.writeUTF("sign,"+playerName+",");
			socket.flush();
		}
		
		private function socketDataHandler(event:ProgressEvent):void{
			showDebugInfo(event.toString());
			var bufferString:String = socket.readUTFBytes(socket.bytesAvailable);
			showDebugInfo(bufferString);
			var requestArray:Array = bufferString.split(",");
			trace(requestArray.length);
			for(var i:uint = 0;i<requestArray.length-1;i++){
				showDebugInfo(requestArray[i]);
				if(requestArray[i] == "playerenter"){
					var playerEnterName:String = requestArray[i+1];
					var playerEnterIndex:int = requestArray[i+2];
					if(this.playerEnterFun!=null){
						this.playerEnterFun(playerEnterName, playerEnterIndex);
					}
					i = i+2;
				}
				else if(requestArray[i] == "playerquit"){
					playerEnterIndex = requestArray[i+1];
					if(this.playerQuitFun!=null){
						this.playerQuitFun(playerEnterIndex);
					}
					i = i+2;
				}
				else if(requestArray[i] == "check"){
					trace("writen");
					socket.writeUTF("here,\n");
					socket.flush();
				}
			}
		}
		
		private function ioErrorHandler(event:IOErrorEvent):void {
			showDebugInfo(event.toString());
			socket.close();
    	}

    	private function securityErrorHandler(event:SecurityErrorEvent):void {
			showDebugInfo(event.toString());
			socket.close();
    	}
		
		private function closeHandler(event:Event):void {
			showDebugInfo(event.toString());
			socket.close();
    	}
		
		private function showDebugInfo(info:String):void {
			if(debugInfo!=null){
				debugInfo.text = info;
			}
			trace(info);
		}
	}
}

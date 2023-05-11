# 操作命令

**启动** ：`java -jar Main.jar`

***************************************************************************************

help:        Show help             
list:        List online friends   
task:        List all sending tasks
exit:        Exit the chat system  
connect:     Connect with someone       connect@ip:port       e.g., connect@127.0.0.1:7777
msg:         Send a message             msg@name@content      e.g., msg@wang@hello        
push:        Send a file                put@name@file         e.g., push@wang@111.txt     
pause:       Pause a sending task       pause@taskNum         e.g., pause@1               
restart:     restart a sending task     restart@taskNum       e.g., restart@1             

***************************************************************************************

完成功能：

- 连接、查询、私聊好友
- 发送文件，可暂停，可继续发送。
- 查询正在发送文件的任务列表，包括发送方，文件名，接收方，发送进度。
- 接受文件，显示接受进度条。
  - 待解决：显示的进度条会刷新终端，与终端输入命令相冲突。
- 心跳检测：用定时每隔5秒向好友发送UDP的心跳信息，超过15秒没收到回应就认为好友下线

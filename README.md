mvn clean scala:compile  package -DskipTests
sbt clean compile assembly
#准备工作
1，安装 JDK 1.8.x  
2，安装 maven  
3，设置 maven 仓库为阿里云镜像, 第三方库的 courier 镜像（修改用户目录下的 .m2/settings.xml 增加 mirrors），整体效果
```xml
<settings>
<mirrors>
   <mirror>
    <id>alimaven</id>
    <name>aliyun maven</name>
    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
    <mirrorOf>central</mirrorOf>       
  </mirror>
   <mirror>
      <id>courier</id>
      <name>courier</name>
      <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
      <mirrorOf>central</mirrorOf>       
    </mirror>
</mirrors>
</settings>
```
4，GIT克隆本项目源码  
5，修改webserver配置文件 `webserver/main/resources/webserver.properties`设置监听host和端口
6，修改collect配置文件 `collect/main/resources/application.conf` 设置akka集群，数据库 相关
                       `collect/main/resources/consumer.properties` 设置kafka消费者参数
                       `collect/main/resources/nodes.properties` 设置启动节点端口
7，修改process配置文件 `process/main/resources/application.conf` 设置akka集群相关
                       `process/main/resources/nodes.properties` 设置启动节点端口
  



#Windows开发步骤   
1，安装 Scala 2.12.x  
2，使用IDEA打开根目录  
3，安装IDEA scala插件  
4，启动三个模块各自Main函数

#Linux部署步骤  
1，cd 进入源码目录  
2，先执行以下命令编译
```
mvn clean compile
```

3，开启进程守护。可以使用supervisor或者pm2（先安装nodejs，然后安装pm2），下面是pm2 的启动例子
   依次按顺序执行如下sh：
   1.startWeb.sh     启动webserver。
   2.startCollect.sh 启动集群seednode开始消费kafka队列。
   3.startProcess.sh 启动集群工作节点处理发送邮件及持久化。
```$xslt
//记得先按CTRL+C结束第6步的进程，然后将第6步的命令保存为.sh文件，如 StartServer.sh
chmod 755 StartXXX.sh
pm2 start StartXXX.sh
```


##Webserver
```
cd webserver
mvn exec:java -Dexec.mainClass="com.koall.web.WebServer"
```

##Collect
```
cd collect
mvn exec:java -Dexec.mainClass="com.koall.email.Collector"
```

##Process
```
cd process
mvn exec:java -Dexec.mainClass="com.koall.email.Processor"
```

#新增邮件接口
```
在process/src/main/resource中编辑mail.json,添加新的接口
"method" : {
    "title" : ${title},
    "template" : ${template}
}

调用方调用webserver监听的 host:port/sendMail/${method},发送的参数
由template中需要的参数和{"to": "some@mail.com"}组成的json按post请求发送过来。
注意参数json的value需要为String类型。

对应的template中编辑参数,放入process/src/main/resource/template中
```
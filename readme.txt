WebServer
监听http端口，暴露相应的接口，将收到的消息插入到kafka队列中去
启动参数 host port

com.koall.email.Collector
邮件集群的任务分配者，负责从kafka队列中获取消息，并生成任务分配给processer处理
启动参数 ports

com.koall.email.Processor
邮件集群的任务处理者，负责执行分配过来的邮件任务
启动参数 ports
# NodeMQ 一款超轻量级、高性能边缘MQTT消息代理服务器 
[简体中文](README-CN.md)  . [English](README.md)

> 本项目是一个大胆的试验性项目，基于GraalVM 构建可直接行的的二进制文件（无需安装JDK）。项目启动内存仅需20MB,毫米级的启动速度,让你的服务器空闲起来吧！

##  功能
- [标准MQTT协议](#国际化)
    - [MQTT 3](#官方网站)
    - [MQTT 3.1.1](#官方网站)
    - [MQTT 5](#官方网站)
- [Websocket协议](#内容目录)
- [TLS/SSL加密](#内容目录)
- [Proxy代理协议](#内容目录)
- [服务等级](#项目介绍)
    - [qos0 至多一次](#官方网站)
    - [qos1 至少一次](#官方网站)
    - [qos2 仅仅一次](#官方网站)
- [Topic过滤](#图形演示)
    - [# 多级匹配](#官方网站)
    - [+ 一级匹配](#官方网站)
- [发布订阅](#图形演示) 
- [共享订阅](#功能)
- [保留消息](#功能)
- [遗嘱消息](#功能)
- [HTTP协议](#架构)
- [多实例](#架构)
- [Apacche 2.0](LICENSE)

## 快速开始

> 默认提供了Windows跟Linux-x64的可运行二进制文件，其他平台需要自行源码打包编译，打包完成后可以联系我们，提供可运行二进制给我们哦！

### 二进制下载

| 系统          | 下载                                                      |
|-------------|---------------------------------------------------------|
| Windows     | [二进制文件](https://wiki.smqtt.cc/smqttx/mqtt/1.mqtt.html)  |
| Linux-amd64 | [二进制文件](https://wiki.smqtt.cc/smqttx/mqtt/1.mqtt.html)  |

目前我们提供了Windows平台以及Linux-X86的可运行的二进制文件

### Linux源码编译

#### 安装GraalVM-CE
[下载链接](#https://github.com/graalvm/graalvm-ce-builds/releases/)

#### 安装zlib-devel

Debian:

```
sudo apt-get update

sudo apt-get install zlib1g-dev

```

Redhat:
```
sudo yum update -y

sudo yum install zlib-devel

```


#### 打包编译(linux)

下载源代码,进入NodeMQ项目目录

```shell
./mvnw package -P native
```

编译完成后会出现如下日志:

```shell
------------------------------------------------------------------------------------------------------------------------
Recommendations:
 INIT: Adopt '--strict-image-heap' to prepare for the next GraalVM release.
 HEAP: Set max heap for improved and more predictable memory usage.
 CPU:  Enable more CPU features with '-march=native' for improved performance.
Print of Dashboard dump output ended.
------------------------------------------------------------------------------------------------------------------------
                       26.6s (14.1% of total time) in 243 GCs | Peak RSS: 2.78GB | CPU load: 3.58
------------------------------------------------------------------------------------------------------------------------
Produced artifacts:
 /root/nodemq/NodeMQ-master/target/NodeMQ (executable)
========================================================================================================================
Finished generating 'NodeMQ' in 3m 7s.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  03:13 min
[INFO] Finished at: 2024-04-19T15:06:06+08:00
[INFO] ------------------------------------------------------------------------
```

打包完成后，会在target目录下生成可执行文件NodeMQ,运行`./NodeMQ`运行服务

## 配置详解

运行项目后，会在运行目录生成一个`mqtt.json`文件(如果存在可读取mqtt.json文件)，详情如下:

```json

{
  "mqtt" : [ {
    "host" : "0.0.0.0",
    "port" : 1883,
    "maxMessageSize" : 65535,
    "connectTimeout" : 3,
    "wiretap" : false,
    "ssl" : null,
    "useWebsocket" : false,
    "websocketPath" : "/mqtt",
    "proxy" : false,
    "maxQosLevel" : 2,
    "supportWildcardSubscribe" : true,
    "supportShareSubscribe" : true,
    "retrySize" : 10,
    "retryInterval" : 2000
  }, {
    "host" : "0.0.0.0",
    "port" : 8083,
    "maxMessageSize" : 65535,
    "connectTimeout" : 3,
    "wiretap" : false,
    "ssl" : null,
    "useWebsocket" : true,
    "websocketPath" : "/mqtt",
    "proxy" : false,
    "maxQosLevel" : 2,
    "supportWildcardSubscribe" : true,
    "supportShareSubscribe" : true,
    "retrySize" : 10,
    "retryInterval" : 2000
  } ],
  "http" : {
    "port" : 8080,
    "accessLog" : false,
    "ssl" : null
  },
  "system" : {
    "shareStrategy" : "RANDOM",
    "maxSessionMessageSize" : 1000,
    "maxRetainMessageSize" : 1000,
    "maxRetainExpiryInterval" : 604800,
    "unConfirmFlightWindowSize" : 100
  },
  "log" : {
    "level" : "INFO",
    "persisted" : false
  }
}
```


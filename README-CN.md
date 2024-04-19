# NodeMQ 一款超轻量级、高性能边缘MQTT消息代理服务器 
[简体中文](README-CN.md)  . [English](README.md)

> 本项目是一个大胆的试验性项目，基于GraalVM 构建可直接行的的二进制文件。项目启动内存仅需20MB,毫米级的启动速度,让你的服务器空闲起来吧！

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
- [HTTP协议](#架构)
- [Apacche 2](#许可证)

## 快速开始

### 二进制启动


目前我们提供了Windows平台以及Linux-X86的可运行的二进制文件


sudo apt-get update

sudo apt-get install zlib1g-dev

sudo yum instal l zlib-devel
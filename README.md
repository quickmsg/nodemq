# NodeMQ An ultra-lightweight, high-performance edge MQTT message proxy server
[English](README-CN.md). [English](README.md)

> This project is a bold experimental project to build a straight-line binary based on GraalVM (no JDK installation required). Project startup memory only 20MB, millimeter-level startup speed, let your server idle!

## Function
- [Standard MQTT protocol](#internationalization)
  - [MQTT 3](#Official website)
  - [MQTT 3.1.1](#official website)
  - [MQTT 5]( Official website)
- [Websocket protocol](#Content directory)
- [TLS/SSL encryption](#Content directory)
- [Proxy protocol](#Content directory)
- [Service level](#Project introduction)
  - [qos0 at most once](#Official website)
  - [qos1 at least once](#Official website)
  - [qos2 Only once](#Official website)
- [Topic filtering](#graphic presentation)
  - [# multi-level matching](#official website)
  - [+ Level 1 match](#Official website)
- [Publish subscribe](#Graphic presentation)
- [Share subscription](#function)
- [Retain message](#function)
- [Will message](#function)
- [HTTP protocol](#architecture)
- [multi-instance](#schema)
- [Apacche 2.0](LICENSE)

## Get started fast

> The default provides Windows and Linux-x64 runable binary files, other platforms need to package their own source code compilation, you can contact us after packaging, provide runable binary to us oh!

### Binary download

| system | download |
|-------------|---------------------------------------------------------|
| | Windows binaries (https://wiki.smqtt.cc/smqttx/mqtt/1.mqtt.html) |
| Linux - amd64 | binary file] [| (https://wiki.smqtt.cc/smqttx/mqtt/1.mqtt.html)

Currently we offer the Windows platform as well as Linux-X86 runnable binaries

### Linux source code compilation

#### Install GraalVM-CE
[download](https://github.com/graalvm/graalvm-ce-builds/releases/)

#### Install zlib-devel

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


#### Package and compile (linux)

Download the source code and go to the NodeMQ project directory

```shell
./mvnw package -P native
```

After the compilation is complete, the following log appears:

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
= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
Finished generating 'NodeMQ' in 3m 7s.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  03:13 min
[INFO] Finished at: 2024-04-19T15:06:06+08:00
[INFO] ------------------------------------------------------------------------
```

After packaging is complete, the executable NodeMQ is generated in the target directory, and the service is run by running './NodeMQ '

## Configuration details

After running the project, a 'mqtt.json' file is generated in the run directory (if there is a readable mqtt.json file), as follows:

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
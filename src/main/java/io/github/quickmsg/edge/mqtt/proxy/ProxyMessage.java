package io.github.quickmsg.edge.mqtt.proxy;


/**
 * @author luxurong
 */

public class ProxyMessage {

    private  int destinationPort;

    private  int sourcePort;

    private  String sourceAddress;

    private  String destinationAddress;

    private String ssl;

    private String sslCn;

    private String sslVersion;


    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getSsl() {
        return ssl;
    }

    public void setSsl(String ssl) {
        this.ssl = ssl;
    }

    public String getSslCn() {
        return sslCn;
    }

    public void setSslCn(String sslCn) {
        this.sslCn = sslCn;
    }

    public String getSslVersion() {
        return sslVersion;
    }

    public void setSslVersion(String sslVersion) {
        this.sslVersion = sslVersion;
    }
}

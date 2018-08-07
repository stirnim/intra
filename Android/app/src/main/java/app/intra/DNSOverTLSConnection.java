package app.intra;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.net.Socket;


import javax.net.ssl.SSLContext;

import app.intra.util.DnsMetadata;
import okhttp3.Callback;


public class DNSOverTLSConnection implements ServerConnection {


    private final String hostname;


    public static DNSOverTLSConnection get(String hostname) {
        return new DNSOverTLSConnection(hostname);
    }

    private DNSOverTLSConnection(String hostname) {
        this.hostname = hostname;
    }

    public void performDnsRequest(byte[] data, DOTCallback callback) {
        int DoTPORT = 853;

        DatagramPacket outPacket = new DatagramPacket(data, data.length);
        InetAddress addr;

        try {
            addr = InetAddress.getByName(hostname);

            Log.d("dns", "Trying to perform DNS-over-TLS lookup via " + addr.toString());
            Socket dnsSocket;

            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            dnsSocket = context.getSocketFactory()
                    .createSocket(addr, DoTPORT);

            //Create TLS v1.2 socket
            //service.protect(dnsSocket);
            DataOutputStream dos = new DataOutputStream(dnsSocket.getOutputStream());
            byte[] packet = outPacket.getData();
            dos.writeShort(packet.length);
            dos.write(packet);
            dos.flush();

            DataInputStream stream = new DataInputStream(dnsSocket.getInputStream());
            int length = stream.readUnsignedShort();
            byte[] returnpacketdata = new byte[length];
            stream.read(returnpacketdata);
            dnsSocket.close();
            callback.onDOTAnswer(returnpacketdata);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void performDnsRequest(DnsMetadata metadata, byte[] data, Callback cb) {

    }

    @Override
    public String getUrl() {
        return hostname;
    }

}
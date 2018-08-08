package app.intra;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.net.Socket;
import java.net.UnknownHostException;


import javax.net.ssl.SSLContext;

import app.intra.util.DnsMetadata;
import okhttp3.Callback;


public class DNSOverTLSConnection implements ServerConnection {


    private final String hostname;
    private InetAddress serverIP = null;
    private static final String TAG="DNSOverTLSConnection";


    public static DNSOverTLSConnection get(String hostname) {
        Log.d(TAG,"creating TLS connection object with hostname "+hostname);
        try {
            return new DNSOverTLSConnection(hostname);
        } catch (UnknownHostException e){
            return null;
        }
    }

    private DNSOverTLSConnection(String hostname) throws UnknownHostException{
        this.hostname = hostname;
            InetAddress addr = InetAddress.getByName(hostname);
            this.serverIP = addr;
            Log.d(TAG,"Resolved Host: "+this.serverIP);
    }

    public void performDnsRequest(final byte[] data, final DOTCallback callback) {
        final int DoTPORT = 853;
        if (serverIP==null){
            Log.e(TAG,"Cannot perform DNS query, we could not get a ip for "+hostname);

        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramPacket outPacket = new DatagramPacket(data, data.length);
                try {
                    Log.d(TAG, "Trying to perform DNS-over-TLS lookup via " + serverIP);
                    Socket dnsSocket;

                    SSLContext context = SSLContext.getInstance("TLSv1.2");
                    context.init(null, null, null);
                    dnsSocket = context.getSocketFactory()
                            .createSocket(serverIP, DoTPORT);

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
                    Log.d(TAG, "Got answer for query");
                    callback.onDOTAnswer(returnpacketdata);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    public void performDnsRequest(DnsMetadata metadata, byte[] data, Callback cb) {
//unused, required to implement the ServerConnection interface
    }

    @Override
    public String getUrl() {
        return hostname;
    }

}
package app.intra;

import android.net.VpnService;
import android.util.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

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

    public void performDnsRequest(final byte[] data, final DOTCallback callback, final VpnService vpn) {
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
                    long start = System.currentTimeMillis();
                    SSLSocket dnsSocket;

                    // TODO: Handle TLS connection errors e.g. refused
                    // TODO: Allow self-signed certificate, maybe allow hostname mismatch
                    SSLContext context = SSLContext.getInstance("TLSv1.2");
                    context.init(null, null, null);
                    dnsSocket = (SSLSocket) context.getSocketFactory()
                            .createSocket(serverIP, DoTPORT);

                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    SSLSession s = dnsSocket.getSession();

                    // Verify the certicate hostname
                    // This is due to lack of SNI support in the current SSLSocket.
                    if (!hv.verify(hostname, s)) {
                        throw new SSLHandshakeException("Expected " + hostname + ", " +
                                "found " + s.getPeerPrincipal());
                    }

                    // At this point SSLSocket performed certificate verification and
                    // we have performed hostname verification, so it is safe to proceed.

                    vpn.protect(dnsSocket);

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
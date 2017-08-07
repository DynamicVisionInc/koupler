package com.monetate.koupler;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;

// added from GTC listeners
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.DummySession;

import com.gtc.gtclisteners.common.Utilities;

// all units
import com.gtc.gtclisteners.receivers.ReceiverWorkerContainer;

// calamp specific
import com.gtc.gtclisteners.receivers.calamp.calamp32.CalampGeofenceFlag;
import com.gtc.gtclisteners.receivers.calamp.calamp32.Calamp32ReceiverWorker;
import com.gtc.gtclisteners.receivers.ReceiverWorkerContainer;

// geometris specific
import com.gtc.gtclisteners.receivers.geometris.GeometrisReceiverWorker;

import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;


/**
 * UDP Listener
 */
public class UdpKoupler extends Koupler implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpKoupler.class);
    protected DatagramSocket socket = null;
    private static final int BUF_SIZE = 8192;
    private byte[] buf = new byte[BUF_SIZE];
    public static final AtomicLong PACKETS = new AtomicLong(0);

    public UdpKoupler(KinesisEventProducer producer, int port) throws SocketException {
        super(producer, 1);
        LOGGER.info("Firing up UDP listener on [{}]", port);
        socket = new DatagramSocket(port);
        socket.setReceiveBufferSize(1024 * 1024 * 1024);
    }

    public void run() {
        try {
            while (true) {

              Properties props = new Properties();
              String propertiesFile = "./conf/kpl.properties";
              InputStream input = new FileInputStream(propertiesFile);
              props.load(input);
              String manufacturer = props.getProperty("Manufacturer");

              LOGGER.info("Launching " + manufacturer + " Listener");

                DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                socket.receive(packet);

                InetAddress IPAddress = packet.getAddress();
                LOGGER.info("ip address [{}]", IPAddress);

                int myPort = packet.getPort();
                LOGGER.info("Port number [{}]", myPort);

                PACKETS.getAndIncrement();
                byte[] received = new byte[packet.getLength()];
                System.arraycopy(buf, 0, received, 0, packet.getLength());

                if (manufacturer.equals("Calamp")) {

                  // Instantiate the Calamp Receiver
                  DummySession session = new DummySession();
              		ReceiverWorkerContainer container = new ReceiverWorkerContainer();
                  CalampGeofenceFlag geofenceFlag = new CalampGeofenceFlag();
                  boolean garminEnabled = false;

                  Calamp32ReceiverWorker crw = new Calamp32ReceiverWorker(received, session, container, geofenceFlag, garminEnabled, socket);

                  crw.run();

                } else if (manufacturer.equals("Geometris")) {
                  // Instantiate the Geometris Receiver
                  DummySession session = new DummySession();
              		ReceiverWorkerContainer container = new ReceiverWorkerContainer();

                  String received_string = new String(buf, 0, packet.getLength()).trim();

                  GeometrisReceiverWorker grw = new GeometrisReceiverWorker(received_string, session, container, socket);

                  grw.run();

                }

                String event_hex_string = Utilities.toHexString(received);

                String event = Base64.getEncoder().encodeToString(event_hex_string.getBytes());
                //String event = Base64.getEncoder().encodeToString(received);
                //String event = new String(buf, 0, packet.getLength()).trim();

                LOGGER.info("Raw event: " + event);

                LOGGER.debug("Queueing event [{}]", event);
                producer.queueEvent(event);
            }
        } catch (Exception e){
            LOGGER.error("Problem with socket.", e);

        } finally {
            socket.close();
        }

    }
}

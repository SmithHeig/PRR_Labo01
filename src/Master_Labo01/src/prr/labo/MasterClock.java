/**
 * PRR: Labo1
 * Autor: J.Châtillon, J.Smith
 * Date: 30.09.2018
 *
 * Sources:
 *      Multicast: https://www.baeldung.com/java-broadcast-multicast
 */

package prr.labo;

import java.io.Console;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MasterClock {
    private boolean debug;
    private Date date;
    private byte id;
    private int k;
    private InetAddress group;
    private int port;

    private Thread tDelay;
    private Thread tGap;

    public MasterClock(){
        debug = true;
        id = 0;
        port = 4446;
        k = 2;
        try {
            group = InetAddress.getByName("230.0.0.0");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        tGap = new Thread() {
            public void run() {
                gap();
            }
        };


        tDelay = new Thread() {
            public void run() {
                delay();
            }
        };

        tGap.start();
        tDelay.start();

    }

    public void gap(){
        System.out.println("Serveur gap has been started");
        String msg;
        byte[] bufSYNC = new byte[2];
        byte[] bufFollowUP = new byte[10];
        long time;
        do{
            try {
                if(id == -1){       // on ne veut pas que l'id ait la valeur 0 (valeur par défaut des slaves)
                    id = 1;
                } else{
                    ++id;
                }

               bufSYNC[0] = 0x0;
               bufSYNC[1] = id;
               multicast(bufSYNC);

               bufFollowUP[0] = 0x1;


                //TimeUnit.MILLISECONDS.sleep(2000);    // Simulation du gap
                time = System.currentTimeMillis();

               System.arraycopy(longToBytes(time), 0, bufFollowUP, 1, 8);
               bufFollowUP[9] = id;

               multicast(bufFollowUP);
               System.out.println("Serveur SYNC finish: " + id);
               TimeUnit.MILLISECONDS.sleep(2000);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while(true);
    }

    public void delay(){
        byte[] tampon = new byte[256];
        byte[] tmpData;
        byte[] response = new byte[10];
        long curTime = 0;
        do{
            // Obtenir un socket de datagramme
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(4445);
                // Attendre le message du client
                DatagramPacket paquet = new DatagramPacket(tampon,tampon.length);
                socket.receive(paquet);
                if(debug){
                    System.out.println("Serveur: delay reciew");
                }
                curTime = System.currentTimeMillis();

                // Obtenir l'adresse et le port du client
                InetAddress addresseClient = paquet.getAddress();
                int portClient = paquet.getPort();


                // Reemettre le message recu
                tmpData = paquet.getData();
                System.arraycopy(longToBytes(curTime), 0, response, 1, 8);
                response[0] = 0x3;
                response[9] = tmpData[1];
                System.out.println("IP cli:" + addresseClient);
                paquet= new DatagramPacket(tampon,tampon.length,addresseClient,portClient);
                socket.send(paquet);
                socket.close();

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }while(true);
    }

    public void sendDataGram(String msg) {

        byte[] buf = msg.getBytes();

        try {
            String received;
            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), 4445);
            DatagramSocket socket = null;
            socket = new DatagramSocket();
            socket.send(packet);
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            received = new String(packet.getData(), 0, packet.getLength());
            socket.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void multicast(String multicastMessage) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        byte [] buf = multicastMessage.getBytes();

        DatagramPacket packet
                = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
    }

    private void multicast(byte[] multicastMessage) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet
                = new DatagramPacket(multicastMessage, multicastMessage.length, group, port);
        socket.send(packet);
        socket.close();
    }

    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }
}

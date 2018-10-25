package prr.labo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Properties;



public class Slave {
    private boolean isDebug;
    private long delay;
    private long shif;
    private long gap;
    boolean delayIsRunning;


    private Thread tDelay;
    private Thread tGap;

    private Random random;

    int k;


    /** Construscteur**/
    public Slave(){
        isDebug = true;
        random = new Random();
        delay = 0;
        gap = 0;
        delayIsRunning = false;

        k = 2000;

        // Création du thread qui calcule l'écart (Porocole Synchronisation)
        tGap= new Thread() {
            public void run() {

                doGap();
            }
        };

        // Création du thread qui calcule le délai (Porocole Delay)
        tDelay = new Thread() {
            public void run() {
                delaySlaveSpeaker();
            }
        };


        tGap.start();

    }


    // SYNC
    public void doGap(){
        if(isDebug){
            System.out.println("Client: Gap function has been started");
        }

        byte serverId = 0;
        long timeServeur = 0;
        long timeClient = 0;

        try {

            byte[] tampon = new byte[255];      // Tompon de communication
            byte[] tmpData;

            // Joindre le groupe pour recevoir le message diffuse
            MulticastSocket socket = new MulticastSocket(CommunicationConfig.masterMulticalPort);
                InetAddress groupe = InetAddress.getByName(CommunicationConfig.masterMulticastIp);
            socket.joinGroup(groupe);

            do {
                // Attendre le message du serveur
                DatagramPacket paquet = new DatagramPacket(tampon,tampon.length);
                socket.receive(paquet);
                tmpData = paquet.getData();

                //Message SYNC == 0x0
                if(tmpData[0] == CommunicationConfig.syncMessage){
                    timeClient = System.currentTimeMillis();
                    // récupération de l'id
                    serverId = tmpData[1];

                } else if(tmpData[0] == CommunicationConfig.followUpMessage){ // Message  FOLLOW_UP = 0x1
                    System.out.println(serverId + " ID: " + tmpData[9]);
                    // Contrôle si le message reçu est le bon
                    if(tmpData[9] == serverId){
                        // récupèration de l'heure
                        timeServeur = bytesToLong(Arrays.copyOfRange(tmpData, 1, 9));
                        System.out.println(" TimeSrv: " + timeServeur );
                        System.out.println(" TimeSrv: " + timeClient );
                        setGap(timeServeur - timeClient);

                        if(!delayIsRunning){
                            tDelay.start();
                            delayIsRunning = true;
                        }
                    }
                }
            }while(tmpData[0] != 0xf);
            socket.leaveGroup(groupe);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void setDelay(long delay){
        this.delay = delay;
        if(isDebug){
            System.out.println("Client: Delay has been set to" + delay);
        }
    }

    private synchronized  void setGap(long gap){
        this.gap = gap;
        if(isDebug){
            System.out.println("Client: Gap has been set to: " + gap);
        }
    }

    private synchronized long getShift(){
        return delay + gap;
    }

    private void delaySlaveSpeaker(){

        if(isDebug){
            System.out.println("Client: Delay function has been started");
        }
        byte clientId = 0;
        long timeEs = 0;
        long timeMaster = 0;
        int timeToWait = 0;

        byte[] message = new byte[2];
        byte[] rcvMessage = new byte[10];
        byte[] tampon = new byte[256];

        InetAddress address = null;
        String strAdress;
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            //address = InetAddress.getByName("localhost");
            //strAdress = inetAddress.getHostAddress();
            System.err.println(inetAddress.getHostAddress());
            do{

                // Attente aléatoire [4k;60k]
                timeToWait = k * (random.nextInt(57) + 4);  // FIXME: Random
                message[0] = 0x2;

                message[1] = ++clientId;

                DatagramPacket paquet = new DatagramPacket(tampon,tampon.length, inetAddress,4445);
                DatagramSocket socket = new DatagramSocket();
                // Envoyer le message
                timeEs = System.currentTimeMillis();
                socket.send(paquet);

                socket.setSoTimeout(timeToWait);

                try{
                    DatagramPacket rcvPacket = new DatagramPacket(tampon,tampon.length,address,4445);
                    socket.receive(rcvPacket);
                    rcvMessage = rcvPacket.getData();

                    if(rcvMessage.length >= 10){
                        if(rcvMessage[0] == 0x3){

                            // Contrôle si le message reçu est le bon
                            if(rcvMessage[10] == clientId){
                                // récupèration de l'heure
                                timeMaster = bytesToLong(Arrays.copyOfRange(rcvMessage, 1, 9));


                                // Attention, calcule en entier. le résultat est précis à +- 1ms;
                                setDelay((timeMaster - timeEs) / 2);

                            }
                        }
                        TimeUnit.MILLISECONDS.sleep(k);

//                        wait(timeToWait);
                    }
                } catch (SocketTimeoutException e) {
                    // timeout exception.
                    System.out.println("Timeout reached!!! " + e);
                }


            } while(true);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

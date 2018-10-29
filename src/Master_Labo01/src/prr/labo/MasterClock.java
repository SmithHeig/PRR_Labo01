/**
 * PRR: Labo1
 * Autor: J.Châtillon, J.Smith
 * Date: 30.09.2018
 *
 * Sources:
 *      Multicast: https://www.baeldung.com/java-broadcast-multicast
 */

package prr.labo;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MasterClock {
    private final boolean isDebug;
    private final boolean simulateLattency;

    private Random random;

    // Il est nécessaire d'avoir 2 threads, qui s'occupe du delai et l'autre de l'écart
    private Thread tDelay;
    private Thread tGap;

    private boolean isFinish;   // permet d'arrêter proprement les threads lors

    public MasterClock(boolean isDebug, boolean simulateLattency){

        this.isDebug = isDebug;
        this.simulateLattency = simulateLattency;

        isFinish = false;


        random = new Random();

        // déclaration des threads
        tGap = new Thread() {
            public void run() {
                doGap();
            }
        };
        tDelay = new Thread() {
            public void run() {
                doDelay();
            }
        };

        // Démarage des threads
        tGap.start();
        tDelay.start();

    }

    public void doGap(){

        if(isDebug){
            System.out.println("Server: Gap function has been started");
        }

        byte id = 0;
        byte[] bufSYNC = new byte[2];
        byte[] bufFollowUP = new byte[10];
        long time;
        do{
            try {
                // on ne veut pas que l'id ait la valeur 0 (valeur par défaut des slaves)
                if(id == -1){
                    id = 1;
                } else{
                    ++id;
                }

                // Message SYNC
                bufSYNC[0] = CommunicationConfig.syncMessage;
                bufSYNC[1] = id;
                multicast(bufSYNC);



                // Simulation de latence si besoin
                simulateLattency();

                // Message FOLLOW_UP
                time = System.currentTimeMillis();
                bufFollowUP[0] = CommunicationConfig.followUpMessage;

                System.arraycopy(longToBytes(time), 0, bufFollowUP, 1, 8);
                bufFollowUP[9] = id;

                multicast(bufFollowUP);

                TimeUnit.MILLISECONDS.sleep(CommunicationConfig.k);

                if(isDebug){
                    System.out.println("Serveur SYNC finish with id: " + id);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while(!isFinish);

        if(isDebug){
            System.out.println("Server: Gap function has been finished");
        }
    }

    protected void doDelay(){
        if(isDebug){
            System.out.println("Server: Delay function has been started");
        }
        byte[] tampon = new byte[256];
        byte[] tmpData;
        byte[] response = new byte[10];
        long curTime = 0;

        try {
            DatagramSocket socket = new DatagramSocket(CommunicationConfig.masterP2PPort);
            do{




                // Attendre le message du client
                DatagramPacket paquet = new DatagramPacket(tampon,tampon.length);
                socket.receive(paquet);

                // Re-mettre le message recu
                tmpData = paquet.getData();

                if(tmpData[0] == CommunicationConfig.delayRequestMessage) {

                    // Simulation de latence si besoin
                    simulateLattency();

                    curTime = System.currentTimeMillis();

                    // Obtenir l'adresse et le port du client
                    InetAddress addresseClient = paquet.getAddress();

                    int portClient = paquet.getPort();


                    // Re-mettre le message recu
                    tmpData = paquet.getData();
                    System.arraycopy(longToBytes(curTime), 0, response, 1, 8);
                    response[0] = CommunicationConfig.delayResponsetMessage;
                    response[9] = tmpData[1];


                    if (isDebug) {
                        System.out.println("Server Delay_Response sent to ip:" + addresseClient + "  with id: " + response[9]);
                    }
                    paquet = new DatagramPacket(response, response.length, addresseClient, portClient);
                    socket.send(paquet);
                }




            }while(!isFinish);
            socket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(isDebug){
            System.out.println("Server: Delay function has been finished");
        }
    }

    /**
     * Permet de terminer le serveur proprement
     */
    public synchronized void shutDown(){
        isFinish = true;
    }


    /**
     * Permet d'envpyer un message par multicast
     * @param multicastMessage  message à envoyer
     * @throws IOException
     */
    protected void multicast(byte[] multicastMessage) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress groupe = InetAddress.getByName(CommunicationConfig.masterMulticastIp);
        DatagramPacket packet
                = new DatagramPacket(multicastMessage, multicastMessage.length, groupe, CommunicationConfig.masterMulticalPort);
        socket.send(packet);
        socket.close();
    }

    /**
     * Méthode permettant de convertire un lonn en un tableau de byte.
     * cf: https://stackoverflow.com/questions/1586882/how-do-i-convert-a-byte-to-a-long-in-java
     * @param x long à convertire
     * @return valeur dans un tableau de bytes
     */
    protected byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * Simule une latence en endormant la méthode pendant 100 à 2000 ms (valeur aléatoire choisie arbitrairement)
     */
    protected void simulateLattency(){
        // Simulation de lattence entre 100 et 2000 ms
        if(simulateLattency) {
            try {
                TimeUnit.MILLISECONDS.sleep((random.nextInt(20) + 1) * 100);    // Simulation du doGap
            } catch (InterruptedException e) {
                System.err.println("Error latency simulation");
                e.printStackTrace();
            }
        }
    }

}

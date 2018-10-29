package prr.labo;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class Slave {
    private long delay;
    private long gap;

    private InetAddress serveurAdress;      // Adresse du serveur maître

    private Thread tDelay;
    private Thread tGap;
    private boolean isFinish;
    private Random random;

    private boolean isDebug;


    /**
     * Construscteur
     * @param isDebug Affiche des commentaires utile pour le débug
     */
    public Slave(boolean isDebug){
        this.isDebug = isDebug;
        random = new Random();
        delay = 0;
        gap = 0;
        isFinish = false;

        // Adress du serveur par défaut
        try {
            serveurAdress = InetAddress.getByName("localhost");
        } catch (Exception e){
            System.err.println(e);
        }

        // Création du thread qui calcule l'écart (Porocole Synchronisation)
        tGap = new Thread() {
            public void run() {
                doGap();
            }
        };

        // Création du thread qui calcule le délai (Porocole Delay)
        tDelay = new Thread() {
            public void run() {
                doDelay();
            }
        };

        tGap.start();

    }


    /**
     * Méthode qui permet de calculer l'écart (SYNC).
     */
    protected void doGap(){

        if(isDebug){
            System.out.println("Client: Gap function has been started");
        }

        byte serverId = 0;
        long timeServeur = 0;
        long timeClient = 0;
        boolean delayIsRunning = false;

        try {

            byte[] tampon = new byte[255];      // Tompon de communication
            byte[] tmpData;

            // Joindre le groupe pour recevoir le message diffuse
            MulticastSocket socket = new MulticastSocket(CommunicationConfig.masterMulticalPort);
                InetAddress groupe = InetAddress.getByName(CommunicationConfig.masterMulticastIp);
                socket.joinGroup(groupe);

            do {
                // Attendre le message du serveur
                DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);
                socket.receive(paquet);
                tmpData = paquet.getData();

                //Message SYNC == 0x0
                if(tmpData[0] == CommunicationConfig.syncMessage){
                    timeClient = System.currentTimeMillis();
                    // récupération de l'id
                    serverId = tmpData[1];

                } else if(tmpData[0] == CommunicationConfig.followUpMessage){ // Message  FOLLOW_UP = 0x1


                    // Contrôle si le message reçu est le bon
                    if(tmpData[9] == serverId){
                        // récupèration de l'heure
                        timeServeur = bytesToLong(Arrays.copyOfRange(tmpData, 1, 9));
                        setGap(timeServeur - timeClient);

                        // lancement du 2ième thread
                        if(!delayIsRunning){
                            setServeurAdress(paquet.getAddress());

                            delayIsRunning = true;
                            tDelay.start();
                        }
                    }
                }
            }while(isFinish);
            socket.leaveGroup(groupe);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode qui permet de calculer le delay (DELAY_REQUEST/DELAY_RESPONSE). Doit se lancer après que le la méthode
     *  doGap ait été effectuée au moins une fois.
     */
    protected void doDelay(){

        if(isDebug){
            System.out.println("Client: Delay function has been started");
        }
        byte clientId = 0;
        long timeEs = 0;
        long timeMaster = 0;
        long timeToWait = 0;

        byte[] message = new byte[2];
        byte[] rcvMessage = new byte[10];


        InetAddress inetAddress = getServeurAdress();
        try {

            DatagramSocket socket = new DatagramSocket();

            do{
                ++clientId;

                // Attente aléatoire [4k;60k]
                timeToWait = CommunicationConfig.k * ((long) random.nextInt(57) + 4);
                timeToWait = 2000;
                message[0] = CommunicationConfig.delayRequestMessage;

                message[1] = clientId;
                DatagramPacket paquet = new DatagramPacket(message, message.length, inetAddress, CommunicationConfig.masterP2PPort);

                // Envoyer le message
                timeEs = System.currentTimeMillis();
                socket.send(paquet);
                if(isDebug){
                    System.out.println("DelayRequest sent with id:" + clientId);
                }


                // Timout set après le temps maximal d'attente et un peu plus pour ne pas rester bloqué.
                socket.setSoTimeout((int)CommunicationConfig.k * 6 + 100);
                try{
                    DatagramPacket rcvPacket = new DatagramPacket(rcvMessage,rcvMessage.length);
                    socket.receive(rcvPacket);


                    if(rcvMessage[0] == CommunicationConfig.delayResponsetMessage){
                        // Contrôle si le message reçu est le bon
                        if(rcvMessage[9] == clientId){
                            // récupèration de l'heure
                            timeMaster = bytesToLong(Arrays.copyOfRange(rcvMessage, 1, 9));

                            // Attention, calcule en entier. le résultat est précis à +- 1ms;
                            setDelay((timeMaster - timeEs) / (long)2);

                        }
                    }

                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout reached!!! " );
                    e.printStackTrace();
                }

                TimeUnit.MILLISECONDS.sleep(timeToWait);


            } while(!isFinish);

            socket.close();

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

    /****  SETTERS
     * Ils sont tous en synchronised car ils ont accès à des variables partagées
    ***/

    private synchronized void setDelay(long delay){
        this.delay = delay;
        if(isDebug){
            System.out.println("Client: Delay has been set to: " + delay);
        }
    }

    private synchronized void setServeurAdress(InetAddress serveurAdress){
        if(isDebug){
            System.out.println("Client: Server address has been set to: " + serveurAdress);
        }
        this.serveurAdress = serveurAdress;
    }

    private synchronized  void setGap(long gap){
        this.gap = gap;
        if(isDebug){
            System.out.println("Client: Gap has been set to: " + gap);
        }
    }


    /**
     * Permet de terminer le serveur proprement
     */
    public synchronized void shutDown(){
        isFinish = true;
    }


    private synchronized InetAddress getServeurAdress(){
        return this.serveurAdress;
    }

    /****  getter
     * Ils sont tous en synchronised car ils ont accès à des variables partagées
     ***/
    private synchronized long getShift(){
        return delay + gap;
    }

    /**
     *
     * @return l'heure du serveur + le décalage
     */
    public synchronized long getTime(){
        return System.currentTimeMillis() + getShift();
    }


    /**
     * Méthode permettant de convertire un tableau de byte en long.
     * cf: https://stackoverflow.com/questions/1586882/how-do-i-convert-a-byte-to-a-long-in-java
     * @param bytes tableau de bytes à convertire
     * @return  valeur en long
     */
    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();  //need flip
        return buffer.getLong();
    }
}

package prr.labo;

/**
 * Contient les configurations de communication.
 * Le message à envoyer doit être le plus court possible. Donc on utilise un tableau de Bytes.
 * La première byte contient le type de message.
 * Le dernier Byte du message contient l'id (de type byte).
 * Si on doit communiquer une date (de type long), il est écrit entre le type et l'id sur 8bytes.
 */
public class CommunicationConfig {

    /*
        Code pour savoir quelle est le type de message
     */
    public final static byte syncMessage = 0x0;
    public final static byte followUpMessage = 0x1;
    public final static byte delayRequestMessage = 0x2;
    public final static byte delayResponsetMessage = 0x3;

    public final static String masterMulticastIp = "230.0.0.0";
    public final static int masterMulticalPort = 4446;

    // port master communication point par point
    public final static int masterP2PPort = 4445;

    public final static long k = 2000;               // temps interval [ms]
}

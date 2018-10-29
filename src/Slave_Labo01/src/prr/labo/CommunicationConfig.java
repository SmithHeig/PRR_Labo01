package prr.labo;

public class CommunicationConfig {

    /*
        Code pour savoir quelle est letype de message
     */
    public final static byte syncMessage = 0x0;
    public final static byte followUpMessage = 0x1;
    public final static byte delayRequestMessage = 0x2;
    public final static byte delayResponsetMessage = 0x3;
    public final static byte errorEndMessage = 0xf;

    public final static String masterMulticastIp = "230.0.0.0";
    public final static int masterMulticalPort = 4446;

    public final static int masterP2PPort = 4445;

    /*
        Position de l'id dans le message. Il se trouve toujours à la dernière position et à la taille d'un byte.
     */
    public final static int idPosSmallArray = 1;
    public final static int idPosBigArray = 9;             //


    public final static long k = 2000;               // [ms]
}

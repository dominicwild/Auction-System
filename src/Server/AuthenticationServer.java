package Server;


import Security.AuctionSecurity;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

/**
 * A server that clients must retrieve a interface to use the distributed
 * system.
 *
 * @author DominicWild
 */
public class AuthenticationServer implements Runnable{

    protected static PrivateKey myKey;          //The private key of the authentication server.
    private final static int PORT = 7778;       //The port which it listens upon.
    private static ReplicationManager server;          //The replication manager of the auction system.
    private Socket clientSocket;                //The clients socket which it is dealing with.

    /**
     * Creates a new instance of Authentication server to deal with client requests.
     * @param server The server which manages the auction system.
     */
    public AuthenticationServer(ReplicationManager server) {
        AuthenticationServer.server = server;
    }
    
    /**
     * Creates a new instance of Authentication server to deal with client requests.
     * @param clientSocket The socket of the client we're dealing with.
     */
    public AuthenticationServer(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    
    public void execute() {
        AuthenticationServer.myKey = AuctionSecurity.registerKeys("Server", false); 
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(PORT);
            System.out.println("Authentication Server up and listening.");
            while (true) { //Listen for client connections
                Socket client = listener.accept();
                Thread t = new Thread(new AuthenticationServer(client));
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(AuthenticationServer.class.getName()).log(Level.SEVERE, "IO Exception in main loop.", ex);
        }
    }

    @Override
    public void run() {
        System.out.println("New connection on: " + this.clientSocket.toString());
        try {
            //Set up object to send data to the server
            ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream objIn = new ObjectInputStream(clientSocket.getInputStream());
            logPrint("Initialisation set-up");
            //We expected a challenge from the client
            logPrint("Awaiting challenge from client for verification.");
            AuctionSecurity.recieveChallenge(objIn, objOut, myKey);
            logPrint("Finished verification procedure. Awaiting client identification request.");

            //Verify connecting user on who they assert they are.
            String assertedUser = (String) objIn.readObject();
            logPrint("Requested authentication as: " + assertedUser);
            logPrint("Issuing challenge...");
            PublicKey usersKey = AuctionSecurity.getPublicKey(assertedUser);
            if (!AuctionSecurity.initiateChallenge(objIn, objOut, usersKey)) {
                logPrint("Invalid user connection.");
            }
            
            //Upon successful verification, send an interface that the user requests.
            logPrint("Successful authentication of user: " + assertedUser);
            objOut.writeObject("verified");
            String requestedInterface = (String) objIn.readObject();
            logPrint("Requested interface: " + requestedInterface);
            logPrint("Sending interface....");
            this.createSessionObject(requestedInterface, assertedUser);
            Serializable rmiObject = (Serializable) Naming.lookup("rmi://localhost/NamedObjects/" + requestedInterface + assertedUser);
            SealedObject[] encryptedItems = this.encrypt(rmiObject, usersKey);
            objOut.writeObject(encryptedItems);
            logPrint("Interface sent, client session closed.");
        } catch (IOException ex) {
            logPrint("Client has been disconnected due to IOException.");
        } catch (ClassNotFoundException ex) {
            logPrint("ClassNotFoundException while user authenticating.");
        } catch (NotBoundException ex) {
            logPrint("NotBoundException while user authenticating.");
        }
    }
    
    /**
     * Encrypts the provided object with a symmetric AES key, then that key is encrypted with the provided public key. Both of these are encrypted within SealedObjects and returned.
     * @param object The object to be encrypted.
     * @param userKey The public key to encrypt the symmetric key with.
     * @return Both provided objects encrypted with their respective keys.
     */
    public SealedObject[] encrypt(Serializable object, PublicKey userKey){
        SealedObject encyptedObject = null;
        SealedObject encryptedKey = null;
        try {
            SecretKey key = KeyGenerator.getInstance("AES").generateKey();  //Make symmetric key to be encypted with public key
            Cipher primaryCipher = Cipher.getInstance(key.getAlgorithm());  //Primary being first layer
            primaryCipher.init(Cipher.ENCRYPT_MODE, key);
            encyptedObject = new SealedObject(object,primaryCipher);
            Cipher secondaryCipher = Cipher.getInstance(userKey.getAlgorithm());    //Secondary, second layer of encryption
            secondaryCipher.init(Cipher.ENCRYPT_MODE, userKey);
            encryptedKey = new SealedObject(key,secondaryCipher);
        } catch (NoSuchAlgorithmException ex) {
            logPrint("NoSuchAlgorithmException while user authenticating.");
        } catch (NoSuchPaddingException ex) {
            logPrint("NoSuchPaddingException while user authenticating.");
        } catch (InvalidKeyException ex) {
            logPrint("InvalidKeyException while user authenticating.");
        } catch (IOException ex) {
            logPrint("IOException while user authenticating.");
        } catch (IllegalBlockSizeException ex) {
            logPrint("IllegalBlockSizeException while user authenticating.");
        }
        
        return new SealedObject[]{encryptedKey,encyptedObject}; 
    }

    /**
     * Print a log of which each client does/accomplishes on the authentication server.
     * @param message 
     */
    private void logPrint(String message) {
        System.out.println("[" + this.clientSocket.getRemoteSocketAddress().toString() + "]: " + message);
    }

    /**
     * Creates a session object which is an instance of the requested service binded to their identity.
     * @param requestedInterface The interface a user requested.
     * @param owner The username identifier of that user.
     */
    private void createSessionObject(String requestedInterface, String owner) {
        try {
            String url;
            switch (requestedInterface) {   //Switch to created an add necessary object to RMI.
                case "Seller":
                    AuctionSeller seller = new AuctionSeller(server,owner);
                    url = "/NamedObjects/" + requestedInterface + owner;
                    Naming.rebind(url, seller);
                    AuthenticationServer.server.addSession(url, seller);
                    break;
                case "Bidder":
                    AuctionUser user = new AuctionUser(server,owner);
                    url = "/NamedObjects/" + requestedInterface + owner;
                    Naming.rebind(url, user);
                    AuthenticationServer.server.addSession(url, user);
                    break;
                default:
            }
        } catch (RemoteException ex) {
            logPrint("RemoteException adding session object during authentication " + requestedInterface + ".");
        } catch (MalformedURLException ex) {
            logPrint("MalformedURLException adding session object during authentication " + requestedInterface + ".");
        }
    }

}

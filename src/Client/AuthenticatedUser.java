package Client;



import Security.AuctionSecurity;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

/**
 * Any user within the system that requires authentication in order to access
 * services. Implements automatic connection to a authentication server.
 *
 * @author DominicWild
 */
public abstract class AuthenticatedUser {

    protected String name;                  //The username of the user
    protected PrivateKey privateKey;        //Their personal private key
    private final int AUTH_PORT = 7778;     //The port to listen to on a socket for authorisation.
    private final String AUTH_ADDRESS = "localhost"; //The address of the authentication server.
    private final String PASSCODE = "1111"; //The secret passcode for registration.

    /**
     * Creates a new authenticated user, with the passed name for identification.
     * @param name The name to identify as on the system.
     */
    public AuthenticatedUser() {
        Scanner input = new Scanner(System.in);
        System.out.print("Please input your username: "); //Ask user to identify with services
        this.name = input.nextLine();
        
        if (!new File(AuctionSecurity.CLIENT_STORAGE + name + "/Private.key").exists()) { //If no private key exists for this invidual on file, we must make one.
            input = new Scanner(System.in);
            System.out.println("User not registered.\nInsert secret code for registration: ");
            if (input.nextLine().equals(PASSCODE)) { //Ensure the user is someone whom we have given permission to register.
                this.privateKey = AuctionSecurity.registerUser(name);
            } else {
                System.out.println("That's the incorrect passcode. Therefore you cannot register.");
                System.exit(0);
            }
        } else {
            this.privateKey = AuctionSecurity.getPrivateKey(name);
        }
        this.name = name;
    }

    /**
     * Gets the specified service, as a string, from the RMI server through authenticated means.
     * @param service The service to aquire.
     * @return The service object that the user requested.
     */
    public Object getService(String service) {
        try {
            Socket authServer = new Socket(AUTH_ADDRESS, AUTH_PORT); //Connect to authentication server.
            //Set up objects to read and write responses to and from them server.
            ObjectInputStream objIn = new ObjectInputStream(authServer.getInputStream());
            ObjectOutputStream objOut = new ObjectOutputStream(authServer.getOutputStream());
            //Challenge the server and verify it.
            PublicKey serverKey = AuctionSecurity.getPublicKey("Server");
            System.out.print("Verifying server.... ");
            if (!AuctionSecurity.initiateChallenge(objIn, objOut, serverKey)) { //If we can't verify, then we exit.
                System.out.println("Signature of server does not match nonce challenge, invalid server connection.");
                return null;
            }
            System.out.println("Server verified!");
            //Identify ourselves with the server
            System.out.print("Authenticating credentials.... ");
            objOut.writeObject(this.name); //State who we are to the server
            AuctionSecurity.recieveChallenge(objIn, objOut, privateKey); //Await and deal with our challenge
            if (!objIn.readObject().equals("verified")) { //Check servers result
                System.out.println("Server rejected verification of user.");
                return null;
            }
            System.out.println("Credentials confirmed!");
            objOut.writeObject(service); //Send our service request string.
            return getServiceObject((SealedObject[]) objIn.readObject()); //Retreive our service object the server is sending us.
        } catch (IOException ex) {
            System.out.println("Can't aquire service: " + service + " from authentication server.");
            System.exit(1);
        } catch (ClassNotFoundException ex) {
            System.out.println("Service not found.");
        }

        return null;
    }
    
    private Object getServiceObject(SealedObject[] encryptedObjects){
        Object serviceObject = null;
        
        try {
            SecretKey keyToService = (SecretKey) encryptedObjects[0].getObject(privateKey);
            serviceObject = encryptedObjects[1].getObject(keyToService);
        } catch (IOException ex) {
            Logger.getLogger(AuthenticatedUser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AuthenticatedUser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AuthenticatedUser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AuthenticatedUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return serviceObject;
    }

    public void execute() {
        Scanner input = new Scanner(System.in);
        String command = "";
        try {
            while (true) { //Reads specified user commands.
                System.out.print("> ");
                command = input.nextLine();
                if (command.equals("exit")) {
                    break;
                }
                this.processCommand(command.toLowerCase().split(" "));
            }
        } catch (RemoteException ex) {
            System.out.println("Lost connection to server. Server may have been closed/crashed.");
        } catch (NoSuchElementException ex){
            System.out.println("No command found.");
        }
        System.out.println("Closing interface.");
    }
    
    public abstract void processCommand(String[] paramters) throws RemoteException;
    
}

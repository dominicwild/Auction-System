package Security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General methods to be used by users of the system to authenticate themselves,
 * create keys and signatures and use these for verification purposes.
 *
 * @author DominicWild
 */
public class AuctionSecurity {

    //Default values for algorithms, providers and random number generation methods used.
    public static final String ALGORITHM_KEY = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM_RAND = "SHA1PRNG";
    private static final String PROVIDER_RAND = "SUN";
    private static final String SIG_ALGORITHM = "SHA1withRSA";
    //Path variables
    private static final String SERVER_STORAGE = "Database/";     //Demonstrates data held on server side.
    public static final String CLIENT_STORAGE = "Clients/";    //Demonstrates data held on client side.
    public static final String ACCOUNT_PATH = SERVER_STORAGE + "Accounts/"; //Directory to hold all personal account information.
    private static final String PUBLIC_KEY_PATH = "PublicKeys/";    //Directory to hold all public keys that everyone has access to.

    /**
     * Registers a user with the security system. This involves creating a
     * public and private key for a user, among creating a details file
     * containing their email.
     *
     * @param owner The name of the person we're registering with our security
     * system.
     * @return The private key we made for the person being registered.
     */
    public static PrivateKey registerUser(String owner) {
        if (!owner.equals("Server")) {
            PrivateKey key = registerKeys(owner, true);
            makeDetailsFile(owner);
            return key;
        } else {
            System.out.println("Invalid register name.");
            System.exit(1);
        }
        return null;
    }

    /**
     * Registers only a set of keys for someone in our system.
     *
     * @param owner The person to generate the key files for.
     * @return The private key for the person we have registered keys for.
     */
    public static PrivateKey registerKeys(String owner, boolean isClient) {
        KeyPair keys = makeKeyPair();
        storeKeys(keys, owner, isClient);
        return keys.getPrivate();
    }

    /**
     * Creates a details file for a person, which holds user information. Such
     * as an email.
     *
     * @param owner The person we're creating a details file for.
     */
    private static void makeDetailsFile(String owner) {
        String userAccountPath = ACCOUNT_PATH + owner + "/";
        new File(userAccountPath).mkdirs(); //Ensure path exists
        File details = new File(userAccountPath + "Details.csv");
        certifyExistence(details); //Make sure file exists
        Scanner in = new Scanner(System.in);
        System.out.print("Insert email: "); //Query and store email
        String email = in.nextLine();
        try {
            PrintWriter writer = new PrintWriter(details);
            writer.print(email);
            writer.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "File not find while creating details file: " + details.getAbsolutePath(), ex);
        }
    }

    /**
     * Stores a passed KeyPairs keys to be registered under a given passed name.
     *
     * @param keys The KeyPair to register.
     * @param name The name to be stored under.
     * @param isClient If this function was called by a client or not.
     */
    private static void storeKeys(KeyPair keys, String name, boolean isClient) {
        String userAccountPath = "";
        if (isClient) {
            userAccountPath = CLIENT_STORAGE + name + "/";
        } else {
            userAccountPath = SERVER_STORAGE + name + "/";
        }
        new File(PUBLIC_KEY_PATH).mkdirs(); //Ensure necessary directories exist
        new File(userAccountPath).mkdirs();
        File filePublicKey = new File(PUBLIC_KEY_PATH + name + "Public.key");
        File filePrivateKey = new File(userAccountPath + "Private.key");
        certifyExistence(filePublicKey);
        certifyExistence(filePrivateKey);
        storeSerialObject(filePublicKey, keys.getPublic());
        storeSerialObject(filePrivateKey, keys.getPrivate());
    }

    /**
     * Stores a passed KeyPairs keys to be registered under a given passed name.
     *
     * @param keys The KeyPair to register.
     * @param name The name to be stored under.
     */
    private static void storeKeys(KeyPair keys, String name) {
        storeKeys(keys, name, true);
    }

    /**
     * Gets the public key of a specified owner.
     *
     * @param owner The public key of the owner to retrieve.
     * @return The public key of the passed owner.
     */
    public static PublicKey getPublicKey(String owner) {
        return (PublicKey) getSerializedObject(PUBLIC_KEY_PATH + owner + "Public.key");
    }

    /**
     * Gets the private key of a specified owner.
     *
     * @param owner The private key of the owner to retrieve.
     * @return The private key of the passed owner.
     */
    public static PrivateKey getPrivateKey(String owner) {
        String dir;
        if (owner.equals("Server")) {
            dir = SERVER_STORAGE;
        } else {
            dir = CLIENT_STORAGE + owner + "/";
        }

        return (PrivateKey) getSerializedObject(dir + "Private.key");

    }

    /**
     * Stores a given serialised object o, to a passed file.
     *
     * @param file The file to store object o in.
     * @param o The object to store in a serialised file.
     */
    private static void storeSerialObject(File file, Object o) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(o);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Can't find file: " + file.getAbsolutePath(), ex);
        } catch (IOException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Failure to serialize object " + o, ex);
        }
    }

    /**
     * Signs a passed integer with the given private key and returns the signed
     * bytes created from such a signing.
     *
     * @param nonce The number to use for signing.
     * @param signWith The private key to sign them with.
     * @return The signed bytes as a result of the signing.
     */
    public static byte[] sign(int nonce, PrivateKey signWith) {
        try {
            Signature sig = Signature.getInstance(SIG_ALGORITHM);
            sig.initSign(signWith);
            byte[] nonceBytes = ByteBuffer.allocate(4).putInt(nonce).array(); //Turn integer into a 4 size byte array for signing
            sig.update(nonceBytes);
            return sig.sign();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Can't find algorithm for signature: " + SIG_ALGORITHM, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Invalid private key for signature.", ex);
        } catch (SignatureException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "SignatureException during signing of bytes.", ex);
        }
        return null;
    }

    /**
     * Verifies the signature of some signed bytes of the passed nonce integer.
     *
     * @param signedBytes The bytes that have been signed.
     * @param nonce The nonce by which the signedBytes should refer to.
     * @param ownersKey The owner's public key who signed the bytes.
     * @return A boolean representing of if the bytes are verified to be from
     * the owners public key or not.
     */
    public static boolean verifySig(byte[] signedBytes, int nonce, PublicKey ownersKey) {
        try {
            Signature sig = Signature.getInstance(SIG_ALGORITHM);
            sig.initVerify(ownersKey);
            byte[] nonceBytes = ByteBuffer.allocate(4).putInt(nonce).array(); //Turn integer into a 4 size byte array for signing
            sig.update(nonceBytes);
            return sig.verify(signedBytes);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Can't find algorithm for signature: " + SIG_ALGORITHM, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Invalid private key for signature.", ex);
        } catch (SignatureException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "SignatureException during signing of bytes.", ex);
        }
        return false;
    }

    /**
     * A call to initiate a challenge against a machine for verification. The
     * machine being challenged must make a subsequent call to receive challenge
     * for this call to be fully successful.
     *
     * @param in An object input stream with the receiver being the person to be
     * challenged.
     * @param out An object output stream with the receiver being the person to
     * be challenged.
     * @param challengedKey The public key of the person we're challenging, used
     * to verify their identity.
     * @return A boolean representation of if they successfully identified
     * themselves.
     */
    public static boolean initiateChallenge(ObjectInputStream in, ObjectOutputStream out, PublicKey challengedKey) {
        try {
            int nonce = (int) (Math.random() * Integer.MAX_VALUE); //Generate random value for the challenged to sign.
            out.writeObject(nonce); //Send challange
            byte[] signedBytes = (byte[]) in.readObject();
            return AuctionSecurity.verifySig(signedBytes, nonce, challengedKey); //Verify their signature.
        } catch (IOException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "IOException while initiating challenge.", ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "ClassNotFound while initiating challenge.", ex);
        }
        return false;
    }

    /**
     * A method to deal with the issuing of a challenge.
     *
     * @param in An object input stream with the receiver being the person
     * setting the challenge.
     * @param out An object output stream with the receiver being the person
     * setting the challenge.
     * @param myKey The key of the person being challenged to verify their
     * identity.
     */
    public static void recieveChallenge(ObjectInputStream in, ObjectOutputStream out, PrivateKey myKey) {
        try {
            int nonce = (int) in.readObject(); //Retreive challenge
            byte[] signedBytes = AuctionSecurity.sign(nonce, myKey);
            out.writeObject(signedBytes); //Send off signed bytes
        } catch (IOException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "IOException while awaiting a challenge.", ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "ClassNotFound while awaiting a challenge.", ex);
        }
    }

    /**
     * Gets the serialised object from a specified file name.
     *
     * @param fileName The name of the file with the secret key.
     * @return The SecretKey object serialised within a the specified file.
     */
    private static Object getSerializedObject(String fileName) {
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            return in.readObject(); //Reads the object in the file.
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "File not found " + fileName + ".", ex);
        } catch (IOException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "I/O Exception occured when attempting to get secret key.", ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "No object found in file.", ex);
        }
        return null;
    }

    /**
     * Ensures the existence of the passed file. If it does not exist, it is
     * created.
     *
     * @param file The file to ensure the existence of.
     */
    private static void certifyExistence(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Can't make file: " + file.getAbsolutePath(), ex);
            }
        }
    }

    /**
     * Creates a key pair, to generate a pair of public and private keys.
     *
     * @return The KeyPair object generated.
     */
    private static KeyPair makeKeyPair() {
        try { //Generate KeyPair with defined algorithms and specified parameters.
            KeyPairGenerator keyMaker = KeyPairGenerator.getInstance(ALGORITHM_KEY);
            keyMaker.initialize(KEY_SIZE, SecureRandom.getInstance(ALGORITHM_RAND, PROVIDER_RAND));
            return keyMaker.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Invalid algorithm provided for key generator.", ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(AuctionSecurity.class.getName()).log(Level.SEVERE, "Invalid provider provided for key generator.", ex);
        }
        return null;
    }

}

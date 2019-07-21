
import Server.Auction;
import Server.AuctionUser;
import ServiceInterfaces.AuctionUserInterface;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.KeyGenerator;



/**
 *
 * @author DominicWild
 */
public class Test {
    
    public static void main(String[] args) throws IOException{
        
        try {
            Scanner in = new Scanner(System.in);
            
//        String command = in.next();
//        double start = in.nextDouble();
//        double end = in.nextDouble();
//        String desc = in.nextLine();
//        System.out.println(command);
//        System.out.println(start);
//        System.out.println(end);
//        System.out.println(desc);
//        int one = Integer.MAX_VALUE;
//        int two = (int)((double)Integer.MAX_VALUE*0.8);
//        
//        System.out.println("one: " + (byte)one + " two: " + (byte)two);
//
//        
//        System.out.println(String.format("%012d", 1465465400));
            HashMap<Integer, Auction> liveAuctions = new HashMap<>();
            HashMap<Integer, Auction> liveAuctions2 = new HashMap<>();
            
            Auction a = new Auction("asfa",4534,"sfkdjs",34543);
            Auction a2 = new Auction("asfertera",4534,"sfkdjs",34543);
            liveAuctions.put(1, a2);
            liveAuctions2.put(1, a2);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(liveAuctions);
            oos.close();

            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(baos.toByteArray());
            BigInteger sum = new BigInteger(1,m.digest());
            
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
            oos2.writeObject(liveAuctions2);
            oos2.close();

            MessageDigest m2 = MessageDigest.getInstance("MD5");
            m2.update(baos2.toByteArray());
            BigInteger sum2 = new BigInteger(1, m2.digest());

            System.out.println(KeyGenerator.getInstance("DES").generateKey().getEncoded().length);

            AuctionUser user = new AuctionUser(null,"Server"); 
            Naming.rebind("Test", user);
            UnicastRemoteObject.unexportObject(user, true);
            for (String s : Naming.list("rmi://localhost/")) {
//                if (s.contains("/NamedObjects/")) {
                    System.out.println(s);
//                }
            }
            
            AuctionUserInterface i = (AuctionUserInterface) Naming.lookup("rmi://localhost/Test");
            i.auctionListings();
            

//        File file = new File("User/test/3/3/5/3");
//        System.out.println(file.mkdirs());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotBoundException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
}

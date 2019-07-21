package Client;


import Server.ReplicationManager;
import ServiceInterfaces.AuctionUserInterface;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Program by which a bidder interacts with the auction system.
 *
 * @author DominicWild
 */
public class UserProgram extends AuthenticatedUser {

    private AuctionUserInterface bidder;        //The interface by which the bidder interacts with the auction system.
    

    public static void main(String[] args) {
        UserProgram user = new UserProgram();
        try {
            user.displayAuctions();
        } catch (RemoteException ex) {
            Logger.getLogger(ReplicationManager.class.getName()).log(Level.SEVERE,null, ex);
            System.out.println("Lost connection to server. Server may have been closed/crashed.");
            System.exit(1);
        }
        user.execute();
    }

    /**
     * Initialises the bidder interface and common input variables.
     */
    public UserProgram() {
        super();
        AuctionUserInterface bidder = (AuctionUserInterface) this.getService("Bidder");
        this.bidder = bidder;
      
    }

    /**
     * Displays all auctions on the server to console.
     */
    public void displayAuctions() throws RemoteException {
        System.out.println(this.bidder.auctionListings());
    }

    /**
     * Processes commands that can run by this program.
     *
     * @param command The command to run.
     */
    public void processCommand(String[] args) throws RemoteException {
        try {
            switch (args[0]) {
                case "show":
                    this.displayAuctions();
                    break;
                case "bid":
                    this.bid(args);
                    break;
                default:
                    System.out.println("Unknown command.");
                    break;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid command. Should be: " + this.commandTip(args[0]));
        }
    }

    /**
     * Takes a bid string, in the form [auction id] [amount] to attempt to place
     * a bid on an auction with the auction id presented.
     *
     * @param bidString a String in the form [auction id] [amount].
     */
    private void bid(String[] args) throws RemoteException {
        int auctionID = Integer.parseInt(args[1]);
        double amount = Double.parseDouble(args[2]);
        String result = this.bidder.bid(auctionID, amount);
        System.out.println(result);
    }

    /**
     * Returns the format of a argument in the program.
     *
     * @param arg The command to get the format/tip of.
     * @return A string of the tip/format.
     */
    private String commandTip(String arg) {
        switch (arg) {
            case "bid":
                return "bid [auctionID] [amount]";
            default:
                return arg + " is an unknown command.";
        }
    }

}

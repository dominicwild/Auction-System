package Server;


import ServiceInterfaces.AuctionUserInterface;
import java.rmi.RemoteException;

/**
 * Implementation of the interface that bidders use to interact with the system.
 *
 * @author DominicWild
 */
public class AuctionUser extends NamedObject implements AuctionUserInterface {

    private ReplicationManager server; //The server this implementation will run on.

    /**
     * Creates an instance of this bidder implementation associated with a
     * passed server.
     *
     * @param server The server to run this implementation on.
     */
    public AuctionUser(ReplicationManager server, String owner) throws RemoteException {
        super(owner);
        this.server = server;
    }

    /**
     * Places a bid on an auction with the specified auction id.
     * @param auctionID The id of the auction to place the bid on.
     * @param amount The amount to bid.
     * @return An output on whether the bid was successful or not.
     */
    @Override
    public String bid(int auctionID, double amount) throws RemoteException {
        String email = this.server.emailFromFile(this.owner);
        try {
            this.noteUse();
            if (this.server.placeBid(auctionID, amount, this.owner, email)) { //See if bid was successfully placed on server.
                System.out.println("Bid of " + amount + " has successfully been placed on auction " + auctionID + " by " + this.owner + " with email " + email);
                return "Your bid of " + amount + " has successfully been placed on auction " + auctionID;
            } else {
                return "Your bid has failed to placed on auction " + auctionID;
            }
        } catch (NullPointerException e) {
            return "The auction that was attempted to be bid on has either been closed or doesn't exist.";
        }
    }

    /**
     * Returns a string of all current live auctions on the server.
     * @return Listing of all live auctions on the server.
     */
    @Override
    public String auctionListings() throws RemoteException {
        this.noteUse();
        return this.server.getListings();
    }
}

package ServiceInterfaces;



import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface by which bidders interact with the system.
 * @author DominicWild
 */
public interface AuctionUserInterface extends Remote {

    /**
     * Registers a bid on an item with the specified amount.
     * @param auctionID The id of the auction to place the
     * @param amount The amount to bid on the auction.
     */
    public String bid(int auctionID, double amount) throws RemoteException;

    /**
     * Prints a list of all available auctions to console.
     */
    public String auctionListings() throws RemoteException;

}

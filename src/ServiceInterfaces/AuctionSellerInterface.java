package ServiceInterfaces;


import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface by which sellers interact with the Auction System.
 * @author DominicWild
 */
public interface AuctionSellerInterface extends Remote {

    /**
     * Creates an auction listing on the server with the passed parameters
     *
     * @param startPrice The starting price of this item.
     * @param description The description of this item.
     * @param minPrice The minimum acceptable price for this item.
     * @return The auction ID of this created listing.
     */
    public int createAuction(double startPrice, String description, double minPrice) throws RemoteException;

    /**
     * Closes the auction listing with the passed auction ID. The status of the
     * auction is then printed to console. If there is a winner, the winner
     * details are printed. If not, then it will state the reserved price was
     * not met.
     *
     * @param owner The name of the person who created this auction. Only the owner may close the auction.
     * @param auctionID The auction ID of this of the auction to remove.
     */
    public String closeAuction(int auctionID) throws RemoteException;

}

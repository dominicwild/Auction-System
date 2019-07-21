package Server;


import ServiceInterfaces.AuctionSellerInterface;
import java.rmi.RemoteException;

/**
 * An implementation of an interface sellers will use to interact with the
 * Auction system.
 *
 * @author DominicWild
 */
public class AuctionSeller extends NamedObject implements AuctionSellerInterface {

    private ReplicationManager server;       //The auction server this implementation is associated with.

    /**
     * Creates an instance of AuctionSeller to the specified server to manage
     * auction transactions.
     *
     * @param server The server to associate with.
     * @param owner The owner of this object.
     */
    public AuctionSeller(ReplicationManager server, String owner) throws RemoteException {
        super(owner);
        this.server = server;
    }

    /**
     * Creates an auction and registers this with the server.
     *
     * @param startPrice The starting price of the auction.
     * @param description The description of the item on auction.
     * @param minPrice The minimum expected price for the auction item.
     * @return The id of the auction created.
     */
    @Override
    public int createAuction(double startPrice, String description, double minPrice) throws RemoteException {
        if (startPrice < 0) { //If we get a negative startPrice, assume a startPrice of 0.
            startPrice = 0;
        }
        if (minPrice < 0) { //If we get a negative minPrice, assume a minPrice of 0.
            minPrice = 0;
        }
        Auction a = new Auction(this.owner, startPrice, description, minPrice);
        this.noteUse();
        return this.server.addAuction(a);
    }

    /**
     * Closes a auction with the specified ID and gives the status on its
     * conclusion.
     *
     * @param auctionID The auction id of the auction to end.
     * @return An output on the status of the conclusion of the auction.
     */
    @Override
    public String closeAuction(int auctionID) throws RemoteException {
        Auction toClose = this.server.removeAuction(this.owner, auctionID); //Close the auction on server side.
        if (toClose == null) { //Ensure the auction we're going to close exists/is valid
            return "You can't remove auction with auctionID " + auctionID + ". This may be because it does not exist, has already been removed or you don't own it.";
        }
        
        String preMessage = "[Auction Closed]\n";
        this.noteUse();
        if (toClose.getReservePrice() < toClose.getCurrentPrice()) { //If the reserve price is greater
            return preMessage + "The winner is: " + toClose.getBidderName() + " with email: " + toClose.getBidderEmail();
        } else {
            return preMessage + "This auction failed to meet its reserved price of " + toClose.getReservePrice() + " with highest bid " + toClose.getCurrentPrice();
        }

    }
}

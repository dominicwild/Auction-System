package Server;

import java.io.Serializable;

/**
 * Representation of an Auction and its related variables.
 * @author DominicWild
 */
public class Auction implements Serializable {

    private double currentPrice;            //Current highest bid amount
    private String description;             //Description of the item
    private double reservePrice;            //The reserve price
    private String bidderEmail;             //The email of the current highest bidder.
    private String bidderName;              //The name of the current highest bidder.
    private String owner;                   //The name of the person who created the auction.

    /**
     * Basic constructor to make an auction.
     * @param startPrice The starting price for the item in this auction.
     * @param description The description of what the item under auction is.
     * @param minPrice The minimum expected price for this auction.
     */
    public Auction(String owner, double startPrice, String description, double minPrice) {
        this.currentPrice = startPrice;
        this.description = description;
        this.reservePrice = minPrice;
        this.bidderName = "***No one***"; //default to no max bidder.
        this.bidderEmail = "***No Email***"; //default to no email.
        this.owner = owner;
    }
    /**
     * Returns the email of the highest bidder.
     * @return The email of the highest bidder.
     */
    public String getBidderEmail() {
        return bidderEmail;
    }

    /**
     * Sets the highest bidders email.
     * @param bidderEmail The email to set the highest bidders email to.
     */
    public void setBidderEmail(String bidderEmail) {
        this.bidderEmail = bidderEmail;
    }

    /**
     * Gets the name of the highest bidder.
     * @return The name of the highest bidder.
     */
    public String getBidderName() {
        return bidderName;
    }

    /**
     * Sets the name of the highest bidder.
     * @param bidderName The name to set the highest bidders name to.
     */
    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    /**
     * Gets the current highest bid value on this auction.
     * @return The current highest bid value on this auction.
     */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /**
     * Gets the reserve price for this auction.
     * @return The reserve price for this auction.
     */
    public double getReservePrice() {
        return reservePrice;
    }

    /**
     * Sets the current highest bid for this auction.
     * @param currentPrice The value to set the highest bid to.
     */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    /**
     * Gets the description of this auction item.
     * @return The description of this auction item.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the owner of this auction. The one who created it.
     * @return The name of the owner of this auction.
     */
    public String getOwner() {
        return owner;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Auction) {
            Auction a = (Auction) obj;
            return a.getBidderEmail().equals(this.getBidderEmail())
                    && a.getBidderName().equals(this.getBidderName())
                    && a.getCurrentPrice() == this.getCurrentPrice()
                    && a.getDescription().equals(this.getDescription())
                    && a.getOwner().equals(this.getOwner())
                    && a.getReservePrice() == this.getReservePrice();
        } else {
            return super.equals(obj);
        }
    }




}

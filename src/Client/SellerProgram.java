package Client;


import ServiceInterfaces.AuctionSellerInterface;
import java.rmi.RemoteException;

/**
 * A program to be used by sellers to registered their auctions on the system.
 *
 * @author DominicWild
 */
public class SellerProgram extends AuthenticatedUser {

    private AuctionSellerInterface seller;      //The interace to use to interact with the Auction System.

    public static void main(String[] args) {
        SellerProgram auctioner = new SellerProgram();
        System.out.println("Seller Program running, input command below!");
        auctioner.execute(); //Execute program.
    }

    /**
     * Initialise interface used to make the system function.
     */
    public SellerProgram() {
        super(); //Pass name up for authentication to be handled.
        this.seller = (AuctionSellerInterface) this.getService("Seller");
    }

    /**
     * Process the specified command entered by the user.
     *
     * @param command The command to execute.
     */
    public void processCommand(String[] args) throws RemoteException {
        try {
            switch (args[0]) {
                case "create":
                    this.createAuction(args);
                    break;
                case "close":
                    this.closeAuction(args);
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
     * Creates an auction on the server.
     *
     * @param commandString The parameters for which to create the auction. In
     * the format [startPrice] [minPrice] [description]
     */
    private void createAuction(String[] args) throws RemoteException {
        double startPrice = Double.parseDouble(args[1]);
        double minPrice = Double.parseDouble(args[2]);
        String description = "";

        for (int i = 3; i < args.length; i++) { //Read in all of the description strings
            if (i != args.length - 1) {
                description += args[i] + " ";
            } else { //On the final string we're adding, we don't want the extra space.
                description += args[i];
            }
        }
        int id = this.seller.createAuction(startPrice, description, minPrice);
        System.out.println("Created auction with id: " + id); //Give feedback on auction created.
    }

    /**
     * Close an auction specified by its id.
     *
     * @param commandString The parameters for this auction to be closed. In the
     * format: id
     */
    private void closeAuction(String[] args) throws RemoteException {
        String result = this.seller.closeAuction(Integer.parseInt(args[1]));
        System.out.println(result);
    }

    /**
     * Returns the format of a argument in the program.
     * @param arg The command to get the format/tip of.
     * @return A string of the tip/format.
     */
    private String commandTip(String arg) {
        switch(arg){
            case "create":
                return "create [startPrice] [minPrice] [description]";
            case "close":
                return "close [auctionID]";
            default:
                return arg + " is an unknown command.";
        }
    }

}

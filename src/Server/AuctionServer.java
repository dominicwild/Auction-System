package Server;

import Client.UserProgram;
import Security.AuctionSecurity;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.util.Util;

/**
 * An auction server that hosts all auctions.
 *
 * @author DominicWild
 */
public class AuctionServer extends ChannelMember {

    private HashMap<Integer, Auction> liveAuctions = new HashMap<>();   //Holds all auctions currently running
    private final String TEST_FILE = "auctions.csv";                    //File that loads some test auctions into the system.
    private static final int INITIAL_ID = 1000;                         //The initial ID to start at.

    private int incrementID = INITIAL_ID;                               //The incrementing id for making auction id's.
    private long checkSum = 0;                                          //Make a checksum for validating the state of the database

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.joinGroupChannel();
        server.init();
        server.waitForCommands();
    }

    /**
     * A general initialisation function.
     */
    private void init() {
        this.rpc.setServerObject(this);
        this.stateInitialisation(); //Get state before dealing with rpc calls.
        this.rpc.start();
    }

    /**
     * Initialize the state of this auction server when it boots up and joins
     * the auction channel.
     */
    private void stateInitialisation() {
        View view = this.auctionChannel.getView();
        boolean stateSet = false;
        while (!stateSet) { //Can't join unless we get state.
            try {
                this.auctionChannel.getState(view.get(1), 5000, true); //Get the state of the oldest auction server.
                stateSet = true;
            } catch (Exception ex) {
                Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Error getting state.", ex);
            }
        }
        Message msg = new Message(view.get(0), new Object[]{this.rpcChannel.getAddress(), this.auctionChannel.getAddress()});
        try { //Send rpc mapping message.
            this.auctionChannel.send(msg);
        } catch (Exception ex) {
            Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Error sending mapping of rpc channel address to auctionChannel address.", ex);
        }
    }

    /**
     * Waits for commands to be executed on the server.
     */
    public void waitForCommands() {
        try {
            Scanner input = new Scanner(System.in);
            String command = "";
            System.out.println("Auction Server running, input command below!");
            while (!command.equals("exit")) {
                System.out.print("> "); //Give prompt to enter command
                command = input.nextLine();
                processCommand(command);
            }
        } catch (NoSuchElementException e) {
            System.out.println("No command found. Exiting.");
        }
    }

    /**
     * Processes the passed commands adequately.
     *
     * @param command The command to process and run.
     */
    private void processCommand(String command) {
        if (command.trim().equals("length")) { //Prints how many auctions we have.
            System.out.println("There are currently " + this.liveAuctions.size() + " running auctions.");
        } else if (command.trim().equals("rand")) { //Add a random auction for testing
            this.addAuction(new Auction("Rand", 1, "Rand", 2));
        }
    }

    /**
     * Adds an auction to the system.
     *
     * @param a The auction to add to the system.
     * @return The id of the currently created auction or -1 if auction was
     * failed to be added. This may be due to capacity limits.
     */
    public int addAuction(Auction a) {
        int attempts = 0; //Id to identify auction.
        synchronized (this.liveAuctions) { //Must ensure consistent state when adding a new auction, to not get conflicting ID's.
            while (liveAuctions.containsKey(incrementID)) {
                if (attempts < 5000) {
                    incrementID++;
                    attempts++;
                } else { //Been here for an unusually long time, assume we have reached capacity.
                    return -1;
                }
            }
            this.addAuction(a, incrementID);
            System.out.println("Added auction with ID " + incrementID);
        }
        this.updateCheckSum(a);
        this.updateCheckSum(this.incrementID);
        return incrementID;
    }

    /**
     * Adds an auction with a specified id to the system.
     *
     * @param a The auction to add.
     * @param id The ID that identifies it.
     */
    private void addAuction(Auction a, int id) {
        liveAuctions.put(id, a);
    }

    /**
     * Removes a auction with the id specified.
     *
     * @param remover The person attempting to remove this auction.
     * @param auctionID The id of the auction to remove.
     * @return The auction that was removed. Or null, if an invalid remove was
     * attempted by a non-remover.
     */
    public Auction removeAuction(String remover, int auctionID) {
        try {
            synchronized (this.liveAuctions.get(auctionID)) { //Ensure no one is currently accessing the auction we're trying to remove.
                if (this.liveAuctions.get(auctionID).getOwner().equals(remover)) {
                    this.updateCheckSum(remover);
                    this.updateCheckSum(auctionID);
                    return liveAuctions.remove(auctionID);
                } else {
                    return null;
                }
            }
        } catch (NullPointerException e) { //If we get null, then auction does not exist.
            return null;
        }
    }

    public Auction getAuction(int auctionID) {
        return this.liveAuctions.get(auctionID);
    }

    /**
     * Place a bid on the specified auction, with the specified details.
     *
     * @param auctionID The id of the auction to bid on.
     * @param amount The amount to bid.
     * @param name The name of the bidder.
     * @param email The email of the bidder.
     * @return A boolean representing whether the bid was successful or not.
     */
    public boolean placeBid(int auctionID, double amount, String name, String email) {
        synchronized (this.liveAuctions.get(auctionID)) { //Ensure no one else attempting to bid (or do anything) on auction
            Auction a = this.liveAuctions.get(auctionID);
            if (a.getCurrentPrice() < amount) { //Check if valid bid and update values.
                a.setCurrentPrice(amount);
                a.setBidderName(name);
                a.setBidderEmail(emailFromFile(name));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Gets a listing of all current bids.
     *
     * @return A string of all current bids.
     */
    public String getListings() {
        if (this.liveAuctions.isEmpty()) { //Check if there are bids
            return "There are no listings.";
        } else {
            //Print a formatted table for the current bids and their visible details.
            String separator = "------------------------------------------------------------------------------";
            String listings = separator + "\n";
            listings += String.format("|%-10s|%-11s|%-12s|%-40s|", "Auction ID", "Highest Bid", "Name", "Description") + "\n";
            listings += separator + "\n";
            for (int id : this.liveAuctions.keySet()) {
                Auction a = this.liveAuctions.get(id);
                listings += String.format("|%-10d|%-11.2f|%-12s|%-40s|", id, a.getCurrentPrice(), a.getBidderName(), a.getDescription()) + "\n";
            }
            listings += separator + "\n";
            return listings;
        }
    }

    /**
     * Loads test data from a file called auctions.csv.
     */
    private void loadTestData() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(TEST_FILE)));) {
            String line = line = reader.readLine();
            int i = 0;
            while (line != null) { //Read until no lines left
                //Add auctions in accordance to csv file values
                String[] parameters = line.split(",");
                double startPrice = Double.parseDouble(parameters[0].trim());
                String description = parameters[1];
                double minPrice = Double.parseDouble(parameters[2].trim());
                this.addAuction(new Auction("Test", startPrice, description, minPrice), i);
                line = reader.readLine();
                i++;
            }
            System.out.println("Added test data.");
        } catch (FileNotFoundException ex) {
            System.out.println("Couldn't find testing data file: auctions.csv.");
        } catch (IOException ex) {
            System.out.println("Failed to read testing data file: auctions.csv.");
        }
    }

    public String emailFromFile(String name) {
        String userAccountPath = AuctionSecurity.ACCOUNT_PATH + name + "/";
        try {
            return Files.readAllLines(Paths.get(userAccountPath + "Details.csv")).get(0);
        } catch (IOException ex) {
            Logger.getLogger(UserProgram.class.getName()).log(Level.SEVERE, "IOException retrieving stored email in: Details.csv", ex);
        }
        return null;
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        System.out.println("Getting state for a new node.");
        synchronized (this.liveAuctions) { //Ensure no one changes state while we're sending it.
            Util.objectToStream(new Object[]{this.incrementID, this.liveAuctions, this.checkSum}, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        System.out.println("Setting state");
        synchronized (this.liveAuctions) { //Ensure no one accesses the state while we're setting it
            Object[] state = (Object[]) Util.objectFromStream(new DataInputStream(input));
            this.incrementID = (int) state[0];
            this.liveAuctions.clear();
            this.liveAuctions.putAll((HashMap<Integer, Auction>) state[1]);
            this.checkSum = (long) state[2];
        }
        System.out.println(liveAuctions.size() + " auctions loaded from connection to cluster.");
    }

    /**
     * Triggers when receiving a Message from a JChannel.
     *
     * @param msg The message we received.
     */
    @Override
    public void receive(Message msg) {
        System.out.println("Got a message.");
        this.handleCommandMessage(msg);
    }

    /**
     * Handles a Message that is formatted as a command. Makes the server
     * execute a command which cannot be done using the RPC channel.
     *
     * @param msg The command message detailing what must be done.
     */
    private void handleCommandMessage(Message msg) {
        try {
            Object[] args = (Object[]) msg.getObject();
            Command command = (Command) args[0];
            switch (command) {
                case SYNC:
                    this.sync((Address) args[1]);
                default:
                    System.out.println("Got unknown command.");
            }
        } catch (ClassCastException e) {
            System.out.println("Invalid command message format. Unexpected class found in arguments.");
        }
    }

    /**
     * Synchronises state of this auction server with the server that has the
     * passed syncTarget address. Used by the replication manager when a de-sync
     * occurs.
     *
     * @param syncTarget The address of the server to copy state from.
     */
    public void sync(Address syncTarget) {
        try {
            System.out.println("Syncing with: " + syncTarget.toString());
            this.auctionChannel.getState(syncTarget, 5000);
        } catch (Exception ex) {
            Logger.getLogger(AuctionServer.class.getName()).log(Level.SEVERE, "Error getting synchronized state.", ex);
        }
    }

    /**
     * Updates check in reference to the given auction. Adds all unicode
     * characters to the checksum, among adding prices multiplied by 100 to the
     * checksum.
     *
     * @param a The auction to update the checksum with.
     */
    private void updateCheckSum(Auction a) {
        this.updateCheckSum(new String[]{a.getBidderEmail(), a.getBidderName(), a.getDescription(), a.getOwner()});
        this.updateCheckSum(a.getCurrentPrice());
        this.updateCheckSum(a.getReservePrice());
    }

    /**
     * Update checksum in reference to array is strings passed. Adds all the
     * unicode values of the characters to the checksum.
     *
     * @param toAdd The array of strings to add.
     */
    private void updateCheckSum(String[] toAdd) {
        for (String s : toAdd) {
            for (int i = 0; i < s.length(); i++) {
                this.checkSum += (int) s.charAt(i);
            }
        }
    }

    /**
     * Update checksum in reference to the string passed. Adds all the unicode
     * values of the characters to the checksum.
     *
     * @param toAdd The array of strings to add.
     */
    private void updateCheckSum(String toAdd) {
        this.updateCheckSum(new String[]{toAdd});
    }

    /**
     * Adds a double, which is assumed to a price in the auction server context,
     * to the checksum by turning the price into an integer and adding it to the
     * checksum.
     *
     * @param price The price to add to the checksum.
     */
    private void updateCheckSum(double price) {
        this.checkSum += (int) Math.floor(price * 100);
    }

    /**
     * Adds an integer to the checksum.
     *
     * @param The integer to add to the checksum.
     */
    private void updateCheckSum(int integer) {
        this.checkSum += integer;
    }

    /**
     * Gets the checksum for this auction servers state.
     *
     * @return A checksum representing the state of the server as a whole.
     */
    public long getCheckSum() {
        return this.checkSum;
    }

}

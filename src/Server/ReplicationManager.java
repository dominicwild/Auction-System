package Server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;

/**
 * A class that models a replication manager. It is responsible for ensuring the
 * organisation of AuctionServers in the AuctionSystem. It must be the first
 * thing launched to successfully coordinate all the AuctionServers.
 *
 * @author DominicWild
 */
public class ReplicationManager extends ChannelMember {

    private RequestOptions options;                      //The options for RPC requests.
    private HashMap<Address, Address> rpcMappings;       //Used to map RPC channel addresses to AuctionChannel addresses for message sending.
    private HashMap<String,Remote> sessions;            //The current sessions in progress of connected users.
    private final int RMI_PORT = 1099;

    public static void main(String[] args) {
        ReplicationManager repServer = new ReplicationManager();
        AuthenticationServer authServer = new AuthenticationServer(repServer);
        authServer.execute();
    }

    /**
     * Initialises the managers basic variables and state.
     */
    public ReplicationManager() {
        super();
        this.init();
        Thread idleWatch = new Thread(new IdleWatcher(this.sessions));
        idleWatch.setDaemon(true);
        idleWatch.start();
    }

    /**
     * Initialises the server with the objects it hosts for clients to connect
     * to. Among registering these on the RMI server for distribution.
     */
    public void init() {
        this.joinGroupChannel();
        this.rpc.start();
        this.rpcMappings = new HashMap<>();
        this.sessions = new HashMap<>();
        this.options = new RequestOptions(ResponseMode.GET_ALL, 5000);
        bindRMIObjects();
    }

    /**
     * Binds the RMI objects to the RMI server.
     */
    private void bindRMIObjects() {
        try {
            Naming.rebind("Seller", new AuctionSeller(this,"Server"));
            Naming.rebind("Bidder", new AuctionUser(this,"Server"));
        } catch (RemoteException ex) {
            ex.printStackTrace();
            System.out.println("No RMI server found to register services.");
            System.exit(1);
        } catch (MalformedURLException ex) {
            System.out.println("Malformed URL during initialization.");
        }
    }
    
    /**
     * Binds the RMI objects to the RMI server.
     */
    public void bindObject(String ref, UnicastRemoteObject o) {
        try {
            Naming.rebind(ref, o);
        } catch (RemoteException ex) {
            System.out.println("No RMI server found to register services.");
            System.exit(1);
        } catch (MalformedURLException ex) {
            System.out.println("Malformed URL during initialization.");
        }
    }

    /**
     * Attempts to add an auction to all replication servers and performs
     * necessary sanitary checks, among follow-ups from those checks.
     *
     * @param a The auction to add.
     * @return The ID of the auction added.
     */
    int addAuction(Auction a) {
        this.checkSumVerification();
        MethodCall method = new MethodCall("addAuction", new Object[]{a}, new Class[]{Auction.class});
        RspList<Integer> responses = this.getResponses(method);

        return (int) this.consistencyCheck(responses);
    }

    /**
     * Attempts to remove an auction from all replication servers and performs
     * necessary sanitary checks, among follow-ups from those checks. If the
     * owner is not the person who made the Auction, null is returned.
     *
     * @param owner The owner of the auction.
     * @param auctionID The ID of the auction to close.
     * @return The Auction object removed.
     */
    Auction removeAuction(String owner, int auctionID) {
        this.checkSumVerification();
        MethodCall method = new MethodCall("removeAuction", new Object[]{owner, auctionID}, new Class[]{String.class, int.class});
        RspList<Integer> responses = this.getResponses(method);

        return (Auction) this.consistencyCheck(responses);
    }

    boolean placeBid(int auctionID, double amount, String name, String email) {
        this.checkSumVerification();
        MethodCall method = new MethodCall("placeBid", new Object[]{auctionID, amount, name, email}, new Class[]{int.class, double.class, String.class, String.class});
        RspList<Integer> responses = this.getResponses(method);

        return (boolean) this.consistencyCheck(responses);
    }

    String emailFromFile(String name) {
        this.checkSumVerification();
        MethodCall method = new MethodCall("emailFromFile", new Object[]{name}, new Class[]{String.class});
        RspList<Integer> responses = this.getResponses(method);

        return (String) this.consistencyCheck(responses);
    }

    String getListings() {
        this.checkSumVerification();
        MethodCall method = new MethodCall("getListings", new Object[]{}, new Class[]{});
        RspList<Integer> responses = this.getResponses(method);

        return (String) this.consistencyCheck(responses);
    }

    /**
     * Checks for consistency within replication server responses. It then
     * returns, based on the balance of probabilities, the most likely correct
     * responseOfNode. Among handling any inconsistent servers by synching them.
     *
     * @param responses The list of responses from a Rpc.
     * @return The most likely correct responseOfNode.
     */
    private Object consistencyCheck(RspList responses) {
        HashMap<Object, ArrayList<Address>> freqTable = this.freqHashMap(responses);

        logSeparator();

        int largest = -1;
        Object keyLargest = null;
        for (Object key : freqTable.keySet()) { //Iterate over frequency table to find highest frequency
            if (largest < freqTable.get(key).size()) {
                largest = freqTable.get(key).size();
                keyLargest = key;
            }
        }

        this.syncReplicas(freqTable, keyLargest);

        return keyLargest;
    }

    /**
     * Makes a HashMap that points to an ArrayList that has all the addresses of
     * AuctionServers who gave that particular responseOfNode.
     *
     * @param responses A Response List to make a frequency table out of.
     */
    private HashMap<Object, ArrayList<Address>> freqHashMap(RspList responses) {
        HashMap<Object, ArrayList<Address>> table = new HashMap<>();
        boolean added = false;  //Determines if we've added a particular responseOfNode to the HashMap yet
        for (Object o : responses.keySet()) { //Iterate over all responses to make a frequency table
            Address respondentAddress = (Address) o;
            Object responseOfNode = responses.get(respondentAddress).getValue();
            logResponse(respondentAddress, responseOfNode);
            for (Object tallyKey : table.keySet()) { //tallyKey is a responseOfNode
                if (Objects.equals(tallyKey, responseOfNode)) { //If response of node matches a response in our frequency table, add the address to its ArrayList.
                    table.get(tallyKey).add(respondentAddress);
                    added = true;
                }
            }
            if (!added) { //If we didn't add the responseOfNode add, no entry for it in the table
                table.put(responseOfNode, new ArrayList<>()); //So we add an entry
                table.get(responseOfNode).add(respondentAddress);
            }
            added = false;
        }
        return table;
    }

    /**
     * Synchronises all the nodes who did not give the correct response.
     *
     * @param table The frequency table of all responses given.
     * @param correctKey The accepted correct response.
     */
    private void syncReplicas(HashMap<Object, ArrayList<Address>> table, Object correctKey) {
        Address syncNode = table.get(correctKey).get(0);
        //Get address of auction channel from rpc mapping.
        MethodCall syncMethod = new MethodCall("sync", new Object[]{this.rpcMappings.get(syncNode)}, new Class[]{Address.class});
        System.out.println("Nodes will sync with: " + syncNode.toString());
        for (Object key : table.keySet()) {
            if (key != correctKey) { //If the response isn't the expected response
                for (Address toSync : table.get(key)) { //Go through addresses of all nodes with incorrect response
                    try {
                        this.rpc.callRemoteMethod(toSync, syncMethod, options); //Then send them a message telling them to synchronise with someone who has the correct response.
                    } catch (Exception ex) {
                        Logger.getLogger(ReplicationManager.class.getName()).log(Level.SEVERE, "Error in sending synchronisation message.", ex);
                    }
                }
            }
        }
    }

    private void logResponse(Address respondee, Object value) {
        System.out.printf("[%s]: Gives value = %s\n", respondee.toString(), value);
    }

    private void logSeparator() {
        System.out.println("---------------------------------------------------------------------------");
    }

    /**
     * Receives mappings from rpc channel addresses to auction channel addresses
     * and adds them to the map.
     *
     * @param msg The message containing the mappings.
     */
    @Override
    public void receive(Message msg) {
        Object[] args = (Object[]) msg.getObject();
        if (args[0] instanceof Address && args[1] instanceof Address) {
            System.out.println("Added: " + (Address) args[0] + " -> " + (Address) args[1]);
            this.rpcMappings.put((Address) args[0], (Address) args[1]);
        }
    }

    /**
     * Adjusts mappings in relation to change in view. Removing any mappings
     * which are now invalid.
     */
    @Override
    public void viewAccepted(View view) {
        this.logSeparator();
        View rpcView = this.rpcChannel.getView();
        HashMap<Address, Address> updatedMappings = new HashMap<>();
        for (Address a : rpcView.getMembers()) { //Update the mappings for all current members
            if (!a.equals(this.rpcChannel.getAddress())) { //If the address is not our own address
                System.out.println("Updated: " + (Address) a + " -> " + (Address) this.rpcMappings.get(a));
                updatedMappings.put(a, this.rpcMappings.get(a)); //Add it to a new mapping
            }
        } //Results in mapping with only members in our view (up-to-date).
        this.rpcMappings = updatedMappings;
        this.logSeparator();
    }

    /**
     * Executes a passed MethodCall on all AuctionServers and then collects the
     * responses and returns them.
     *
     * @param method The method to invoke on the AuctionServers.
     * @return The RspList of the responses each AuctionServer gave.
     */
    private RspList getResponses(MethodCall method) {
        RspList<Integer> responses = null;

        try {
            responses = this.rpc.callRemoteMethods(null, method, options);
        } catch (Exception ex) {
            Logger.getLogger(ReplicationManager.class.getName()).log(Level.SEVERE, "Error when sending remote call to servers.", ex);
        }
        return responses;
    }

    /**
     * Performs a check on the checksums of all AuctionServers and synchronises
     * if needed.
     */
    private void checkSumVerification() {
        MethodCall method = new MethodCall("getCheckSum", new Object[]{}, new Class[]{});
        RspList<Integer> responses = this.getResponses(method);

        this.consistencyCheck(responses);
    }
    
    /**
     * Adds a new object to the list of current sessions on the server.
     * @param url The URL on the RMI server to the object.
     * @param remote The object the RMI URL refers to.
     */
    protected synchronized void addSession(String url,Remote remote){
        this.sessions.put("//localhost:" + RMI_PORT + url, remote);
        Registry r;
        
    }

}

package Server;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.protocols.UDP;

/**
 * An application that is part of the group communication channel.
 *
 * @author DominicWild
 */
public abstract class ChannelMember extends ReceiverAdapter {

    /**
     * UDP only needs to send 1 packet to multicast, 
     * NACKACK2 maintains FIFO ordering, 
     * FD_SOCK is a heartbeat which makes sure all clients on connection are alive, 
     * GMS handles new computers joining the JGroup channel (membership),
     * STATE_TRANSFER handles delivering of state between nodes.
     */
    public static final String PROTOCOL_STACK = "UDP:PING:pbcast.NAKACK2:FD_SOCK:pbcast.GMS:pbcast.STATE_TRANSFER";

    public static final String CHANNEL_AUCTION_NAME = "AuctionSystem";          //Name of Message channel
    public static final String CHANNEL_RPC_NAME = "AuctionSystem_RPC";          //Name of RPC channel.

    protected JChannel auctionChannel;                                          //Channel for sending messages
    protected JChannel rpcChannel;                                              //Channel for sending RPC requests
    protected RpcDispatcher rpc;                                                //RPC object to distribute and recieve RPC requests.

    protected void joinGroupChannel() {
        try {
            this.auctionChannel = new JChannel(PROTOCOL_STACK);
            this.rpcChannel = new JChannel(PROTOCOL_STACK);
            this.auctionChannel.getProtocolStack().getBottomProtocol().setValue("log_discard_msgs", false);
            this.rpcChannel.getProtocolStack().getBottomProtocol().setValue("log_discard_msgs", false);
            this.auctionChannel.setDiscardOwnMessages(true);
            this.rpcChannel.setDiscardOwnMessages(true);

            this.auctionChannel.setReceiver(this);

            this.rpc = new RpcDispatcher();
            this.rpc.setChannel(rpcChannel);

            this.rpcChannel.connect(CHANNEL_RPC_NAME);
            this.auctionChannel.connect(CHANNEL_AUCTION_NAME);

        } catch (Exception ex) {
            Logger.getLogger(ReplicationManager.class.getName()).log(Level.SEVERE, "Error within creating JChannel.", ex);
        }
    }

}

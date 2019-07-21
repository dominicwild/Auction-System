
package ServiceInterfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An interface that defines objects capable of use by the idleWatcher
 * @author DominicWild
 */
public interface IdleWatcherInterface extends Remote{
    public long getLastAction() throws RemoteException; //Gets the last time this object took an action.
}

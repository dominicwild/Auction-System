
package Server;

import ServiceInterfaces.IdleWatcherInterface;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * A object with a name attached to identify it.
 *
 * @author DominicWild
 */
public abstract class NamedObject extends UnicastRemoteObject implements IdleWatcherInterface {

    protected String owner;                   //The person authorized to use this object.
    protected long lastAction;                //The last noted time which the object was used.

    public NamedObject(String owner) throws RemoteException {
        this.owner = owner;
        this.noteUse();
    }
    
    public void noteUse()throws RemoteException {
        this.lastAction = System.currentTimeMillis();
    }

    @Override
    public long getLastAction() throws RemoteException{
        return lastAction;
    }
}

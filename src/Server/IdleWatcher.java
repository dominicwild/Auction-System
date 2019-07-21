package Server;

import ServiceInterfaces.IdleWatcherInterface;
import java.net.MalformedURLException;
import java.rmi.Naming;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches RMI server for idle NamedObjects and removes them.
 *
 * @author DominicWild
 */
public class IdleWatcher implements Runnable {

    private static double MAX_IDLE_MINS = 30;       //Max time we can have an idle session.
    private static double CHECK_DELAY_MINS = 5;   //Delay between checks for idle sessions.
    private HashMap<String, Remote> sessions;        //A reference to the sessions currently viewed as active.

    public IdleWatcher(HashMap<String, Remote> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String[] rmiList = Naming.list("rmi://localhost/NamedObjects");
                long timeDiff = 0;      //Difference in time between now and last action taken by NamedObject
                for (String url : rmiList) {
                    if (url.contains("/NamedObjects/")) {
                        IdleWatcherInterface checkObject = (IdleWatcherInterface) Naming.lookup(url);
                        timeDiff = System.currentTimeMillis() - checkObject.getLastAction();
                        System.out.println("Checking...");
                        if (timeDiff > 1000 * 60 * MAX_IDLE_MINS && this.sessions.keySet().contains(url)) { //See if it exceeds max idle time
                            UnicastRemoteObject.unexportObject(this.sessions.get(url), true); //Remove all traces of object
                            this.sessions.remove(url);
                            Naming.unbind(url);
                            System.out.println("Unexported for idle activity...");
                        }
                    }
                }
                Thread.sleep((long) (1000 * 60 * CHECK_DELAY_MINS));
            }
        } catch (RemoteException ex) {
            System.out.println("RemoteException during IdleCheck.");
        } catch (MalformedURLException ex) {
            System.out.println("MalformedURLException during IdleCheck.");
        } catch (NotBoundException ex) {
            System.out.println("NotBoundException during IdleCheck.");
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException during IdleCheck.");
        }
    }

}

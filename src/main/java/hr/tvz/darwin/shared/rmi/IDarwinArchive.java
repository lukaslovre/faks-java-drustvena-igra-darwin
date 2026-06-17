package hr.tvz.darwin.shared.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDarwinArchive extends Remote {
    int getTotalGlobalResearchPoints() throws RemoteException;
    int getTotalGamesPlayed() throws RemoteException;
}
package info.kgeorgiy.ja.polchinsky.bank;

import java.io.Serializable;
import java.rmi.Remote;

public interface Person extends Remote, Serializable {
    String getName();

    String getSurname();

    String getPassportId();
}

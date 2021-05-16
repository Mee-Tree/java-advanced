package info.kgeorgiy.ja.polchinsky.bank;

public class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    private final String passportId;

    public AbstractPerson(final String name, final String surname, final String passportId) {
        this.name = name;
        this.surname = surname;
        this.passportId = passportId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassportId() {
        return passportId;
    }
}

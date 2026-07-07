package nevg.nirton.Service;

public interface BannedUserService {

    boolean recordVisitAndCheckIfBanned(String ipAddress, String username);

    boolean checkIfIpAddressIsBanned(String ipAddress);
}

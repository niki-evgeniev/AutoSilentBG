package nevg.autosilent.Service;

public interface BannedUserService {

    boolean recordVisitAndCheckIfBanned(String ipAddress, String username, boolean countAsUniqueVisitor);

    boolean checkIfIpAddressIsBanned(String ipAddress);
}

package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.IpAddress;
import nevg.autosilent.Repository.IpAddressRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.BannedUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class BannedUserServiceImpl implements BannedUserService {

    private final IpAddressRepository ipAddressRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final int maxRequests;
    private final Duration windowDuration;
    private final Duration banDuration;
    private final ConcurrentMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    @Autowired
    public BannedUserServiceImpl(IpAddressRepository ipAddressRepository,
                                 UserRepository userRepository,
                                 @Value("${AutoSilent.security.rate-limit.max-requests:120}") int maxRequests,
                                 @Value("${AutoSilent.security.rate-limit.window:1m}") Duration windowDuration,
                                 @Value("${AutoSilent.security.rate-limit.ban-duration:15m}") Duration banDuration) {
        this(ipAddressRepository, userRepository, Clock.systemDefaultZone(), maxRequests, windowDuration, banDuration);
    }

    BannedUserServiceImpl(IpAddressRepository ipAddressRepository,
                          UserRepository userRepository,
                          Clock clock,
                          int maxRequests,
                          Duration windowDuration,
                          Duration banDuration) {
        this.ipAddressRepository = ipAddressRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.maxRequests = maxRequests;
        this.windowDuration = windowDuration;
        this.banDuration = banDuration;
    }

    @Override
    @Transactional
    public synchronized boolean recordVisitAndCheckIfBanned(String address, String username) {
        LocalDateTime now = LocalDateTime.now(clock);
        IpAddress ipAddress = ipAddressRepository.findByAddress(address).orElseGet(() -> newIpAddress(address, now));

        ipAddress.setLastSeen(now);
        ipAddress.setCountVisits(ipAddress.getCountVisits() + 1);
        linkUserWhenAvailable(ipAddress, username);

        if (hasActiveBan(ipAddress, now)) {
            ipAddressRepository.save(ipAddress);
            return true;
        }

        if (ipAddress.isBanned()) {
            ipAddress.setBanned(false);
            ipAddress.setBannedUntil(null);
        }

        if (registerRequest(address) > maxRequests) {
            ipAddress.setBanned(true);
            ipAddress.setBannedUntil(now.plus(banDuration));
        }

        ipAddressRepository.save(ipAddress);
        return ipAddress.isBanned();
    }

    @Override
    @Transactional
    public synchronized boolean checkIfIpAddressIsBanned(String address) {
        return ipAddressRepository.findByAddress(address)
                .map(ipAddress -> {
                    LocalDateTime now = LocalDateTime.now(clock);
                    if (hasActiveBan(ipAddress, now)) return true;
                    if (ipAddress.isBanned()) {
                        ipAddress.setBanned(false);
                        ipAddress.setBannedUntil(null);
                        ipAddressRepository.save(ipAddress);
                    }
                    return false;
                })
                .orElse(false);
    }

    private IpAddress newIpAddress(String address, LocalDateTime now) {
        IpAddress ipAddress = new IpAddress();
        ipAddress.setAddress(address);
        ipAddress.setFirstSeen(now);
        ipAddress.setLastSeen(now);
        return ipAddress;
    }

    private void linkUserWhenAvailable(IpAddress ipAddress, String username) {
        if (ipAddress.getUser() == null && username != null && !username.isBlank()) {
            userRepository.findByEmailIgnoreCase(username).ifPresent(ipAddress::setUser);
        }
    }

    private boolean hasActiveBan(IpAddress ipAddress, LocalDateTime now) {
        return ipAddress.isBanned()
                && (ipAddress.getBannedUntil() == null || ipAddress.getBannedUntil().isAfter(now));
    }

    private int registerRequest(String address) {
        long now = clock.millis();
        RequestWindow window = requestWindows.compute(address, (key, current) -> {
            if (current == null || now - current.startedAt() >= windowDuration.toMillis()) {
                return new RequestWindow(now, 1);
            }
            return new RequestWindow(current.startedAt(), current.requestCount() + 1);
        });
        return window.requestCount();
    }

    private record RequestWindow(long startedAt, int requestCount) {
    }
}

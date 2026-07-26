package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.IpAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IpAddressRepositoryConcurrencyTest {

    @Autowired
    private IpAddressRepository ipAddressRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void concurrentFirstVisitsCreateOneAddressAndPreserveBothUpdates() throws Exception {
        String address = "198.51.100.42";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = submitVisit(executor, ready, start, address);
            Future<?> second = submitVisit(executor, ready, start, address);

            ready.await();
            start.countDown();
            first.get();
            second.get();
        }

        IpAddress storedAddress = ipAddressRepository.findByAddress(address).orElseThrow();
        assertThat(storedAddress.getCountVisits()).isEqualTo(2);
        assertThat(ipAddressRepository.findAll())
                .filteredOn(ipAddress -> address.equals(ipAddress.getAddress()))
                .hasSize(1);
    }

    private Future<?> submitVisit(ExecutorService executor,
                                  CountDownLatch ready,
                                  CountDownLatch start,
                                  String address) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            transactionTemplate.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                ipAddressRepository.insertIfAbsent(UUID.randomUUID(), address, now);
                IpAddress ipAddress = ipAddressRepository.findByAddressForUpdate(address).orElseThrow();
                ipAddress.setCountVisits(ipAddress.getCountVisits() + 1);
            });
            return null;
        });
    }
}

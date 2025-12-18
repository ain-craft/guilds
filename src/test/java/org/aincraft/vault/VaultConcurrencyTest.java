package org.aincraft.vault;

import org.aincraft.multiblock.Rotation;
import org.aincraft.storage.InMemoryVaultRepository;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for vault concurrent access and duplication prevention.
 */
@DisplayName("Vault Concurrency")
class VaultConcurrencyTest {

    private InMemoryVaultRepository vaultRepository;
    private Vault vault;
    private static final int SLOT = 0;
    private static final int DIAMOND_COUNT = 64;

    @BeforeEach
    void setUp() {
        vaultRepository = new InMemoryVaultRepository();
        vault = new Vault(
                "guild-123",
                "world",
                0, 64, 0,
                Rotation.NONE,
                UUID.randomUUID()
        );

        // Put 64 diamonds in slot 0
        ItemStack diamonds = createMockItemStack(Material.DIAMOND, DIAMOND_COUNT);
        ItemStack[] contents = new ItemStack[Vault.STORAGE_SIZE];
        contents[SLOT] = diamonds;
        vault.setContents(contents);

        vaultRepository.save(vault);
    }

    @Nested
    @DisplayName("compareAndSetSlot")
    class CompareAndSetSlotTests {

        @Test
        @DisplayName("should succeed when slot matches expected state")
        void shouldSucceedWhenSlotMatchesExpected() {
            ItemStack expected = createMockItemStack(Material.DIAMOND, DIAMOND_COUNT);
            ItemStack newItem = null; // removing item

            boolean result = vaultRepository.compareAndSetSlot(vault.getId(), SLOT, expected, newItem);

            assertThat(result).isTrue();
            assertThat(vaultRepository.getSlot(vault.getId(), SLOT)).isNull();
        }

        @Test
        @DisplayName("should fail when slot doesn't match expected state")
        void shouldFailWhenSlotDoesntMatchExpected() {
            // Someone else already took the diamonds
            vaultRepository.compareAndSetSlot(vault.getId(), SLOT,
                    createMockItemStack(Material.DIAMOND, DIAMOND_COUNT), null);

            // Now try to take diamonds again with stale expectation
            ItemStack staleExpected = createMockItemStack(Material.DIAMOND, DIAMOND_COUNT);
            boolean result = vaultRepository.compareAndSetSlot(vault.getId(), SLOT, staleExpected, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should fail when expected null but slot has item")
        void shouldFailWhenExpectedNullButSlotHasItem() {
            boolean result = vaultRepository.compareAndSetSlot(vault.getId(), SLOT, null,
                    createMockItemStack(Material.GOLD_INGOT, 32));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should succeed when both expected and current are null")
        void shouldSucceedWhenBothExpectedAndCurrentAreNull() {
            // Clear the slot first
            vaultRepository.compareAndSetSlot(vault.getId(), SLOT,
                    createMockItemStack(Material.DIAMOND, DIAMOND_COUNT), null);

            // Now place something in empty slot
            ItemStack newItem = createMockItemStack(Material.GOLD_INGOT, 32);
            boolean result = vaultRepository.compareAndSetSlot(vault.getId(), SLOT, null, newItem);

            assertThat(result).isTrue();
            ItemStack slot = vaultRepository.getSlot(vault.getId(), SLOT);
            assertThat(slot).isNotNull();
            assertThat(slot.getType()).isEqualTo(Material.GOLD_INGOT);
        }
    }

    @Nested
    @DisplayName("Concurrent Access")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("only one player should successfully withdraw when two try simultaneously")
        void onlyOnePlayerShouldSuccessfullyWithdraw() throws InterruptedException {
            int numPlayers = 2;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numPlayers);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numPlayers);

            for (int i = 0; i < numPlayers; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready

                        // Simulate reading current slot state (what player sees in GUI)
                        ItemStack expected = createMockItemStack(Material.DIAMOND, DIAMOND_COUNT);

                        // Try to withdraw (CAS operation)
                        boolean success = vaultRepository.compareAndSetSlot(
                                vault.getId(), SLOT, expected, null);

                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Start all threads at once
            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Only one should succeed, the other should fail
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failCount.get()).isEqualTo(1);

            // Slot should be empty
            assertThat(vaultRepository.getSlot(vault.getId(), SLOT)).isNull();
        }

        @Test
        @DisplayName("should prevent duplication under high concurrency")
        void shouldPreventDuplicationUnderHighConcurrency() throws InterruptedException {
            int numPlayers = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numPlayers);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger totalDiamondsWithdrawn = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numPlayers);

            for (int i = 0; i < numPlayers; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        ItemStack expected = createMockItemStack(Material.DIAMOND, DIAMOND_COUNT);
                        boolean success = vaultRepository.compareAndSetSlot(
                                vault.getId(), SLOT, expected, null);

                        if (success) {
                            successCount.incrementAndGet();
                            totalDiamondsWithdrawn.addAndGet(DIAMOND_COUNT);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Exactly one success, no duplication
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(totalDiamondsWithdrawn.get()).isEqualTo(DIAMOND_COUNT);
        }

        @Test
        @DisplayName("should handle interleaved deposits and withdrawals")
        void shouldHandleInterleavedDepositsAndWithdrawals() throws InterruptedException {
            // Start with empty slot
            vaultRepository.compareAndSetSlot(vault.getId(), SLOT,
                    createMockItemStack(Material.DIAMOND, DIAMOND_COUNT), null);

            int numOperations = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numOperations);
            AtomicInteger depositSuccesses = new AtomicInteger(0);
            AtomicInteger withdrawSuccesses = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numOperations);

            for (int i = 0; i < numOperations; i++) {
                final boolean isDeposit = i % 2 == 0;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        if (isDeposit) {
                            // Try to deposit 32 iron into empty slot
                            ItemStack newItem = createMockItemStack(Material.IRON_INGOT, 32);
                            if (vaultRepository.compareAndSetSlot(vault.getId(), SLOT, null, newItem)) {
                                depositSuccesses.incrementAndGet();
                            }
                        } else {
                            // Try to withdraw iron
                            ItemStack expected = createMockItemStack(Material.IRON_INGOT, 32);
                            if (vaultRepository.compareAndSetSlot(vault.getId(), SLOT, expected, null)) {
                                withdrawSuccesses.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Each withdraw must have had a corresponding deposit
            // (can't withdraw more than deposited)
            assertThat(withdrawSuccesses.get()).isLessThanOrEqualTo(depositSuccesses.get());
        }
    }

    @Nested
    @DisplayName("getFreshContents")
    class GetFreshContentsTests {

        @Test
        @DisplayName("should return copy, not reference")
        void shouldReturnCopyNotReference() {
            ItemStack[] contents1 = vaultRepository.getFreshContents(vault.getId());
            ItemStack[] contents2 = vaultRepository.getFreshContents(vault.getId());

            // Should be different array instances
            assertThat(contents1).isNotSameAs(contents2);

            // Modifying one shouldn't affect the other
            contents1[SLOT] = null;
            assertThat(contents2[SLOT]).isNotNull();
        }

        @Test
        @DisplayName("should reflect current DB state")
        void shouldReflectCurrentDbState() {
            // Initial state
            ItemStack[] initial = vaultRepository.getFreshContents(vault.getId());
            assertThat(initial[SLOT]).isNotNull();
            assertThat(initial[SLOT].getAmount()).isEqualTo(DIAMOND_COUNT);

            // Withdraw
            vaultRepository.compareAndSetSlot(vault.getId(), SLOT,
                    createMockItemStack(Material.DIAMOND, DIAMOND_COUNT), null);

            // Fresh read should show empty
            ItemStack[] afterWithdraw = vaultRepository.getFreshContents(vault.getId());
            assertThat(afterWithdraw[SLOT]).isNull();
        }
    }

    /**
     * Creates a mock ItemStack for testing.
     * Uses Mockito to avoid Bukkit server initialization.
     */
    private ItemStack createMockItemStack(Material material, int amount) {
        ItemStack mock = mock(ItemStack.class);
        when(mock.getType()).thenReturn(material);
        when(mock.getAmount()).thenReturn(amount);
        when(mock.getMaxStackSize()).thenReturn(64);
        when(mock.isSimilar(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ItemStack other = invocation.getArgument(0);
            return other != null && other.getType() == material;
        });
        when(mock.clone()).thenReturn(mock);
        return mock;
    }
}

package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

  @Mock private NotificationService notificationService;
  @Mock private ProductRepository productRepository;
  @InjectMocks private ProductService productService;

  @Test
  public void shouldNotifyDelay() {
    // GIVEN
    Product product = new Product(null, 15, 0, "NORMAL", "RJ45 Cable", null, null, null);

    Mockito.when(productRepository.save(product)).thenReturn(product);

    // WHEN
    productService.notifyDelay(product);

    // THEN
    assertEquals(0, product.getAvailable());
    assertEquals(15, product.getLeadTime());
    Mockito.verify(notificationService, Mockito.times(1))
        .sendDelayNotification(product.getLeadTime(), product.getName());
  }

  @Test
  public void shouldDecrementNormalProductWhenAvailable() {
    Product product = new Product(null, 7, 3, "NORMAL", "USB Cable", null, null, null);

    productService.handleProduct(product);

    assertEquals(2, product.getAvailable());
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService, Mockito.times(1))
        .sendDelayNotification(product.getLeadTime(), product.getName());
  }

  @Test
  public void shouldNotifyNormalProductWhenOutOfStock() {
    Product product = new Product(null, 7, 0, "NORMAL", "USB Dongle", null, null, null);

    productService.handleProduct(product);

    assertEquals(0, product.getAvailable());
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService, Mockito.times(1))
        .sendDelayNotification(product.getLeadTime(), product.getName());
  }

  @Test
  public void shouldNotifySeasonalProductWhenOutOfStock() {
    Product product = new Product(null, 7, 0, "NORMAL", "USB Dongle", null, null, null);

    productService.handleProduct(product);

    assertEquals(0, product.getAvailable());
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService, Mockito.times(1))
        .sendDelayNotification(product.getLeadTime(), product.getName());
  }

  @Test
  public void shouldNotifyExpirationForProduct() {
    Product product =
        new Product(null, 10, 5, "EXPIRABLE", "Milk", LocalDate.of(2026, 3, 3), null, null);

    productService.handleProduct(product);

    assertEquals(0, product.getAvailable());
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService)
        .sendExpirationNotification("Milk", LocalDate.of(2026, 3, 3));
  }

  @Test
  public void shouldDecrementSeasonalProductStockWithinSeason() {
    Product product =
        new Product(
            null,
            4,
            2,
            "SEASONAL",
            "Watermelon",
            null,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31));

    productService.handleProduct(product);

    assertEquals(1, product.getAvailable());
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService, never()).sendOutOfStockNotification(Mockito.anyString());
  }

  @Test
  void shouldSendOutOfStockWhenLeadTimeIsAfterSeasonEnd() {
    Product product =
        new Product(
            null,
            10,
            5,
            "SEASONAL",
            "Watermelon",
            null,
            LocalDate.now().minusDays(5),
            LocalDate.now().minusDays(2));

    productService.handleProduct(product);

    assertEquals(0, product.getAvailable());
    Mockito.verify(notificationService).sendOutOfStockNotification("Watermelon");
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService, never())
        .sendDelayNotification(Mockito.anyInt(), Mockito.anyString());
  }

  @Test
  void shouldSendOutOfStockWhenSeasonHasNotStartedYet() {
    Product product =
        new Product(
            null,
            2, // leadTime
            5, // available
            "SEASONAL",
            "Grapes",
            null,
            LocalDate.now().plusDays(3), // season start in future
            LocalDate.now().plusDays(10) // now + 2 days is not after season end
            );

    productService.handleProduct(product);

    assertEquals(5, product.getAvailable()); // your code does not set available to 0 here
    Mockito.verify(notificationService).sendOutOfStockNotification("Grapes");
    Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService, never())
        .sendDelayNotification(Mockito.anyInt(), Mockito.anyString());
  }

  @Test
  void shouldNotifyDelayWhenSeasonStartedAndLeadTimeInSeason() {
    Product product =
        new Product(
            null,
            2,
            0,
            "SEASONAL",
            "Peach",
            null,
            LocalDate.now().minusDays(2), // already started
            LocalDate.now().plusDays(10) // now + 2 days is before season end
            );

    productService.handleProduct(product);

    //        Mockito.verify(productRepository).save(product);
    Mockito.verify(notificationService).sendDelayNotification(2, "Peach");
    Mockito.verify(notificationService, never()).sendOutOfStockNotification(Mockito.anyString());
  }

    @Test
    void shouldDecrementAvailableWhenExpirableProductIsInStockAndNotExpired() {
        Product product = new Product(
                null,
                10,
                5,
                "EXPIRABLE",
                "Milk",
                LocalDate.now().plusDays(3),
                null,
                null
        );

        productService.handleProduct(product);

        assertEquals(4, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verify(notificationService, never()).sendExpirationNotification(Mockito.anyString(), Mockito.any());
    }
}

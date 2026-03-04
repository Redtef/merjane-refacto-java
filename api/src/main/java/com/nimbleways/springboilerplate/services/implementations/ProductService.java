package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
@AllArgsConstructor
public class ProductService {

    public static final String NORMAL = "NORMAL";
    public static final String SEASONAL = "SEASONAL";
    public static final String EXPIRABLE = "EXPIRABLE";

    private final ProductRepository productRepository;
    private final NotificationService notificationService;



    public void handleProduct(Product product) {
        switch (product.getType()) {
            case NORMAL -> {
                handleNormalProduct(product);
            }
            case SEASONAL -> {
                handleSeasonalProduct(product);
            }
            case EXPIRABLE -> {
                handleExpiredProduct(product);
            }
        }
    }

    public void notifyDelay(Product product) {
        notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
    }

    private void handleSeasonalProduct(Product product) {
        boolean isWithinSeason = LocalDate.now().isAfter(product.getSeasonStartDate()) && LocalDate.now().isBefore(product.getSeasonEndDate());
        boolean hasStock = product.getAvailable() > 0;

        if (isWithinSeason && hasStock) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            boolean leadTimeAfterSeasonEnd = LocalDate.now().plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate());
            if (leadTimeAfterSeasonEnd) {
                // season ended
                notificationService.sendOutOfStockNotification(product.getName());
                product.setAvailable(0);
                productRepository.save(product);
            } else if (product.getSeasonStartDate().isAfter(LocalDate.now())) {
                // season Not started
                notificationService.sendOutOfStockNotification(product.getName());
                productRepository.save(product);
            } else {
                // inside season
                notifyDelay(product);
            }
        }
    }

    private void handleExpiredProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            product.setAvailable(product.getAvailable() - 1);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
        }
        productRepository.save(product);
    }



    private void handleNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
        }
        productRepository.save(product);
        notifyDelay(product);
    }
}
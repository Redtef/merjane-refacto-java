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

    private void notifyDelay(Product product) {
        productRepository.save(product);
        notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
    }

    private void handleSeasonalProduct(Product p) {
        boolean isWithinSeason = LocalDate.now().isAfter(p.getSeasonStartDate()) && LocalDate.now().isBefore(p.getSeasonEndDate());
        boolean hasStock = p.getAvailable() > 0;

        if (isWithinSeason && hasStock) {
            p.setAvailable(p.getAvailable() - 1);
            productRepository.save(p);
        } else {
            boolean leadTimeAfterSeasonEnd = LocalDate.now().plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate());
            if (leadTimeAfterSeasonEnd) {
                notificationService.sendOutOfStockNotification(p.getName());
                p.setAvailable(0);
                productRepository.save(p);
            } else if (p.getSeasonStartDate().isAfter(LocalDate.now())) {
                notificationService.sendOutOfStockNotification(p.getName());
                productRepository.save(p);
            } else {
                notifyDelay(p);
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
        notifyDelay(product);
    }
}
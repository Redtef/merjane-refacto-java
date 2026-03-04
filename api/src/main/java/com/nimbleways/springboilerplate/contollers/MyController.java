package com.nimbleways.springboilerplate.contollers;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.OrderService;
import com.nimbleways.springboilerplate.services.implementations.ProductService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO : rename to OrderController for better naming
 */
@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class MyController {

    private final ProductService productService;
    private final OrderService orderService;

    @PostMapping("{orderId}/processOrder")
    @ResponseStatus(HttpStatus.OK)
    public ProcessOrderResponse processOrder(@PathVariable Long orderId) {
        // fetch order
        Order order = orderService.getOrder(orderId);

        // what is this used for ?
//        List<Long> ids = new ArrayList<>();
//        ids.add(orderId);
        Set<Product> products = order.getItems();
        for (Product product : products) {
            if (product.getType().equals("NORMAL")) {
                if (product.getAvailable() > 0) {
                    product.setAvailable(product.getAvailable() - 1);
                    pr.save(product);
                } else {
                    int leadTime = product.getLeadTime();
                    if (leadTime > 0) {
                        ps.notifyDelay(leadTime, product);
                    }
                }
            } else if (product.getType().equals("SEASONAL")) {
                // Add new season rules
                if ((LocalDate.now().isAfter(product.getSeasonStartDate()) && LocalDate.now().isBefore(product.getSeasonEndDate())
                        && product.getAvailable() > 0)) {
                    product.setAvailable(product.getAvailable() - 1);
                    pr.save(product);
                } else {
                    ps.handleSeasonalProduct(product);
                }
            } else if (product.getType().equals("EXPIRABLE")) {
                if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
                    product.setAvailable(product.getAvailable() - 1);
                    pr.save(product);
                } else {
                    ps.handleExpiredProduct(product);
                }
            }
        }

        return new ProcessOrderResponse(order.getId());
    }
}

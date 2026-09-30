package com.hcb.controller.admin;

import com.hcb.model.entity.Order;
import com.hcb.model.entity.Product;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import com.hcb.repository.OrderRepository;
import com.hcb.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String dashboard(Model model) {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByPaymentStatus(PaymentStatus.PENDING);
        long verifiedOrders = orderRepository.countByPaymentStatus(PaymentStatus.VERIFIED);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);

        List<Order> verifiedOrdersList = orderRepository.findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus.VERIFIED);
        BigDecimal totalRevenue = verifiedOrdersList.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        List<Order> recentOrders = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8)).getContent();

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("verifiedOrders", verifiedOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("recentOrders", recentOrders);

        return "admin/dashboard";
    }
}

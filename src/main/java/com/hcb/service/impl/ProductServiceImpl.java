package com.hcb.service.impl;

import com.hcb.model.entity.Product;
import com.hcb.repository.ProductRepository;
import com.hcb.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueAndAvailableTrueOrderBySortOrderAsc();
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderBySortOrderAsc();
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Optional<Product> getProductBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    @Override
    public Optional<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Override
    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product details) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        existing.setName(details.getName());
        existing.setDescription(details.getDescription());
        existing.setPrice(details.getPrice());
        existing.setCostPrice(details.getCostPrice());
        existing.setStockQuantity(details.getStockQuantity());
        existing.setLowStockThreshold(details.getLowStockThreshold());
        existing.setActive(details.isActive());
        existing.setAvailable(details.isAvailable());
        existing.setSortOrder(details.getSortOrder());
        existing.setWeightGrams(details.getWeightGrams());
        existing.setSku(details.getSku());

        if (details.getImageFilename() != null && !details.getImageFilename().isEmpty()) {
            existing.setImageFilename(details.getImageFilename());
        }

        return productRepository.save(existing);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setActive(!p.isActive());
            productRepository.save(p);
        });
    }

    @Override
    @Transactional
    public void toggleAvailable(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setAvailable(!p.isAvailable());
            productRepository.save(p);
        });
    }

    @Override
    @Transactional
    public void updateStock(Long id, int newStock) {
        productRepository.findById(id).ifPresent(p -> {
            p.setStockQuantity(Math.max(0, newStock));
            productRepository.save(p);
        });
    }
}

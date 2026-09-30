package com.hcb.service;

import com.hcb.model.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> getActiveProducts();

    List<Product> getAllProducts();

    Optional<Product> getProductById(Long id);

    Optional<Product> getProductBySlug(String slug);

    Optional<Product> getProductBySku(String sku);

    Product saveProduct(Product product);

    Product updateProduct(Long id, Product productDetails);

    void toggleActive(Long id);

    void toggleAvailable(Long id);

    void updateStock(Long id, int newStock);
}

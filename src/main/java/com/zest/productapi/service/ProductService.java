package com.zest.productapi.service;

import com.zest.productapi.dto.ItemDto;
import com.zest.productapi.dto.ProductDto;
import com.zest.productapi.entity.Item;
import com.zest.productapi.entity.Product;
import com.zest.productapi.exception.ResourceNotFoundException;
import com.zest.productapi.repository.ItemRepository;
import com.zest.productapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;

    public Page<ProductDto.Response> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public ProductDto.Response getProductById(Long id) {
        Product product = findById(id);
        return toResponse(product);
    }

    public ProductDto.Response createProduct(ProductDto.Request request) {
        String username = getCurrentUsername();
        Product product = Product.builder()
                .productName(request.getProductName())
                .createdBy(username)
                .build();
        return toResponse(productRepository.save(product));
    }

    public ProductDto.Response updateProduct(Long id, ProductDto.Request request) {
        Product product = findById(id);
        product.setProductName(request.getProductName());
        product.setModifiedBy(getCurrentUsername());
        return toResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<ItemDto.Response> getItemsByProductId(Long productId) {
        findById(productId); // validate product exists
        return itemRepository.findByProductId(productId).stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Async
    public CompletableFuture<Void> logProductAccess(Long productId) {
        log.info("Async - Product accessed: id={}", productId);
        return CompletableFuture.completedFuture(null);
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ProductDto.Response toResponse(Product product) {
        return ProductDto.Response.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .createdBy(product.getCreatedBy())
                .createdOn(product.getCreatedOn())
                .modifiedBy(product.getModifiedBy())
                .modifiedOn(product.getModifiedOn())
                .build();
    }

    private ItemDto.Response toItemResponse(Item item) {
        return ItemDto.Response.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .quantity(item.getQuantity())
                .build();
    }
}

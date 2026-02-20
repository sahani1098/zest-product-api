package com.zest.productapi.service;

import com.zest.productapi.dto.ProductDto;
import com.zest.productapi.entity.Product;
import com.zest.productapi.exception.ResourceNotFoundException;
import com.zest.productapi.repository.ItemRepository;
import com.zest.productapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .productName("Test Product")
                .createdBy("testuser")
                .createdOn(LocalDateTime.now())
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllProducts_returnsPage() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<ProductDto.Response> result = productService.getAllProducts(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Test Product");
    }

    @Test
    void getProductById_existingId_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductDto.Response response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("Test Product");
    }

    @Test
    void getProductById_nonExistingId_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createProduct_returnsCreatedProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDto.Request request = new ProductDto.Request("Test Product");
        ProductDto.Response response = productService.createProduct(request);

        assertThat(response.getProductName()).isEqualTo("Test Product");
        assertThat(response.getCreatedBy()).isEqualTo("testuser");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_existingId_returnsUpdated() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        sampleProduct.setProductName("Updated Product");
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDto.Request request = new ProductDto.Request("Updated Product");
        ProductDto.Response response = productService.updateProduct(1L, request);

        assertThat(response.getProductName()).isEqualTo("Updated Product");
    }

    @Test
    void deleteProduct_existingId_deletesSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(productRepository).delete(any(Product.class));

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).delete(sampleProduct);
    }

    @Test
    void deleteProduct_nonExistingId_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

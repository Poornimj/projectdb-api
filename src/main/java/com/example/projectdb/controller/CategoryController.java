package com.example.projectdb.controller;

import com.example.projectdb.dto.CategoryRequest;
import com.example.projectdb.dto.ProductRequest;
import com.example.projectdb.entity.Category;
import com.example.projectdb.entity.Product;
import com.example.projectdb.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import com.example.projectdb.dto.CategoryUpdateRequest;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategory(
            @PathVariable Integer id) {

        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/{id}/products")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Product>> getCategoryProducts(
            @PathVariable Integer id) {

        Category category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        List<Product> products = List.copyOf(category.getProducts());
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Category> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());

        for (ProductRequest productRequest : request.products()) {

            if (productRequest.categoryId() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Omit categoryId: products belong to the new category");
            }

            Product product = new Product();
            product.setName(productRequest.name());
            product.setDescription(productRequest.description());
            product.setPrice(productRequest.price());
            product.setStockQuantity(productRequest.stockQuantity());
            product.setSupplierId(productRequest.supplierId());

            category.addProduct(product);
        }

        Category savedCategory = categoryRepository.save(category);

        return ResponseEntity
                .created(URI.create("/api/categories/" + savedCategory.getId()))
                .body(savedCategory);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Category> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        Category category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        category.setName(request.name());
        category.setDescription(request.description());

        Category updatedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(updatedCategory);
    }
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {

        Category category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        if (!category.getProducts().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a category that contains products");
        }

        categoryRepository.delete(category);
        categoryRepository.flush();

        return ResponseEntity.noContent().build();
    }
}
package com.webflux.models.services;

import com.webflux.models.documents.Category;
import com.webflux.models.documents.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {
    Flux<Product> findAll();
    Mono<Product> findById(String id);
    Mono<Product> save(Product product);
    Mono<Void> delete(Product product);
    Flux<Product> findAllWithUpperCaseName();
    Flux<Product> findAllWithUpperCaseNameAndRepeat(Integer repeat);
    Flux<Category> findAllCategory();
    Mono<Category> findCategoryById(String id);
    Mono<Category> saveCategory(Category category);
}

package com.webflux.controllers;

import com.webflux.models.documents.Product;
import com.webflux.models.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductRestController {
    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public Flux<Product> list() {
        Flux<Product> productFlux = productRepository.findAll()
                .map(product -> {
                    product.setName(product.getName().toUpperCase());
                    return product;
                }).doOnNext(product -> log.info(product.getName()));
        return productFlux;
    }

    @GetMapping("/{id}")
    public Mono<Product> getById(@PathVariable String id) {
        //return productRepository.findById(id);
        Flux<Product> productFlux = productRepository.findAll();
        Mono<Product> productMono = productFlux.filter(product -> product.getId().equals(id)).next()
                .doOnNext(product -> log.info(product.getName()));
        return productMono;
    }
}

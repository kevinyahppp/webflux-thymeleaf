package com.webflux;

import com.webflux.models.documents.Category;
import com.webflux.models.documents.Product;
import com.webflux.models.services.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
@Slf4j
public class WebfluxApplication implements CommandLineRunner {

	@Autowired
	private ProductService productService;

	@Autowired
	private ReactiveMongoTemplate reactiveMongoTemplate;

	public static void main(String[] args) {
		SpringApplication.run(WebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		reactiveMongoTemplate.dropCollection("products").subscribe();
		reactiveMongoTemplate.dropCollection("categories").subscribe();

		Category category1 = new Category("Electronic");
		Category category2 = new Category("Video Games");
		Category category3 = new Category("Computer");
		Category category4 = new Category("Phones");

		Flux.just(category1, category2, category3, category4)
				.flatMap(productService::saveCategory)
				.doOnNext(category -> log.info("Category created: {}, Id: {}",
						category.getName(), category.getId()))
						.thenMany(
								Flux.just(
										new Product("TV", 250.0, category1),
										new Product("Laptop", 550.0, category3),
										new Product("Switch", 350.0, category2),
										new Product("PS5", 500.0, category2),
										new Product("Xbox Series X", 500.0, category2),
										new Product("iPhone 11", 450.0, category4),
										new Product("Macbook Pro", 1000.0, category3),
										new Product("Mouse", 400.0, category1),
										new Product("Samsung Galaxy", 300.0, category4)
								).flatMap(product -> {
									product.setCreateAt(new Date());
									return productService.save(product);
								})
						)
				.subscribe(product -> log.info("Insert: {} {}",
						product.getId(), product.getName()));
	}
}

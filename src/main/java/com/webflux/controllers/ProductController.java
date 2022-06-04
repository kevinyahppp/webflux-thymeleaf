package com.webflux.controllers;

import com.webflux.models.documents.Category;
import com.webflux.models.documents.Product;
import com.webflux.models.services.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Slf4j
@SessionAttributes("product")
@Controller
public class ProductController {

    @Autowired
    private ProductService productService;
    @Value("${config.uploads.path}")
    private String filepath;

    @ModelAttribute("categories")
    public Flux<Category> categories() {
        return productService.findAllCategory();
    }

    @GetMapping("/view/{id}")
    public Mono<String> view(Model model, @PathVariable String id) {
        return productService.findById(id)
                .doOnNext(product -> {
                    model.addAttribute("title", "Detail Product");
                    model.addAttribute("product", product);
                }).switchIfEmpty(Mono.just(new Product()))
                .flatMap(product -> {
                    if (product.getId() == null) {
                        return Mono.error(new InterruptedException("Product not exist"));
                    }
                    return Mono.just(product);
                }).then(Mono.just("view"))
                .onErrorResume(throwable -> Mono.just("redirect:/list?error=product+not+exist"));
    }

    @GetMapping("/uploads/pic/{pictureName:.+}")
    public Mono<ResponseEntity<Resource>> viewPicture(@PathVariable String pictureName) throws MalformedURLException {
        Path path = Paths.get(filepath).resolve(pictureName).toAbsolutePath();
        Resource resource = new UrlResource(path.toUri());
        return Mono.just(
                ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource)
        );
    }

    @GetMapping({"/list", "/"})
    public Mono<String> list(Model model) {
        Flux<Product> productFlux = productService.findAllWithUpperCaseName();
        productFlux.subscribe(product -> log.info(product.getName()));
        model.addAttribute("productFlux", productFlux);
        model.addAttribute("title", "Products list");
        return Mono.just("list");
    }

    @GetMapping("/form")
    public Mono<String> create(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("title", "Product Form");
        model.addAttribute("button", "Create");
        return Mono.just("form");
    }

    @PostMapping("/form")
    public Mono<String> save(@Valid Product product, BindingResult bindingResult,
                             @RequestPart(name = "file") FilePart filePart,
                             Model model, SessionStatus sessionStatus) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Error product's form");
            model.addAttribute("button", "Save");
            return Mono.just("form");
        } else {
            sessionStatus.setComplete();

            Mono<Category> categoryMono = productService.findCategoryById(product.getCategory().getId());

            return categoryMono.flatMap(category -> {
                if (product.getCreateAt() == null) {
                    product.setCreateAt(new Date());
                }
                if (!filePart.filename().isEmpty()) {
                    product.setPicture(UUID.randomUUID().toString().concat("-")
                            .concat(filePart.filename())
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", ""));
                }
                product.setCategory(category);
                return productService.save(product);
            }) .doOnNext(product1 -> {
                log.info("Product saved: {} Id: {}, Category: {}",
                        product1.getName(), product1.getId(), product1.getCategory());
            })
                    .flatMap(product1 -> {
                        if (!filePart.filename().isEmpty()) {
                            return filePart.transferTo(new File(
                                    filepath.concat(product1.getPicture())));
                        }
                        return Mono.empty();
                    })
                    .thenReturn("redirect:/list?success=product+saved+successfully");
        }
    }

    @GetMapping("/delete/{id}")
    public Mono<String> delete(@PathVariable String id) {
        return productService.findById(id)
                .defaultIfEmpty(new Product())
                .flatMap(product -> {
                    if (product.getId() == null) {
                        return Mono.error(new InterruptedException("Product not exist to delete"));
                    }
                    return Mono.just(product);
                })
                .flatMap(product -> {
                    log.info("Deleted product: {}, Id: {}",
                            product.getName(), product.getId());
                    return productService.delete(product);
                })
                .thenReturn("redirect:/list?success=product+deleted+successfully")
                .onErrorResume(throwable -> Mono.just("redirect:/list?error=product+not+exist+to+delete"));
    }

    @GetMapping("/form/{id}")
    public Mono<String> edit(@PathVariable String id, Model model) {
        Mono<Product> productMono = productService.findById(id)
                .doOnNext(product -> log.info("Product: {}", product.getName()))
                .defaultIfEmpty(new Product());
        model.addAttribute("title", "Edit Product");
        model.addAttribute("product", productMono);
        model.addAttribute("button", "Edit");
        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono<String> editV2(@PathVariable String id, Model model) {
        return productService.findById(id)
                .doOnNext(product -> {
                    log.info("Product: {}", product.getName());
                    model.addAttribute("title", "Edit Product");
                    model.addAttribute("product", product);
                    model.addAttribute("button", "Edit");
                }).defaultIfEmpty(new Product())
                .flatMap(product -> {
                    if (product.getId() == null) {
                        return Mono.error(new InterruptedException("Product not exist"));
                    }
                    return Mono.just(product);
                })
                .thenReturn("/form")
                .onErrorResume(throwable -> Mono.just("redirect:/list?error=product+not+exist"));
    }

    @GetMapping("/list-datadriver")
    public String listDataDriver(Model model) {
        Flux<Product> productFlux = productService.findAllWithUpperCaseName()
                .delayElements(Duration.ofSeconds(1));
        productFlux.subscribe(product -> log.info(product.getName()));
        model.addAttribute("productFlux", new ReactiveDataDriverContextVariable(productFlux, 2));
        model.addAttribute("title", "Products list");
        return "list";
    }

    @GetMapping("/list-full")
    public String listFull(Model model) {
        Flux<Product> productFlux = productService.findAllWithUpperCaseNameAndRepeat(5000);
        model.addAttribute("productFlux", productFlux);
        model.addAttribute("title", "Products list");
        return "list";
    }

    @GetMapping("/list-chunked")
    public String listChunked(Model model) {
        Flux<Product> productFlux = productService.findAllWithUpperCaseNameAndRepeat(5000);
        model.addAttribute("productFlux", productFlux);
        model.addAttribute("title", "Products list");
        return "list-chunked";
    }
}

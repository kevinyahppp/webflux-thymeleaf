package com.webflux.models.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("categories")
public class Category {

    @Id
    @NotEmpty
    private String id;
    private String name;

    public Category(String name) {
        this.name = name;
    }
}

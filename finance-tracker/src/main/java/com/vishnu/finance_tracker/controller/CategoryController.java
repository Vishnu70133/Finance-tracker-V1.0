package com.vishnu.finance_tracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.vishnu.finance_tracker.model.Category;
import com.vishnu.finance_tracker.service.CategoryService;



@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
private CategoryService categoryService;

@PostMapping("/{userId}")
public Category createCategory(@PathVariable Long userId,
                               @RequestBody Category category) {
    return categoryService.createCategory(userId, category);
}

@GetMapping
public java.util.List<Category> getAllCategories() {
    return categoryService.getAllCategories();
}

}

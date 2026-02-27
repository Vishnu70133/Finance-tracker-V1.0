package com.vishnu.finance_tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



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

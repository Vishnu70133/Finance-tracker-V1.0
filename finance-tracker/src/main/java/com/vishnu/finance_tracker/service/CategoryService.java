package com.vishnu.finance_tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vishnu.finance_tracker.model.Category;
import com.vishnu.finance_tracker.model.User;
import com.vishnu.finance_tracker.repository.CategoryRepository;
import com.vishnu.finance_tracker.repository.UserRepository;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    public Category createCategory(Long userId, Category category) {
        User user = userRepository.findById(userId).orElseThrow();
        category.setUser(user);
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}

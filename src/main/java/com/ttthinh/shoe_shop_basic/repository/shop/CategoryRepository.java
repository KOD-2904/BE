package com.ttthinh.shoe_shop_basic.repository.shop;

import com.ttthinh.shoe_shop_basic.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByName(String name);
    List<Category> findAll();

    Category getCategoriesById(String id);

    List<Category> id(String id);
}

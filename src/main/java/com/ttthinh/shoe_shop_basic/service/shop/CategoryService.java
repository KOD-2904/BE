package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.CategoryRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.CategoryResponse;

import java.util.List;

public interface CategoryService {
    public CategoryResponse createCategory(CategoryRequest request);
//    public CategoryResponse getCategoryById(int id);
    public List<CategoryResponse> getAllCategories();
    public CategoryResponse update(String categoryId ,CategoryRequest request);

     CategoryResponse getCategory(String id);
}

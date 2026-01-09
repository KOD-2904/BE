package com.ttthinh.shoe_shop_basic.repository.shop;

import com.ttthinh.shoe_shop_basic.entity.Category;
import com.ttthinh.shoe_shop_basic.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {

}

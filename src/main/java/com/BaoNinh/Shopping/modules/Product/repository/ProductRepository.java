package com.BaoNinh.Shopping.modules.Product.repository;

import com.BaoNinh.Shopping.modules.Product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}

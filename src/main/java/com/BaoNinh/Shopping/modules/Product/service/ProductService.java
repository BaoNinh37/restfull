package com.BaoNinh.Shopping.modules.Product.service;

import com.BaoNinh.Shopping.modules.Product.dto.ProductRequest;
import com.BaoNinh.Shopping.modules.Product.dto.ProductResponse;
import com.BaoNinh.Shopping.modules.Product.entity.Product;
import com.BaoNinh.Shopping.modules.Product.exception.ProductNotFoundException;
import com.BaoNinh.Shopping.modules.Product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> getAllProducts() {
		return productRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductResponse getProductById(Long id) {
		return toResponse(findProduct(id));
	}

	public ProductResponse createProduct(ProductRequest request) {
		Product product = new Product(
				request.name(), request.description(), request.price(), stockOrZero(request.stock()));
		return toResponse(productRepository.save(product));
	}

	public ProductResponse updateProduct(Long id, ProductRequest request) {
		Product product = findProduct(id);
		product.updateDetails(
				request.name(), request.description(), request.price(), stockOrZero(request.stock()));
		return toResponse(productRepository.save(product));
	}

	public void deleteProduct(Long id) {
		productRepository.delete(findProduct(id));
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));
	}

	private ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getStock());
	}

	private Integer stockOrZero(Integer stock) {
		return stock == null ? 0 : stock;
	}
}

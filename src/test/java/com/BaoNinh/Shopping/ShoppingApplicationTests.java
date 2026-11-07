package com.BaoNinh.Shopping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.BaoNinh.Shopping.modules.Auth.repository.AppUserRepository;
import com.BaoNinh.Shopping.modules.Product.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
class ShoppingApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearData() {
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void productCrudWorks() throws Exception {
		String token = registerAndLoginAdmin("admin@example.com", "AdminPassword123");
		String product = """
				{"name":"Keyboard","description":"Mechanical keyboard","price":49.99,"stock":10}
				""";

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(product))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Keyboard"))
				.andExpect(jsonPath("$.description").value("Mechanical keyboard"));
		Long id = productRepository.findAll().get(0).getId();

		mockMvc.perform(get("/api/products/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(10));

		mockMvc.perform(put("/api/products/{id}", id)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Keyboard Pro","description":"Updated keyboard","price":79.99,"stock":5}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Keyboard Pro"))
				.andExpect(jsonPath("$.description").value("Updated keyboard"));
		mockMvc.perform(get("/api/products/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Updated keyboard"));

		mockMvc.perform(delete("/api/products/{id}", id)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/products/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsInvalidProduct() throws Exception {
		String token = registerAndLoginAdmin("admin@example.com", "AdminPassword123");
		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":" ","description":"Invalid","price":-1,"stock":-1}
								"""))
				.andExpect(status().isBadRequest());
		assertEquals(0, productRepository.count());
	}

	@Test
	void registrationHashesPasswordAndAssignsUserRole() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"Shopper@Example.com","password":"ShopperPassword123"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("shopper@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		var user = userRepository.findByEmail("shopper@example.com").orElseThrow();
		assertEquals("USER", user.getRole().name());
		org.junit.jupiter.api.Assertions.assertTrue(
				passwordEncoder.matches("ShopperPassword123", user.getPasswordHash()));
		org.junit.jupiter.api.Assertions.assertNotEquals("ShopperPassword123", user.getPasswordHash());
	}

	@Test
	void duplicateEmailIsRejected() throws Exception {
		String registration = """
				{"email":"shopper@example.com","password":"ShopperPassword123"}
				""";
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
				.andExpect(status().isConflict());
	}

	@Test
	void registrationRejectsPasswordsBeyondBcryptByteLimit() throws Exception {
		String password = "é".repeat(37);
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"shopper@example.com","password":"%s"}
								""".formatted(password)))
				.andExpect(status().isBadRequest());
		assertEquals(0, userRepository.count());
	}

	@Test
	void loginIssuesJwtAndUserCannotWriteProducts() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"shopper@example.com","password":"ShopperPassword123"}
								"""))
				.andExpect(status().isCreated());

		var login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"shopper@example.com","password":"ShopperPassword123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

		String product = """
				{"name":"Keyboard","description":"Mechanical keyboard","price":49.99,"stock":10}
				""";
		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(product))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(product))
				.andExpect(status().isUnauthorized());
		assertEquals(0, productRepository.count());
	}

	@Test
	void loginRejectsInvalidCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"shopper@example.com","password":"ShopperPassword123"}
								"""))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"shopper@example.com","password":"WrongPassword123"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private String registerAndLoginAdmin(String email, String password) throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isCreated());
		jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email.toLowerCase());
		var login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
	}
}

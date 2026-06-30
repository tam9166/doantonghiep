package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Product; // Nhớ import Product
import poly.edu.quanlynhahang.entity.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> { // Dùng Integer hay Long ở đây đều được
    
    // Đã đổi tên hàm: Tìm công thức dựa vào nguyên một Object Product
    List<Recipe> findByProduct(Product product);

    List<Recipe> findByIngredient(poly.edu.quanlynhahang.entity.Ingredient ingredient);
}
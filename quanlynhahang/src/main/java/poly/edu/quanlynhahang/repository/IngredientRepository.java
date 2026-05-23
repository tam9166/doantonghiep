package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
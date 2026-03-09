package com.vishnu.finance_tracker.repository;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vishnu.finance_tracker.dto.CategorySummaryDTO;
import com.vishnu.finance_tracker.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdAndDeletedFalse(Long userId);

    Page<Transaction> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    List<Transaction> findByUserIdAndDateBetweenAndDeletedFalse(
        Long userId,
        LocalDate start,
        LocalDate end
);

@Query("""
SELECT COALESCE(SUM(t.amount),0)
FROM Transaction t
WHERE t.user.id = :userId
AND t.type = 'EXPENSE'
AND t.deleted = false
AND t.date BETWEEN :start AND :end
""")
double sumExpensesByDateRange(
        @Param("userId") Long userId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
);

@Query("""
SELECT new com.vishnu.finance_tracker.dto.CategorySummaryDTO(
    t.category.name,
    SUM(t.amount)
)
FROM Transaction t
WHERE t.user.id = :userId
AND t.deleted = false
AND t.type = 'EXPENSE'
GROUP BY t.category.name
""")
List<CategorySummaryDTO> getCategorySummary(Long userId);

@Query("""
SELECT COALESCE(SUM(t.amount),0)
FROM Transaction t
WHERE t.user.id = :userId
AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :category, '%'))
AND t.type = 'EXPENSE'
AND t.date BETWEEN :start AND :end
AND t.deleted = false
""")
double sumExpenseByCategoryAndDateRange(
        @Param("userId") Long userId,
        @Param("category") String category,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
);

@Query("""
SELECT t.category.name
FROM Transaction t
WHERE t.user.id = :userId
AND t.type = 'EXPENSE'
AND t.deleted = false
GROUP BY t.category.name
ORDER BY SUM(t.amount) DESC
LIMIT 1
""")
String getHighestCategory(@Param("userId") Long userId);

List<Transaction> findByUserIdAndTypeAndDeletedFalseOrderByAmountAsc(
        Long userId,
        String type
);

List<Transaction> findByUserIdAndTypeAndDeletedFalseOrderByAmountDesc(
        Long userId,
        String type
);
List<Transaction> findByUserIdAndAmountGreaterThanAndDeletedFalse(Long userId, Double amount);

List<Transaction> findByUserIdAndAmountLessThanAndDeletedFalse(Long userId, Double amount);

List<Transaction> findByUserIdAndAmountAndDeletedFalse(Long userId, Double amount);

@Query("""
SELECT t.category.name
FROM Transaction t
WHERE t.user.id = :userId
AND t.type = 'EXPENSE'
AND t.deleted = false
GROUP BY t.category.name
ORDER BY SUM(t.amount) ASC
LIMIT 1
""")
String getLowestCategory(@Param("userId") Long userId);

Transaction findTopByUserIdAndTypeAndDeletedFalseAndDateBetweenOrderByAmountDesc(
        Long userId,
        String type,
        LocalDate start,
        LocalDate end
);

@Query("""
SELECT t.category.name, SUM(t.amount)
FROM Transaction t
WHERE t.user.id = :userId
AND t.type = 'EXPENSE'
AND t.deleted = false
GROUP BY t.category.name
ORDER BY SUM(t.amount) DESC
""")
List<Object[]> findTopCategories(Long userId);

@Query("""
SELECT t.category.name, SUM(t.amount)
FROM Transaction t
WHERE t.user.id = :userId
AND t.type = 'EXPENSE'
AND t.deleted = false
GROUP BY t.category.name
ORDER BY SUM(t.amount) ASC
""")
List<Object[]> findLowestCategories(Long userId);

List<Transaction> findByUserIdAndDateAndCategoryIdAndDeletedFalse(
        Long userId,
        LocalDate date,
        Long categoryId
);

@Query("""
SELECT COALESCE(SUM(t.amount),0)
FROM Transaction t
WHERE t.user.id = :userId
AND LOWER(t.category.name) = LOWER(:category)
AND t.type = 'EXPENSE'
AND t.date BETWEEN :start AND :end
""")
double sumCategoryExpensesByDateRange(
        Long userId,
        String category,
        LocalDate start,
        LocalDate end
);
}

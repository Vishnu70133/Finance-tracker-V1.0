package com.vishnu.finance_tracker;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdAndDeletedFalse(Long userId);

    Page<Transaction> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    List<Transaction> findByUserIdAndDateBetweenAndDeletedFalse(
        Long userId,
        LocalDate start,
        LocalDate end
);
@Query("""
SELECT new com.vishnu.finance_tracker.CategorySummaryDTO(
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


}

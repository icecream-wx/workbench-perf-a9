package com.example.workbenchperfa9.repository;

import com.example.workbenchperfa9.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmpNo(String empNo);

    boolean existsByEmpNo(String empNo);

    boolean existsByEmpNoIgnoreCase(String empNo);

    boolean existsByEmpNoIgnoreCaseAndIdNot(String empNo, Long id);

    @Query("""
            select e from Employee e
            where lower(e.empNo) like lower(concat('%', :keyword, '%'))
               or lower(e.name) like lower(concat('%', :keyword, '%'))
            """)
    Page<Employee> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

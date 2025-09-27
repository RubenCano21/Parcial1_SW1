package ${package}.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ${package}.entity.${table.className};

@Repository
public interface ${table.className}Repository extends JpaRepository<${table.className}, Long> {
}

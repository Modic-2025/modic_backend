package hanium.modic.backend.base;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.CaseFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;

@Service
public class DatabaseCleanUp implements InitializingBean {
	@PersistenceContext
	private EntityManager entityManager;

	private List<String> tableNames;

	@Override
	public void afterPropertiesSet() {
		tableNames = entityManager.getMetamodel().getEntities().stream()
			.map(type -> type.getJavaType())
			.filter(clazz -> clazz.getAnnotation(Entity.class) != null)
			.map(clazz -> {
				Table table = clazz.getAnnotation(Table.class);
				if (table != null && !table.name().isBlank()) {
					return table.name(); // 명시된 테이블 이름 사용
				}
				// 클래스명을 소문자 스네이크 케이스로 변환
				return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName());
			})
			.collect(Collectors.toList());
	}

	@Transactional
	public void execute() {
		entityManager.flush();

		for (String tableName : tableNames) {
			entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
			entityManager.createNativeQuery("ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1").executeUpdate();
		}
	}
}
package edu.lab.core.course.workspace;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.course.Course;
import edu.lab.core.course.CourseRepository;
import edu.lab.core.course.workspace.dto.CourseWorkspaceModuleItemRequest;
import edu.lab.core.course.workspace.dto.CourseWorkspaceModuleItemResponse;
import edu.lab.core.course.workspace.dto.CourseWorkspaceModulesResponse;
import edu.lab.core.course.workspace.dto.CourseWorkspaceModulesUpdateRequest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseWorkspaceService {

	private static final List<CourseWorkspaceModuleKey> DEFAULT_ORDER = List.of(
		CourseWorkspaceModuleKey.OVERVIEW,
		CourseWorkspaceModuleKey.LEARNING,
		CourseWorkspaceModuleKey.HOMEWORK,
		CourseWorkspaceModuleKey.GRADES,
		CourseWorkspaceModuleKey.ANNOUNCEMENT,
		CourseWorkspaceModuleKey.INTERACTION
	);

	private final CourseRepository courseRepository;
	private final CourseWorkspaceModuleRepository moduleRepository;

	@Transactional(readOnly = true)
	public CourseWorkspaceModulesResponse listModules(UUID courseId) {
		List<CourseWorkspaceModule> modules = ensureModules(courseId);
		return new CourseWorkspaceModulesResponse(courseId, modules.stream().map(this::toItem).toList());
	}

	@Transactional
	public CourseWorkspaceModulesResponse updateModules(UUID courseId, CourseWorkspaceModulesUpdateRequest request) {
		if (request == null || request.modules() == null || request.modules().isEmpty()) {
			throw new BadRequestException("模块配置不能为空");
		}

		List<CourseWorkspaceModule> existing = ensureModules(courseId);
		for (CourseWorkspaceModuleItemRequest item : request.modules()) {
			if (item == null || item.moduleKey() == null) {
				throw new BadRequestException("模块项缺少 moduleKey");
			}
			CourseWorkspaceModule module = existing.stream()
				.filter(it -> it.getModuleKey() == item.moduleKey())
				.findFirst()
				.orElseThrow(() -> new BadRequestException("未知模块: " + item.moduleKey()));
			module.setEnabled(item.enabled());
			module.setSortOrder(item.sortOrder() == null ? module.getSortOrder() : Math.max(item.sortOrder(), 0));
		}

		moduleRepository.saveAll(existing);
		List<CourseWorkspaceModule> sorted = moduleRepository.findByCourseIdOrderBySortOrderAsc(courseId);
		return new CourseWorkspaceModulesResponse(courseId, sorted.stream().map(this::toItem).toList());
	}

	@Transactional
	public void ensureDefaultWorkspaceModules(UUID courseId) {
		ensureModules(courseId);
	}

	private List<CourseWorkspaceModule> ensureModules(UUID courseId) {
		List<CourseWorkspaceModule> modules = moduleRepository.findByCourseIdOrderBySortOrderAsc(courseId);
		EnumSet<CourseWorkspaceModuleKey> existing = modules.stream()
			.map(CourseWorkspaceModule::getModuleKey)
			.collect(() -> EnumSet.noneOf(CourseWorkspaceModuleKey.class), EnumSet::add, EnumSet::addAll);

		if (existing.size() == CourseWorkspaceModuleKey.values().length) {
			return modules;
		}

		Course courseRef = courseRepository.findById(courseId)
			.orElseThrow(() -> new BadRequestException("课程不存在"));
		List<CourseWorkspaceModule> created = new ArrayList<>();
		int sort = 10;
		for (CourseWorkspaceModuleKey key : DEFAULT_ORDER) {
			if (existing.contains(key)) {
				sort += 10;
				continue;
			}
			CourseWorkspaceModule module = new CourseWorkspaceModule();
			module.setCourse(courseRef);
			module.setModuleKey(key);
			module.setEnabled(true);
			module.setSortOrder(sort);
			created.add(module);
			sort += 10;
		}

		if (!created.isEmpty()) {
			moduleRepository.saveAll(created);
		}
		return moduleRepository.findByCourseIdOrderBySortOrderAsc(courseId);
	}

	private CourseWorkspaceModuleItemResponse toItem(CourseWorkspaceModule module) {
		return new CourseWorkspaceModuleItemResponse(module.getModuleKey(), module.isEnabled(), module.getSortOrder());
	}
}
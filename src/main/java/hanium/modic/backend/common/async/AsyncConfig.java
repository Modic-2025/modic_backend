package hanium.modic.backend.common.async;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	@Bean(name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
	public ThreadPoolTaskExecutor asyncTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(2);
		executor.setThreadNamePrefix("async");
		executor.setQueueCapacity(1000000);
		executor.setAwaitTerminationSeconds(5); // shutdown시 5초 대기
		executor.setWaitForTasksToCompleteOnShutdown(true); // shutdown 시 남은 작업 대기
		executor.setTaskDecorator(new MdcTaskDecorator());
		executor.initialize();
		executor.getThreadPoolExecutor().prestartAllCoreThreads();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return asyncTaskExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new AsyncExceptionHandler();
	}

	// 비동기 예외 핸들러
	@Slf4j
	private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

		@Override
		public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
			log.error("Error on async execution: ", throwable);
		}
	}

	private static class MdcTaskDecorator implements TaskDecorator {

		@Override
		public Runnable decorate(Runnable runnable) {
			Map<String, String> contextMap = MDC.getCopyOfContextMap();
			return () -> {
				try {
					if (contextMap != null) {
						MDC.setContextMap(contextMap);
					}
					runnable.run();
				} finally {
					MDC.clear();
				}
			};
		}
	}
}
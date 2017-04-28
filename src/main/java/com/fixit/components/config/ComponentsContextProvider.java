/**
 * 
 */
package com.fixit.components.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.fixit.components.search.SearchExecutor;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/25 22:50:38 GMT+3
 */
@Component
public class ComponentsContextProvider implements ApplicationContextAware {

	private static ApplicationContext context;

	public static ApplicationContext getApplicationContext() {
		return context;
	}

	public static Object getBean(String beanName) {
		return context.getBean(beanName);
	}

	public static <T> T getBean(Class<T> requiredType) {
		return context.getBean(requiredType);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}
	

	
	public static SearchExecutor getSearchExecutor() {
		return context.getBean(SearchExecutor.class);
	}

}

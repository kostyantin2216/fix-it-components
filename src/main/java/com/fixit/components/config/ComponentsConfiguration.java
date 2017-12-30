/**
 * 
 */
package com.fixit.components.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.google.maps.GeoApiContext;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/25 22:49:04 GMT+3
 */
@Configuration
@ComponentScan(basePackages = {"com.fixit.components"})
public class ComponentsConfiguration {
	
	@Bean
	public GeoApiContext geoApiContext() {
		return new GeoApiContext.Builder()
			    .apiKey("AIzaSyCrauSZvT0g65hfwChz0Jr-p6xLcLgBE9g")
			    .build();
	}

}

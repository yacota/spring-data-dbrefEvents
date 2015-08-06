/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package proposal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.io.IOException;
import org.springframework.data.mongodb.core.convert.CustomMappingMongoConverter;

/**
 *
 * @author jllach
 */
@Configuration
public class MongoDbConfiguration {
    

	@Autowired
	private MongoDbFactory dbFactory;
	@Autowired
	private ApplicationContext appContext;


	@Bean(name = "mongoTemplate")
	public MongoTemplate        mongoTemplate() throws IOException {
		return new MongoTemplate(dbFactory, mongoMappingConverter());
	}
    
    /**
     * Custom mongo mapping converter that fires afterload and after convert events when dealing with DBRef
	 * @return
	 * @throws IOException 
	 */
	@Bean
	public MappingMongoConverter mongoMappingConverter() throws IOException {
        MappingMongoConverter converter = new CustomMappingMongoConverter(new DefaultDbRefResolver(dbFactory), mongoMappingContext());
		return converter;
	}
	
	@Bean
	public MongoMappingContext  mongoMappingContext() throws IOException {
		MongoMappingContext mongoContext = new MongoMappingContext();
		mongoContext.setApplicationContext(appContext);
		return mongoContext;
	}

}

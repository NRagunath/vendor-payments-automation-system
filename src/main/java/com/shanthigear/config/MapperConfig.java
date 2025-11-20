package com.shanthigear.config;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NamingConventions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for ModelMapper.
 * Provides a pre-configured ModelMapper bean for DTO-entity mapping.
 */
@Configuration
public class MapperConfig {

    /**
     * Creates and configures a ModelMapper instance.
     * 
     * @return Configured ModelMapper instance
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        // Configure the mapper with strict matching and proper naming conventions
        modelMapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT)
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
            .setSourceNamingConvention(NamingConventions.JAVABEANS_ACCESSOR)
            .setDestinationNamingConvention(NamingConventions.JAVABEANS_MUTATOR)
            .setSkipNullEnabled(true)
            .setPropertyCondition(Conditions.isNotNull())
            .setAmbiguityIgnored(true);
            
        return modelMapper;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.springframework.data.mongodb.core.convert;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.DefaultSpELExpressionEvaluator;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.mapping.model.SpELContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.mapping.model.SpELExpressionParameterValueProvider;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * The only relevant part here is the method potentiallyReadOrResolveDbRef , readCollectionOrArray and the new readAndConvertDBRef. The rest is pure copy&paster
 */
public class CustomMappingMongoConverter 
extends      MappingMongoConverter {
    
    private SpELContext spELContext;

    public CustomMappingMongoConverter(DbRefResolver dbRefResolver, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(dbRefResolver, mappingContext);
        this.spELContext = new SpELContext(DBObjectPropertyAccessor.INSTANCE);
    }

    public CustomMappingMongoConverter(MongoDbFactory mongoDbFactory, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(mongoDbFactory, mappingContext);
        this.spELContext = new SpELContext(DBObjectPropertyAccessor.INSTANCE);
    }
    
    //
    // THE PROPOSED CHANGE IS JUST in the following 3 methods ... the rest is pure c&p 
    //
    
    
    //
    // Dealing with single DBRef
    //
    @SuppressWarnings("unchecked")
	private <T> T potentiallyReadOrResolveDbRef(DBRef dbref, TypeInformation<?> type, ObjectPath path, Class<?> rawType) {
		if (rawType.equals(DBRef.class)) {
			return (T) dbref;
		}
		Object object = dbref == null ? null : path.getPathItem(dbref.getId(), dbref.getCollectionName());
        //CHANGE STARTS
		if (object != null) {
			return (T) object;
		} else {
            return readAndConvertDBRef(dbref, type, path, rawType);
        }
        //CHANGE ENDS
	}
    
    //
    // Dealing with a Collection of DBRef
    //
    private Object readCollectionOrArray(TypeInformation<?> targetType, BasicDBList sourceValue, ObjectPath path) {

		Assert.notNull(targetType, "Target type must not be null!");
		Assert.notNull(path, "Object path must not be null!");

		Class<?> collectionType = targetType.getType();

		if (sourceValue.isEmpty()) {
			return getPotentiallyConvertedSimpleRead(new HashSet<Object>(), collectionType);
		}

		TypeInformation<?> componentType = targetType.getComponentType();
		Class<?> rawComponentType = componentType == null ? null : componentType.getType();

		collectionType = Collection.class.isAssignableFrom(collectionType) ? collectionType : List.class;
		Collection<Object> items = targetType.getType().isArray() ? new ArrayList<Object>() : CollectionFactory.createCollection(collectionType, rawComponentType, sourceValue.size());

		for (int i = 0; i < sourceValue.size(); i++) {

			Object dbObjItem = sourceValue.get(i);

			if (dbObjItem instanceof DBRef) {
                //CHANGE STARTS
                if (DBRef.class.equals(rawComponentType)) {
                    items.add(dbObjItem);
                } else {
                    items.add(readAndConvertDBRef((DBRef)dbObjItem, componentType, path, rawComponentType));
                }
                // CHANGE ENDS
			} else if (dbObjItem instanceof DBObject) {
				items.add(read(componentType, (DBObject) dbObjItem, path));
			} else {
				items.add(getPotentiallyConvertedSimpleRead(dbObjItem, rawComponentType));
			}
		}

		return getPotentiallyConvertedSimpleRead(items, targetType.getType());
	}
    
    //
    // NEW METHOD
    //
    private <T> T readAndConvertDBRef(DBRef dbref, TypeInformation<?> type, ObjectPath path, Class<?> rawType) {
        DBObject readRef = readRef(dbref);
        // after load event
        ((ApplicationEventPublisher)this.applicationContext).publishEvent(new AfterLoadEvent<T>(readRef, (Class<T>)rawType));
        T t = (T) read(type, readRef, path);
        // after convert event
        ((ApplicationEventPublisher)this.applicationContext).publishEvent(new AfterConvertEvent<T>(readRef,t));
        return t;
    }
    
    
    //
    // Inner classes not changed from the ones spring-data-mongodb uses
    //
        private class MongoDbPropertyValueProvider implements PropertyValueProvider<MongoPersistentProperty> {

            private final DBObjectAccessor source;
            private final SpELExpressionEvaluator evaluator;
            private final ObjectPath path;

            
            public MongoDbPropertyValueProvider(DBObject source, SpELExpressionEvaluator evaluator, ObjectPath path) {
                Assert.notNull(source);
                Assert.notNull(evaluator);
                this.source = new DBObjectAccessor(source);
                this.evaluator = evaluator;
                this.path = path;
            }

            public <T> T getPropertyValue(MongoPersistentProperty property) {

                String expression = property.getSpelExpression();
                Object value = expression != null ? evaluator.evaluate(expression) : source.get(property);

                if (value == null) {
                    return null;
                }

                return readValue(value, property.getTypeInformation(), path);
            }
        }
        
        private class ConverterAwareSpELExpressionParameterValueProvider 
        extends       SpELExpressionParameterValueProvider<MongoPersistentProperty> {

            private final ObjectPath path;

            public ConverterAwareSpELExpressionParameterValueProvider(SpELExpressionEvaluator evaluator,
                    ConversionService conversionService, ParameterValueProvider<MongoPersistentProperty> delegate, ObjectPath path) {
                super(evaluator, conversionService, delegate);
                this.path = path;
            }

            @Override
            protected <T> T potentiallyConvertSpelValue(Object object, PreferredConstructor.Parameter<T, MongoPersistentProperty> parameter) {
                return readValue(object, parameter.getType(), path);
            }
        }
    
    
    //
    // Methods copied from parent class, refering to this custom class instead of parentOne ... but is pure c&p
    //
        
    public Object getValueInternal(MongoPersistentProperty prop, DBObject dbo, SpELExpressionEvaluator evaluator, ObjectPath path) {
        return new CustomMappingMongoConverter.MongoDbPropertyValueProvider(dbo, evaluator, path).getPropertyValue(prop);
	}
    
    @SuppressWarnings("unchecked")
	private <T> T readValue(Object value, TypeInformation<?> type, ObjectPath path) {

		Class<?> rawType = type.getType();

		if (conversions.hasCustomReadTarget(value.getClass(), rawType)) {
			return (T) conversionService.convert(value, rawType);
		} else if (value instanceof DBRef) {
			return potentiallyReadOrResolveDbRef((DBRef) value, type, path, rawType);
		} else if (value instanceof BasicDBList) {
			return (T) readCollectionOrArray(type, (BasicDBList) value, path);
		} else if (value instanceof DBObject) {
			return (T) read(type, (DBObject) value, path);
		} else {
			return (T) getPotentiallyConvertedSimpleRead(value, rawType);
		}
	}
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getPotentiallyConvertedSimpleRead(Object value, Class<?> target) {

		if (value == null || target == null || target.isAssignableFrom(value.getClass())) {
			return value;
		}

		if (conversions.hasCustomReadTarget(value.getClass(), target)) {
			return conversionService.convert(value, target);
		}

		if (Enum.class.isAssignableFrom(target)) {
			return Enum.valueOf((Class<Enum>) target, value.toString());
		}

		return conversionService.convert(value, target);
	}
    
    private static final String INCOMPATIBLE_TYPES = "Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions. Parent object was: %4$s";
    @SuppressWarnings("unchecked")
	private <S extends Object> S read(TypeInformation<S> type, DBObject dbo, ObjectPath path) {

		if (null == dbo) {
			return null;
		}

		TypeInformation<? extends S> typeToUse = typeMapper.readType(dbo, type);
		Class<? extends S> rawType = typeToUse.getType();

		if (conversions.hasCustomReadTarget(dbo.getClass(), rawType)) {
			return conversionService.convert(dbo, rawType);
		}

		if (DBObject.class.isAssignableFrom(rawType)) {
			return (S) dbo;
		}

		if (typeToUse.isCollectionLike() && dbo instanceof BasicDBList) {
			return (S) readCollectionOrArray(typeToUse, (BasicDBList) dbo, path);
		}

		if (typeToUse.isMap()) {
			return (S) readMap(typeToUse, dbo, path);
		}

		if (dbo instanceof BasicDBList) {
			throw new MappingException(String.format(INCOMPATIBLE_TYPES, dbo, BasicDBList.class, typeToUse.getType(), path));
		}

		// Retrieve persistent entity info
		MongoPersistentEntity<S> persistentEntity = (MongoPersistentEntity<S>) mappingContext
				.getPersistentEntity(typeToUse);
		if (persistentEntity == null) {
			throw new MappingException("No mapping metadata found for " + rawType.getName());
		}

		return read(persistentEntity, dbo, path);
	}
    
    private <S extends Object> S read(final MongoPersistentEntity<S> entity, final DBObject dbo, final ObjectPath path) {

		final DefaultSpELExpressionEvaluator evaluator = new DefaultSpELExpressionEvaluator(dbo, spELContext);

		ParameterValueProvider<MongoPersistentProperty> provider = getParameterProvider(entity, dbo, evaluator, path);
		EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
		S instance = instantiator.createInstance(entity, provider);

		final PersistentPropertyAccessor accessor = new ConvertingPropertyAccessor(entity.getPropertyAccessor(instance),
				conversionService);

		final MongoPersistentProperty idProperty = entity.getIdProperty();
		final S result = instance;

		// make sure id property is set before all other properties
		Object idValue = null;

		if (idProperty != null) {
			idValue = getValueInternal(idProperty, dbo, evaluator, path);
			accessor.setProperty(idProperty, idValue);
		}

		final ObjectPath currentPath = path.push(result, entity, idValue);

		// Set properties not already set in the constructor
		entity.doWithProperties(new PropertyHandler<MongoPersistentProperty>() {
			public void doWithPersistentProperty(MongoPersistentProperty prop) {

				// we skip the id property since it was already set
				if (idProperty != null && idProperty.equals(prop)) {
					return;
				}

				if (!dbo.containsField(prop.getFieldName()) || entity.isConstructorArgument(prop)) {
					return;
				}

				accessor.setProperty(prop, getValueInternal(prop, dbo, evaluator, currentPath));
			}
		});

		// Handle associations
		entity.doWithAssociations(new AssociationHandler<MongoPersistentProperty>() {
			public void doWithAssociation(Association<MongoPersistentProperty> association) {

				final MongoPersistentProperty property = association.getInverse();
				Object value = dbo.get(property.getFieldName());

				if (value == null) {
					return;
				}

				DBRef dbref = value instanceof DBRef ? (DBRef) value : null;

				DbRefProxyHandler handler = new DefaultDbRefProxyHandler(spELContext, mappingContext, CustomMappingMongoConverter.this);
				DbRefResolverCallback callback = new DefaultDbRefResolverCallback(dbo, currentPath, evaluator,CustomMappingMongoConverter.this);

				accessor.setProperty(property, dbRefResolver.resolveDbRef(property, dbref, callback, handler));
			}
		});

		return result;
	}
    
    private ParameterValueProvider<MongoPersistentProperty> getParameterProvider(MongoPersistentEntity<?> entity,
			DBObject source, DefaultSpELExpressionEvaluator evaluator, ObjectPath path) {

		MongoDbPropertyValueProvider provider = new MongoDbPropertyValueProvider(source, evaluator, path);
		PersistentEntityParameterValueProvider<MongoPersistentProperty> parameterProvider = new PersistentEntityParameterValueProvider<MongoPersistentProperty>(
				entity, provider, path.getCurrentObject());

		return new CustomMappingMongoConverter.ConverterAwareSpELExpressionParameterValueProvider(evaluator, conversionService, parameterProvider, path);
	}
}

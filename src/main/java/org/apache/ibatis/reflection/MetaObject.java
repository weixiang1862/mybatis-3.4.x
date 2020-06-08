/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 类似于MetaClass，不过MetaObject提供了对象级别的信息
 *
 * @author Clinton Begin
 */
public class MetaObject {

    private final Object originalObject;
    private final ObjectWrapper objectWrapper;
    private final ObjectFactory objectFactory;
    private final ObjectWrapperFactory objectWrapperFactory;
    private final ReflectorFactory reflectorFactory;

    /**
     * 初始化originalObject、objectWrapper以及各种factory
     *
     * @param object
     * @param objectFactory
     * @param objectWrapperFactory
     * @param reflectorFactory
     */
    private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
        this.originalObject = object;
        this.objectFactory = objectFactory;
        this.objectWrapperFactory = objectWrapperFactory;
        this.reflectorFactory = reflectorFactory;

        if (object instanceof ObjectWrapper) {
            this.objectWrapper = (ObjectWrapper) object;
        } else if (objectWrapperFactory.hasWrapperFor(object)) {
            this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
        } else if (object instanceof Map) {
            this.objectWrapper = new MapWrapper(this, (Map) object);
        } else if (object instanceof Collection) {
            this.objectWrapper = new CollectionWrapper(this, (Collection) object);
        } else {
            this.objectWrapper = new BeanWrapper(this, object);
        }
    }

    /**
     * 创建MetaObject，如果object为空，包装成SystemMetaObject.NULL_META_OBJECT
     *
     * @param object
     * @param objectFactory
     * @param objectWrapperFactory
     * @param reflectorFactory
     * @return
     */
    public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
        if (object == null) {
            return SystemMetaObject.NULL_META_OBJECT;
        } else {
            return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
        }
    }

    /************************field get方法*************************/
    public Object getOriginalObject() {
        return originalObject;
    }

    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public ObjectWrapperFactory getObjectWrapperFactory() {
        return objectWrapperFactory;
    }

    public ReflectorFactory getReflectorFactory() {
        return reflectorFactory;
    }


    /*******************见objectWrapper具体实现********************/
    public String findProperty(String propName, boolean useCamelCaseMapping) {
        return objectWrapper.findProperty(propName, useCamelCaseMapping);
    }

    public String[] getGetterNames() {
        return objectWrapper.getGetterNames();
    }

    public String[] getSetterNames() {
        return objectWrapper.getSetterNames();
    }

    public Class<?> getSetterType(String name) {
        return objectWrapper.getSetterType(name);
    }

    public Class<?> getGetterType(String name) {
        return objectWrapper.getGetterType(name);
    }

    public boolean hasSetter(String name) {
        return objectWrapper.hasSetter(name);
    }

    public boolean hasGetter(String name) {
        return objectWrapper.hasGetter(name);
    }


    /**
     * 获取name对应的值
     *
     * @param name
     * @return
     */
    public Object getValue(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            // 有children，先解析出当前层级的MetaObject，再在当前层级上继续get children value
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                return null;
            } else {
                return metaValue.getValue(prop.getChildren());
            }
        } else {
            // 不存在children，直接调用ObjectWrapper的get
            return objectWrapper.get(prop);
        }
    }

    /**
     * 设置property的值为value
     *
     * Map<User></> 007 --> JB ([007].name)
     * @param name
     * @param value
     */
    public void setValue(String name, Object value) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            // 如果metaValue为null，但是value不是空，或者prop还有children（即使当前metaValue为null，也要实例化一个出来），则调用setValue
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                if (value == null && prop.getChildren() != null) {
                    // don't instantiate child path if value is null
                    return;
                } else {
                    // 初始化当前prop对应的对象
                    metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
                }
            }
            // 到这一步，metaValue肯定不为空
            metaValue.setValue(prop.getChildren(), value);
        } else {
            objectWrapper.set(prop, value);
        }
    }

    /**
     * 获取name（foo，bar，list[i]）对应的property
     *
     * @param name
     * @return
     */
    public MetaObject metaObjectForProperty(String name) {
        Object value = getValue(name);
        return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
    }

    /**
     * 是否是集合，根据objectWrapper来判断
     *
     * @return
     */
    public boolean isCollection() {
        return objectWrapper.isCollection();
    }

    /**
     * 集合add
     *
     * @param element
     */
    public void add(Object element) {
        objectWrapper.add(element);
    }

    /**
     * 集合addAll
     *
     * @param list
     * @param <E>
     */
    public <E> void addAll(List<E> list) {
        objectWrapper.addAll(list);
    }

}

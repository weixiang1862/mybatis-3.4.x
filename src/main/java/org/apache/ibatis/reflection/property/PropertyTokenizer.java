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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 用来解析类似于users[1].name、user.name这种字符串，以
 * 迭代器模式实现了Iterator，next用来寻找.后面的属性
 *
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
    // 属性名称
    private String name;
    // 属性名称加上index，如users[1]
    private final String indexedName;
    // 索引，如users[1]中的1
    private String index;
    // 下一个属性元素，如users[1].name中的name
    private final String children;

    // 根据fullName
    public PropertyTokenizer(String fullname) {
        int delim = fullname.indexOf('.');
        // children为逗号后的，indexedName为逗号前的
        if (delim > -1) {
            name = fullname.substring(0, delim);
            children = fullname.substring(delim + 1);
        } else {
            name = fullname;
            children = null;
        }
        indexedName = name;
        // 如果存在[]，需要截取出index
        delim = name.indexOf('[');
        if (delim > -1) {
            index = name.substring(delim + 1, name.length() - 1);
            name = name.substring(0, delim);
        }
    }

    public String getName() {
        return name;
    }

    public String getIndex() {
        return index;
    }

    public String getIndexedName() {
        return indexedName;
    }

    public String getChildren() {
        return children;
    }

    /**
     * children不为空则为true
     *
     * @return
     */
    @Override
    public boolean hasNext() {
        return children != null;
    }

    /**
     * 使用children构建下一个PropertyTokenizer
     *
     * @return
     */
    @Override
    public PropertyTokenizer next() {
        return new PropertyTokenizer(children);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
    }
}

package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * 类型参数解析器，用来解析类字段、方法返回值、方法参数的实际类型（这些类型可能是泛型类型，需要返回泛型类型的实际类型）
 *
 * @author Iwao AVE!
 */
public class TypeParameterResolver2 {

    /**
     * Resolve field type.
     * <p>
     * 解析field类型
     *
     * @param field   the field
     * @param srcType the src type
     * @return The field type as {@link Type}. If it has type parameters in the declaration,<br>
     * they will be resolved to the actual runtime {@link Type}s.
     */
    public static Type resolveFieldType(Field field, Type srcType) {
        Type fieldType = field.getGenericType();
        Class<?> declaringClass = field.getDeclaringClass();
        return resolveType(fieldType, srcType, declaringClass);
    }

    /**
     * Resolve return type.
     * <p>
     * 解析返回值类型
     *
     * @param method  the method
     * @param srcType the src type
     * @return The return type of the method as {@link Type}. If it has type parameters in the declaration,<br>
     * they will be resolved to the actual runtime {@link Type}s.
     */
    public static Type resolveReturnType(Method method, Type srcType) {
        Type returnType = method.getGenericReturnType();
        Class<?> declaringClass = method.getDeclaringClass();
        return resolveType(returnType, srcType, declaringClass);
    }

    /**
     * Resolve param types.
     * <p>
     * 解析参数类型
     *
     * @param method  the method
     * @param srcType the src type
     * @return The parameter types of the method as an array of {@link Type}s. If they have type parameters in the
     * declaration,<br>
     * they will be resolved to the actual runtime {@link Type}s.
     */
    public static Type[] resolveParamTypes(Method method, Type srcType) {
        Type[] paramTypes = method.getGenericParameterTypes();
        Class<?> declaringClass = method.getDeclaringClass();
        Type[] result = new Type[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            result[i] = resolveType(paramTypes[i], srcType, declaringClass);
        }
        return result;
    }


    /**
     * 上面的方法实际调用resolveType
     *
     * @param type
     * @param srcType
     * @param declaringClass
     * @return
     */
    private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
        if (type instanceof TypeVariable) {
            // 处理TypeVariable
            return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
        } else if (type instanceof ParameterizedType) {
            // 处理ParameterizedType
            return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
        } else if (type instanceof GenericArrayType) {
            // 处理GenericArrayType
            return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
        } else {
            // class，直接返回
            return type;
        }
    }

    /**
     * 解析GenericArrayType
     *
     * @param genericArrayType
     * @param srcType
     * @param declaringClass
     * @return
     */
    private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
        Type componentType = genericArrayType.getGenericComponentType();
        Type resolvedComponentType = null;
        if (componentType instanceof TypeVariable) {
            resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
        } else if (componentType instanceof GenericArrayType) {
            resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
        } else if (componentType instanceof ParameterizedType) {
            resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
        }
        // class数组
        if (resolvedComponentType instanceof Class) {
            return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
        } else {
            return new GenericArrayTypeImpl(resolvedComponentType);
        }
    }

    /**
     * 解析ParameterizedType
     *
     * @param parameterizedType
     * @param srcType
     * @param declaringClass
     * @return
     */
    private static ParameterizedType resolveParameterizedType(ParameterizedType parameterizedType, Type srcType, Class<?> declaringClass) {
        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        Type[] args = new Type[typeArgs.length];
        for (int i = 0; i < typeArgs.length; i++) {
            if (typeArgs[i] instanceof TypeVariable) {
                // ParameterizedType的具体类型可能对应TypeVariable
                args[i] = resolveTypeVar((TypeVariable<?>) typeArgs[i], srcType, declaringClass);
            } else if (typeArgs[i] instanceof ParameterizedType) {
                // ParameterizedType的具体类型可能还是ParameterizedType
                args[i] = resolveParameterizedType((ParameterizedType) typeArgs[i], srcType, declaringClass);
            } else if (typeArgs[i] instanceof WildcardType) {
                // ParameterizedType的具体类型可能对应WildcardType
                args[i] = resolveWildcardType((WildcardType) typeArgs[i], srcType, declaringClass);
            } else {
                // class
                args[i] = typeArgs[i];
            }
        }
        return new ParameterizedTypeImpl(rawType, null, args);
    }

    /**
     * 解析WildcardType类型
     *
     * @param wildcardType
     * @param srcType
     * @param declaringClass
     * @return
     */
    private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
        Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
        Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
        return new WildcardTypeImpl(lowerBounds, upperBounds);
    }

    /**
     * 解析WildcardType的上下限
     *
     * @param bounds
     * @param srcType
     * @param declaringClass
     * @return
     */
    private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
        Type[] result = new Type[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i] instanceof TypeVariable) {
                result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
            } else if (bounds[i] instanceof ParameterizedType) {
                result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
            } else if (bounds[i] instanceof WildcardType) {
                result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
            } else {
                result[i] = bounds[i];
            }
        }
        return result;
    }

    /**
     * 解析TypeVariable
     *
     * @param typeVar
     * @param srcType
     * @param declaringClass
     * @return
     */
    private static Type resolveTypeVar(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass) {
        Type result;
        // 获取srcType对应的class
        Class<?> clazz;
        if (srcType instanceof Class) {
            clazz = (Class<?>) srcType;
        } else if (srcType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) srcType;
            clazz = (Class<?>) parameterizedType.getRawType();
        } else {
            throw new IllegalArgumentException("The 2nd arg must be Class or ParameterizedType, but was: " + srcType.getClass());
        }

        // todo 感觉有点问题，clazz和declaringClass相同时不应该直接拿typeVar的bounds，应该解析出typeVar在declaringClass上的位置，然后尝试从srcType对应的位置上去拿实际类型
        if (clazz == declaringClass) {
            Type[] bounds = typeVar.getBounds();
            if (bounds.length > 0) {
                return bounds[0];
            }
            return Object.class;
        }

        // 尝试通过父类解析实际类型
        Type superclass = clazz.getGenericSuperclass();
        result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
        if (result != null) {
            return result;
        }

        // 尝试通过父接口解析实际类型
        Type[] superInterfaces = clazz.getGenericInterfaces();
        for (Type superInterface : superInterfaces) {
            result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superInterface);
            if (result != null) {
                return result;
            }
        }
        // 缺省返回Object（应该永远都走不到这一步？）
        return Object.class;
    }

    /**
     * 解析出typeVar的实际类型
     *
     * @param typeVar        类型参数
     * @param srcType        实际声明的type
     * @param declaringClass 该typeVar声明的class
     * @param clazz          srcType对应的class
     * @param superclass     srcType的父type
     * @return
     */
    private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz, Type superclass) {
        if (superclass instanceof ParameterizedType) {
            // super ParameterizedType
            ParameterizedType parentAsType = (ParameterizedType) superclass;
            // super class
            Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();
            // super的TypeVars
            TypeVariable<?>[] parentTypeVars = parentAsClass.getTypeParameters();

            // 如果srcType是ParameterizedType，需要将srcType上的类型传递给parentAsType
            if (srcType instanceof ParameterizedType) {
                parentAsType = translateParentTypeVars((ParameterizedType) srcType, clazz, parentAsType);
            }
            // 找到typeVar对应parentAsClass上类型参数的位置，该位置的实际type就是我们要找的Type
            if (declaringClass == parentAsClass) {
                for (int i = 0; i < parentTypeVars.length; i++) {
                    if (typeVar.equals(parentTypeVars[i])) {
                        return parentAsType.getActualTypeArguments()[i];
                    }
                }
            }
            // 使用parentAsClass作为srcType，继续向上追溯
            if (declaringClass.isAssignableFrom(parentAsClass)) {
                return resolveTypeVar(typeVar, parentAsType, declaringClass);
            }
        } else if (superclass instanceof Class && declaringClass.isAssignableFrom((Class<?>) superclass)) {
            // 使用superclass作为srcType，继续向上追溯
            return resolveTypeVar(typeVar, superclass, declaringClass);
        }
        // 如果走到这里，说明superclass不是ParameterizedType并且declaringClass也不是superclass的祖先类（换句话说，superClass就是declaringClass，且不是ParameterizedType，那么这个typeVar无法推导出来，只能是Object）
        return null;
    }

    /**
     * 将srcType上的参数类型向上传递
     *
     * @param srcType
     * @param srcClass
     * @param parentType
     * @return
     */
    private static ParameterizedType translateParentTypeVars(ParameterizedType srcType, Class<?> srcClass, ParameterizedType parentType) {
        // parent actual typeVars
        Type[] parentTypeArgs = parentType.getActualTypeArguments();

        // src actual typeVars
        Type[] srcTypeArgs = srcType.getActualTypeArguments();
        // src typeParameter
        TypeVariable<?>[] srcTypeVars = srcClass.getTypeParameters();

        Type[] newParentArgs = new Type[parentTypeArgs.length];
        boolean noChange = true;
        // 遍历parent actual typeVars，如果为TypeVariable，并且和src typeParameter一样，那么把src actual typeVars对应位置的类型传递给父类
        for (int i = 0; i < parentTypeArgs.length; i++) {
            if (parentTypeArgs[i] instanceof TypeVariable) {
                for (int j = 0; j < srcTypeVars.length; j++) {
                    if (srcTypeVars[j].equals(parentTypeArgs[i])) {
                        noChange = false;
                        newParentArgs[i] = srcTypeArgs[j];
                    }
                }
            } else {
                newParentArgs[i] = parentTypeArgs[i];
            }
        }
        // 无变化还是返回原parentType，有变化则构建新的ParameterizedType
        return noChange ? parentType : new ParameterizedTypeImpl((Class<?>) parentType.getRawType(), null, newParentArgs);
    }

    private TypeParameterResolver2() {
        super();
    }

    static class ParameterizedTypeImpl implements ParameterizedType {
        private Class<?> rawType;

        private Type ownerType;

        private Type[] actualTypeArguments;

        public ParameterizedTypeImpl(Class<?> rawType, Type ownerType, Type[] actualTypeArguments) {
            super();
            this.rawType = rawType;
            this.ownerType = ownerType;
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public String toString() {
            return "ParameterizedTypeImpl [rawType=" + rawType + ", ownerType=" + ownerType + ", actualTypeArguments=" + Arrays.toString(actualTypeArguments) + "]";
        }
    }

    static class WildcardTypeImpl implements WildcardType {
        private Type[] lowerBounds;

        private Type[] upperBounds;

        WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
            super();
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds;
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds;
        }
    }

    static class GenericArrayTypeImpl implements GenericArrayType {
        private Type genericComponentType;

        GenericArrayTypeImpl(Type genericComponentType) {
            super();
            this.genericComponentType = genericComponentType;
        }

        @Override
        public Type getGenericComponentType() {
            return genericComponentType;
        }
    }
}